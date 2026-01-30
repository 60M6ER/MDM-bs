<template>
  <div class="column full-height" style="min-height: 0;">
    <div class="row items-center q-pb-sm">
      <q-btn color="primary" icon="add" label="Зарегистрировать новое" @click="preprovOpen = true" />
      <preprovision-dialog v-model="preprovOpen" @closed="reload" />
      <q-space />
      <q-btn flat round dense icon="refresh" :loading="loadingList" @click="reload" />
    </div>

    <q-list bordered class="col scroll" style="min-height: 0;">
      <q-item
        v-for="d in page.content"
        :key="d.deviceId"
        clickable
        :active="d.deviceId === selectedId"
        @click="$emit('select', d.deviceId)"
      >
        <q-item-section>
          <q-item-label class="text-weight-medium">
            {{ d.serialNumber || shortId(d.deviceId) }}
          </q-item-label>
          <q-item-label caption>
            {{ d.manufacturer || '—' }} {{ d.model || '' }}
          </q-item-label>
        </q-item-section>

        <q-item-section side>
          <q-btn icon="delete" color="negative" flat round dense @click.stop="confirmDelete(d)" />
        </q-item-section>
      </q-item>

      <q-item v-if="!loadingList && page.content.length === 0">
        <q-item-section>Устройств пока не создано.</q-item-section>
      </q-item>
    </q-list>

    <div class="row items-center justify-between q-pt-sm" style="min-height: 0;">
      <div class="text-caption">Всего: {{ page.totalElements }}</div>
      <q-pagination
        v-model="uiPage"
        :max="Math.max(page.totalPages, 1)"
        :max-pages="7"
        boundary-numbers
        size="sm"
        @update:model-value="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { apiClient } from 'src/services/apiClient.js'
import PreprovisionDialog from 'src/components/devices/PreprovisionDialog.vue'

defineProps({
  selectedId: { type: String, default: null }
})
defineEmits(['select'])

const $q = useQuasar()
const preprovOpen = ref(false)
const loadingList = ref(false)
const uiPage = ref(1)
const size = ref(30)
const page = ref({ content: [], page: 0, size: 30, totalElements: 0, totalPages: 0 })

function shortId(uuid) { return String(uuid).split('-')[0] }

async function loadList(p = 0) {
  loadingList.value = true
  try {
    const resp = await apiClient.get('/devices', { params: { page: p, size: size.value, sort: 'createdAt,desc' } })
    page.value = resp.data
  } finally {
    loadingList.value = false
  }
}
function handlePageChange(newUiPage) { loadList(newUiPage - 1) }
function reload() { loadList(uiPage.value - 1) }

function confirmDelete(device) {
  const title = device.serialNumber || shortId(device.deviceId)
  $q.dialog({
    title: 'Удалить устройство?',
    message: `Устройство ${title} будет удалено без возможности восстановления.`,
    persistent: true,
    ok: { label: 'Удалить', color: 'negative' },
    cancel: { label: 'Отмена', flat: true }
  }).onOk(() => deleteDevice(device.deviceId))
}

async function deleteDevice(deviceId) {
  try {
    $q.loading.show()
    await apiClient.delete(`/devices/${deviceId}`)
    await reload()
    $q.notify({ type: 'positive', message: 'Устройство удалено' })
  } catch (e) {
    $q.notify({ type: 'negative', message: 'Ошибка при удалении устройства: ' + e })
  } finally {
    $q.loading.hide()
  }
}

onMounted(() => loadList(0))
</script>
