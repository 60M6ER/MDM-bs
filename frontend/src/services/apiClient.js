import axios from 'axios'

const API_BASE = '/api/v1'

let handlingUnauthorized = false

export const apiClient = axios.create({
  baseURL: API_BASE,
  withCredentials: false,
})

apiClient.interceptors.request.use((config) => {
  // AbortSignal support (fetch-style)
  if (config.signal) {
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
        s.removeEventListener('abort', onAbort)
      }
    }
  }

  return config
})

apiClient.interceptors.response.use(
  (r) => {
    if (r.config && typeof r.config.cleanupAbort === 'function') {
      r.config.cleanupAbort()
    }
    return r
  },
  async (error) => {
    if (axios.isCancel(error)) {
      return Promise.reject(error)
    }

    if (error && error.config && typeof error.config.cleanupAbort === 'function') {
      error.config.cleanupAbort()
    }

    const status = error?.response?.status
    if (status === 401) {
      // избегаем каскада редиректов при параллельных запросах
      if (!handlingUnauthorized) {
        handlingUnauthorized = true
        try {
          const { useAuthStore } = await import('src/stores/auth')
          const auth = useAuthStore()
          auth.logout()
        } catch {
          // ignore
        }

        try {
          // Если у нас уже страница логина — ничего не делаем
          const isOnLogin = window.location.pathname.endsWith('/login')
            || (window.location.hash && window.location.hash.includes('/login'))

          if (!isOnLogin) {
            // Собираем redirect максимально безопасно для history/hash режимов
            const hasHashRoute = window.location.hash && window.location.hash.startsWith('#/')
            const currentRoute = hasHashRoute
              ? window.location.hash.substring(1) // '/path?x=1'
              : (window.location.pathname + window.location.search)

            const redirectParam = encodeURIComponent(currentRoute)

            if (hasHashRoute) {
              // hash-mode
              window.location.assign(`/#/login?redirect=${redirectParam}`)
            } else {
              // history-mode
              window.location.assign(`/login?redirect=${redirectParam}`)
            }
          }
        } finally {
          // оставляем флаг поднятым до полной навигации; если редиректа не было — сбросим
          setTimeout(() => { handlingUnauthorized = false }, 1000)
        }
      }
    }

    return Promise.reject(error)
  }
)
