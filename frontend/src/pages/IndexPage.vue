<template>
  <q-page class="q-pa-md">
    <q-splitter v-model="split" horizontal :limits="[20, 80]">
      <!-- LEFT: список устройств + пагинация -->
      <template #before>
        <div class="column full-height">
          <div class="row items-center q-pb-sm">
            <q-btn
              color="primary"
              icon="add"
              label="Зарегистрировать новое"
              @click="preprovOpen = true"
            />
            <preprovision-dialog v-model="preprovOpen" @closed="reload" />
            <q-space />
            <q-btn
              flat round dense icon="refresh"
              :loading="loadingList"
              @click="reload"
              aria-label="Обновить"
            />
          </div>


          <q-list bordered class="col scroll">
            <q-item
              v-for="d in page.content"
              :key="d.deviceId"
              clickable
              :active="d.deviceId === selectedId"
              @click="select(d.deviceId)"
            >
              <q-item-section>
                <q-item-label class="text-weight-medium">
<!--                  {{ d.deviceName || d.inventoryNumber || shortId(d.deviceId) }}-->
                  {{ d.serialNumber || shortId(d.deviceId) }}
                </q-item-label>
                <q-item-label caption>
                  {{ d.manufacturer || '—' }} {{ d.model || '' }}
                </q-item-label>
              </q-item-section>
              <q-item-section side>
                <q-badge outline align="middle">{{ d.model || '—' }}</q-badge>
              </q-item-section>
              <q-item-section side>
                <q-btn
                  icon="delete"
                  color="negative"
                  flat
                  round
                  dense
                  @click.stop="confirmDelete(d)"
                />
              </q-item-section>
            </q-item>
            <q-item v-if="!loadingList && page.content.length === 0">
              <q-item-section>Устройств пока не создано.</q-item-section>
            </q-item>
          </q-list>

          <div class="row items-center justify-between q-pt-sm">
            <div class="text-caption">
              Всего: {{ page.totalElements }}
            </div>
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

      <!-- RIGHT: детали устройства -->
      <template #after>
        <div class="q-pa-md fit">
          <device-details-card
            v-if="selectedId"
            :device-id="selectedId"
            key="details"
          />
          <div v-else class="row items-center justify-center fit text-grey-6">
            Выберите устройство в списке слева
          </div>
        </div>
      </template>
    </q-splitter>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { apiClient } from "src/services/apiClient.js"
import DeviceDetailsCard from 'src/components/devices/DeviceDetailsCard.vue'
import PreprovisionDialog from 'src/components/devices/PreprovisionDialog.vue'

const $q = useQuasar()
const preprovOpen = ref(false)
const split = ref(35)
const loadingList = ref(false)
const uiPage = ref(1)            // UI 1-based
const size = ref(30)             // как в бэке @PageableDefault(size=30)
const page = ref({
  content: [],
  page: 0,
  size: size.value,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true
})

const selectedId = ref(null)

function shortId(uuid) {
  return String(uuid).split('-')[0]
}

async function loadList(p = 0) {
  loadingList.value = true
  try {
    const resp = await apiClient.get('/devices', {
      params: {
        page: p,           // backend ждёт 0-based
        size: size.value,
        sort: 'createdAt,desc'
      }
    })
    page.value = resp.data
    // если выбранный id отсутствует на новой странице — обнулим
    if (!page.value.content.some(d => d.deviceId === selectedId.value)) {
      // не сбрасываем выбранный насильно — просто оставим как есть
      // при желании: selectedId.value = null
    }
  } finally {
    loadingList.value = false
  }
}

function handlePageChange(newUiPage) {
  // UI -> backend: 1-based -> 0-based
  loadList(newUiPage - 1)
}

function reload() {
  loadList(uiPage.value - 1)
}

function select(id) {
  selectedId.value = id
}

function confirmDelete(device) {
  const title = device.serialNumber || shortId(device.deviceId)

  $q.dialog({
    title: 'Удалить устройство?',
    message: `Устройство ${title} будет удалено без возможности восстановления.`,
    persistent: true,
    ok: {
      label: 'Удалить',
      color: 'negative'
    },
    cancel: {
      label: 'Отмена',
      flat: true
    }
  }).onOk(() => {
    deleteDevice(device.deviceId)
  })
}

async function deleteDevice(deviceId) {
  try {
    $q.loading.show()
    console.log('id:' + deviceId)

    await apiClient.delete(`/devices/${deviceId}`)

    await reload()

    $q.notify({
      type: 'positive',
      message: 'Устройство удалено'
    })
  } catch (e){
    $q.notify({
      type: 'negative',
      message: 'Ошибка при удалении устройства'
    })
    console.log(e)
  } finally {
    $q.loading.hide()
  }
}

onMounted(() => {
  loadList(0)
})
</script>
