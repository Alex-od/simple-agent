const { useRoute, useRouter } = VueRouter
const { computed } = Vue

export default {
  name: 'Sidebar',
  template: `
    <aside class="sidebar">
      <div class="sidebar-logo">⚡ RAG Admin</div>
      <nav class="sidebar-nav">
        <a v-for="item in items"
           :key="item.path"
           :class="['nav-item', { active: isActive(item.path) }]"
           @click="navigate(item.path)">
          <span class="nav-icon">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </a>
      </nav>
    </aside>
  `,
  setup() {
    const route = useRoute()
    const router = useRouter()

    const items = [
      { path: '/dashboard', label: 'Dashboard', icon: '\u{1F4CA}' },
      { path: '/config',    label: 'Config',    icon: '\u{2699}\u{FE0F}' },
      { path: '/indexing',  label: 'Indexing',   icon: '\u{1F4C1}' },
      { path: '/playground', label: 'Playground', icon: '\u{1F4AC}' },
    ]

    function isActive(path) {
      return route.path === path
    }

    function navigate(path) {
      router.push(path)
    }

    return { items, isActive, navigate }
  },
}
