// src/stores/auth.js
import { defineStore } from 'pinia'
import { apiClient } from 'src/services/apiClient'

// локальный helper, чтобы не размазывать логику по actions
const setAuthHeader = (token) => {
  console.log('[auth] setAuthHeader →', token ? 'Bearer…' : '(clear)')
  if (token) {
    apiClient.defaults.headers.common.Authorization = `Bearer ${token}`
  } else {
    delete apiClient.defaults.headers.common.Authorization
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: null,
    user: null,         // при необходимости
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
  },
  persist: {
    paths: ['token', 'user'],
    storage: localStorage
  },
  actions: {
    setToken(token) {
      this.token = token || null
      setAuthHeader(this.token)
    },
    initFromPersist() {
      setAuthHeader(this.token)
    },
    async login(username, password) {
      console.log('[auth] login:start', { username })
      const {data} = await apiClient.post('/auth/login', {username, password})
      console.log('[auth] login:resp', data)
      // допускаем разные варианты контракта
      const success = data?.success ?? true
      if (!success) {
        const msg = data?.message || 'Ошибка авторизации'
        const code = data?.code || 'UNKNOWN'
        const err = new Error(msg)
        err.code = code
        throw err
      }

      const token = data?.accessToken ?? data?.token
      if (!token) {
        const err = new Error('Нет accessToken в ответе')
        err.code = 'NO_TOKEN'
        throw err
      }

      this.setToken(token)
      this.user = data?.user ?? null
      console.log('[auth] login:state', { token: this.token, user: this.user })
      return {message: data?.message || 'ok', code: data?.code || 'OK'}
    },
    logout() {
      console.log('[auth] logout has been asked →')
      this.setToken(null)
      this.user = null
    }
  }
})
