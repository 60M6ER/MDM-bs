<template>
  <q-dialog
    v-model="isOpen"
    maximized="false"
    persistent
    @show="handleShow"
    @hide="handleHide"
  >
    <q-card class="logs-dialog">
      <q-card-section class="logs-dialog__toolbar row items-center q-col-gutter-md">
        <div class="col">
          <div class="text-subtitle1 text-weight-medium">Логи сервера</div>
          <div class="text-caption text-grey-5">
            На экране: {{ lines.length }} • В файле: {{ totalLines }} • Диапазон: {{ visibleRangeLabel }}<span v-if="searchSummary"> • {{ searchSummary }}</span>
          </div>
        </div>

        <div class="col-12 col-md-5">
          <q-input
            v-model="searchQuery"
            dense
            outlined
            clearable
            dark
            color="white"
            label="Поиск по логам"
            @keydown.enter.prevent="runSearch"
            @clear="clearSearch"
          >
            <template #append>
              <q-btn flat round dense icon="search" color="white" @click="runSearch" />
            </template>
          </q-input>
        </div>

        <div class="col-auto row q-gutter-sm">
          <div class="logs-dialog__live-toggle row items-center no-wrap">
            <q-icon name="sync" size="18px" color="grey-5" />
            <q-toggle
              v-model="liveUpdatesEnabled"
              dense
              dark
              color="green"
              keep-color
            />
          </div>
          <q-btn
            flat
            dense
            color="white"
            icon="keyboard_arrow_up"
            :disable="searchMatches.length === 0 || searchLoading"
            @click="goToPreviousMatch"
          />
          <q-btn
            flat
            dense
            color="white"
            icon="keyboard_arrow_down"
            :disable="searchMatches.length === 0 || searchLoading"
            @click="goToNextMatch"
          />
          <q-btn
            flat
            dense
            color="white"
            icon="close"
            @click="isOpen = false"
          />
        </div>
      </q-card-section>

      <q-separator dark />

      <q-card-section class="logs-dialog__content q-pa-none">
        <div ref="viewportRef" class="logs-dialog__viewport" @scroll="handleScroll">
          <div v-if="olderLoading" class="logs-dialog__status">Подгружаю более старые строки…</div>

          <div v-if="initialLoading" class="logs-dialog__status logs-dialog__status--center">
            Загружаю логи…
          </div>

          <div v-else-if="lines.length === 0" class="logs-dialog__status logs-dialog__status--center">
            Логи пока пусты.
          </div>

          <div v-else class="logs-dialog__lines">
            <div
              v-for="line in lines"
              :key="line.lineNumber"
              :ref="(el) => setLineRef(line.lineNumber, el)"
              class="logs-dialog__line"
              :class="{
                'logs-dialog__line--active': line.lineNumber === activeMatchLine,
                'logs-dialog__line--warn': getLineLevel(line.text) === 'warn',
                'logs-dialog__line--error': getLineLevel(line.text) === 'error'
              }"
            >
              <span class="logs-dialog__line-number">{{ line.lineNumber }}</span>
              <span class="logs-dialog__line-text" v-html="renderLine(line.text)" />
            </div>
          </div>

          <div v-if="showNewerLoading" class="logs-dialog__status">Подгружаю новые строки…</div>
        </div>

        <q-btn
          v-if="showScrollToBottom"
          class="logs-dialog__scroll-bottom"
          round
          unelevated
          color="grey-8"
          text-color="white"
          icon="keyboard_arrow_down"
          @click="jumpToLatest()"
        />
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { apiClient } from 'src/services/apiClient.js'
import { notifyApiError } from 'src/services/apiErrors.js'

const isOpen = defineModel({ type: Boolean, default: false })

const $q = useQuasar()
const viewportRef = ref(null)
const lineRefs = new Map()
const lines = ref([])
const totalLines = ref(0)
const hasOlder = ref(false)
const hasNewer = ref(false)
const initialLoading = ref(false)
const olderLoading = ref(false)
const newerLoading = ref(false)
const showNewerLoading = ref(false)
const searchLoading = ref(false)
const searchQuery = ref('')
const searchMatches = ref([])
const activeMatchIndex = ref(-1)
const pollTimerId = ref(null)
const liveUpdatesEnabled = ref(true)
const showScrollToBottom = ref(false)
const isRestoringPosition = ref(false)

const INITIAL_LIMIT = 100
const PAGE_LIMIT = 100
const MAX_RECORDS_IN_MEMORY = 1000
const BOTTOM_THRESHOLD_PX = 48

const activeMatchLine = computed(() => {
  if (activeMatchIndex.value < 0 || activeMatchIndex.value >= searchMatches.value.length) {
    return null
  }
  return searchMatches.value[activeMatchIndex.value]
})

const searchSummary = computed(() => {
  if (!searchQuery.value.trim()) {
    return ''
  }
  if (searchLoading.value) {
    return 'поиск…'
  }
  if (searchMatches.value.length === 0) {
    return 'совпадений нет'
  }
  return `${activeMatchIndex.value + 1} из ${searchMatches.value.length}`
})

const visibleRangeLabel = computed(() => {
  if (lines.value.length === 0) {
    return '—'
  }

  const firstLineNumber = lines.value[0]?.lineNumber
  const lastLineNumber = lines.value[lines.value.length - 1]?.lineNumber

  if (firstLineNumber == null || lastLineNumber == null) {
    return '—'
  }

  return `${firstLineNumber}-${lastLineNumber}`
})

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

function renderLine(text) {
  const safe = escapeHtml(text ?? '')
  const query = searchQuery.value.trim()

  if (!query) {
    return safe
  }

  const pattern = new RegExp(`(${escapeRegExp(query)})`, 'ig')
  return safe.replace(pattern, '<mark class="logs-dialog__highlight">$1</mark>')
}

function getLineLevel(text) {
  const normalized = String(text || '').toUpperCase()
  if (normalized.includes(' ERROR ')) {
    return 'error'
  }
  if (normalized.includes(' WARN ')) {
    return 'warn'
  }
  return null
}

function setLineRef(lineNumber, element) {
  if (element) {
    lineRefs.set(lineNumber, element)
  } else {
    lineRefs.delete(lineNumber)
  }
}

function mergeLines(incomingLines) {
  if (!Array.isArray(incomingLines) || incomingLines.length === 0) {
    return
  }

  const merged = new Map(lines.value.map((line) => [line.lineNumber, line]))
  for (const line of incomingLines) {
    merged.set(line.lineNumber, line)
  }

  lines.value = Array.from(merged.values()).sort((a, b) => a.lineNumber - b.lineNumber)
}

function appendNewerLines(incomingLines) {
  if (!Array.isArray(incomingLines) || incomingLines.length === 0) {
    return
  }

  const knownLineNumbers = new Set(lines.value.map((line) => line.lineNumber))
  const toAppend = incomingLines
    .filter((line) => !knownLineNumbers.has(line.lineNumber))
    .sort((a, b) => a.lineNumber - b.lineNumber)

  if (toAppend.length > 0) {
    lines.value.push(...toAppend)
  }
}

function replaceLines(incomingLines) {
  lines.value = Array.isArray(incomingLines)
    ? [...incomingLines].sort((a, b) => a.lineNumber - b.lineNumber)
    : []
}

function getFirstLoadedLine() {
  return lines.value.length > 0 ? lines.value[0].lineNumber : null
}

function getLastLoadedLine() {
  return lines.value.length > 0 ? lines.value[lines.value.length - 1].lineNumber : null
}

function isNearBottom() {
  const viewport = viewportRef.value
  if (!viewport) return true
  return viewport.scrollHeight - viewport.clientHeight - viewport.scrollTop < BOTTOM_THRESHOLD_PX
}

function updateScrollToBottomVisibility() {
  showScrollToBottom.value = !isNearBottom()
}

function trimLinesIfNeeded(direction = 'newer') {
  const viewport = viewportRef.value
  if (!viewport || lines.value.length <= MAX_RECORDS_IN_MEMORY) {
    return
  }

  if (direction === 'older') {
    trimBottomLinesIfNeeded(viewport)
    return
  }

  trimTopLinesIfNeeded(viewport)
}

function trimTopLinesIfNeeded(viewport) {
  let removed = 0
  let removedHeight = 0
  while (lines.value.length - removed > MAX_RECORDS_IN_MEMORY) {
    const candidate = lines.value[removed]
    const element = lineRefs.get(candidate.lineNumber)
    if (!element) {
      removed += 1
      continue
    }

    const bottom = element.offsetTop + element.offsetHeight
    if (bottom < viewport.scrollTop) {
      removedHeight += element.offsetHeight
      removed += 1
      continue
    }

    break
  }

  if (removed > 0) {
    lines.value = lines.value.slice(removed)
    viewport.scrollTop = Math.max(0, viewport.scrollTop - removedHeight)
  }
}

function trimBottomLinesIfNeeded(viewport) {
  let keepUntil = lines.value.length
  const visibleBottom = viewport.scrollTop + viewport.clientHeight

  while (keepUntil > MAX_RECORDS_IN_MEMORY) {
    const candidate = lines.value[keepUntil - 1]
    const element = lineRefs.get(candidate.lineNumber)
    if (!element) {
      keepUntil -= 1
      continue
    }

    if (element.offsetTop > visibleBottom) {
      keepUntil -= 1
      continue
    }

    break
  }

  if (keepUntil < lines.value.length) {
    lines.value = lines.value.slice(0, keepUntil)
  }
}

async function fetchLogs(params) {
  const { data } = await apiClient.get('/system/logs', { params })
  totalLines.value = data?.totalLines ?? 0
  hasOlder.value = !!data?.hasOlder
  hasNewer.value = !!data?.hasNewer
  return data?.lines || []
}

async function loadLatest(limit = INITIAL_LIMIT) {
  initialLoading.value = true
  try {
    const chunk = await fetchLogs({ mode: 'latest', limit })
    replaceLines(chunk)
    await forceScrollToBottomOnOpen()
  } catch (err) {
    notifyApiError($q, err, 'Не удалось загрузить логи')
  } finally {
    initialLoading.value = false
  }
}

async function loadOlder() {
  if (olderLoading.value || !hasOlder.value) {
    return
  }

  const firstLine = getFirstLoadedLine()
  if (!firstLine) {
    return
  }

  olderLoading.value = true
  const viewport = viewportRef.value
  const previousHeight = viewport?.scrollHeight ?? 0
  const previousTop = viewport?.scrollTop ?? 0

  try {
    const chunk = await fetchLogs({ mode: 'older', line: firstLine, limit: PAGE_LIMIT })
    mergeLines(chunk)
    await nextTick()
    if (viewport) {
      viewport.scrollTop = viewport.scrollHeight - previousHeight + previousTop
    }
    trimLinesIfNeeded('older')
    await nextTick()
    updateScrollToBottomVisibility()
  } catch (err) {
    notifyApiError($q, err, 'Не удалось подгрузить старые строки логов')
  } finally {
    olderLoading.value = false
  }
}

async function loadNewer(options = {}) {
  const { silent = false } = options

  if (newerLoading.value || !isOpen.value) {
    return
  }

  const lastLine = getLastLoadedLine()
  if (!lastLine) {
    return
  }

  newerLoading.value = true
  showNewerLoading.value = !silent
  const shouldStickToBottom = isNearBottom()

  try {
    const chunk = await fetchLogs({ mode: 'newer', line: lastLine, limit: PAGE_LIMIT })
    appendNewerLines(chunk)
    await nextTick()
    trimLinesIfNeeded('newer')
    await nextTick()
    if (shouldStickToBottom) {
      scrollToBottom()
    } else {
      updateScrollToBottomVisibility()
    }
  } catch (err) {
    notifyApiError($q, err, 'Не удалось подгрузить новые строки логов')
  } finally {
    newerLoading.value = false
    showNewerLoading.value = false
  }
}

function getVisibleLineCount() {
  const viewport = viewportRef.value
  if (!viewport || lines.value.length === 0) {
    return INITIAL_LIMIT
  }

  const top = viewport.scrollTop
  const bottom = top + viewport.clientHeight
  let count = 0

  for (const line of lines.value) {
    const element = lineRefs.get(line.lineNumber)
    if (!element) {
      continue
    }

    const elementTop = element.offsetTop
    const elementBottom = elementTop + element.offsetHeight
    const isVisible = elementBottom >= top && elementTop <= bottom
    if (isVisible) {
      count += 1
    }
  }

  return Math.min(Math.max(count, INITIAL_LIMIT), MAX_RECORDS_IN_MEMORY)
}

async function jumpToLatest() {
  const limit = getVisibleLineCount()
  await loadLatest(limit)
}

function scrollToBottom(smooth = false) {
  const viewport = viewportRef.value
  if (!viewport) return
  isRestoringPosition.value = true
  viewport.scrollTo({ top: viewport.scrollHeight, behavior: smooth ? 'smooth' : 'auto' })
  window.requestAnimationFrame(() => {
    isRestoringPosition.value = false
    updateScrollToBottomVisibility()
  })
}

async function forceScrollToBottomOnOpen() {
  await nextTick()
  scrollToBottom()

  window.requestAnimationFrame(() => {
    scrollToBottom()
  })

  window.setTimeout(() => {
    if (isOpen.value) {
      scrollToBottom()
    }
  }, 120)
}

function handleScroll(event) {
  if (isRestoringPosition.value || !isOpen.value) {
    return
  }

  const target = event?.target
  if (!target) {
    return
  }

  updateScrollToBottomVisibility()

  if (!initialLoading.value && target.scrollTop <= BOTTOM_THRESHOLD_PX) {
    loadOlder()
  }
}

async function runSearch() {
  const query = searchQuery.value.trim()
  if (!query) {
    clearSearch()
    return
  }

  searchLoading.value = true
  try {
    const { data } = await apiClient.get('/system/logs/search', {
      params: { query, limit: 500 }
    })
    searchMatches.value = data?.matches || []
    activeMatchIndex.value = searchMatches.value.length > 0 ? 0 : -1
    if (activeMatchIndex.value >= 0) {
      await focusMatch(activeMatchIndex.value)
    }
  } catch (err) {
    notifyApiError($q, err, 'Не удалось выполнить поиск по логам')
  } finally {
    searchLoading.value = false
  }
}

function clearSearch() {
  searchMatches.value = []
  activeMatchIndex.value = -1
}

async function goToPreviousMatch() {
  if (searchMatches.value.length === 0) {
    return
  }
  activeMatchIndex.value =
    (activeMatchIndex.value - 1 + searchMatches.value.length) % searchMatches.value.length
  await focusMatch(activeMatchIndex.value)
}

async function goToNextMatch() {
  if (searchMatches.value.length === 0) {
    return
  }
  activeMatchIndex.value = (activeMatchIndex.value + 1) % searchMatches.value.length
  await focusMatch(activeMatchIndex.value)
}

async function focusMatch(matchIndex) {
  const lineNumber = searchMatches.value[matchIndex]
  if (!lineNumber) {
    return
  }

  if (!lineRefs.has(lineNumber)) {
    try {
      const previousFirstLine = getFirstLoadedLine()
      const chunk = await fetchLogs({ mode: 'around', line: lineNumber, before: 50, after: 80 })
      mergeLines(chunk)
      await nextTick()
      trimLinesIfNeeded(lineNumber < (previousFirstLine ?? lineNumber) ? 'older' : 'newer')
      await nextTick()
    } catch (err) {
      notifyApiError($q, err, 'Не удалось перейти к найденной строке')
      return
    }
  }

  const element = lineRefs.get(lineNumber)
  const viewport = viewportRef.value
  if (!element || !viewport) {
    return
  }

  const top = element.offsetTop - viewport.clientHeight / 2 + element.clientHeight / 2
  viewport.scrollTo({ top: Math.max(0, top), behavior: 'smooth' })
}

function startPolling() {
  if (!liveUpdatesEnabled.value) {
    stopPolling()
    return
  }

  stopPolling()
  pollTimerId.value = window.setInterval(() => {
    if (isOpen.value && liveUpdatesEnabled.value) {
      loadNewer({ silent: true })
    }
  }, 3000)
}

function stopPolling() {
  if (pollTimerId.value !== null) {
    window.clearInterval(pollTimerId.value)
    pollTimerId.value = null
  }
}

async function handleShow() {
  liveUpdatesEnabled.value = true
  clearSearch()
  searchQuery.value = ''
  lineRefs.clear()
  lines.value = []
  totalLines.value = 0
  hasOlder.value = false
  hasNewer.value = false
  if (viewportRef.value) {
    viewportRef.value.scrollTop = 0
  }
  await loadLatest()
  updateScrollToBottomVisibility()
  startPolling()
}

function handleHide() {
  stopPolling()
  lineRefs.clear()
  showScrollToBottom.value = false
}

watch(isOpen, (open) => {
  if (!open) {
    stopPolling()
  }
})

watch(liveUpdatesEnabled, (enabled) => {
  if (!isOpen.value) {
    return
  }

  if (enabled) {
    startPolling()
  } else {
    stopPolling()
  }
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.logs-dialog {
  width: 80vw;
  max-width: none;
  height: 80vh;
  max-height: none;
  background: #050505;
  color: #fff;
  display: flex;
  flex-direction: column;
}

.logs-dialog__toolbar {
  flex: 0 0 auto;
}

.logs-dialog__content {
  flex: 1 1 auto;
  min-height: 0;
  position: relative;
}

.logs-dialog__viewport {
  height: 100%;
  overflow: auto;
  background: #050505;
  font-family: Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
}

.logs-dialog__lines {
  padding: 12px 0;
}

.logs-dialog__line {
  display: flex;
  gap: 14px;
  padding: 2px 18px;
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-word;
}

.logs-dialog__line--warn {
  background: rgba(255, 193, 7, 0.12);
}

.logs-dialog__line--error {
  background: rgba(244, 67, 54, 0.14);
}

.logs-dialog__line--active {
  background: rgba(255, 255, 255, 0.12);
}

.logs-dialog__line-number {
  flex: 0 0 72px;
  color: #8f9aa3;
  text-align: right;
  user-select: none;
}

.logs-dialog__line-text {
  flex: 1 1 auto;
  color: #fff;
}

.logs-dialog__status {
  padding: 14px 18px;
  color: #b0bec5;
}

.logs-dialog__status--center {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logs-dialog__scroll-bottom {
  position: absolute;
  right: 30px;
  bottom: 30px;
  z-index: 3;
  opacity: 0.78;
  backdrop-filter: blur(6px);
}

.logs-dialog__live-toggle {
  padding: 0 6px 0 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
}

:deep(.logs-dialog__highlight) {
  background: #fbc02d;
  color: #000;
  padding: 0 2px;
  border-radius: 2px;
}
</style>
