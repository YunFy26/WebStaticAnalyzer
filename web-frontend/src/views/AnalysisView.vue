<template>
  <div class="analysis-view">
    <el-card shadow="never" class="page-header">
      <h2>{{ t('analysis.title') }}</h2>
      <p class="subtitle">{{ t('analysis.subtitle') }}</p>
    </el-card>

    <!-- Analysis Pipeline -->
    <el-card shadow="never" style="margin-top: 20px">
      <template #header>
        <span style="font-weight: 600">{{ t('analysis.pipeline') }}</span>
      </template>
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step :title="t('analysis.step1')" :description="t('analysis.step1Desc')" />
        <el-step :title="t('analysis.step2')" :description="t('analysis.step2Desc')" />
        <el-step :title="t('analysis.step3')" :description="t('analysis.step3Desc')" />
        <el-step :title="t('analysis.step4')" :description="t('analysis.step4Desc')" />
      </el-steps>
    </el-card>

    <!-- Analysis Modules -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12" v-for="(mod, i) in modules" :key="i">
        <el-card shadow="hover" class="module-card" :body-style="{ padding: '24px' }">
          <div class="module-header">
            <el-icon :size="32" :color="mod.color"><component :is="mod.icon" /></el-icon>
            <div>
              <h3>{{ mod.title }}</h3>
              <p class="module-desc">{{ mod.desc }}</p>
            </div>
          </div>
          <div class="module-tags">
            <el-tag v-for="tag in mod.tags" :key="tag" size="small" effect="plain" style="margin-right: 6px">{{ tag }}</el-tag>
          </div>
          <el-button
            :type="mod.btnType"
            :loading="mod.loading"
            :disabled="mod.done"
            style="margin-top: 16px; width: 100%"
            @click="runAnalysis(mod)"
          >
            {{ mod.done ? t('analysis.completed') : mod.loading ? t('analysis.running') : mod.btnText }}
          </el-button>
          <el-progress
            v-if="mod.loading"
            :percentage="mod.progress"
            :stroke-width="4"
            style="margin-top: 10px"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- Full Analysis -->
    <el-card shadow="never" style="margin-top: 20px">
      <template #header>
        <span style="font-weight: 600">{{ t('analysis.fullAnalysis') }}</span>
      </template>
      <el-button
        type="danger"
        size="large"
        :icon="VideoPlay"
        :loading="fullRunning"
        style="width: 100%; font-size: 16px"
        @click="runFullAnalysis"
      >
        {{ fullRunning ? t('analysis.fullRunning') : t('analysis.fullBtn') }}
      </el-button>
      <el-progress
        v-if="fullRunning"
        :percentage="fullProgress"
        :stroke-width="6"
        :format="() => `Step ${activeStep}/4`"
        style="margin-top: 12px"
      />
    </el-card>

    <!-- Log Output -->
    <el-card shadow="never" style="margin-top: 20px" v-if="logs.length">
      <template #header>
        <span style="font-weight: 600">{{ t('analysis.log') }}</span>
      </template>
      <div class="log-container">
        <div v-for="(log, i) in logs" :key="i" :class="['log-line', log.level]">
          <span class="log-time">{{ log.time }}</span>
          <el-tag :type="log.level === 'info' ? 'primary' : log.level === 'success' ? 'success' : 'warning'" size="small" effect="dark" style="margin: 0 8px">{{ log.level.toUpperCase() }}</el-tag>
          <span>{{ log.msg }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, watchEffect } from 'vue'
import { VideoPlay } from '@element-plus/icons-vue'
import { useI18n } from '../i18n'

const { t, lang } = useI18n()

const activeStep = ref(0)
const fullRunning = ref(false)
const fullProgress = ref(0)
const logs = ref([])

function ts() {
  return new Date().toLocaleTimeString()
}

const modules = reactive([
  { titleKey: 'analysis.mod1Title', descKey: 'analysis.mod1Desc', btnTextKey: 'analysis.mod1Btn', tagsKey: 'analysis.mod1Tags',
    title: '', desc: '', btnText: '', tags: [], icon: 'Reading', color: '#409eff', btnType: 'primary', loading: false, done: false, progress: 0 },
  { titleKey: 'analysis.mod2Title', descKey: 'analysis.mod2Desc', btnTextKey: 'analysis.mod2Btn', tagsKey: 'analysis.mod2Tags',
    title: '', desc: '', btnText: '', tags: [], icon: 'Share', color: '#67c23a', btnType: 'success', loading: false, done: false, progress: 0 },
  { titleKey: 'analysis.mod3Title', descKey: 'analysis.mod3Desc', btnTextKey: 'analysis.mod3Btn', tagsKey: 'analysis.mod3Tags',
    title: '', desc: '', btnText: '', tags: [], icon: 'MagicStick', color: '#e6a23c', btnType: 'warning', loading: false, done: false, progress: 0 },
  { titleKey: 'analysis.mod4Title', descKey: 'analysis.mod4Desc', btnTextKey: 'analysis.mod4Btn', tagsKey: 'analysis.mod4Tags',
    title: '', desc: '', btnText: '', tags: [], icon: 'WarnTriangleFilled', color: '#f56c6c', btnType: 'danger', loading: false, done: false, progress: 0 }
])

watchEffect(() => {
  // Trigger reactivity on lang change
  const _lang = lang.value
  for (const mod of modules) {
    mod.title = t(mod.titleKey)
    mod.desc = t(mod.descKey)
    mod.btnText = t(mod.btnTextKey)
    mod.tags = t(mod.tagsKey)
  }
})

function addLog(level, msg) {
  logs.value.push({ time: ts(), level, msg })
}

function simulateProgress(mod) {
  return new Promise(resolve => {
    mod.loading = true
    mod.progress = 0
    const interval = setInterval(() => {
      mod.progress += Math.random() * 15 + 5
      if (mod.progress >= 100) {
        mod.progress = 100
        mod.loading = false
        mod.done = true
        clearInterval(interval)
        resolve()
      }
    }, 300)
  })
}

async function runAnalysis(mod) {
  addLog('info', `${t('analysis.logStarting')}: ${mod.title}`)
  await simulateProgress(mod)
  addLog('success', `${t('analysis.logCompleted')}: ${mod.title}`)
}

async function runFullAnalysis() {
  fullRunning.value = true
  fullProgress.value = 0
  activeStep.value = 0
  logs.value = []
  addLog('info', t('analysis.logFullStart'))

  for (let i = 0; i < modules.length; i++) {
    activeStep.value = i
    fullProgress.value = (i / 4) * 100
    addLog('info', `Step ${i + 1}/4: ${modules[i].title}`)
    await simulateProgress(modules[i])
    addLog('success', `Step ${i + 1}/4 ${t('analysis.logCompleted')}: ${modules[i].title}`)
  }

  activeStep.value = 4
  fullProgress.value = 100
  fullRunning.value = false
  addLog('success', t('analysis.logFullDone'))
}
</script>

<style scoped>
.page-header { border-left: 4px solid #409eff; }
.subtitle { color: #909399; font-size: 14px; margin-top: 4px; }

.module-card { margin-bottom: 20px; border-radius: 8px; }
.module-header { display: flex; align-items: flex-start; gap: 14px; }
.module-header h3 { margin: 0; font-size: 16px; }
.module-desc { color: #909399; font-size: 13px; margin-top: 4px; }
.module-tags { margin-top: 12px; }

.log-container {
  max-height: 300px;
  overflow-y: auto;
  font-family: 'Menlo', 'Consolas', monospace;
  font-size: 13px;
  background: #1d1e1f;
  border-radius: 6px;
  padding: 12px;
}
.log-line { padding: 4px 0; color: #bfcbd9; }
.log-time { color: #666; margin-right: 4px; }
</style>
