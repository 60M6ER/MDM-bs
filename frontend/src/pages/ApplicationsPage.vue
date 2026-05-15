<template>
  <q-page class="q-pa-md column">
    <q-splitter v-model="leftSplit" :limits="[18, 40]" class="col applications-page" style="min-height: 0;">
      <template #before>
        <applications-list-pane
          :key="`apps-list-${refreshStamp}`"
          class="fit"
          :selected-id="selectedId"
          @select="selectedId = $event"
        />
      </template>

      <template #after>
        <q-splitter v-model="rightSplit" :limits="[35, 75]" class="fit" style="min-height: 0;">
          <template #before>
            <div class="fit">
              <application-details-card
                v-if="selectedId"
                :key="`app-details-${selectedId}-${refreshStamp}`"
                :application-id="selectedId"
                class="fit"
                @current-release-updated="handleCurrentReleaseUpdated"
                @refresh-requested="handleRefreshRequested"
              />
              <div v-else class="row items-center justify-center fit text-grey-6">
                Выберите приложение в списке слева
              </div>
            </div>
          </template>

          <template #after>
            <div class="fit">
              <application-releases-pane
                v-if="selectedId"
                :key="`app-releases-${selectedId}-${refreshStamp}`"
                class="fit"
                :application-id="selectedId"
                :current-release-id="currentReleaseId"
                @uploaded="handleReleaseUploaded"
              />
              <div v-else class="row items-center justify-center fit text-grey-6">
                Выберите приложение в списке слева
              </div>
            </div>
          </template>
        </q-splitter>
      </template>
    </q-splitter>
  </q-page>
</template>

<script setup>
import { ref, watch } from 'vue'
import ApplicationsListPane from 'src/components/applications/ApplicationsListPane.vue'
import ApplicationDetailsCard from 'src/components/applications/ApplicationDetailsCard.vue'
import ApplicationReleasesPane from 'src/components/applications/ApplicationReleasesPane.vue'

const leftSplit = ref(28)
const rightSplit = ref(55)
const selectedId = ref(null)
const refreshStamp = ref(0)
const currentReleaseId = ref(null)

function handleReleaseUploaded() {
  refreshStamp.value += 1
}

function handleCurrentReleaseUpdated(release) {
  currentReleaseId.value = release?.id ?? null
}

function handleRefreshRequested() {
  refreshStamp.value += 1
}

watch(selectedId, () => {
  currentReleaseId.value = null
})
</script>

<style scoped>
.applications-page {
  min-height: 0;
}
</style>
