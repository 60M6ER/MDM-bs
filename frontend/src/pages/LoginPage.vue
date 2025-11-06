<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref(null)

async function doLogin () {
  errorMsg.value = null
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (e) {
    const [msg] = String(e?.message || 'Ошибка авторизации').split('|')
    errorMsg.value = msg
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <!-- фуллскрин контейнер, центровка по вертикали и горизонтали -->
  <div class="fullscreen bg-grey-1 flex flex-center">
    <div class="column items-center">

      <!-- логотип -->
      <q-img
        src="https://cdn.quasar.dev/logo/svg/quasar-logo.svg"
        alt="Logo"
        style="width:80px;height:80px"
        class="q-mb-md"
        no-spinner
      />

      <!-- карточка формы фиксированной ширины -->
      <q-card class="q-pa-md" style="width: 260px;">
        <q-card-section class="q-pt-none">
          <div class="text-h6 text-center">Вход</div>
        </q-card-section>

        <q-card-section>
          <div class="column q-gutter-y-sm">
            <q-input
              v-model="username"
              label="Логин"
              dense
              outlined
              autofocus
            />
            <q-input
              v-model="password"
              label="Пароль"
              type="password"
              dense
              outlined
              @keyup.enter="doLogin"
            />
            <q-btn
              label="Войти"
              color="primary"
              class="full-width q-mt-sm"
              :loading="loading"
              @click="doLogin"
            />
            <div v-if="errorMsg" class="text-negative q-mt-md">{{ errorMsg }}</div>
          </div>
        </q-card-section>
      </q-card>

    </div>
  </div>
</template>
