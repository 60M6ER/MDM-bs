<template>
  <q-card flat bordered>
    <q-toolbar class="q-pa-sm">
      <q-input v-model="filter" dense outlined placeholder="Поиск..." class="full-width" />
      <q-btn flat round icon="add" @click="$emit('select','new')" />
    </q-toolbar>
    <q-list separator>
      <q-item
        v-for="d in filtered"
        :key="d.id"
        clickable
        :active="d.id === selectedId"
        active-class="bg-grey-2"
        @click="$emit('select', d.id)"
      >
        <q-item-section>{{ d.name }}</q-item-section>
        <q-item-section side class="text-caption text-grey">{{ d.externalId }}</q-item-section>
      </q-item>
    </q-list>
  </q-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
// TODO: заменить на реальный API
//const props = defineProps(['selectedId'])
const deps = ref([])
const filter = ref('')

const filtered = computed(() =>
  deps.value.filter(d => d.name.toLowerCase().includes(filter.value.toLowerCase()))
)

onMounted(async () => {
  // пример заглушки — заменить на fetch('/api/departments')
  deps.value = [
    { id:'1', name:'Склад', externalId:'SKL-001'},
    { id:'2', name:'Офис', externalId:'OFF-001'},
    { id:'3', name:'Сервис', externalId:'SRV-010'}
  ]
})
</script>
