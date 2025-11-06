import axios from 'axios'
import { routerInstance as router } from 'src/boot/router-instance'
import { useAuthStore } from 'src/stores/auth'

const API_BASE = '/api/v1'

const PUBLIC_ENDPOINTS = [
  '/auth/',
  '/health_check'
]

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
    const url = config.url || ''
    const isPublic = PUBLIC_ENDPOINTS.some(x => url.includes(x))

    if (!isPublic) {
      const redirect = router.currentRoute.value.fullPath || '/'
      router.replace({ path: '/login', query: { redirect } })
      // прерываем сам запрос
      return Promise.reject(new axios.Cancel('No token — redirect to login'))
    }
  }

  // AbortSignal support (fetch-style)
  if (config.signal) {
    // if already aborted before request
    if (config.signal.aborted) {
      const src = axios.CancelToken.source()
      config.cancelToken = src.token
      src.cancel('Request aborted by signal')
      return Promise.reject(new Error('Request aborted by signal'))
    }

    const source = axios.CancelToken.source()
    config.cancelToken = source.token

    const onAbort = () => source.cancel('Request aborted by signal')
    config.signal.addEventListener('abort', onAbort)

    // store cleanup to remove listener after response
    config.cleanupAbort = () => {
      const s = config.signal
      if (s && typeof s.removeEventListener === 'function') {
        // best-effort cleanup; if listener уже снят, ничего не делаем
        s.removeEventListener('abort', onAbort)
      }
    }
  }

  return config
})

// 401 → logout + редирект на логин c сохранением возврата
apiClient.interceptors.response.use(
  (r) => {
    if (r.config && typeof r.config.cleanupAbort === 'function') {
      r.config.cleanupAbort()
    }
    return r
  },
  (error) => {
    if (axios.isCancel(error)) {
      return Promise.reject(error)
    }

    if (error && error.config && typeof error.config.cleanupAbort === 'function') {
      error.config.cleanupAbort()
    }

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
