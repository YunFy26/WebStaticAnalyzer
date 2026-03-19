<template>
  <div class="taint-view">
    <el-card shadow="never" class="page-header">
      <div class="header-row">
        <div>
          <h2>{{ t('taint.title') }}</h2>
          <p class="subtitle">{{ t('taint.subtitle') }}</p>
        </div>
        <div class="header-stats">
          <el-tag effect="dark" type="warning" size="large">Sources: {{ taintSources.length }}</el-tag>
          <el-tag effect="dark" type="danger" size="large">Sinks: {{ taintSinks.length }}</el-tag>
          <el-tag effect="dark" type="primary" size="large">Flows: {{ taintFlows.length }}</el-tag>
        </div>
      </div>
    </el-card>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" type="border-card" style="margin-top: 16px">
      <!-- Source Config -->
      <el-tab-pane :label="t('taint.sourceConfig')" name="sources">
        <div class="tab-desc">
          <el-icon><InfoFilled /></el-icon>
          {{ t('taint.sourceDesc') }}
        </div>
        <el-table :data="paginatedSources" stripe size="default" style="width: 100%">
          <el-table-column label="#" width="50" align="center">
            <template #default="{ $index }">{{ (sourcePage - 1) * pageSize + $index + 1 }}</template>
          </el-table-column>
          <el-table-column :label="t('taint.kind')" width="80">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ row.kind }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('taint.method')" min-width="400">
            <template #default="{ row }">
              <code class="sig">{{ row.method }}</code>
            </template>
          </el-table-column>
          <el-table-column :label="t('taint.index')" width="70" align="center">
            <template #default="{ row }">
              <el-tag type="primary" effect="dark" size="small">{{ row.index }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('taint.type')" min-width="200">
            <template #default="{ row }">
              <span class="type-name">{{ shortType(row.type) }}</span>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="sourcePage"
          :page-size="pageSize"
          :total="taintSources.length"
          layout="prev, pager, next, total"
          style="margin-top: 12px; justify-content: center"
        />
      </el-tab-pane>

      <!-- Sink Config -->
      <el-tab-pane :label="t('taint.sinkConfig')" name="sinks">
        <div class="tab-desc">
          <el-icon><WarnTriangleFilled /></el-icon>
          {{ t('taint.sinkDesc') }}
        </div>
        <el-table :data="taintSinks" stripe size="default" style="width: 100%">
          <el-table-column label="#" width="50" align="center" type="index" />
          <el-table-column :label="t('taint.method')" min-width="400">
            <template #default="{ row }">
              <code class="sig">{{ row.method }}</code>
            </template>
          </el-table-column>
          <el-table-column :label="t('taint.index')" width="70" align="center">
            <template #default="{ row }">
              <el-tag type="danger" effect="dark" size="small">{{ row.index }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('taint.risk')" width="120">
            <template #default="{ row }">
              <el-tag type="danger" effect="dark" size="small">{{ row.risk }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('taint.sqlFragment')" min-width="300">
            <template #default="{ row }">
              <code class="sql-frag" v-html="highlightSql(row.sql)"></code>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- Taint Flows -->
      <el-tab-pane :label="t('taint.taintFlows')" name="flows">
        <div class="tab-desc">
          <el-icon><Connection /></el-icon>
          {{ t('taint.flowDesc') }}
        </div>

        <div v-for="flow in taintFlows" :key="flow.id" class="flow-card">
          <div class="flow-header">
            <div>
              <el-tag :type="flow.severity === 'HIGH' ? 'danger' : 'warning'" effect="dark" size="small" style="margin-right: 8px">
                {{ flow.severity }}
              </el-tag>
              <el-tag type="danger" effect="plain" size="small">{{ flow.type }}</el-tag>
              <span class="flow-id">#{{ flow.id }}</span>
            </div>
          </div>

          <div class="flow-path">
            <div class="path-node source-node">
              <div class="node-label">SOURCE</div>
              <div class="node-content">{{ flow.source.method }}</div>
              <div class="node-detail">param[{{ flow.source.index }}]: {{ flow.source.param }}</div>
            </div>

            <template v-for="(step, si) in flow.path.slice(1, -1)" :key="si">
              <div class="path-arrow">
                <el-icon :size="16"><Right /></el-icon>
              </div>
              <div class="path-node transfer-node">
                <div class="node-label">TRANSFER</div>
                <div class="node-content">{{ step }}</div>
              </div>
            </template>

            <div class="path-arrow">
              <el-icon :size="16"><Right /></el-icon>
            </div>
            <div class="path-node sink-node">
              <div class="node-label">SINK</div>
              <div class="node-content">{{ flow.sink.method }}</div>
              <div class="node-detail" v-html="'SQL: ' + highlightSql(flow.sink.sql)"></div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- Flow Graph -->
      <el-tab-pane :label="t('taint.flowGraph')" name="graph">
        <div class="tab-desc">
          <el-icon><PictureFilled /></el-icon>
          {{ t('taint.graphDesc') }}
        </div>
        <div class="graph-controls">
          <el-button-group>
            <el-button @click="gZoomIn"><el-icon><ZoomIn /></el-icon></el-button>
            <el-button @click="gZoomOut"><el-icon><ZoomOut /></el-icon></el-button>
            <el-button @click="gScale = 1"><el-icon><RefreshRight /></el-icon></el-button>
          </el-button-group>
        </div>
        <div class="graph-container" @wheel.prevent="gHandleWheel">
          <div :style="{ transform: `scale(${gScale})`, transformOrigin: 'top left' }" v-html="flowSvg"></div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { instance } from '@viz-js/viz'
import { taintSources, taintSinks, taintFlows, taintFlowDot } from '../data/demo'
import { useI18n } from '../i18n'

const { t } = useI18n()

const activeTab = ref('flows')
const sourcePage = ref(1)
const pageSize = 10
const flowSvg = ref('')
const gScale = ref(1)

let vizInstance = null

onMounted(async () => {
  vizInstance = await instance()
  try {
    flowSvg.value = vizInstance.renderString(taintFlowDot, { format: 'svg' })
  } catch (e) {
    flowSvg.value = `<p style="color:red">Error: ${e.message}</p>`
  }
})

const paginatedSources = computed(() => {
  const start = (sourcePage.value - 1) * pageSize
  return taintSources.slice(start, start + pageSize)
})

function shortType(t) {
  const parts = t.split('.')
  return parts[parts.length - 1]
}

function highlightSql(sql) {
  return sql.replace(/\$\{(\w+)\}/g, '<span class="hl-danger">${$1}</span>')
            .replace(/#\{(\w+)\}/g, '<span class="hl-safe">#{$1}</span>')
}

function gZoomIn() { gScale.value = Math.min(gScale.value + 0.2, 3) }
function gZoomOut() { gScale.value = Math.max(gScale.value - 0.2, 0.3) }
function gHandleWheel(e) { e.deltaY < 0 ? gZoomIn() : gZoomOut() }
</script>

<style scoped>
.page-header { border-left: 4px solid #409eff; }
.header-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.header-stats { display: flex; gap: 8px; }
.subtitle { color: #909399; font-size: 14px; margin-top: 4px; }

.tab-desc {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 14px; margin-bottom: 16px;
  background: #f0f2f5; border-radius: 6px;
  color: #666; font-size: 13px;
}

.sig { font-size: 12px; color: #333; word-break: break-all; }
.type-name { color: #1677ff; font-family: monospace; font-size: 13px; }

.sql-frag {
  font-size: 12px; background: #f8f8f8; padding: 4px 8px;
  border-radius: 4px; display: inline-block; word-break: break-all;
}

:deep(.hl-danger) {
  color: #fff; background: #f56c6c; padding: 1px 4px; border-radius: 3px; font-weight: 700;
}
:deep(.hl-safe) {
  color: #fff; background: #67c23a; padding: 1px 4px; border-radius: 3px; font-weight: 700;
}

/* Flow cards */
.flow-card {
  border: 1px solid #ebeef5; border-radius: 8px; padding: 16px;
  margin-bottom: 16px; background: #fff;
}
.flow-header { margin-bottom: 12px; }
.flow-id { color: #999; font-size: 13px; margin-left: 8px; }

.flow-path {
  display: flex; align-items: stretch; gap: 0;
  overflow-x: auto; padding: 8px 0;
}
.path-arrow {
  display: flex; align-items: center; padding: 0 6px; color: #999;
}
.path-node {
  min-width: 200px; padding: 10px 14px; border-radius: 8px;
  border: 2px solid; flex-shrink: 0;
}
.source-node { border-color: #f56c6c; background: #fef0f0; }
.transfer-node { border-color: #e6a23c; background: #fdf6ec; }
.sink-node { border-color: #67c23a; background: #f0f9eb; }

.node-label {
  font-size: 10px; font-weight: 700; letter-spacing: 1px; margin-bottom: 4px;
}
.source-node .node-label { color: #f56c6c; }
.transfer-node .node-label { color: #e6a23c; }
.sink-node .node-label { color: #67c23a; }

.node-content { font-size: 13px; font-weight: 600; color: #333; }
.node-detail { font-size: 12px; color: #666; margin-top: 4px; word-break: break-all; }

/* Graph */
.graph-controls { margin-bottom: 12px; }
.graph-container {
  overflow: auto; border: 1px solid #ebeef5; border-radius: 6px;
  background: #fafafa; min-height: 300px; max-height: 600px; padding: 16px;
}
.graph-container :deep(svg) { max-width: 100%; }
</style>
