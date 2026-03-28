const { inject } = Vue

export default {
  name: 'Toast',
  template: `
    <div class="toast-container">
      <div v-for="toast in toasts"
           :key="toast.id"
           :class="['toast', toast.type]">
        <span class="toast-message" style="user-select:text; cursor:text;">{{ toast.message }}</span>
        <button class="toast-close" @click="removeToast(toast.id)" title="Закрыть">✕</button>
      </div>
    </div>
  `,
  setup() {
    const toasts = inject('toasts')
    const removeToast = inject('removeToast')
    return { toasts, removeToast }
  },
}
