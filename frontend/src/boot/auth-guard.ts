import { boot } from 'quasar/wrappers'
import { useAuthStore } from 'src/stores/auth'
import type { RouteLocationNormalized } from 'vue-router'

export default boot(({ router }) => {
  router.beforeEach((to: RouteLocationNormalized) => {
    const auth = useAuthStore() // Pinia уже установлена приложением

    // защищённые маршруты
    if ((to.meta as any).requiresAuth && !auth.isAuthenticated) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }

    // уже авторизован и идём на /login — вернуть назад
    if (to.name === 'login' && auth.isAuthenticated) {
      return (to.query.redirect as string) || '/'
    }

    return true
  })
})
