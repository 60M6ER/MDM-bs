// src/stores/auth.js
import { defineStore } from 'pinia'
import { apiClient } from 'src/services/apiClient'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: null,
    user: null,         // при необходимости
  }),
  persist: true,        // pinia-persist
  actions: {
    async login(username, password) {
      const { data } = await apiClient.post('/auth/login', { username, password })
      // ожидаем унифицированный ответ AuthResponse
      if (!data?.success) {
        const msg = data?.message || 'Ошибка авторизации'
        const code = data?.code || 'UNKNOWN'
        throw new Error(`${msg}|${code}`)
      }
      const token = data?.accessToken
      if (!token) {
        throw new Error('Нет accessToken в ответе')
      }
      this.token = token
      // сразу проставим заголовок для текущей сессии axios
      apiClient.defaults.headers.common.Authorization = `Bearer ${token}`
      return { message: data.message, code: data.code }
    },
    logout() {
      this.token = null
      delete apiClient.defaults.headers.common.Authorization
      this.user = null
    }
  }
})
