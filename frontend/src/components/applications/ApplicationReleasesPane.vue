<template>
  <div
    class="fit releases-pane"
    :class="{ 'releases-pane--dragover': isDropHighlightVisible }"
    @dragenter.prevent="onDragEnter"
    @dragover.prevent="onDragOver"
    @dragleave.prevent="onDragLeave"
    @drop.prevent="onDrop"
  >
    <div v-if="isDropHighlightVisible" class="releases-pane__overlay">
      <div class="releases-pane__overlay-text">Перетащите APK-файл для загрузки</div>
    </div>

    <q-card flat bordered class="fit column">
    <q-card-section class="row items-center">
      <div class="text-subtitle1 text-weight-medium">Релизы</div>
      <q-space />
      <q-btn
        flat
        round
        dense
        icon="refresh"
        :loading="loading"
        :disable="!applicationId"
        @click="reload"
      />
    </q-card-section>

    <q-separator />

    <q-card-section class="q-pa-md">
      <div class="row items-center q-col-gutter-sm q-mb-md">
        <div class="col">
          <q-btn
            color="primary"
            icon="upload"
            label="Добавить"
            :disable="!applicationId"
            :loading="uploading"
            @click="openFileDialog"
          />
        </div>
      </div>

      <q-input
        v-model="search"
        dense
        outlined
        clearable
        debounce="300"
        label="Поиск по версии или сборке"
        :disable="!applicationId"
      >
        <template #prepend>
          <q-icon name="search" />
        </template>
      </q-input>

      <input
        ref="fileInput"
        type="file"
        accept=".apk,application/vnd.android.package-archive"
        class="hidden"
        @change="onFileSelected"
      >
    </q-card-section>

    <q-separator />

    <q-card-section class="col scroll q-pa-none">
      <div v-if="!applicationId" class="row items-center justify-center full-height text-grey-6 q-pa-md">
        Выберите приложение, чтобы работать с релизами
      </div>

      <div v-else-if="loading && releases.length === 0" class="q-pa-md column q-gutter-sm">
        <q-skeleton type="rect" height="76px" />
        <q-skeleton type="rect" height="76px" />
        <q-skeleton type="rect" height="76px" />
      </div>

      <q-list v-else bordered separator>
        <q-item v-for="release in releases" :key="release.id" clickable class="release-item">
          <div v-if="release.id === currentReleaseId" class="release-current-badge">Текущая</div>

          <q-item-section>
            <q-item-label class="text-weight-medium">
              <span v-html="highlightMatch(release.versionName || 'Без versionName')" />
            </q-item-label>
            <q-item-label caption>
              <span v-html="highlightBuild(release.versionCode)" />
            </q-item-label>
            <q-item-label caption>
              Загружен {{ formatDate(release.uploadedAt) }}
            </q-item-label>
            <q-item-label caption>
              {{ formatSize(release.sizeBytes) }}
            </q-item-label>

            <div class="q-mt-sm">
              <q-btn
                flat
                dense
                no-caps
                icon="download"
                label="Скачать"
                color="primary"
                :href="buildDownloadUrl(release.id)"
                target="_blank"
                rel="noopener noreferrer"
              />
            </div>
          </q-item-section>
        </q-item>

        <q-item v-if="!loading && releases.length === 0">
          <q-item-section>Релизов пока нет.</q-item-section>
        </q-item>
      </q-list>
    </q-card-section>

    <q-separator />

    <q-card-section class="row items-center justify-between">
      <div class="text-caption">Всего: {{ page.totalElements }}</div>
      <q-pagination
        v-model="uiPage"
        :max="Math.max(page.totalPages, 1)"
        :max-pages="7"
        boundary-numbers
        size="sm"
        :disable="!applicationId"
        @update:model-value="handlePageChange"
      />
    </q-card-section>
    </q-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { apiClient } from 'src/services/apiClient.js'

const props = defineProps({
  applicationId: { type: Number, default: null },
  currentReleaseId: { type: Number, default: null }
})

const emit = defineEmits(['uploaded'])

const $q = useQuasar()
const loading = ref(false)
const uploading = ref(false)
const isDragOver = ref(false)
const isPageFileDragActive = ref(false)
const dragDepth = ref(0)
const search = ref('')
const fileInput = ref(null)
const uiPage = ref(1)
const page = ref({
  content: [],
  page: 0,
  size: 30,
  totalElements: 0,
  totalPages: 0
})

const releases = computed(() => page.value.content || [])
const isDropHighlightVisible = computed(() =>
  Boolean(props.applicationId) && (isDragOver.value || isPageFileDragActive.value)
)

function formatDate(value) {
  try {
    return value ? new Date(value).toLocaleString() : '—'
  } catch {
    return String(value || '—')
  }
}

function formatSize(sizeBytes) {
  if (typeof sizeBytes !== 'number') return 'Размер неизвестен'
  if (sizeBytes < 1024) return `${sizeBytes} Б`
  if (sizeBytes < 1024 * 1024) return `${(sizeBytes / 1024).toFixed(1)} КБ`
  if (sizeBytes < 1024 * 1024 * 1024) return `${(sizeBytes / (1024 * 1024)).toFixed(1)} МБ`
  return `${(sizeBytes / (1024 * 1024 * 1024)).toFixed(1)} ГБ`
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function highlightMatch(value) {
  const safeValue = escapeHtml(value)
  const normalizedSearch = String(search.value || '').trim()

  if (!normalizedSearch) {
    return safeValue
  }

  const pattern = new RegExp(`(${escapeRegExp(normalizedSearch)})`, 'ig')
  return safeValue.replace(pattern, '<mark class="release-highlight">$1</mark>')
}

function highlightBuild(versionCode) {
  return highlightMatch(`Сборка ${versionCode ?? '—'}`)
}

function buildDownloadUrl(releaseId) {
  return `/api/v1/applications/releases/${releaseId}/file`
}

function getErrorMessage(err) {
  return err?.response?.data?.message || err?.message || 'Произошла ошибка при загрузке релиза'
}

function isApkFile(file) {
  if (!file) return false
  const name = String(file.name || '').toLowerCase()
  const type = String(file.type || '').toLowerCase()
  return name.endsWith('.apk') || type === 'application/vnd.android.package-archive'
}

function openFileDialog() {
  fileInput.value?.click()
}

async function uploadFile(file) {
  if (!file || !props.applicationId) {
    return
  }

  if (!isApkFile(file)) {
    $q.notify({ type: 'warning', message: 'Можно загружать только APK-файлы' })
    return
  }

  const formData = new FormData()
  const requestPayload = {
    appId: props.applicationId
  }

  formData.append(
    'request',
    new Blob([JSON.stringify(requestPayload)], { type: 'application/json' })
  )
  formData.append('file', file)

  uploading.value = true
  try {
    await apiClient.post('/applications/releases', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    uiPage.value = 1
    await loadReleases(0)
    emit('uploaded')
    $q.notify({ type: 'positive', message: 'Релиз загружен' })
  } catch (err) {
    $q.notify({ type: 'negative', message: getErrorMessage(err) })
  } finally {
    uploading.value = false
  }
}

async function onFileSelected(event) {
  const file = event?.target?.files?.[0]
  event.target.value = ''
  await uploadFile(file)
}

function hasFiles(event) {
  const types = event?.dataTransfer?.types
  return Array.isArray(types)
    ? types.includes('Files')
    : Array.from(types || []).includes('Files')
}

function activatePageDrag(event) {
  if (!props.applicationId || uploading.value) return
  if (!hasFiles(event)) return
  isPageFileDragActive.value = true
}

function deactivatePageDrag() {
  isPageFileDragActive.value = false
}

function onDragEnter(event) {
  if (!props.applicationId || uploading.value) return
  if (!hasFiles(event)) return
  isPageFileDragActive.value = true
  dragDepth.value += 1
  isDragOver.value = true
}

function onDragOver(event) {
  if (!props.applicationId || uploading.value) return
  if (!hasFiles(event)) return
  isPageFileDragActive.value = true
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'copy'
  }
  isDragOver.value = true
}

function onDragLeave(event) {
  if (!props.applicationId || uploading.value) return
  if (!hasFiles(event)) return
  dragDepth.value = Math.max(0, dragDepth.value - 1)
  if (dragDepth.value === 0) {
    isDragOver.value = false
  }
}

async function onDrop(event) {
  if (!props.applicationId || uploading.value) return
  if (!hasFiles(event)) return

  dragDepth.value = 0
  isDragOver.value = false
  isPageFileDragActive.value = false

  const file = event?.dataTransfer?.files?.[0]
  await uploadFile(file)
}

function onWindowDragLeave(event) {
  if (event.clientX <= 0 || event.clientY <= 0 || event.clientX >= window.innerWidth || event.clientY >= window.innerHeight) {
    dragDepth.value = 0
    isDragOver.value = false
    deactivatePageDrag()
  }
}

function onWindowDrop() {
  dragDepth.value = 0
  isDragOver.value = false
  deactivatePageDrag()
}

async function loadReleases(p = 0) {
  if (!props.applicationId) {
    page.value = { content: [], page: 0, size: 30, totalElements: 0, totalPages: 0 }
    return
  }

  loading.value = true
  try {
    const { data } = await apiClient.get('/applications/releases', {
      params: {
        appId: props.applicationId,
        search: search.value || undefined,
        page: p,
        size: page.value.size || 30,
        sort: 'uploadedAt,desc'
      }
    })
    page.value = data
  } finally {
    loading.value = false
  }
}

function handlePageChange(newUiPage) {
  loadReleases(newUiPage - 1)
}

function reload() {
  loadReleases(uiPage.value - 1)
}

watch(() => props.applicationId, () => {
  dragDepth.value = 0
  isDragOver.value = false
  isPageFileDragActive.value = false
  uiPage.value = 1
  search.value = ''
  loadReleases(0)
}, { immediate: true })

watch(search, () => {
  uiPage.value = 1
  loadReleases(0)
})

onMounted(() => {
  window.addEventListener('dragenter', activatePageDrag)
  window.addEventListener('dragover', activatePageDrag)
  window.addEventListener('dragleave', onWindowDragLeave)
  window.addEventListener('drop', onWindowDrop)
})

onBeforeUnmount(() => {
  window.removeEventListener('dragenter', activatePageDrag)
  window.removeEventListener('dragover', activatePageDrag)
  window.removeEventListener('dragleave', onWindowDragLeave)
  window.removeEventListener('drop', onWindowDrop)
})
</script>

<style scoped>
.releases-pane {
  position: relative;
  overflow: hidden;
}

.releases-pane--dragover {
  border-color: var(--q-primary);
}

.releases-pane__overlay {
  position: absolute;
  inset: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(25, 118, 210, 0.12);
  border: 2px dashed var(--q-primary);
  pointer-events: none;
}

.releases-pane__overlay-text {
  padding: 12px 18px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--q-primary);
  font-weight: 600;
}

:deep(.release-item) {
  position: relative;
  align-items: flex-start;
  padding-top: 14px;
  padding-right: 96px;
  padding-bottom: 12px;
}

.release-current-badge {
  position: absolute;
  top: 10px;
  right: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #dcedc8;
  color: #2e7d32;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

:deep(.release-highlight) {
  padding: 0 2px;
  border-radius: 3px;
  background: #ffe082;
  color: inherit;
}
</style>
