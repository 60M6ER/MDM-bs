import { defineStore } from '#q-app/wrappers'
import { createPinia } from 'pinia'

/*
 * If not building with SSR mode, you can
 * directly export the Store instantiation;
 *
 * The function below can be async too; either use
 * async/await or return a Promise which resolves
 * with the Store instance.
 */

export default defineStore((/* { ssrContext } */) => {
  const pinia = createPinia()

  pinia.use(({ store }) => {
    const key = `app:${store.$id}`

    // восстановление
    try {
      const saved = localStorage.getItem(key)
      if (saved) store.$patch(JSON.parse(saved))
    } catch (err) {
      console.warn('[pinia-persist] restore failed', err)
    }

    // сохранение
    store.$subscribe((_mutation, state) => {
      let toSave = state
      if (store.$id === 'auth') {
        toSave = { token: state.token, user: state.user ?? null }
      }
      try {
        localStorage.setItem(key, JSON.stringify(toSave))
      } catch (err) {
        console.warn('[pinia-persist] persist failed', err)
      }
    })
  })


  return pinia
})
