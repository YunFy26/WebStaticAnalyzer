<template>
  <div class="callgraph-view">
    <el-card shadow="never" class="page-header">
      <h2>{{ t('callgraph.title') }}</h2>
      <p class="subtitle">{{ t('callgraph.subtitle') }}</p>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-select
            v-model="selectedMethod"
            :placeholder="t('callgraph.selectPlaceholder')"
            filterable
            style="width: 100%"
            @change="renderGraph"
          >
            <el-option
              v-for="key in Object.keys(callGraphs)"
              :key="key"
              :label="key"
              :value="key"
            />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button-group>
            <el-button @click="zoomIn"><el-icon><ZoomIn /></el-icon></el-button>
            <el-button @click="zoomOut"><el-icon><ZoomOut /></el-icon></el-button>
            <el-button @click="resetZoom"><el-icon><RefreshRight /></el-icon></el-button>
          </el-button-group>
        </el-col>
        <el-col :span="6" style="text-align: right">
          <el-button type="primary" plain :icon="Download" @click="downloadSvg" :disabled="!svgContent">{{ t('callgraph.downloadSvg') }}</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px" v-if="selectedMethod">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="font-weight: 600">
            <el-icon><Share /></el-icon>
            {{ selectedMethod }}
          </span>
          <el-tag effect="dark" size="small">output/callFlows/{{ selectedMethod }}.dot</el-tag>
        </div>
      </template>
      <div class="graph-container" ref="graphContainer" @wheel.prevent="handleWheel">
        <div class="graph-inner" :style="graphStyle" v-html="svgContent"></div>
      </div>
    </el-card>

    <el-empty v-else :description="t('callgraph.emptyTip')" style="margin-top: 40px" />

    <!-- DOT Source -->
    <el-card shadow="never" style="margin-top: 16px" v-if="selectedMethod">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="font-weight: 600">{{ t('callgraph.dotSource') }}</span>
          <el-button size="small" @click="showDot = !showDot">{{ showDot ? t('callgraph.hide') : t('callgraph.show') }}</el-button>
        </div>
      </template>
      <el-collapse-transition>
        <pre v-if="showDot" class="dot-source">{{ callGraphs[selectedMethod] }}</pre>
      </el-collapse-transition>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { instance } from '@viz-js/viz'
import { Download } from '@element-plus/icons-vue'
import { callGraphs } from '../data/demo'
import { useI18n } from '../i18n'

const { t } = useI18n()

const route = useRoute()
const selectedMethod = ref('')
const svgContent = ref('')
const showDot = ref(false)
const scale = ref(1)
const graphContainer = ref(null)

let vizInstance = null

const graphStyle = computed(() => ({
  transform: `scale(${scale.value})`,
  transformOrigin: 'top left'
}))

onMounted(async () => {
  vizInstance = await instance()
  if (route.params.method && callGraphs[route.params.method]) {
    selectedMethod.value = route.params.method
    renderGraph()
  }
})

watch(() => route.params.method, (val) => {
  if (val && callGraphs[val]) {
    selectedMethod.value = val
    renderGraph()
  }
})

function renderGraph() {
  if (!vizInstance || !selectedMethod.value) return
  const dot = callGraphs[selectedMethod.value]
  if (!dot) return
  try {
    svgContent.value = vizInstance.renderString(dot, { format: 'svg' })
    scale.value = 1
  } catch (e) {
    svgContent.value = `<p style="color:red">Error rendering graph: ${e.message}</p>`
  }
}

function zoomIn() { scale.value = Math.min(scale.value + 0.2, 3) }
function zoomOut() { scale.value = Math.max(scale.value - 0.2, 0.3) }
function resetZoom() { scale.value = 1 }

function handleWheel(e) {
  if (e.deltaY < 0) zoomIn()
  else zoomOut()
}

function downloadSvg() {
  const blob = new Blob([svgContent.value], { type: 'image/svg+xml' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${selectedMethod.value}.svg`
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.page-header { border-left: 4px solid #409eff; }
.subtitle { color: #909399; font-size: 14px; margin-top: 4px; }

.graph-container {
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
  min-height: 400px;
  max-height: 700px;
  padding: 16px;
  cursor: grab;
}

.graph-inner {
  transition: transform 0.15s ease;
}

.graph-inner :deep(svg) {
  max-width: 100%;
}

.dot-source {
  background: #1d1e1f;
  color: #a9b7c6;
  padding: 16px;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.5;
  overflow-x: auto;
  max-height: 400px;
}
</style>
