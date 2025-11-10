import { defineStore } from '#q-app/wrappers'
import { createPinia } from 'pinia'
import piniaPersist from 'pinia-plugin-persistedstate'

/*
 * If not building with SSR mode, you can
 * directly export the Store instantiation;
 *
 * The function below can be async too; either use
 * async/await or return a Promise which resolves
 * with the Store instance.
 */

export default defineStore(async (/* { ssrContext } */) => {
  const pinia = createPinia()

  pinia.use(piniaPersist)

  try {
    const { useAuthStore } = await import('src/stores/auth')
    const auth = useAuthStore(pinia)
    if (auth?.initFromPersist) auth.initFromPersist()
  } catch {
    // optional: keep silent in case auth store is not yet defined
  }

  return pinia
})
