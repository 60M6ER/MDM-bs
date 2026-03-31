<template>
  <div class="column full-height" style="min-height: 0;">
    <div class="row items-center q-pb-sm">
      <div class="text-subtitle1 text-weight-medium">Приложения</div>
      <q-space />
      <q-btn flat round dense icon="refresh" :loading="loadingList" @click="reload" />
    </div>

    <q-list bordered class="col scroll" style="min-height: 0;">
      <q-item
        v-for="app in page.content"
        :key="app.id"
        clickable
        :active="app.id === selectedId"
        active-class="bg-blue-1 text-primary"
        @click="$emit('select', app.id)"
      >
        <q-item-section>
          <q-item-label class="text-weight-medium">
            {{ app.name || app.key }}
          </q-item-label>
          <q-item-label caption>
            {{ app.key }}
          </q-item-label>
        </q-item-section>
      </q-item>

      <q-item v-if="!loadingList && page.content.length === 0">
        <q-item-section>Приложений пока нет.</q-item-section>
      </q-item>
    </q-list>

    <div class="row items-center justify-between q-pt-sm">
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
import { onMounted, ref } from 'vue'
import { apiClient } from 'src/services/apiClient.js'

defineProps({
  selectedId: { type: Number, default: null }
})

defineEmits(['select'])

const loadingList = ref(false)
const uiPage = ref(1)
const size = ref(30)
const page = ref({ content: [], page: 0, size: 30, totalElements: 0, totalPages: 0 })

async function loadList(p = 0) {
  loadingList.value = true
  try {
    const resp = await apiClient.get('/applications', {
      params: { page: p, size: size.value, sort: 'createdAt,desc' }
    })
    page.value = resp.data
  } finally {
    loadingList.value = false
  }
}

function handlePageChange(newUiPage) {
  loadList(newUiPage - 1)
}

function reload() {
  loadList(uiPage.value - 1)
}

onMounted(() => loadList(0))
</script>
