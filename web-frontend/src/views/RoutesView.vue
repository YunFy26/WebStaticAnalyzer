<template>
  <div class="routes-view">
    <el-card shadow="never" class="page-header">
      <div class="header-row">
        <div>
          <h2>{{ t('routes.title') }}</h2>
          <p class="subtitle">{{ t('routes.subtitle') }}</p>
        </div>
        <div class="header-stats">
          <el-tag effect="dark" type="primary" size="large">Controllers: {{ controllerRoutes.length }}</el-tag>
          <el-tag effect="dark" type="success" size="large">Routes: {{ totalRoutes }}</el-tag>
        </div>
      </div>
    </el-card>

    <!-- Search & Filter -->
    <el-card shadow="never" style="margin-top: 16px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-input v-model="searchText" :placeholder="t('routes.searchPlaceholder')" clearable prefix-icon="Search" />
        </el-col>
        <el-col :span="6">
          <el-select v-model="httpFilter" :placeholder="t('routes.httpMethod')" clearable style="width: 100%">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="controllerFilter" :placeholder="t('routes.controller')" clearable style="width: 100%">
            <el-option v-for="c in controllerRoutes" :key="c.simpleName" :label="c.simpleName" :value="c.simpleName" />
          </el-select>
        </el-col>
      </el-row>
    </el-card>

    <!-- Controller Sections -->
    <div v-for="(ctrl, ci) in filteredControllers" :key="ci" style="margin-top: 16px">
      <el-card shadow="never">
        <template #header>
          <div class="ctrl-header">
            <div>
              <span class="ctrl-index">{{ ci + 1 }}</span>
              <span class="ctrl-name">{{ ctrl.simpleName }}</span>
              <span class="ctrl-full">{{ ctrl.className }}</span>
            </div>
            <div>
              <el-tag v-for="url in ctrl.baseUrls" :key="url" effect="plain" type="info" style="margin-left: 6px">{{ url }}</el-tag>
              <el-tag type="primary" effect="dark" size="small" style="margin-left: 10px">{{ ctrl.methods.length }} {{ t('routes.routes') }}</el-tag>
            </div>
          </div>
        </template>

        <el-table :data="filteredMethods(ctrl)" stripe highlight-current-row size="default" style="width: 100%">
          <el-table-column label="#" width="50" align="center">
            <template #default="{ $index }">
              <span style="color: #999">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('routes.urlPattern')" min-width="280">
            <template #default="{ row }">
              <code class="url-pattern">{{ row.urls[0] }}</code>
            </template>
          </el-table-column>
          <el-table-column :label="t('routes.http')" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="httpTagType(row.httpMethod)" effect="dark" size="small" disable-transitions>{{ row.httpMethod }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('routes.method')" min-width="180">
            <template #default="{ row }">
              <span class="method-name">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('routes.parameters')" min-width="200">
            <template #default="{ row }">
              <span class="params">{{ row.params.join(', ') || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('routes.callGraph')" width="120" align="center">
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                size="small"
                @click="goToCallGraph(ctrl, row)"
              >
                <el-icon><Share /></el-icon> {{ t('routes.view') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { controllerRoutes } from '../data/demo'
import { useI18n } from '../i18n'

const { t } = useI18n()
const router = useRouter()
const searchText = ref('')
const httpFilter = ref('')
const controllerFilter = ref('')

const totalRoutes = computed(() =>
  controllerRoutes.reduce((s, c) => s + c.methods.length, 0)
)

const filteredControllers = computed(() => {
  let result = controllerRoutes
  if (controllerFilter.value) {
    result = result.filter(c => c.simpleName === controllerFilter.value)
  }
  if (searchText.value || httpFilter.value) {
    result = result.filter(c => filteredMethods(c).length > 0)
  }
  return result
})

function filteredMethods(ctrl) {
  return ctrl.methods.filter(m => {
    if (httpFilter.value && m.httpMethod !== httpFilter.value) return false
    if (searchText.value) {
      const q = searchText.value.toLowerCase()
      return m.urls[0].toLowerCase().includes(q) || m.name.toLowerCase().includes(q)
    }
    return true
  })
}

function httpTagType(method) {
  const map = { GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger', PATCH: '' }
  return map[method] || 'info'
}

function goToCallGraph(ctrl, method) {
  const key = ctrl.simpleName + '.' + method.name
  router.push({ name: 'CallGraphDetail', params: { method: key } })
}
</script>

<style scoped>
.page-header { border-left: 4px solid #409eff; }
.header-row { display: flex; justify-content: space-between; align-items: center; }
.header-stats { display: flex; gap: 8px; }
.subtitle { color: #909399; font-size: 14px; margin-top: 4px; }

.ctrl-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.ctrl-index {
  display: inline-flex; align-items: center; justify-content: center;
  width: 24px; height: 24px; border-radius: 50%; background: #409eff;
  color: #fff; font-size: 12px; font-weight: 700; margin-right: 8px;
}
.ctrl-name { font-weight: 700; font-size: 16px; }
.ctrl-full { color: #999; font-size: 12px; font-family: monospace; margin-left: 10px; }

.url-pattern {
  background: #f0f2f5; padding: 2px 8px; border-radius: 4px;
  color: #c41d7f; font-size: 13px; border-left: 3px solid #c41d7f;
}
.method-name { color: #1677ff; font-family: monospace; font-weight: 600; }
.params { color: #666; font-size: 13px; }
</style>
