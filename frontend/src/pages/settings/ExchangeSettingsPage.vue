<template>
  <div class="q-pa-md">
    <div class="text-h6 q-mb-md">Обмен с устройствами</div>

    <q-form class="q-gutter-md" @submit.prevent="onSave">

      <q-input
        v-model.number="model.exchangePeriodSec"
        type="number"
        label="Период обмена (секунды)"
        outlined
        dense
        min="5"
        hint="Как часто устройство обменивается данными с сервером"
      />

      <q-input
        v-model="model.qrPayloadText"
        type="textarea"
        autogrow
        outlined
        dense
        label="Текст для QR-кода"
        hint="Формат и содержимое определяет Android-разработчик"
        class="font-mono"
      />

      <q-separator />

      <div class="row q-gutter-sm">
        <q-btn
          color="primary"
          label="Сохранить"
          type="submit"
          :loading="loading"
        />
        <q-btn
          flat
          label="Отмена"
          @click="onCancel"
          :disable="loading"
        />
      </div>

      <q-banner v-if="statusMsg" :type="statusType" class="q-mt-md">
        {{ statusMsg }}
      </q-banner>
    </q-form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useQuasar } from 'quasar'
import { apiClient } from 'src/services/apiClient.js'

interface ExchangeSettings {
  exchangePeriodSec: number
  qrPayloadText: string
}

const $q = useQuasar()
const loading = ref(false)
const statusMsg = ref<string | null>(null)
const statusType = ref<'positive' | 'negative' | 'warning' | 'info'>('info')

const model = reactive<ExchangeSettings>({
  exchangePeriodSec: 60,
  qrPayloadText: ''
})

function getErrorMessage(err: unknown): string {
  if (err && typeof err === 'object') {
    const e = err as { response?: { data?: { message?: string } }; message?: string }
    return e.response?.data?.message || e.message || 'Произошла ошибка'
  }
  return 'Произошла ошибка'
}

async function load() {
  loading.value = true
  try {
    const { data } = await apiClient.get('/settings/exchange_settings')
    if (typeof data.exchangePeriodSec === 'number') {
      model.exchangePeriodSec = data.exchangePeriodSec
    }
    if (typeof data.qrPayloadText === 'string') {
      model.qrPayloadText = data.qrPayloadText
    }
  } catch {
    statusMsg.value = 'Не удалось загрузить настройки'
    statusType.value = 'warning'
    $q.notify({ type: 'warning', message: 'Не удалось загрузить настройки' })
  } finally {
    loading.value = false
  }
}

async function onSave() {
  loading.value = true
  try {
    await apiClient.put('/settings/exchange_settings', model)
    statusMsg.value = 'Настройки сохранены'
    statusType.value = 'positive'
    $q.notify({ type: 'positive', message: 'Настройки сохранены' })
  } catch (err) {
    const msg = getErrorMessage(err)
    statusMsg.value = msg
    statusType.value = 'negative'
    $q.notify({ type: 'negative', message: msg })
  } finally {
    loading.value = false
  }
}

function onCancel() {
  load()
}

onMounted(load)
</script>
