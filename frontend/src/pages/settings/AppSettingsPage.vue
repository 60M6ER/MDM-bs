<template>
  <div class="q-pa-md">
    <div class="text-h6 q-mb-md">Внешняя авторизация</div>

    <q-form class="q-gutter-md" @submit.prevent="onSave">
      <q-toggle v-model="model.enabled" label="Включить внешнюю авторизацию" />

      <div v-if="model.enabled">
        <q-select
          v-model="model.provider"
          :options="providerOptions"
          label="Провайдер"
          outlined
          dense
          emit-value
          map-options
        />

        <q-separator />

        <!-- LDAP settings -->
        <div v-if="model.provider === 'LDAP'" class="q-gutter-md">
          <div class="text-subtitle2">LDAP</div>
          <q-banner dense class="bg-grey-2 text-grey-9">Минимальный набор (AD): URL и Base DN. Остальные поля — в «Расширенных».</q-banner>
          <q-toggle v-model="showAdvancedLdap" label="Показать расширенные настройки" dense />
          <q-input v-model="model.ldap.url" label="URL LDAP (ldaps://host:636 или ldap://host:389)" outlined dense />
          <q-input v-model="model.ldap.baseDn" label="Base DN (например, dc=example,dc=com)" outlined dense />
          <div v-if="showAdvancedLdap">
            <q-input v-model="model.ldap.userSearchFilter" label="Фильтр поиска пользователя (например, (uid={0}))" outlined dense />
            <q-input v-model="model.ldap.bindDn" label="Bind DN (необязательно)" outlined dense />
            <q-input v-model="model.ldap.bindPassword" :type="showBindPwd ? 'text' : 'password'" label="Bind пароль" outlined dense>
              <template #append>
                <q-icon :name="showBindPwd ? 'visibility_off' : 'visibility'" class="cursor-pointer" @click="showBindPwd = !showBindPwd" />
              </template>
            </q-input>
            <div class="row q-col-gutter-sm">
              <div class="col-12 col-sm-4">
                <q-toggle v-model="model.ldap.startTLS" label="StartTLS" />
              </div>
              <div class="col-12 col-sm-8">
                <q-toggle v-model="model.ldap.trustAllCerts" label="Доверять всем сертификатам (только для теста)" />
              </div>
            </div>
          </div>
        </div>

        <!-- AD settings -->
        <div v-if="model.provider === 'AD'" class="q-gutter-md">
          <div class="text-subtitle2">Active Directory (AD)</div>
          <q-banner dense class="bg-grey-2 text-grey-9">Укажите домен и контроллеры домена. Сервера можно перечислить через запятую или по одному в строке.</q-banner>
          <q-input v-model="model.ad.domain" label="Домен (например, example.local)" outlined dense />
          <q-input v-model="adUrlsText" type="textarea" autogrow label="Серверы/URL контроллеров (через запятую или с новой строки)" outlined dense />
          <q-input v-model="model.ad.referral" label="Referral (например, follow/ignore)" outlined dense />
        </div>

        <!-- OIDC settings -->
        <div v-if="model.provider === 'OIDC'" class="q-gutter-md">
          <div class="text-subtitle2">OpenID Connect (OIDC)</div>
          <q-input v-model="model.oidc.issuerUri" label="Issuer URI (например, https://keycloak/realms/foo)" outlined dense />
          <q-input v-model="model.oidc.clientId" label="Client ID" outlined dense />
          <q-input v-model="model.oidc.clientSecret" :type="showClientSecret ? 'text' : 'password'" label="Client Secret" outlined dense>
            <template #append>
              <q-icon :name="showClientSecret ? 'visibility_off' : 'visibility'" class="cursor-pointer" @click="showClientSecret = !showClientSecret" />
            </template>
          </q-input>
          <q-input v-model="oidcScopes" label="Scopes (через запятую)" outlined dense />
          <q-input v-model="model.oidc.usernameClaim" label="Claim для имени пользователя (например, preferred_username)" outlined dense />
          <q-input v-model="model.oidc.rolesClaim" label="Claim для ролей (например, realm_access.roles)" outlined dense />
        </div>
      </div>

      <q-separator />

      <div class="row q-gutter-sm">
        <q-btn color="primary" label="Сохранить" type="submit" :loading="loading" />
        <q-btn outline color="primary" label="Проверить подключение" @click="onTest" :disable="loading" />
        <q-btn flat label="Отмена" @click="onCancel" :disable="loading" />
      </div>

      <q-banner v-if="statusMsg" :class="{'q-mt-md': true}" :type="statusType">
        {{ statusMsg }}
      </q-banner>
    </q-form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { apiClient } from "src/services/apiClient.js"

// Унифицированная модель настроек внешней авторизации
interface LdapSettings {
  url: string
  baseDn: string
  userSearchFilter: string
  bindDn: string
  bindPassword: string
  startTLS: boolean
  trustAllCerts: boolean
}

interface OidcSettings {
  issuerUri: string
  clientId: string
  clientSecret: string
  redirectUri: string
  scopes: string[]
  usernameClaim: string
  rolesClaim: string
}

interface AdSettings {
  domain: string
  urls: string[]
  referral: string
}

interface ExternalAuthSettings {
  enabled: boolean
  provider: 'LDAP' | 'OIDC' | 'AD'
  ad: AdSettings
  ldap: LdapSettings
  oidc: OidcSettings
}

const $q = useQuasar()
const loading = ref(false)
const showBindPwd = ref(false)
const showClientSecret = ref(false)
const showAdvancedLdap = ref(false)
const statusMsg = ref<string | null>(null)
const statusType = ref<'positive' | 'negative' | 'warning' | 'info'>('info')

const providerOptions = [
  { label: 'Active Directory (AD)', value: 'AD' },
  { label: 'LDAP / Active Directory', value: 'LDAP' },
  { label: 'OpenID Connect (OIDC)', value: 'OIDC' }
]

const model = reactive<ExternalAuthSettings>({
  enabled: false,
  provider: 'LDAP',
  ad: {
    domain: '',
    urls: [],
    referral: ''
  },
  ldap: {
    url: '',
    baseDn: '',
    userSearchFilter: '(uid={0})',
    bindDn: '',
    bindPassword: '',
    startTLS: false,
    trustAllCerts: false
  },
  oidc: {
    issuerUri: '',
    clientId: '',
    clientSecret: '',
    redirectUri: '',
    scopes: ['openid','profile','email'],
    usernameClaim: 'preferred_username',
    rolesClaim: 'realm_access.roles'
  }
})

const oidcScopes = ref(model.oidc.scopes.join(', '))
watch(oidcScopes, (v) => {
  if (!model.oidc) {
    model.oidc = {
      issuerUri: '',
      clientId: '',
      clientSecret: '',
      redirectUri: '',
      scopes: [],
      usernameClaim: 'preferred_username',
      rolesClaim: 'realm_access.roles'
    } as OidcSettings
  }
  model.oidc.scopes = v.split(',').map(s => s.trim()).filter(Boolean)
})

const adUrlsText = ref(model.ad.urls.join('\n'))
watch(adUrlsText, (v) => {
  model.ad.urls = v.split(/[\n,]+/).map(s => s.trim()).filter(Boolean)
})

function buildPayload() {
  if (model.provider === 'AD') {
    return {
      provider: 'AD' as const,
      ad: { ...model.ad }
    }
  }
  if (model.provider === 'LDAP') {
    return {
      provider: 'LDAP' as const,
      ldap: { ...model.ldap }
    }
  }
  return {
    provider: 'OIDC' as const,
    oidc: { ...model.oidc }
  }
}

function getErrorMessage(err: unknown): string {
  if (err && typeof err === 'object') {
    const e = err as { response?: { data?: { message?: string } }; message?: string }
    return e.response?.data?.message || e.message || 'Произошла ошибка'
  }
  return 'Произошла ошибка'
}

async function load() {
  loading.value = true
  try {
    const { data } = await apiClient.get('/settings/e-auth')
    const incoming = data as Partial<ExternalAuthSettings>
    // верхнеуровневые поля
    if (typeof incoming.enabled === 'boolean') model.enabled = incoming.enabled
    if (incoming.provider) model.provider = incoming.provider as ExternalAuthSettings['provider']
    // вложенные объекты: мержим поверх дефолтов, игнорируя null
    if (incoming.ad) model.ad = { ...model.ad, ...incoming.ad }
    if (incoming.ldap) model.ldap = { ...model.ldap, ...incoming.ldap }
    if (incoming.oidc) model.oidc = { ...model.oidc, ...incoming.oidc }

    oidcScopes.value = (model.oidc?.scopes || []).join(', ')
    adUrlsText.value = (model.ad.urls || []).join('\n')
  } catch {
    statusMsg.value = 'Не удалось загрузить настройки'
    statusType.value = 'warning'
    $q.notify({ type: 'warning', message: 'Не удалось загрузить настройки' })
  } finally {
    loading.value = false
  }
}

async function onSave() {
  loading.value = true
  try {
    const payload = buildPayload()
    await apiClient.put('/settings/e-auth', payload)
    statusMsg.value = 'Настройки сохранены'
    statusType.value = 'positive'
    $q.notify({ type: 'positive', message: 'Настройки сохранены' })
  } catch (err: unknown) {
    const msg = getErrorMessage(err)
    statusMsg.value = msg
    statusType.value = 'negative'
    $q.notify({ type: 'negative', message: msg })
  } finally {
    loading.value = false
  }
}

async function onTest() {
  loading.value = true
  try {
    const { data } = await apiClient.post('/settings/e-auth/test', buildPayload())
    const ok = !!data?.ok
    const msg = data?.message || (ok ? 'Подключение успешно' : 'Проверка не удалась')
    statusMsg.value = msg
    statusType.value = ok ? 'positive' : 'negative'
    $q.notify({ type: ok ? 'positive' : 'negative', message: msg })
  } catch (err: unknown) {
    const msg = getErrorMessage(err)
    statusMsg.value = msg
    statusType.value = 'negative'
    $q.notify({ type: 'negative', multiLine: true, message: msg })
  } finally {
    loading.value = false
  }
}

function onCancel() {
  load()
}

onMounted(load)
</script>
