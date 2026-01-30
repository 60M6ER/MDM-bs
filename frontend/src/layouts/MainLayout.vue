<template>
  <q-layout view="hHh lpR fFf" :class="{ 'server-down-blur': serverDown }">

    <q-header elevated class="bg-primary text-white" height-hint="98">
      <q-toolbar>
        <q-toolbar-title>
          <q-avatar>
            <img src="https://cdn.quasar.dev/logo-v2/svg/logo-mono-white.svg">
          </q-avatar>
          Учет мобильных устройств
        </q-toolbar-title>
      </q-toolbar>

      <q-tabs align="left">
        <q-route-tab to="/" label="Статистика" />
        <q-route-tab to="/devices" label="Устройства" />
        <q-route-tab to="/devices-on-map" label="Устройства на карте" />
        <q-route-tab to="/settings" label="Настройки" />
      </q-tabs>
    </q-header>

    <q-page-container>
      <router-view />
    </q-page-container>

    <q-footer elevated class="bg-grey-8 text-white">
      <q-toolbar>
        <q-toolbar-title>
          <q-avatar>
            <img src="https://cdn.quasar.dev/logo-v2/svg/logo-mono-white.svg">
          </q-avatar>
          <div>Title</div>
        </q-toolbar-title>
        <a
          href="/swagger-ui/index.html"
          target="_blank"
          rel="noopener noreferrer"
          class="text-white text-caption"
          style="text-decoration: none;"
        >
          Документация API
        </a>
      </q-toolbar>
    </q-footer>
  </q-layout>
  <ServerDownOverlay />
</template>

<script setup lang="ts">
import ServerDownOverlay from 'src/components/ServerDownOverlay.vue'
import { serverHealth } from 'src/services/serverHealth'
import { computed, onMounted } from 'vue'

const serverDown = computed(() => serverHealth.isAlive.value === false)

onMounted(() => {
  serverHealth.start()
})
</script>

<style scoped>
/* blur & darken when server down (applies to q-page-container) */
.server-down-blur {
  transition: filter .25s ease, opacity .25s ease;
  filter: blur(4px) saturate(.9);
  opacity: .6;
  pointer-events: none;
  user-select: none;
}
</style>
