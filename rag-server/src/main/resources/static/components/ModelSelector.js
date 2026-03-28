const { ref, computed, watch } = Vue

export default {
  name: 'ModelSelector',
  props: {
    label: { type: String, required: true },
    models: { type: Array, default: () => [] },
    activeModel: { type: String, default: '' },
    loading: { type: Boolean, default: false },
  },
  emits: ['select'],
  template: `
    <div class="form-group">
      <label>{{ label }}</label>
      <div style="display: flex; gap: 8px; align-items: center;">
        <select class="select" v-model="selected" :disabled="loading" style="flex: 1;">
          <option v-for="m in normalizedModels" :key="m.id" :value="m.id">
            {{ m.name }}{{ m.provider ? ' (' + m.provider + ')' : '' }}
          </option>
        </select>
        <button v-if="selected !== activeModel"
                class="btn btn-primary btn-sm"
                :disabled="loading"
                @click="$emit('select', selected)">
          Применить
        </button>
      </div>
    </div>
  `,
  setup(props) {
    const selected = ref(props.activeModel)

    watch(() => props.activeModel, (val) => {
      selected.value = val
    })

    const normalizedModels = computed(() => {
      return props.models.map(m => {
        if (typeof m === 'string') return { id: m, name: m, provider: '' }
        return { id: m.id, name: m.name || m.id, provider: m.provider || '' }
      })
    })

    return { selected, normalizedModels }
  },
}
