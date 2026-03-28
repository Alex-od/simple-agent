export default {
  name: 'StatusCard',
  props: {
    title: { type: String, required: true },
    value: { type: String, required: true },
    status: { type: String, default: 'unknown' },
    subtitle: { type: String, default: '' },
  },
  template: `
    <div class="card">
      <div class="card-header">
        <span :class="['status-dot', status]"></span>
        <span class="card-title">{{ title }}</span>
      </div>
      <div class="card-value">{{ value }}</div>
      <div v-if="subtitle" class="card-subtitle">{{ subtitle }}</div>
    </div>
  `,
}
