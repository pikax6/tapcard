<template>
  <el-container class="console">
    <el-aside width="208px" class="side">
      <div class="brand">一卡通结算</div>
      <el-menu :default-active="active" router class="menu">
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          {{ m.text }}
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="top">
        <span class="crumb">{{ title }}</span>
        <span class="who">
          {{ who.name || '未登录' }}
          <em v-if="who.role">{{ who.roleLabel }}</em>
          <el-button link type="primary" class="out" @click="doLogout">退出</el-button>
        </span>
      </el-header>
      <el-main class="body">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useWhoami } from '@/stores/whoami'

const route = useRoute()
const router = useRouter()
const who = useWhoami()

/**
 * 菜单与可见角色。和 router 里的 ROLE_OF 保持同一份口径：
 * 学生只看自己的账单；商户只有机具台；财务与管理员全看。
 */
const MENUS = [
  { path: '/home',   text: '概览',   roles: [1, 2, 3, 9] },
  { path: '/acct',   text: '卡户册', roles: [1, 3, 9] },
  { path: '/dev',    text: '机具台', roles: [2, 3, 9] },
  { path: '/bill',   text: '账单簿', roles: [1, 3, 9] },
  { path: '/settle', text: '对账盘', roles: [3, 9] },
  { path: '/stat',   text: '统计',   roles: [3, 9] }
]

const menus = computed(() => MENUS.filter((m) => m.roles.includes(who.role)))

// 详情页 /acct/:id 不属于任何菜单，高亮回落到卡户册
const active = computed(() => (route.path.startsWith('/acct/') ? '/acct' : route.path))
const title = computed(() => route.meta.title || '')

async function doLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  who.logout()
  router.replace('/login')
}
</script>

<style scoped>
.console { height: 100vh; }
.side { background: #1f2430; }
.brand { color: #fff; font-size: 15px; padding: 18px 16px 12px; letter-spacing: 1px; }
.menu { border-right: none; background: transparent; }
.menu :deep(.el-menu-item) { color: #b8bfcc; }
.menu :deep(.el-menu-item.is-active) { color: #fff; background: #2c3444; }
.top { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #e6e8eb; background: #fff; }
.crumb { font-size: 14px; color: #333; }
.who { font-size: 13px; color: #666; }
.who em { font-style: normal; margin-left: 6px; color: #909399; }
.who .out { margin-left: 14px; font-size: 13px; }
.body { background: #f6f7f9; }
</style>
