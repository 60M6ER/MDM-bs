// src/services/serverHealth.ts
import { ref } from 'vue'
import { apiClient } from 'src/services/apiClient.js' // ваш клиент

// Параметры: менять при необходимости
const HEALTH_PATH = '/health_check' // или '/actuator/health' или '/health'
const POLL_INTERVAL_OK = 30_000      // 30s
const POLL_INTERVAL_DOWN = 30_000    // 30s

const isAlive = ref<boolean | null>(null) // null = ещё не проверяли
const lastCheck = ref<number | null>(null)
let checkTimer: number | null = null
let backoffMult = 1

async function pingOnce(timeoutMs = 4000) {
  try {
    // apiClient должен бросать при ошибке или возвращать ответ
    // Мы используем простой GET; endpoint может быть любой, согласуй с бэком
    const controller = new AbortController()
    const id = setTimeout(() => controller.abort(), timeoutMs)
    const { data } = await apiClient.get(HEALTH_PATH, { signal: controller.signal })
    clearTimeout(id)
    // Если бэкенд возвращает {ok:true} — учитываем это. Иначе — 200 -> жив
    const ok = (data && (data.ok === true)) || (!!data)
    return ok
  } catch (err) {
    return false
  }
}

async function checkAndSchedule() {
  const alive = await pingOnce()
  const now = Date.now()
  lastCheck.value = now
  const prev = isAlive.value
  isAlive.value = alive

  // adjust backoff
  backoffMult = 1

  // schedule next
  const nextInterval = alive ? POLL_INTERVAL_OK : POLL_INTERVAL_DOWN * backoffMult
  if (checkTimer) {
    clearTimeout(checkTimer)
    checkTimer = null
  }
  checkTimer = window.setTimeout(checkAndSchedule, nextInterval)
  return { alive, prev, now }
}

function start() {
  // don't start multiple times
  if (checkTimer) return
  // initial quick check
  checkAndSchedule().catch(() => {
    // ensure scheduling in case of unexpected error
    if (!checkTimer) checkTimer = window.setTimeout(checkAndSchedule, POLL_INTERVAL_DOWN)
  })
}

function stop() {
  if (checkTimer) {
    clearTimeout(checkTimer)
    checkTimer = null
  }
  isAlive.value = null
}

function manualCheck() {
  // immediate check, returns boolean result
  return checkAndSchedule().then(r => r.alive)
}

export const serverHealth = {
  isAlive,
  lastCheck,
  start,
  stop,
  manualCheck
}
