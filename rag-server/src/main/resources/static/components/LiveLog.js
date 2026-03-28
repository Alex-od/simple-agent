const { ref, watch, nextTick } = Vue

export default {
  name: 'LiveLog',
  props: {
    entries: { type: Array, default: () => [] },
  },
  template: `
    <div class="live-log" ref="logEl">
      <div v-for="(entry, i) in entries" :key="i"
           :class="['log-entry', entryClass(entry)]">
        {{ entry }}
      </div>
      <div v-if="entries.length === 0" class="log-entry">Лог пуст</div>
    </div>
  `,
  setup(props) {
    const logEl = ref(null)

    watch(() => props.entries.length, () => {
      nextTick(() => {
        if (logEl.value) {
          logEl.value.scrollTop = logEl.value.scrollHeight
        }
      })
    })

    function entryClass(entry) {
      if (entry.includes('ERROR') || entry.includes('Ошибка')) return 'error'
      if (entry.includes('OK') || entry.includes('Завершено') || entry.includes('COMPLETED')) return 'success'
      return ''
    }

    return { logEl, entryClass }
  },
}
