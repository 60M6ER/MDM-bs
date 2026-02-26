import { boot } from 'quasar/wrappers'
import { useAuthStore } from 'src/stores/auth'
import { watch } from 'vue'

export default boot(({ router }) => {
  const auth = useAuthStore()

  // При перезагрузке страницы token может быть восстановлен из persisted state,
  // но axios не подхватит Authorization автоматически.
  // Поэтому на старте явно инициализируем стор и синхронизируем заголовок.
  auth.initFromPersist()
  auth.setToken(auth.token)

  watch(
    () => auth.token,
    (t) => {
      const cur = router.currentRoute.value
      // диагностика
      console.debug('[auth-watch]', 'token:', t ? 'present' : 'missing', 'route:', cur.fullPath)

      // синхронизация Authorization header с текущим токеном
      auth.setToken(t)
    },
    { immediate: true } // запускаем при старте и синхронизируем Authorization
  )
})
