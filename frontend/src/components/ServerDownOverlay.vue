<template>
  <transition name="fade">
    <div v-if="show" class="server-down-overlay">
      <div class="overlay-backdrop" @click.stop></div>

      <div class="overlay-content q-pa-xl">
        <div class="row items-center justify-center q-gutter-md">
          <div class="col-auto text-h6">Сервер недоступен</div>
        </div>

        <div class="q-mt-md text-body2 text-center">
          Сервер недоступен. Подождите, идёт попытка восстановления...
        </div>

        <div class="row items-center justify-center q-mt-lg">
          <q-spinner-dots size="48px" />
        </div>

        <div class="row items-center justify-center q-gutter-sm q-mt-md">
          <q-btn flat label="Проверить сейчас" @click="manualCheck" :loading="checking" />
        </div>

        <div class="q-mt-md text-caption text-center">
          Последняя проверка: {{ lastCheckDisplay }}
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { serverHealth } from 'src/services/serverHealth'

const checking = ref(false)
const dismissed = ref(false)

const show = computed(() => {
  // show overlay when server is explicitly down (false)
  // and user didn't manually dismiss during this session
  return serverHealth.isAlive.value === false && !dismissed.value
})

const lastCheckDisplay = computed(() => {
  const v = serverHealth.lastCheck.value
  return v ? new Date(v).toLocaleTimeString() : '—'
})

watch(() => serverHealth.isAlive.value, (val) => {
  if (val === true) {
    // auto reset dismissed when server becomes alive
    dismissed.value = false
  }
})

async function manualCheck() {
  checking.value = true
  try {
    await serverHealth.manualCheck()
  } finally {
    checking.value = false
  }
}
</script>

<style scoped>
.server-down-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* darkened backdrop - also increases blur in layout via class */
.overlay-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.55);
  backdrop-filter: blur(4px) saturate(80%);
}

/* central card */
.overlay-content {
  position: relative;
  z-index: 10000;
  background: rgba(255,255,255,0.98);
  border-radius: 12px;
  min-width: 320px;
  max-width: 720px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.4);
  text-align: center;
}

/* simple fade */
.fade-enter-active, .fade-leave-active {
  transition: opacity .25s;
}
.fade-enter-from, .fade-leave-to { opacity: 0 }
</style>
