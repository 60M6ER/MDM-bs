<template>
  <q-dialog v-model="model" persistent>
    <q-card style="min-width: 420px; max-width: 90vw;">
      <q-card-section class="row items-center">
        <div class="text-h6">Регистрация устройства</div>
        <q-space />
        <q-btn flat round dense icon="close" @click="close" />
      </q-card-section>

      <q-separator />

      <q-card-section class="q-pa-lg flex flex-center" v-if="loading">
        <div class="column items-center">
          <q-spinner size="36px" />
          <div class="q-mt-md text-grey-7">Готовим QR-код…</div>
        </div>
      </q-card-section>

      <q-card-section v-else>
        <div class="text-subtitle2 q-mb-sm">
          Время жизни QR: до {{ formatTs(resp.expiresAtUtc) }}
        </div>

<!--        <div class="flex flex-center q-my-md">-->
<!--          <canvas ref="qrCanvas" width="256" height="256" />-->
<!--        </div>-->

        <div class="flex flex-center q-my-md">
          <QrcodeVue :value="qrText" :size="256" level="M" />
        </div>

        <q-expansion-item dense expand-separator icon="code" label="Показать payload (отладка)">
          <q-card>
            <q-card-section>
              <pre class="q-pa-sm bg-grey-2" style="white-space: pre-wrap;">{{ pretty(payload) }}</pre>
            </q-card-section>
          </q-card>
        </q-expansion-item>
      </q-card-section>

      <q-separator />

      <q-card-actions align="right">
        <q-btn flat label="Закрыть" @click="close" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
//import { ref, watch, nextTick } from 'vue'
import { ref, watch, computed } from 'vue'
import { useQuasar } from 'quasar'
import { apiClient } from "src/services/apiClient.js"
//import QRCode from 'qrcode' // требуется зависимость
import QrcodeVue from 'qrcode.vue'

const $q = useQuasar()

// v-model
const model = defineModel({ type: Boolean, required: true })
const emit = defineEmits(['closed'])

const loading = ref(false)
const resp = ref(null)
const payload = ref(null)
//const qrCanvas = ref(null)
const qrText = computed(() => payload.value ? JSON.stringify(payload.value) : '')

function formatTs(ts) {
  try { return new Date(ts).toLocaleString() } catch { return ts }
}
function pretty(obj) {
  try { return JSON.stringify(obj, null, 2) } catch { return String(obj) }
}

async function fetchPreprovision() {
  loading.value = true
  resp.value = null
  payload.value = null
  try {
    const { data } = await apiClient.post('/devices/preprovision')
    resp.value = data
    // минимальный payload для QR
    payload.value = {
      v: 1,
      preDeviceId: data.preDeviceId,
      regKey: data.regKey
    }
    if (
      data.qrPayload &&
      typeof data.qrPayload === 'object' &&
      Object.keys(data.qrPayload).length > 0
    ) {
      payload.value = data.qrPayload
    }
    // await nextTick()
    // // рендерим QR
    // await QRCode.toCanvas(qrCanvas.value, JSON.stringify(payload.value), {
    //   errorCorrectionLevel: 'M',
    //   width: 256,
    //   margin: 2
    // })
  } catch (e) {
    $q.notify({ type: 'negative', message: 'Ошибка при создании предрегистрации' })
    console.error(e)
    close()
  } finally {
    loading.value = false
  }
}

function close() {
  model.value = false
  emit('closed')
}

watch(model, (val) => {
  if (val) {
    fetchPreprovision()
  }
})
</script>
