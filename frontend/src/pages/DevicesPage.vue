<template>
  <q-page class="fit q-pa-md column no-wrap devices-page">
    <q-splitter v-model="split" :limits="[20, 60]" class="col devices-page__splitter" style="min-height: 0;">
      <!-- LEFT -->
      <template #before>
        <devices-list-pane
          class="fit devices-page__pane"
          :selected-id="selectedId"
          @select="selectedId = $event"
        />
      </template>

      <!-- RIGHT -->
      <template #after>
        <div class="fit devices-page__pane">
          <device-details-card
            v-if="selectedId"
            :device-id="selectedId"
            class="fit"
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
import { ref } from 'vue'
import DevicesListPane from 'src/components/devices/DevicesListPane.vue'
import DeviceDetailsCard from 'src/components/devices/DeviceDetailsCard.vue'

const split = ref(40)       // 40% слева
const selectedId = ref(null)
</script>

<style scoped>
.devices-page {
  min-height: 0;
  height: 100%;
  overflow: hidden;
}

.devices-page__splitter {
  min-height: 0;
  overflow: hidden;
  height: 100%;
}

.devices-page__pane {
  min-height: 0;
  overflow: hidden;
  height: 100%;
}

.devices-page :deep(.q-page) {
  min-height: 0;
}

.devices-page__splitter :deep(.q-splitter__panel) {
  min-height: 0;
  height: 100%;
  overflow: hidden;
}

.devices-page__splitter :deep(.q-splitter__before),
.devices-page__splitter :deep(.q-splitter__after) {
  min-height: 0;
  height: 100%;
  overflow: hidden;
}
</style>
