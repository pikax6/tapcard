<template>
  <div class="pad">
    <div class="card">
      <h2>一卡通消费与结算系统</h2>
      <el-form :model="f" label-width="64px" @submit.prevent>
        <el-form-item label="工号">
          <el-input v-model="f.empNo" placeholder="如 2024081084" @keyup.enter="doLogin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="f.pwd" type="password" show-password @keyup.enter="doLogin" />
        </el-form-item>
        <el-button type="primary" style="width: 100%" :loading="busy" @click="doLogin">登录</el-button>
      </el-form>

      <div class="demo">
        <span class="dl">演示账号</span>
        <button v-for="d in demos" :key="d.empNo" class="chip" type="button" @click="fill(d)">
          {{ d.role }}　{{ d.empNo }}
        </button>
      </div>
      <p class="tip">点击账号自动填入，密码统一 123456</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useWhoami } from '@/stores/whoami'

const f = reactive({ empNo: '', pwd: '' })
const busy = ref(false)
const router = useRouter()
const route = useRoute()
const who = useWhoami()

/** 与 sql/ykt_seed.sql 的 op_user 一致，改种子时同步改这里 */
const demos = [
  { role: '学生', empNo: '2024081084' },
  { role: '商户', empNo: 'M0001' },
  { role: '财务', empNo: 'F0001' },
  { role: '管理员', empNo: 'A0001' }
]

/** 每个角色落在自己最有用的那一页：学生看账、商户刷卡、财务看全局 */
const HOME = { 1: '/bill', 2: '/dev', 3: '/home', 9: '/home' }

function fill(d) {
  f.empNo = d.empNo
  f.pwd = '123456'
}

async function doLogin() {
  if (!f.empNo || !f.pwd) {
    ElMessage.warning('工号和密码都要填')
    return
  }
  busy.value = true
  try {
    await who.login(f.empNo, f.pwd)
    router.replace(route.query.back || HOME[who.role] || '/home')
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.pad { height: 100vh; display: flex; align-items: center; justify-content: center; background: #f6f7f9; }
.card { width: 356px; padding: 30px 28px 20px; background: #fff; border-radius: 10px; box-shadow: 0 2px 14px rgba(0, 0, 0, 0.06); }
h2 { font-size: 17px; margin: 0 0 22px; text-align: center; color: #303133; letter-spacing: 0.5px; }
.demo { margin-top: 20px; padding-top: 16px; border-top: 1px dashed #ebeef5; }
.dl { display: block; font-size: 12px; color: #909399; margin-bottom: 9px; }
.chip { display: inline-block; margin: 0 6px 6px 0; padding: 4px 9px; font-size: 12px; color: #606266;
  background: #f4f5f7; border: 1px solid #e4e7ed; border-radius: 12px; cursor: pointer; }
.chip:hover { color: #409eff; border-color: #b3d8ff; background: #ecf5ff; }
.tip { font-size: 12px; color: #a8abb2; margin: 10px 0 0; text-align: center; }
</style>
