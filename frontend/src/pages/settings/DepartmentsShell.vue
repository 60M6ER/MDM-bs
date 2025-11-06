<template>
  <div class="q-pa-md">
    <div class="text-h6 q-mb-md">Подразделения</div>

    <q-splitter
      v-model="split"
      unit="%"
      :limits="[30, 70]"
      separator-class="bg-grey-3"
    >
      <template #before>
        <DepartmentsList @select="goTo" :selectedId="id" />
      </template>

      <template #after>
        <div class="q-pa-md">
          <DepartmentDetails v-if="id" :id="id" />
          <div v-else class="text-grey">Выберите подразделение слева</div>
        </div>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DepartmentsList from 'components/settings/DepartmentsList.vue'
import DepartmentDetails from 'components/settings/DepartmentDetails.vue'

const route = useRoute()
const router = useRouter()
const rawId = route.params.id
const id = ref(typeof rawId === 'string' ? rawId : undefined)
const split = ref(id.value ? 50 : 100)

watch(() => route.params.id, (val) => {
  id.value = typeof val === 'string' ? val : undefined
  split.value = id.value ? 50 : 100
})

function goTo(depId) {
  router.push({ path: `/settings/departments/${depId}` })
}
</script>
