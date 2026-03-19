<template>
  <el-container class="app-container">
    <el-aside width="220px" class="app-aside">
      <div class="logo">
        <el-icon :size="28" color="#409eff"><Monitor /></el-icon>
        <span class="logo-text">WebAnalyzer</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1d1e1f"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/project">
          <el-icon><FolderOpened /></el-icon>
          <span>{{ t('nav.project') }}</span>
        </el-menu-item>
        <el-menu-item index="/analysis">
          <el-icon><DataAnalysis /></el-icon>
          <span>{{ t('nav.analysis') }}</span>
        </el-menu-item>
        <el-menu-item index="/routes">
          <el-icon><Guide /></el-icon>
          <span>{{ t('nav.routes') }}</span>
        </el-menu-item>
        <el-menu-item index="/callgraph">
          <el-icon><Share /></el-icon>
          <span>{{ t('nav.callgraph') }}</span>
        </el-menu-item>
        <el-menu-item index="/taint">
          <el-icon><WarnTriangleFilled /></el-icon>
          <span>{{ t('nav.taint') }}</span>
        </el-menu-item>
      </el-menu>

      <!-- Language Toggle -->
      <div class="lang-toggle">
        <el-switch
          :model-value="lang === 'zh'"
          active-text="中"
          inactive-text="EN"
          @change="toggleLang"
          style="--el-switch-on-color: #409eff; --el-switch-off-color: #67c23a"
        />
      </div>
    </el-aside>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from './i18n'

const { t, lang, toggleLang } = useI18n()

const route = useRoute()
const activeMenu = computed(() => {
  if (route.path.startsWith('/callgraph')) return '/callgraph'
  return route.path
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { height: 100%; }

.app-container {
  height: 100vh;
}

.app-aside {
  background: #1d1e1f;
  border-right: 1px solid #333;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px;
  border-bottom: 1px solid #333;
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.app-main {
  background: #f5f7fa;
  padding: 24px;
  overflow-y: auto;
}

.el-menu {
  border-right: none !important;
  flex: 1;
}

.lang-toggle {
  padding: 16px;
  border-top: 1px solid #333;
  display: flex;
  justify-content: center;
}
</style>
