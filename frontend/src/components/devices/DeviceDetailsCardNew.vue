<template>
  <q-card flat bordered class="fit column">
    <!-- header -->
    <q-card-section class="row items-center">
      <div class="text-subtitle1 text-weight-medium">Карточка устройства</div>
      <q-space />
      <q-btn flat round dense icon="refresh" :loading="loading" @click="fetchDetails" />
    </q-card-section>

    <q-separator />

    <!-- BODY scroll only here -->
    <q-card-section class="col scroll q-pa-md">
      <!-- top block: photo + name + basic -->
      <div class="row q-col-gutter-md">
        <div class="col-12 col-sm-4">
          <q-card bordered class="q-pa-sm">
            <!-- заготовка под фото -->
            <div class="device-photo flex flex-center text-grey-6">
              Фото устройства
            </div>
          </q-card>
        </div>

        <div class="col-12 col-sm-8">
          <div class="text-h6">{{ displayTitle }}</div>
          <div class="text-caption text-grey-7 q-mt-xs">{{ details.deviceId || '' }}</div>

          <div class="q-mt-md">
            <div class="text-caption text-grey-7 q-mb-xs">
              Поля редактируемые (сохранение будет добавлено позже)
            </div>
            <!-- редактируемые -->
            <q-input v-model="edit.deviceName" filled dense label="Имя устройства" />
            <q-input v-model="edit.inventoryNumber" filled dense label="Инвентарный номер" class="q-mt-sm" />
          </div>

          <!-- лаконичные строки -->
          <div class="q-mt-md">
            <q-input v-model="details.manufacturer" label="Производитель" readonly />
            <q-input v-model="details.model" label="Модель" readonly />
            <q-input v-model="details.serialNumber" label="Серийный номер" readonly />
            <q-input v-model="details.osVersion" label="Версия ОС" readonly />
            <q-input v-model="details.ipAddress" label="Последний IP адрес" readonly />
          </div>
        </div>
      </div>

      <q-separator class="q-my-md" />

      <!-- tabs -->
      <q-tabs v-model="tab" dense class="text-grey-8">
        <q-tab name="states" label="Состояния" />
        <q-tab name="events" label="События" />
      </q-tabs>
      <q-separator />

      <q-tab-panels v-model="tab" animated class="q-mt-md">
        <!-- STATES -->
        <q-tab-panel name="states" class="q-pa-none">
          <div class="q-gutter-sm">
            <div class="text-subtitle2">Батарея</div>
            <q-input v-model="details.batteryLevel" label="Уровень" readonly />
            <q-toggle
              v-model="details.charging"
              color="green"
              label="На зарядке"
              left-label
              readonly
            />
            <q-input v-model="details.manufacturer" label="Температура CPU" readonly />

            <q-separator class="q-my-md" />

            <div class="text-subtitle2">Сеть</div>
            <q-input v-model="details.networkType" label="Тип сети" readonly />
            <q-input v-model="details.wifiSsid" label="Wi-Fi SSID" readonly />
          </div>
        </q-tab-panel>

        <!-- EVENTS -->
        <q-tab-panel name="events" class="q-pa-none">
          <div class="row items-center no-wrap q-mb-sm">
            <div class="text-subtitle2">Последние события</div>
            <q-space />
            <q-btn flat dense round icon="refresh" :loading="eventsLoading" @click="fetchEvents(true)" />
          </div>

          <div v-if="!eventsLoading && events.length === 0" class="text-grey-7">
            Событий пока нет.
          </div>

          <q-list v-else bordered class="rounded-borders" dense>
            <q-item v-for="ev in events" :key="ev.id">
              <q-item-section>
                <div class="row items-center no-wrap">
                  <div class="text-weight-medium">{{ eventLabel(ev.event) }}</div>
                  <q-separator vertical inset class="q-mx-sm" />
                  <div class="text-caption text-grey-7">{{ fmtTs(ev.occurredAt) }}</div>
                </div>
                <div v-if="ev.details" class="text-caption text-grey-8 q-mt-xs">
                  {{ ev.details }}
                </div>
              </q-item-section>
            </q-item>
          </q-list>

          <div class="row justify-end q-mt-sm" v-if="canLoadMore">
            <q-btn flat no-caps label="Показать ещё" @click="loadMore" :disable="eventsLoading" />
          </div>
        </q-tab-panel>
      </q-tab-panels>
    </q-card-section>

    <!-- footer actions (позже: сохранить) -->
    <q-separator />
    <q-card-section class="row justify-end">
      <q-btn
        color="primary"
        label="Сохранить"
        :disable="!hasChanges"
        @click="save"
      />
    </q-card-section>
  </q-card>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { apiClient } from 'src/services/apiClient.js'

const props = defineProps({ deviceId: { type: String, required: true } })

const loading = ref(false)
const details = ref({})
const tab = ref('states')

const edit = ref({ deviceName: '', inventoryNumber: '' })
const hasChanges = computed(() =>
  (edit.value.deviceName ?? '') !== (details.value.deviceName ?? '') ||
  (edit.value.inventoryNumber ?? '') !== (details.value.inventoryNumber ?? '')
)

// --- events (как у тебя, почти без изменений)
const events = ref([])
const eventsLoading = ref(false)
const page = ref(0)
const pageSize = 50
const canLoadMore = ref(false)

const displayTitle = computed(() => {
  const d = details.value || {}

  // 1) если есть имя устройства — оно
  const name = (d.deviceName || '').trim()
  if (name) return name

  // 2) иначе "Производитель - Модель" (если что-то есть)
  const mm = [d.manufacturer, d.model].filter(Boolean).join(' ')
  const base = mm || shortId(d.deviceId)

  // 3) суффикс: если есть инвентарный — он, иначе серийный
  const suffix = (d.inventoryNumber || d.serialNumber || '').trim()
  return suffix ? `${base} — ${suffix}` : base
})

function shortId(uuid) { return String(uuid || '').split('-')[0] }
function fmtTs(ts) { if (!ts) return ''; try { return new Date(ts).toLocaleString() } catch { return String(ts) } }

function eventLabel(eventEnum) {
  switch (eventEnum) {
    case 'IP_CHANGED': return 'Изменился IP-адрес'
    case 'REGISTERED': return 'Устройство зарегистрировано'
    default: return String(eventEnum || 'event')
  }
}

async function fetchEvents(reset = true) {
  eventsLoading.value = true
  try {
    if (reset) { page.value = 0; events.value = [] }
    const { data } = await apiClient.get(`/devices/${props.deviceId}/events`, {
      params: { page: page.value, size: pageSize }
    })
    const list = Array.isArray(data?.content) ? data.content : (Array.isArray(data) ? data : [])
    events.value = reset ? list : events.value.concat(list)
    canLoadMore.value = (typeof data?.totalPages === 'number' && typeof data?.number === 'number')
      ? (data.number + 1) < data.totalPages
      : list.length === pageSize
  } finally {
    eventsLoading.value = false
  }
}
function loadMore() { page.value += 1; fetchEvents(false) }

async function fetchDetails() {
  loading.value = true
  try {
    const { data } = await apiClient.get(`/devices/${props.deviceId}`)
    details.value = data || {}
    edit.value.deviceName = details.value.deviceName || ''
    edit.value.inventoryNumber = details.value.inventoryNumber || ''
    await fetchEvents(true)
  } finally {
    loading.value = false
  }
}

async function save() {
  // TODO: подставить ваш endpoint обновления (PATCH/PUT)
  // пример:
  // await apiClient.patch(`/devices/${props.deviceId}`, { deviceName: edit.value.deviceName, inventoryNumber: edit.value.inventoryNumber })
  await fetchDetails()
}

watch(() => props.deviceId, () => fetchDetails(), { immediate: true })
</script>

<style scoped>
.device-photo {
  height: 180px;
  border: 1px dashed rgba(0,0,0,.25);
  border-radius: 8px;
}
</style>
