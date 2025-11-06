<template>
  <q-page class="q-pa-lg">
    <q-card flat bordered class="q-pa-md">
      <q-splitter
        v-model="split"
        unit="%"
        :limits="[15, 35]"
        separator-class="bg-grey-3"
      >
        <!-- Левая панель: вертикальное меню -->
        <template #before>
          <q-list dense bordered separator class="rounded-borders">
            <q-item clickable v-ripple to="/settings/app" exact>
              <q-item-section>Настройки программы</q-item-section>
            </q-item>
            <q-item clickable v-ripple to="/settings/departments" exact>
              <q-item-section>Подразделения</q-item-section>
            </q-item>
          </q-list>
        </template>

        <!-- Правая панель: вложенный контент -->
        <template #after>
          <div class="q-pa-md">
            <router-view />
          </div>
        </template>
      </q-splitter>
    </q-card>
  </q-page>
</template>

<script setup>
import { ref } from 'vue'
const split = ref(20) // 20% ширины слева
</script>
