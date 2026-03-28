const { computed } = Vue

export default {
  name: 'ProgressBar',
  props: {
    current: { type: Number, default: 0 },
    total: { type: Number, default: 0 },
    label: { type: String, default: '' },
  },
  template: `
    <div class="progress-container">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: percentage + '%' }"></div>
      </div>
      <div class="progress-text">{{ current }} / {{ total }} файлов ({{ percentage }}%)</div>
      <div v-if="label" class="progress-label">{{ label }}</div>
    </div>
  `,
  setup(props) {
    const percentage = computed(() => {
      return props.total > 0 ? Math.round(props.current / props.total * 100) : 0
    })
    return { percentage }
  },
}
