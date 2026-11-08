<template>
  <div class="page">
    <div class="head">
      <h3>卡户册</h3>
      <div class="tools">
        <el-input
          v-model="keyword"
          placeholder="学号或姓名"
          clearable
          style="width: 200px"
          @keyup.enter="load"
          @clear="load"
        />
        <el-button type="primary" :loading="busy" @click="load">查询</el-button>
        <el-button v-if="who.isBackOffice" @click="openDlg = true">开户</el-button>
      </div>
    </div>

    <el-table :data="rows" v-loading="busy" stripe border style="width: 100%">
      <el-table-column prop="stuNo" label="学号" width="130" />
      <el-table-column prop="stuName" label="姓名" width="100" />
      <el-table-column prop="dept" label="院系班级" min-width="120" />
      <el-table-column label="余额（元）" width="120" align="right">
        <template #default="{ row }">
          <span :class="row.state === 1 ? 'money' : 'money gray'">{{ showYuan(row.bal) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="卡号" min-width="230">
        <template #default="{ row }">
          <el-tag
            v-for="c in row.cards"
            :key="c.cardNo"
            class="cardno"
            :type="faceType(c.state)"
            effect="plain"
          >
            {{ c.cardNo }}<span class="fstate">{{ faceText(c.state) }}</span>
          </el-tag>
          <span v-if="!row.cards || !row.cards.length" class="dim">无卡</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="stateType(row.state)">{{ stateText(row.state) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开户时间" width="120" align="center">
        <template #default="{ row }">{{ (row.createdAt || '').slice(0, 10) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="210" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="goDetail(row)">详情</el-button>
          <template v-if="who.isBackOffice">
            <el-button link type="warning" @click.stop="openFaceDlg(row)">挂失 / 解挂</el-button>
            <el-button link type="danger" @click.stop="doClose(row)">销户</el-button>
          </template>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="没有符合条件的卡户" />
      </template>
    </el-table>

    <p class="tip">共 {{ rows.length }} 个卡户</p>

    <!-- 开户 -->
    <el-dialog v-model="openDlg" title="开户" width="420px" @closed="resetOpen">
      <el-form :model="openForm" label-width="76px">
        <el-form-item label="学号">
          <el-input v-model="openForm.stuNo" placeholder="如 2024081099" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="openForm.stuName" placeholder="如 测试同学" />
        </el-form-item>
        <el-form-item label="院系班级">
          <el-input v-model="openForm.dept" placeholder="如 软工243" />
        </el-form-item>
      </el-form>
      <p class="tip">开户会同时生成一张卡，卡号由后端按 0810 + 卡户流水生成。</p>
      <template #footer>
        <el-button @click="openDlg = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="doOpen">确定开户</el-button>
      </template>
    </el-dialog>

    <!-- 挂失 / 解挂 -->
    <el-dialog v-model="faceDlg" title="卡面操作" width="420px">
      <el-form label-width="76px">
        <el-form-item label="卡户">
          <span>{{ faceRow.stuName }}（{{ faceRow.stuNo }}）</span>
        </el-form-item>
        <el-form-item label="选择卡号">
          <el-select v-model="faceCardNo" placeholder="选一张卡" style="width: 100%">
            <el-option
              v-for="c in faceRow.cards"
              :key="c.cardNo"
              :label="c.cardNo + '（' + faceText(c.state) + '）'"
              :value="c.cardNo"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <p class="tip">
        挂失只影响这张卡，卡户本身不动；解挂要把挂失时间一起清掉。
        重复挂失、或对未挂失的卡解挂，后端都会拒绝。
      </p>
      <template #footer>
        <el-button @click="faceDlg = false">取消</el-button>
        <el-button type="warning" :loading="busy" @click="doFace('lose')">挂失</el-button>
        <el-button type="primary" :loading="busy" @click="doFace('unlock')">解挂</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { acctApi } from '@/api/acct'
import { showYuan } from '@/utils/money'
import { useWhoami } from '@/stores/whoami'

const who = useWhoami()
const router = useRouter()

const rows = ref([])
const keyword = ref('')
const busy = ref(false)

const openDlg = ref(false)
const openForm = reactive({ stuNo: '', stuName: '', dept: '' })

const faceDlg = ref(false)
const faceRow = ref({})
const faceCardNo = ref('')

function stateText(s) {
  return { 1: '正常', 2: '冻结', 3: '注销' }[s] || '未知'
}
function stateType(s) {
  return { 1: 'success', 2: 'danger', 3: 'info' }[s] || 'info'
}
// 卡面状态与卡户状态是两套：卡户 1正常/2冻结/3注销；卡面 1正常/2挂失/3作废
function faceText(s) {
  return { 1: '正常', 2: '挂失', 3: '作废' }[s] || '未知'
}
function faceType(s) {
  return { 1: 'success', 2: 'warning', 3: 'info' }[s] || 'info'
}

async function load() {
  busy.value = true
  try {
    const data = await acctApi.list(keyword.value || undefined)
    // 学生账号后端只返回自己那一条，这里不需要再过滤
    rows.value = Array.isArray(data) ? data : []
  } catch (e) {
    rows.value = []
  } finally {
    busy.value = false
  }
}

function goDetail(row) {
  router.push('/acct/' + row.id)
}

function resetOpen() {
  openForm.stuNo = ''
  openForm.stuName = ''
  openForm.dept = ''
}

async function doOpen() {
  if (!openForm.stuNo || !openForm.stuName) {
    ElMessage.warning('学号和姓名都要填')
    return
  }
  busy.value = true
  try {
    const id = await acctApi.open({ ...openForm })
    ElMessage.success('开户成功，卡户 id = ' + id)
    openDlg.value = false
    await load()
  } finally {
    busy.value = false
  }
}

function openFaceDlg(row) {
  faceRow.value = row
  const cards = row.cards || []
  // 优先选一张正常卡，没有就退回最后一张（比如全挂失了就让人去解挂）
  const normal = cards.find((c) => c.state === 1)
  faceCardNo.value = normal ? normal.cardNo : (cards.length ? cards[cards.length - 1].cardNo : '')
  faceDlg.value = true
}

async function doFace(action) {
  if (!faceCardNo.value) {
    ElMessage.warning('先选一张卡')
    return
  }
  busy.value = true
  try {
    if (action === 'lose') await acctApi.lose(faceCardNo.value)
    else await acctApi.unlock(faceCardNo.value)
    ElMessage.success(action === 'lose' ? '已挂失' : '已解挂')
    faceDlg.value = false
    await load()          // 重新拉一次，卡号标签的颜色和文字立刻变
  } finally {
    busy.value = false
  }
}

async function doClose(row) {
  try {
    await ElMessageBox.confirm(
      `确定注销「${row.stuName}」的卡户？余额必须为 0，且名下所有卡会一并作废。`,
      '销户确认',
      { type: 'warning', confirmButtonText: '确定销户', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  busy.value = true
  try {
    await acctApi.close(row.id)
    ElMessage.success('已销户')
    await load()
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { background: #fff; border-radius: 8px; padding: 18px 20px; }
.head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
h3 { font-size: 15px; margin: 0; color: #303133; }
.tools { display: flex; gap: 8px; }
.money { font-variant-numeric: tabular-nums; color: #303133; }
.money.gray { color: #c0c4cc; }
.cardno { margin: 0 4px 4px 0; }
.cardno .fstate { margin-left: 5px; opacity: .75; font-size: 11.5px; }
.dim { color: #c0c4cc; font-size: 13px; }
.tip { font-size: 12.5px; color: #909399; margin: 12px 0 0; }
</style>
