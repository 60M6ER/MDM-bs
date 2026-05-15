<template>
  <q-card flat bordered class="fit column">
    <q-card-section class="row items-center">
      <div class="text-subtitle1 text-weight-medium">Карточка приложения</div>
      <q-space />
      <q-btn
        v-if="details?.canReset"
        flat
        round
        dense
        color="negative"
        icon="close"
        :disable="loading || resettingApplication"
        :loading="resettingApplication"
        @click="confirmResetApplication"
      />
      <q-btn flat round dense icon="refresh" :loading="loading" @click="fetchDetails" />
    </q-card-section>

    <q-separator />

    <q-card-section class="col scroll q-pa-md">
      <div v-if="loading" class="column q-gutter-sm">
        <q-skeleton type="text" width="45%" />
        <q-skeleton type="text" width="70%" />
        <q-skeleton type="rect" height="140px" />
      </div>

      <div v-else-if="details" class="q-gutter-md">
        <div>
          <div class="text-h6">{{ details.name || details.key }}</div>
          <div class="text-caption text-grey-7 q-mt-xs">{{ details.key }}</div>
        </div>

        <q-banner v-if="details.active" dense class="bg-green-1 text-green-9">
          Приложение активно
        </q-banner>
        <q-banner v-else dense class="bg-orange-1 text-orange-9">
          Приложение неактивно
        </q-banner>

        <div class="column q-gutter-sm">
          <q-input :model-value="details.name || ''" label="Название" readonly />
          <q-input :model-value="details.packageName || ''" label="Package name" readonly />

          <div class="row q-col-gutter-sm">
            <div class="col">
              <q-input :model-value="currentReleaseLabel" label="Текущая версия" readonly />
            </div>
            <div class="col-auto">
              <q-btn
                outline
                color="primary"
                label="Выбрать"
                class="q-mt-xs"
                :loading="releaseDialogLoading"
                @click="openReleaseDialog"
              />
            </div>
            <div class="col-auto">
              <q-btn
                flat
                color="primary"
                icon="download"
                class="q-mt-xs"
                :disable="!currentRelease"
                @click="downloadCurrentRelease"
              />
            </div>
          </div>

          <q-input :model-value="details.createdAt ? formatDate(details.createdAt) : ''" label="Создано" readonly />
          <q-input :model-value="details.id ?? ''" label="ID" readonly />
        </div>
      </div>

      <div v-else class="row items-center justify-center fit text-grey-6">
        Не удалось загрузить приложение
      </div>
    </q-card-section>

    <q-dialog v-model="releaseDialogOpen" persistent>
      <q-card style="width: 760px; max-width: 95vw;">
        <q-card-section class="row items-center">
          <div class="text-subtitle1 text-weight-medium">Выбор текущего релиза</div>
          <q-space />
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>

        <q-separator />

        <q-card-section class="q-gutter-md">
          <q-input
            v-model="releaseSearch"
            dense
            outlined
            clearable
            debounce="300"
            label="Поиск по версии или сборке"
          >
            <template #prepend>
              <q-icon name="search" />
            </template>
          </q-input>

          <q-list bordered separator>
            <q-item
              v-for="release in releasePage.content"
              :key="release.id"
              clickable
              :active="release.id === currentRelease?.id"
              active-class="bg-green-1 text-green-9"
              @click="selectCurrentRelease(release)"
            >
              <q-item-section>
                <q-item-label class="text-weight-medium">
                  {{ release.versionName || 'Без versionName' }}
                </q-item-label>
                <q-item-label caption>
                  Сборка {{ release.versionCode }}
                </q-item-label>
                <q-item-label caption>
                  Загружен {{ formatDate(release.uploadedAt) }}
                </q-item-label>
              </q-item-section>

              <q-item-section side v-if="release.id === currentRelease?.id">
                <q-badge color="green-2" text-color="green-9" label="Текущая" />
              </q-item-section>
            </q-item>

            <q-item v-if="!releaseDialogLoading && releasePage.content.length === 0">
              <q-item-section>Релизов пока нет.</q-item-section>
            </q-item>
          </q-list>

          <div class="row items-center justify-between">
            <div class="text-caption">Всего: {{ releasePage.totalElements }}</div>
            <q-pagination
              v-model="releaseUiPage"
              :max="Math.max(releasePage.totalPages, 1)"
              :max-pages="7"
              boundary-numbers
              size="sm"
              @update:model-value="handleReleasePageChange"
            />
          </div>
        </q-card-section>
      </q-card>
    </q-dialog>
  </q-card>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { notifyApiError } from 'src/services/apiErrors.js'
import { apiClient } from 'src/services/apiClient.js'

const props = defineProps({
  applicationId: { type: Number, required: true }
})

const emit = defineEmits(['current-release-updated', 'refresh-requested'])

const $q = useQuasar()
const loading = ref(false)
const details = ref(null)
const currentRelease = ref(null)
const releaseDialogOpen = ref(false)
const releaseDialogLoading = ref(false)
const assigningRelease = ref(false)
const resettingApplication = ref(false)
const releaseSearch = ref('')
const releaseUiPage = ref(1)
const releasePage = ref({
  content: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0
})

const currentReleaseLabel = computed(() => {
  if (!currentRelease.value) {
    return ''
  }
  const versionName = currentRelease.value.versionName || 'Без versionName'
  return `${versionName} / сборка ${currentRelease.value.versionCode}`
})

function formatDate(value) {
  try {
    return new Date(value).toLocaleString()
  } catch {
    return String(value || '')
  }
}

async function fetchCurrentRelease() {
  try {
    const { data } = await apiClient.get(`/applications/${props.applicationId}/current-release`)
    currentRelease.value = data || null
    emit('current-release-updated', currentRelease.value)
  } catch (err) {
    if (err?.response?.status === 404) {
      currentRelease.value = null
      emit('current-release-updated', null)
      return
    }
    throw err
  }
}

async function fetchDetails() {
  loading.value = true
  try {
    const [detailsResponse] = await Promise.all([
      apiClient.get(`/applications/${props.applicationId}`),
      fetchCurrentRelease()
    ])
    details.value = detailsResponse.data || null
  } finally {
    loading.value = false
  }
}

async function confirmResetApplication() {
  if (!details.value?.id || resettingApplication.value) {
    return
  }

  const confirmed = await new Promise((resolve) => {
    $q.dialog({
      title: 'Обнулить приложение?',
      message: 'Будут удалены все релизы, package name и текущая версия. Карточка приложения останется.',
      persistent: true,
      ok: {
        color: 'negative',
        label: 'Обнулить'
      },
      cancel: {
        flat: true,
        label: 'Отмена'
      }
    })
      .onOk(() => resolve(true))
      .onCancel(() => resolve(false))
      .onDismiss(() => resolve(false))
  })

  if (!confirmed) {
    return
  }

  resettingApplication.value = true
  try {
    await apiClient.delete(`/applications/${props.applicationId}/reset`)
    currentRelease.value = null
    emit('current-release-updated', null)
    emit('refresh-requested')
    await fetchDetails()
    $q.notify({ type: 'positive', message: 'Приложение обнулено' })
  } catch (err) {
    notifyApiError($q, err)
  } finally {
    resettingApplication.value = false
  }
}

async function loadReleasePage(p = 0) {
  releaseDialogLoading.value = true
  try {
    const { data } = await apiClient.get('/applications/releases', {
      params: {
        appId: props.applicationId,
        search: releaseSearch.value || undefined,
        page: p,
        size: releasePage.value.size || 10,
        sort: 'uploadedAt,desc'
      }
    })
    releasePage.value = data
  } finally {
    releaseDialogLoading.value = false
  }
}

async function openReleaseDialog() {
  releaseDialogOpen.value = true
  releaseUiPage.value = 1
  await loadReleasePage(0)
}

async function selectCurrentRelease(release) {
  if (!release || assigningRelease.value) {
    return
  }

  assigningRelease.value = true
  try {
    const { data } = await apiClient.put(`/applications/${props.applicationId}/current-release`, {
      releaseId: release.id
    })
    currentRelease.value = data || release
    emit('current-release-updated', currentRelease.value)
    emit('refresh-requested')
    releaseDialogOpen.value = false
    $q.notify({ type: 'positive', message: 'Текущий релиз обновлен' })
  } catch (err) {
    notifyApiError($q, err)
  } finally {
    assigningRelease.value = false
  }
}

function downloadCurrentRelease() {
  if (!currentRelease.value?.id) {
    return
  }
  window.open(`/api/v1/applications/releases/${currentRelease.value.id}/file`, '_blank', 'noopener,noreferrer')
}

function handleReleasePageChange(newUiPage) {
  loadReleasePage(newUiPage - 1)
}

watch(() => props.applicationId, () => {
  details.value = null
  currentRelease.value = null
  fetchDetails()
}, { immediate: true })

watch(releaseSearch, () => {
  if (!releaseDialogOpen.value) {
    return
  }
  releaseUiPage.value = 1
  loadReleasePage(0)
})
</script>
