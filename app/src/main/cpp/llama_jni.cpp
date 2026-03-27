#include <jni.h>
#include <android/log.h>

#include <algorithm>
#include <cmath>
#include <mutex>
#include <string>
#include <vector>

#include "llama.h"
#include "ggml-backend.h"

namespace {

constexpr const char * TAG = "simpleagent_llama";
constexpr uint32_t MAX_CONTEXT_SIZE = 2048;
constexpr uint32_t MIN_CONTEXT_SIZE = 256;
constexpr uint32_t CONTEXT_GRANULARITY = 128;

std::mutex g_mutex;
llama_model * g_chat_model = nullptr;
const llama_vocab * g_chat_vocab = nullptr;
llama_model * g_embedding_model = nullptr;
const llama_vocab * g_embedding_vocab = nullptr;
bool g_backend_initialized = false;

void log_error(const std::string & message) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "%s", message.c_str());
}

void llama_log_callback(enum ggml_log_level level, const char * text, void * /* user_data */) {
    if (text == nullptr) {
        return;
    }

    int android_level = ANDROID_LOG_INFO;
    switch (level) {
        case GGML_LOG_LEVEL_ERROR:
            android_level = ANDROID_LOG_ERROR;
            break;
        case GGML_LOG_LEVEL_WARN:
            android_level = ANDROID_LOG_WARN;
            break;
        case GGML_LOG_LEVEL_DEBUG:
            android_level = ANDROID_LOG_DEBUG;
            break;
        case GGML_LOG_LEVEL_INFO:
        case GGML_LOG_LEVEL_CONT:
        default:
            android_level = ANDROID_LOG_INFO;
            break;
    }

    __android_log_print(android_level, TAG, "%s", text);
}

std::string jstring_to_string(JNIEnv * env, jstring value) {
    if (value == nullptr) return "";
    const char * chars = env->GetStringUTFChars(value, nullptr);
    std::string result = chars != nullptr ? chars : "";
    if (chars != nullptr) {
        env->ReleaseStringUTFChars(value, chars);
    }
    return result;
}

std::string token_to_piece(const llama_vocab * vocab, llama_token token) {
    std::vector<char> buffer(256);
    int size = llama_token_to_piece(vocab, token, buffer.data(), static_cast<int32_t>(buffer.size()), 0, true);
    if (size < 0) {
        buffer.resize(static_cast<size_t>(-size));
        size = llama_token_to_piece(vocab, token, buffer.data(), static_cast<int32_t>(buffer.size()), 0, true);
    }
    if (size < 0) return "";
    return std::string(buffer.data(), static_cast<size_t>(size));
}

bool decode_tokens_batched(
        llama_context * ctx,
        const llama_token * tokens,
        int32_t token_count,
        int32_t start_pos,
        int32_t batch_size,
        std::string & error) {
    if (token_count <= 0) {
        return true;
    }

    const int32_t safe_batch_size = std::max<int32_t>(1, batch_size);
    llama_batch batch = llama_batch_init(safe_batch_size, 0, 1);

    for (int32_t offset = 0; offset < token_count; offset += safe_batch_size) {
        batch.n_tokens = 0;
        const int32_t current = std::min<int32_t>(safe_batch_size, token_count - offset);

        for (int32_t i = 0; i < current; ++i) {
            const int32_t index = batch.n_tokens++;
            batch.token[index] = tokens[offset + i];
            batch.pos[index] = start_pos + offset + i;
            batch.n_seq_id[index] = 1;
            batch.seq_id[index][0] = 0;
            batch.logits[index] = (offset + i == token_count - 1) ? 1 : 0;
        }

        if (llama_decode(ctx, batch) != 0) {
            error = "Failed to decode tokens";
            llama_batch_free(batch);
            return false;
        }
    }

    llama_batch_free(batch);
    return true;
}

void release_chat_model_locked() {
    if (g_chat_model != nullptr) {
        llama_model_free(g_chat_model);
        g_chat_model = nullptr;
        g_chat_vocab = nullptr;
    }
}

void release_embedding_model_locked() {
    if (g_embedding_model != nullptr) {
        llama_model_free(g_embedding_model);
        g_embedding_model = nullptr;
        g_embedding_vocab = nullptr;
    }
}

uint32_t tuned_context_size(uint32_t prompt_tokens, int max_tokens) {
    const uint32_t required = prompt_tokens + static_cast<uint32_t>(std::max(32, max_tokens + 24));
    const uint32_t rounded = ((required + CONTEXT_GRANULARITY - 1) / CONTEXT_GRANULARITY) * CONTEXT_GRANULARITY;
    return std::max(MIN_CONTEXT_SIZE, std::min(MAX_CONTEXT_SIZE, rounded));
}

std::string generate_internal(const std::string & prompt, int max_tokens, float temperature) {
    if (g_chat_model == nullptr || g_chat_vocab == nullptr) {
        return "Model is not initialized";
    }

    const int required_prompt_tokens = -llama_tokenize(
            g_chat_vocab, prompt.c_str(), static_cast<int32_t>(prompt.size()),
            nullptr, 0, true, true);
    if (required_prompt_tokens <= 0) {
        return "Failed to tokenize prompt";
    }

    std::vector<llama_token> prompt_tokens(static_cast<size_t>(required_prompt_tokens));
    if (llama_tokenize(
            g_chat_vocab, prompt.c_str(), static_cast<int32_t>(prompt.size()),
            prompt_tokens.data(), static_cast<int32_t>(prompt_tokens.size()),
            true, true) < 0) {
        return "Failed to tokenize prompt";
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = tuned_context_size(static_cast<uint32_t>(prompt_tokens.size()), max_tokens);
    ctx_params.n_batch = std::min<uint32_t>(
            ctx_params.n_ctx,
            std::max<uint32_t>(32, std::min<uint32_t>(192, static_cast<uint32_t>(prompt_tokens.size()))));
    ctx_params.n_ubatch = ctx_params.n_batch;
    ctx_params.n_threads = 4;
    ctx_params.n_threads_batch = 4;
    ctx_params.no_perf = true;

    llama_context * ctx = llama_init_from_model(g_chat_model, ctx_params);
    if (ctx == nullptr) {
        return "Failed to create llama context";
    }

    __android_log_print(
            ANDROID_LOG_INFO,
            TAG,
            "generate_internal prompt_tokens=%d max_tokens=%d temperature=%.2f n_ctx=%u n_batch=%u",
            static_cast<int>(prompt_tokens.size()),
            max_tokens,
            temperature,
            ctx_params.n_ctx,
            ctx_params.n_batch);

    auto chain_params = llama_sampler_chain_default_params();
    chain_params.no_perf = true;
    llama_sampler * sampler = llama_sampler_chain_init(chain_params);
    if (temperature <= 0.01f) {
        llama_sampler_chain_add(sampler, llama_sampler_init_greedy());
    } else {
        llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(sampler, llama_sampler_init_dist(0));
    }

    std::string output;
    output.reserve(prompt.size() + static_cast<size_t>(max_tokens) * 4);

    if (llama_model_has_encoder(g_chat_model)) {
        llama_batch batch = llama_batch_get_one(prompt_tokens.data(), static_cast<int32_t>(prompt_tokens.size()));
        if (llama_encode(ctx, batch) != 0) {
            llama_sampler_free(sampler);
            llama_free(ctx);
            return "Failed to encode prompt";
        }

        llama_token decoder_start_token = llama_model_decoder_start_token(g_chat_model);
        if (decoder_start_token == LLAMA_TOKEN_NULL) {
            decoder_start_token = llama_vocab_bos(g_chat_vocab);
        }
        batch = llama_batch_get_one(&decoder_start_token, 1);
    }

    std::string decode_error;
    if (!llama_model_has_encoder(g_chat_model)) {
        if (!decode_tokens_batched(
                    ctx,
                    prompt_tokens.data(),
                    static_cast<int32_t>(prompt_tokens.size()),
                    0,
                    static_cast<int32_t>(ctx_params.n_batch),
                    decode_error)) {
            llama_sampler_free(sampler);
            llama_free(ctx);
            return decode_error;
        }
    }

    int generated = 0;
    int32_t n_pos = static_cast<int32_t>(prompt_tokens.size());
    const int32_t target_tokens = n_pos + max_tokens;

    while (n_pos < target_tokens) {
        llama_token new_token = llama_sampler_sample(sampler, ctx, -1);
        if (llama_vocab_is_eog(g_chat_vocab, new_token)) {
            break;
        }

        output += token_to_piece(g_chat_vocab, new_token);
        std::string step_error;
        if (!decode_tokens_batched(ctx, &new_token, 1, n_pos, 1, step_error)) {
            output = step_error;
            break;
        }

        n_pos += 1;
        generated++;
    }

    llama_sampler_free(sampler);
    llama_free(ctx);

    if (generated == 0 && output.empty()) {
        return "Model returned no tokens";
    }
    return output;
}

std::vector<float> embed_internal(const std::string & text, std::string & error) {
    if (g_embedding_model == nullptr || g_embedding_vocab == nullptr) {
        error = "Embedding model is not initialized";
        return {};
    }

    const int n_tokens = -llama_tokenize(
            g_embedding_vocab, text.c_str(), static_cast<int32_t>(text.size()),
            nullptr, 0, true, true);
    if (n_tokens <= 0) {
        error = "Failed to tokenize text for embedding";
        return {};
    }

    std::vector<llama_token> tokens(static_cast<size_t>(n_tokens));
    if (llama_tokenize(
            g_embedding_vocab, text.c_str(), static_cast<int32_t>(text.size()),
            tokens.data(), static_cast<int32_t>(tokens.size()),
            true, true) < 0) {
        error = "Failed to tokenize text for embedding";
        return {};
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.embeddings    = true;
    ctx_params.pooling_type  = LLAMA_POOLING_TYPE_MEAN;
    ctx_params.n_ctx         = static_cast<uint32_t>(n_tokens + 4);
    ctx_params.n_batch       = static_cast<uint32_t>(n_tokens + 4);
    ctx_params.n_ubatch      = static_cast<uint32_t>(n_tokens + 4);
    ctx_params.n_threads     = 4;
    ctx_params.n_threads_batch = 4;
    ctx_params.no_perf       = true;

    llama_context * ctx = llama_init_from_model(g_embedding_model, ctx_params);
    if (ctx == nullptr) {
        error = "Failed to create embedding context";
        return {};
    }

    __android_log_print(
            ANDROID_LOG_INFO,
            TAG,
            "embed_internal: token_count=%d n_ctx=%u n_batch=%u",
            n_tokens,
            ctx_params.n_ctx,
            ctx_params.n_batch);

    llama_batch batch = llama_batch_init(static_cast<int32_t>(tokens.size()), 0, 1);
    batch.n_tokens = static_cast<int32_t>(tokens.size());
    for (int32_t i = 0; i < batch.n_tokens; ++i) {
        batch.token[i]       = tokens[i];
        batch.pos[i]         = i;
        batch.n_seq_id[i]    = 1;
        batch.seq_id[i][0]   = 0;
        batch.logits[i]      = 0;
    }

    if (llama_decode(ctx, batch) != 0) {
        error = "llama_decode failed during embedding";
        llama_batch_free(batch);
        llama_free(ctx);
        return {};
    }
    __android_log_print(
            ANDROID_LOG_INFO,
            TAG,
            "embed_internal: decode finished token_count=%d",
            batch.n_tokens);
    llama_batch_free(batch);

    const int n_embd = llama_model_n_embd(g_embedding_model);
    const float * embd = llama_get_embeddings_seq(ctx, 0);
    if (embd == nullptr) {
        embd = llama_get_embeddings_ith(ctx, batch.n_tokens - 1);
    }
    if (embd == nullptr) {
        error = "Failed to get embeddings from context";
        llama_free(ctx);
        return {};
    }

    std::vector<float> result(embd, embd + n_embd);
    llama_free(ctx);

    float norm = 0.0f;
    for (float v : result) norm += v * v;
    norm = std::sqrt(norm);
    if (norm > 0.0f) {
        for (float & v : result) v /= norm;
    }

    return result;
}

} // namespace

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_danichapps_simpleagent_data_remote_LlamaCppNative_nativeIsBackendAvailable(
        JNIEnv *, jobject) {
    return JNI_TRUE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_danichapps_simpleagent_data_remote_LlamaCppNative_nativeIsChatModelReady(
        JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return g_chat_model != nullptr ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_danichapps_simpleagent_data_remote_LlamaCppNative_nativeInitializeChatModel(
        JNIEnv * env, jobject, jstring modelPath) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (!g_backend_initialized) {
        llama_log_set(llama_log_callback, nullptr);
        llama_backend_init();
        ggml_backend_load_all();
        g_backend_initialized = true;
    }

    release_chat_model_locked();

    const std::string model_path = jstring_to_string(env, modelPath);
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;

    g_chat_model = llama_model_load_from_file(model_path.c_str(), model_params);
    if (g_chat_model == nullptr) {
        const std::string error = "Unable to load GGUF model: " + model_path;
        log_error(error);
        return env->NewStringUTF(error.c_str());
    }

    g_chat_vocab = llama_model_get_vocab(g_chat_model);
    if (g_chat_vocab == nullptr) {
        release_chat_model_locked();
        const std::string error = "Unable to obtain model vocabulary";
        log_error(error);
        return env->NewStringUTF(error.c_str());
    }

    return nullptr;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_danichapps_simpleagent_data_remote_LlamaCppNative_nativeInitializeEmbeddingModel(
        JNIEnv * env, jobject, jstring modelPath) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (!g_backend_initialized) {
        llama_log_set(llama_log_callback, nullptr);
        llama_backend_init();
        ggml_backend_load_all();
        g_backend_initialized = true;
    }

    release_embedding_model_locked();

    const std::string model_path = jstring_to_string(env, modelPath);
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;

    g_embedding_model = llama_model_load_from_file(model_path.c_str(), model_params);
    if (g_embedding_model == nullptr) {
        const std::string error = "Unable to load embedding GGUF model: " + model_path;
        log_error(error);
        return env->NewStringUTF(error.c_str());
    }

    g_embedding_vocab = llama_model_get_vocab(g_embedding_model);
    if (g_embedding_vocab == nullptr) {
        release_embedding_model_locked();
        const std::string error = "Unable to obtain embedding model vocabulary";
        log_error(error);
        return env->NewStringUTF(error.c_str());
    }

    return nullptr;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_danichapps_simpleagent_data_remote_LlamaCppNative_nativeIsEmbeddingModelReady(
        JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return g_embedding_model != nullptr ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_danichapps_simpleagent_data_remote_LlamaCppNative_nativeGenerate(
        JNIEnv * env, jobject, jstring prompt, jint maxTokens, jfloat temperature) {
    std::lock_guard<std::mutex> lock(g_mutex);
    const std::string prompt_text = jstring_to_string(env, prompt);
    const std::string result = generate_internal(
            prompt_text,
            std::max(1, static_cast<int>(maxTokens)),
            std::max(0.0f, static_cast<float>(temperature)));
    return env->NewStringUTF(result.c_str());
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_danichapps_simpleagent_data_remote_LlamaCppNative_nativeEmbed(
        JNIEnv * env, jobject, jstring text) {
    std::lock_guard<std::mutex> lock(g_mutex);
    const std::string text_str = jstring_to_string(env, text);
    std::string error;
    const std::vector<float> result = embed_internal(text_str, error);
    if (result.empty()) {
        log_error(error.empty() ? "embed_internal returned empty result" : error);
        return nullptr;
    }
    jfloatArray arr = env->NewFloatArray(static_cast<jsize>(result.size()));
    if (arr != nullptr) {
        env->SetFloatArrayRegion(arr, 0, static_cast<jsize>(result.size()), result.data());
    }
    return arr;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_danichapps_simpleagent_data_remote_LlamaCppNative_nativeReleaseChatModel(
        JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    release_chat_model_locked();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_danichapps_simpleagent_data_remote_LlamaCppNative_nativeReleaseEmbeddingModel(
        JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    release_embedding_model_locked();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_danichapps_simpleagent_data_remote_LlamaCppNative_nativeReleaseAll(
        JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    release_chat_model_locked();
    release_embedding_model_locked();
    if (g_backend_initialized) {
        llama_backend_free();
        g_backend_initialized = false;
    }
}
