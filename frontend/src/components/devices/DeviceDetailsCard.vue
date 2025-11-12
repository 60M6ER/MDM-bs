<template>
  <q-card flat bordered class="fit column">
    <q-card-section class="row items-center">
      <div class="text-h6 q-mr-sm">Карточка устройства</div>
      <q-space />
      <q-btn flat round dense icon="refresh" :loading="loading" @click="fetchDetails" />
    </q-card-section>

    <q-separator />

    <q-card-section class="col scroll q-gutter-md">
      <!-- Идентификация -->
      <div class="row q-col-gutter-md">
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="details.deviceId" label="Device ID" />
        </div>
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="details.deviceName" label="Имя устройства" />
        </div>
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="details.serialNumber" label="Серийный номер" />
        </div>
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="details.inventoryNumber" label="Инвентарный номер" />
        </div>
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="details.status" label="Статус" />
        </div>
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="join(details.manufacturer, details.model)" label="Производитель / Модель" />
        </div>
      </div>

      <!-- Жизненный цикл -->
      <div class="row q-col-gutter-md">
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="fmtTs(details.enrolledAt)" label="Введено в эксплуатацию" />
        </div>
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="fmtTs(details.deactivatedAt)" label="Выведено из эксплуатации" />
        </div>
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="fmtTs(details.deviceCreatedAt)" label="Создано (запись)" />
        </div>
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="fmtTs(details.deviceUpdatedAt)" label="Обновлено (запись)" />
        </div>
      </div>

      <!-- Состояние и система -->
      <div class="row q-col-gutter-md">
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="fmtTs(details.stateLastSeenAt)" label="Последний онлайн" />
        </div>
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="fmtTs(details.stateUpdatedAt)" label="Обновление состояния" />
        </div>
        <div class="col-12 col-md-6">
          <q-toggle dense :model-value="!!details.online" label="Онлайн" disable />
        </div>
        <div class="col-12 col-md-6">
          <q-toggle dense :model-value="!!details.charging" label="Заряжается" disable />
        </div>
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="pct(details.batteryLevel)" label="Батарея" />
        </div>
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="join(details.osVersion, details.appVersion, ' / ')" label="OS / App" />
        </div>
      </div>

      <!-- Сеть -->
      <div class="row q-col-gutter-md">
        <div class="col-12 col-md-4">
          <q-input filled dense readonly :model-value="details.networkType" label="Сеть" />
        </div>
        <div class="col-12 col-md-4">
          <q-input filled dense readonly :model-value="details.wifiSsid" label="Wi-Fi SSID" />
        </div>
        <div class="col-12 col-md-4">
          <q-input filled dense readonly :model-value="details.ipAddress" label="IP адрес" />
        </div>
      </div>

      <!-- Память и температура -->
      <div class="row q-col-gutter-md">
        <div class="col-12 col-md-4">
          <q-input filled dense readonly :model-value="fmtMb(details.storageTotalMb)" label="Память всего" />
        </div>
        <div class="col-12 col-md-4">
          <q-input filled dense readonly :model-value="fmtMb(details.storageFreeMb)" label="Память свободно" />
        </div>
        <div class="col-12 col-md-4">
          <q-input filled dense readonly :model-value="fmtTemp(details.cpuTempC)" label="Температура CPU" />
        </div>
      </div>

      <!-- Владелец -->
      <div class="row q-col-gutter-md">
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="details.ownerDisplay" label="Владелец (отображаемое имя)" />
        </div>
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="details.ownerUserId" label="ID пользователя" />
        </div>
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="fmtTs(details.ownerAssignedAt)" label="Назначено владельцу" />
        </div>
      </div>

      <!-- Геопозиция -->
      <div class="row q-col-gutter-md">
        <div class="col-12 col-md-3">
          <q-input filled dense readonly :model-value="num(details.lat)" label="Широта" />
        </div>
        <div class="col-12 col-md-3">
          <q-input filled dense readonly :model-value="num(details.lon)" label="Долгота" />
        </div>
        <div class="col-12 col-md-3">
          <q-input filled dense readonly :model-value="num(details.accuracyM, ' м')" label="Точность" />
        </div>
        <div class="col-12 col-md-3">
          <q-input filled dense readonly :model-value="num(details.altitudeM, ' м')" label="Высота" />
        </div>
        <div class="col-12 col-md-3">
          <q-input filled dense readonly :model-value="num(details.speedMps, ' м/с')" label="Скорость" />
        </div>
        <div class="col-12 col-md-3">
          <q-input filled dense readonly :model-value="num(details.headingDeg, ' °')" label="Курс" />
        </div>
        <div class="col-12 col-md-3">
          <q-input filled dense readonly :model-value="details.locationSource" label="Источник локации" />
        </div>
        <div class="col-12 col-md-3">
          <q-toggle dense :model-value="!!details.locationIsMock" label="Искусственная локация" disable />
        </div>
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="fmtTs(details.locationTs)" label="Время локации (устройство)" />
        </div>
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="fmtTs(details.locationReceivedAt)" label="Время получения (сервер)" />
        </div>
      </div>

      <!-- Подразделение -->
      <div class="row q-col-gutter-md">
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="details.departmentId" label="Подразделение (ID)" />
        </div>
        <div class="col-12 col-md-6">
          <q-input filled dense readonly :model-value="fmtTs(details.departmentAssignedAt)" label="Назначено в подразделение" />
        </div>
      </div>
    </q-card-section>
    <q-separator />

    <!-- События -->
    <q-card-section class="q-gutter-sm">
      <div class="row items-center no-wrap">
        <div class="text-subtitle2">Последние события</div>
        <q-space />
        <q-btn
          flat dense round icon="refresh"
          :loading="eventsLoading"
          @click="fetchEvents"
        />
      </div>

      <q-skeleton v-if="eventsLoading && events.length === 0" type="text" />
      <q-skeleton v-if="eventsLoading && events.length === 0" type="text" />
      <q-skeleton v-if="eventsLoading && events.length === 0" type="text" />

      <div v-if="!eventsLoading && events.length === 0" class="text-grey-7">
        Событий пока нет.
      </div>

      <q-list
        v-else
        bordered
        class="rounded-borders"
        style="max-height: 240px; overflow:auto"
        dense
      >
        <q-item v-for="ev in events" :key="ev.id">
          <q-item-section>
            <div class="row items-center no-wrap">
              <div class="text-weight-medium">
                {{ eventLabel(ev.event) }}
              </div>
              <q-separator vertical inset class="q-mx-sm" />
              <div class="text-caption text-grey-7">
                {{ fmtTs(ev.occurredAt) }}
              </div>
            </div>
          </q-item-section>
          <q-item-section side v-if="ev.severity">
            <q-badge outline :label="ev.severity" />
          </q-item-section>
        </q-item>
      </q-list>

      <div class="row justify-end q-mt-sm" v-if="canLoadMore">
        <q-btn flat no-caps label="Показать ещё" @click="loadMore" :disable="eventsLoading" />
      </div>
    </q-card-section>
  </q-card>
</template>

<script setup>
import { ref, watch } from 'vue'
import { apiClient } from "src/services/apiClient.js"

const props = defineProps({
  deviceId: { type: String, required: true }
})

const loading = ref(false)
const details = ref({})
const events = ref([])
const eventsLoading = ref(false)
const page = ref(0)
const pageSize = 50
const canLoadMore = ref(false)

function fmtTs(ts) {
  if (!ts) return ''
  try { return new Date(ts).toLocaleString() } catch { return String(ts) }
}
function fmtMb(mb) {
  const v = Number(mb)
  if (Number.isNaN(v)) return ''
  if (v >= 1024) return (v / 1024).toFixed(1) + ' ГБ'
  return v + ' МБ'
}
function fmtTemp(t) {
  const v = Number(t); if (Number.isNaN(v)) return ''
  return v.toFixed(1) + ' °C'
}
function pct(batt) {
  const v = Number(batt); if (Number.isNaN(v)) return ''
  // если приходит 0..100 — убери умножение
  return (v >= 0 && v <= 1 ? (v * 100).toFixed(0) : v.toFixed(0)) + ' %'
}
function join(a, b, sep = ' ') {
  if (!a && !b) return ''
  if (!a) return String(b)
  if (!b) return String(a)
  return `${a}${sep}${b}`
}
function num(v, suffix = '') {
  const n = Number(v); if (Number.isNaN(n)) return ''
  return n + suffix
}

async function fetchEvents(reset = true) {
  eventsLoading.value = true
  try {
    if (reset) {
      page.value = 0
      events.value = []
    }
    const { data } = await apiClient.get(`/devices/${props.deviceId}/events`, {
      params: { page: page.value, size: pageSize }
    })
    const list = Array.isArray(data?.content) ? data.content : (Array.isArray(data) ? data : [])
    events.value = reset ? list : events.value.concat(list)

    // для Page<> от Spring
    if (typeof data?.totalPages === 'number' && typeof data?.number === 'number') {
      canLoadMore.value = (data.number + 1) < data.totalPages
    } else {
      // фолбэк, если вернулся просто массив
      canLoadMore.value = list.length === pageSize
    }
  } finally {
    eventsLoading.value = false
  }
}

function loadMore() {
  page.value += 1
  fetchEvents(false)
}

function eventLabel(eventEnum) {
  // человекочитаемые подписи; дополни своими типами
  switch (eventEnum) {
    case 'BOOT_COMPLETED':
      return 'Загрузка завершена'
    case 'SHUTDOWN':
      return 'Выключение'
    case 'NETWORK_CHANGE':
      return 'Смена сети'
    case 'REGISTERED':
      return 'Устройство зарегистрированно'
    default:
      return String(eventEnum || 'event')
  }
}

async function fetchDetails() {
  loading.value = true
  try {
    const { data } = await apiClient.get(`/devices/${props.deviceId}`)
    details.value = data || {}

    await fetchEvents(true)
  } finally {
    loading.value = false
  }
}

watch(() => props.deviceId, () => fetchDetails(), { immediate: true })
</script>
