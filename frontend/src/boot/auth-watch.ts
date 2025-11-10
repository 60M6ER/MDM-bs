import { boot } from 'quasar/wrappers'
import { useAuthStore } from 'src/stores/auth'
import { watch } from 'vue'

export default boot(({ router }) => {
  const auth = useAuthStore()

  watch(
    () => auth.token,
    (t) => {
      const cur = router.currentRoute.value
      // диагностика
      console.debug('[auth-watch]', 'token:', t ? 'present' : 'missing', 'route:', cur.fullPath)

      // редиректим ТОЛЬКО с защищённых страниц и не с login
      if (!t && cur.name !== 'login' && cur.meta?.requiresAuth) {
        void router.replace({ name: 'login', query: { redirect: cur.fullPath } })
      }
    },
    { immediate: true } // запускаем при старте, покрывает кейс «зашли на защищённую без токена»
  )
})
