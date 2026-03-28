const { createApp, ref, provide } = Vue
const { createRouter, createWebHashHistory } = VueRouter
import Sidebar from './components/Sidebar.js'
import Toast from './components/Toast.js'
import Dashboard from './pages/Dashboard.js'
import Config from './pages/Config.js'
import Indexing from './pages/Indexing.js'
import Playground from './pages/Playground.js'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: Dashboard },
  { path: '/config', component: Config },
  { path: '/indexing', component: Indexing },
  { path: '/playground', component: Playground },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

const App = {
  components: { Sidebar, Toast },
  template: `
    <div class="app">
      <Sidebar />
      <main class="main-content">
        <router-view />
      </main>
      <Toast />
    </div>
  `,
  setup() {
    const toasts = ref([])
    let toastId = 0

    function addToast(message, type = 'info') {
      const id = ++toastId
      toasts.value.push({ id, message, type })
      if (type !== 'error') {
        setTimeout(() => removeToast(id), 4000)
      }
    }

    function removeToast(id) {
      toasts.value = toasts.value.filter(t => t.id !== id)
    }

    provide('toasts', toasts)
    provide('addToast', addToast)
    provide('removeToast', removeToast)

    return {}
  },
}

const app = createApp(App)
app.use(router)
app.mount('#app')
