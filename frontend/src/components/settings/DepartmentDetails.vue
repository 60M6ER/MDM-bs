<template>
  <q-card flat bordered>
    <q-card-section class="row items-center justify-between">
      <div class="text-h6">Подразделение</div>
      <div class="text-grey">{{ dep?.id }}</div>
    </q-card-section>

    <q-separator />

    <q-card-section class="q-gutter-md">
      <q-input v-model="dep.name" label="Название" outlined dense />
      <q-input v-model="dep.externalId" label="Внешний ID" outlined dense />
    </q-card-section>

    <q-separator />

    <q-card-actions align="right">
      <q-btn color="primary" label="Сохранить" @click="save" />
      <q-btn flat label="Удалить" color="negative" @click="remove" />
    </q-card-actions>
  </q-card>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
const props = defineProps(['id'])
const dep = ref({ id: props.id, name: '', externalId: '' })

watch(() => props.id, async (val) => {
  await load(val)
})

onMounted(() => load(props.id))

async function load(id){
  // TODO: заменить на реальный GET /api/departments/{id}
  dep.value = { id, name: 'Офис', externalId: 'OFF-001' }
}

async function save(){
  // TODO: PUT /api/departments/{id} или POST для new
}

async function remove(){
  // TODO: DELETE /api/departments/{id}
}
</script>
