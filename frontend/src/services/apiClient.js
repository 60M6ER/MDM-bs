import axios from 'axios'
import { routerInstance as router } from 'src/boot/router-instance'
import { useAuthStore } from 'src/stores/auth'

// если в деве есть прокси, можно оставить '/api' и сделать rewrite на '/api/v_1'.
// но так проще и явно:
const API_BASE = '/api/v_1'

export const apiClient = axios.create({
  baseURL: API_BASE,
  withCredentials: false,
})

// Подставляем токен, если он уже есть в Pinia (при горячей перезагрузке/refresh)
apiClient.interceptors.request.use((config) => {
  const auth = useAuthStore()
  const token = auth?.token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  } else {
    // если идём на защищённый путь без токена — отправим на логин до запроса
    // исключаем эндпоинты авторизации
    const url = (config.url || '')
    if (!url.includes('/auth/')) {
      const redirect = router.currentRoute.value.fullPath || '/'
      router.replace({ path: '/login', query: { redirect } })
      // прерываем сам запрос
      return Promise.reject(new axios.Cancel('No token — redirect to login'))
    }
  }
  return config
})

// 401 → logout + редирект на логин c сохранением возврата
apiClient.interceptors.response.use(
  (r) => r,
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      const auth = useAuthStore()
      auth.logout()
      const current = router.currentRoute.value.fullPath || '/'
      if (!current.startsWith('/login')) {
        router.replace({ path: '/login', query: { redirect: current } })
      }
    }
    return Promise.reject(error)
  }
)
