<template>
  <div class="project-view">
    <el-card shadow="never" class="page-header">
      <div class="header-row">
        <div>
          <h2>{{ t('project.title') }}</h2>
          <p class="subtitle">{{ t('project.subtitle') }}</p>
        </div>
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          accept=".jar,.zip,.war"
          @change="handleUpload"
        >
          <el-button type="primary" :icon="Upload">{{ t('project.upload') }}</el-button>
        </el-upload>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card stat-blue">
          <div class="stat-content">
            <el-icon :size="40"><Files /></el-icon>
            <div>
              <div class="stat-number">{{ projectInfo.totalFiles }}</div>
              <div class="stat-label">{{ t('project.totalFiles') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card stat-green">
          <div class="stat-content">
            <el-icon :size="40"><Document /></el-icon>
            <div>
              <div class="stat-number">{{ projectInfo.javaFiles }}</div>
              <div class="stat-label">{{ t('project.javaClasses') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card stat-orange">
          <div class="stat-content">
            <el-icon :size="40"><Setting /></el-icon>
            <div>
              <div class="stat-number">{{ projectInfo.configFiles }}</div>
              <div class="stat-label">{{ t('project.configFiles') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>{{ t('project.structure') }}</span>
          <el-tag v-if="uploaded" type="success" effect="dark" size="small">{{ t('project.loaded') }}</el-tag>
          <el-tag v-else type="info" effect="dark" size="small">{{ t('project.awaiting') }}</el-tag>
        </div>
      </template>
      <div v-if="uploaded" class="tree-container">
        <el-tree
          :data="projectTree"
          :props="{ children: 'children', label: 'label' }"
          :default-expand-all="false"
          :default-expanded-keys="['jeecg-boot-3.5.3', 'src/main/java']"
          node-key="label"
          highlight-current
        >
          <template #default="{ node, data }">
            <span class="tree-node">
              <el-icon v-if="data.children" :size="14" color="#e6a23c"><FolderOpened /></el-icon>
              <el-icon v-else :size="14" :color="getFileColor(data.label)"><Document /></el-icon>
              <span style="margin-left: 6px">{{ data.label }}</span>
            </span>
          </template>
        </el-tree>
      </div>
      <el-empty v-else :description="t('project.emptyTip')" />
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { projectTree as demoTree } from '../data/demo'
import { useI18n } from '../i18n'

const { t } = useI18n()

const uploaded = ref(false)
const projectTree = ref([])

const projectInfo = ref({
  totalFiles: 0,
  javaFiles: 0,
  configFiles: 0
})

function countFiles(nodes) {
  let total = 0, java = 0, config = 0
  for (const n of nodes) {
    if (n.children) {
      const c = countFiles(n.children)
      total += c.total; java += c.java; config += c.config
    } else {
      total++
      if (n.label.endsWith('.java')) java++
      if (n.label.endsWith('.xml') || n.label.endsWith('.yml') || n.label.endsWith('.properties')) config++
    }
  }
  return { total, java, config }
}

function handleUpload(file) {
  projectTree.value = demoTree
  const counts = countFiles(demoTree)
  projectInfo.value = { totalFiles: counts.total, javaFiles: counts.java, configFiles: counts.config }
  uploaded.value = true
  ElMessage.success(t('project.loadSuccess') + ': jeecg-boot-3.5.3')
}

function getFileColor(name) {
  if (name.endsWith('.java')) return '#409eff'
  if (name.endsWith('.xml')) return '#e6a23c'
  if (name.endsWith('.yml') || name.endsWith('.properties')) return '#67c23a'
  return '#909399'
}
</script>

<style scoped>
.page-header { border-left: 4px solid #409eff; }
.header-row { display: flex; justify-content: space-between; align-items: center; }
.subtitle { color: #909399; font-size: 14px; margin-top: 4px; }

.stat-card { border-radius: 8px; }
.stat-content { display: flex; align-items: center; gap: 16px; }
.stat-number { font-size: 28px; font-weight: 700; }
.stat-label { color: #909399; font-size: 13px; }
.stat-blue .el-icon { color: #409eff; }
.stat-green .el-icon { color: #67c23a; }
.stat-orange .el-icon { color: #e6a23c; }

.card-header { display: flex; align-items: center; gap: 10px; font-weight: 600; }
.tree-container { max-height: 500px; overflow-y: auto; }
.tree-node { display: flex; align-items: center; }
</style>
