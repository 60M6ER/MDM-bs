<template>
  <q-page class="q-pa-md column">
    <q-splitter v-model="split" :limits="[20, 60]" class="col" style="min-height: 0;">
      <!-- LEFT -->
      <template #before>
        <devices-list-pane
          class="fit"
          :selected-id="selectedId"
          @select="selectedId = $event"
        />
      </template>

      <!-- RIGHT -->
      <template #after>
        <div class="fit">
          <device-details-card-new
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
import DeviceDetailsCardNew from 'src/components/devices/DeviceDetailsCardNew.vue'

const split = ref(40)       // 40% слева
const selectedId = ref(null)
</script>
