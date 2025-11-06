<template>
  <div class="q-pa-md">
    <div class="text-h6 q-mb-md">Настройки программы</div>
    <q-form class="q-gutter-md" @submit.prevent="onSave">
      <q-input v-model="appName" label="Название системы" outlined dense />
      <q-input v-model="baseUrl" label="Базовый URL API" outlined dense />
      <q-toggle v-model="telemetryEnabled" label="Включить телеметрию" />
      <div class="row q-gutter-sm">
        <q-btn color="primary" label="Сохранить" type="submit" />
        <q-btn flat label="Отмена" @click="onCancel" />
      </div>
    </q-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
const appName = ref('Учет мобильных устройств')
const baseUrl = ref('/api')
const telemetryEnabled = ref(true)
function onSave(){ /* TODO: POST/PUT в бэкенд */ }
function onCancel(){ /* TODO: reset */ }
</script>
