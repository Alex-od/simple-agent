## On-device llama.cpp setup

This project is now wired for an on-device llama.cpp flow:
- the Android app looks for a GGUF file on the phone
- offline RAG stays inside the app
- no internet is required at runtime

Default model filename:
- `Qwen2.5-3B-Instruct-Q4_K_M.gguf`

Default expected location on the phone:
- `/storage/50D5-2CE6/agents/Qwen2.5-3B-Instruct-Q4_K_M.gguf`

Fallback location if the external storage path is unavailable:
- `Android/data/com.danichapps.simpleagent/files/llm/Qwen2.5-3B-Instruct-Q4_K_M.gguf`

You can override the path at build time in `local.properties`:
```properties
ON_DEVICE_LLM_MODEL_PATH=/storage/emulated/0/Android/data/com.danichapps.simpleagent/files/llm/Qwen2.5-3B-Instruct-Q4_K_M.gguf
ON_DEVICE_LLM_MODEL_FILENAME=Qwen2.5-3B-Instruct-Q4_K_M.gguf
```

Optional local embedding model for hybrid RAG:
```properties
LOCAL_EMBEDDING_MODEL_PATH=<absolute path on device or app storage to embedding_model.tflite>
```

If `LOCAL_EMBEDDING_MODEL_PATH` is not set, offline RAG still works with BM25-only retrieval.

Important:
- the app side is prepared for llama.cpp on Android
- the actual llama.cpp native backend is not bundled yet in this repository
- you still need to add real llama.cpp Android sources or prebuilt native libraries into `app/src/main/cpp`
