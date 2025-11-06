<template>
  <q-page class="flex flex-center">
    <img
      alt="Quasar logo"
      src="~assets/quasar-logo-vertical.svg"
      style="width: 200px; height: 200px"
    />
    <div class="text-subtitle1">Защищённая строка с сервера:</div>
    <div class="text-body1 q-mt-sm">{{ secureText }}</div>
  </q-page>
</template>

<script setup>
import { ref, onMounted} from "vue";
//import { apiClient} from "src/services/apiClient.js";

const secureText = ref('')

onMounted(async () => {
  try {
    //const { data } = await apiClient.get('/secure')
    // если сервер просто отдаёт строку:
    secureText.value = "typeof data === 'string' ? data : (data?.message || JSON.stringify(data))"
  } catch (err) {
    console.warn('Не удалось получить защищенную строку', err)
  }
})
</script>
