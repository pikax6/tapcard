<template>
  <div class="page">
    <div class="head">
      <div class="left">
        <el-button @click="goBack">← 返回卡户册</el-button>
        <h3>卡户详情</h3>
      </div>
      <el-button :loading="busy" @click="load">刷新</el-button>
    </div>

    <!-- 越权或卡户不存在时走这里，不白屏 -->
    <el-result
      v-if="err"
      icon="warning"
      title="无法查看该卡户"
      :sub-title="err"
    >
      <template #extra>
        <el-button type="primary" @click="goBack">返回卡户册</el-button>
      </template>
    </el-result>

    <template v-else>
      <!-- 卡户信息 -->
      <el-card shadow="never" class="card" v-loading="busy">
        <div class="row">
          <div class="who">
            <span class="name">{{ info.stuName || '—' }}</span>
            <span class="no">{{ info.stuNo }}</span>
            <el-tag :type="stateType(info.state)" size="small">{{ stateText(info.state) }}</el-tag>
          </div>
          <div class="bal">
            <span class="lbl">余额（元）</span>
            <span class="num">{{ showYuan(info.bal) }}</span>
          </div>
        </div>

        <div class="meta">
          <span>院系班级：{{ info.dept || '—' }}</span>
          <span>开户时间：{{ (info.createdAt || '').slice(0, 10) || '—' }}</span>
        </div>

        <div class="faces">
          <span class="lbl">名下卡面</span>
          <el-tag
            v-for="c in info.cards"
            :key="c.cardNo"
            :type="faceType(c.state)"
            effect="plain"
            class="facetag"
          >
            {{ c.cardNo }}<span class="fstate">{{ faceText(c.state) }}</span>
          </el-tag>
          <span v-if="!info.cards || !info.cards.length" class="dim">无卡</span>
        </div>
      </el-card>

      <!-- 流水 -->
      <div class="bar">
        <span class="lbl">账户流水</span>
        <el-date-picker
          v-model="range"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
          @change="loadFlows"
        />
      </div>

      <el-table :data="flows" v-loading="busy" stripe border style="width: 100%">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ (row.createdAt || '').replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="kindType(row.kind)" size="small">{{ kindText(row.kind) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变动（元）" width="120" align="right">
          <template #default="{ row }">
            <span :class="Number(row.chgAmt) < 0 ? 'minus' : 'plus'">
              {{ Number(row.chgAmt) > 0 ? '+' : '' }}{{ showYuan(row.chgAmt) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="变动前" width="110" align="right">
          <template #default="{ row }">{{ showYuan(row.balBefore) }}</template>
        </el-table-column>
        <el-table-column label="变动后" width="110" align="right">
          <template #default="{ row }">{{ showYuan(row.balAfter) }}</template>
        </el-table-column>
        <el-table-column label="关联账单" prop="billId" width="100" align="center" />
        <template #empty>
          <el-empty description="该区间没有流水" />
        </template>
      </el-table>

      <p class="tip">共 {{ flows.length }} 条流水</p>
    </template>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { acctApi } from '@/api/acct'
import { showYuan } from '@/utils/money'

const route = useRoute()
const router = useRouter()

const id = route.params.id
const busy = ref(false)
const err = ref('')
const info = reactive({})
const flows = ref([])
const range = ref(defaultRange())

function defaultRange() {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 30)
  const fmt = (d) => d.toISOString().slice(0, 10)
  return [fmt(start), fmt(end)]
}

function stateText(s) {
  return { 1: '正常', 2: '冻结', 3: '注销' }[s] || '未知'
}
function stateType(s) {
  return { 1: 'success', 2: 'danger', 3: 'info' }[s] || 'info'
}
function faceText(s) {
  return { 1: '正常', 2: '挂失', 3: '作废' }[s] || '未知'
}
function faceType(s) {
  return { 1: 'success', 2: 'warning', 3: 'info' }[s] || 'info'
}
function kindText(k) {
  return { 1: '充值', 2: '消费', 3: '冲正' }[k] || '未知'
}
function kindType(k) {
  return { 1: 'success', 2: 'danger', 3: 'warning' }[k] || 'info'
}

/** 有历史就退一步，没有（直接粘链接进来）就回卡户册，避免退到站外 */
function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/acct')
}

async function load() {
  busy.value = true
  err.value = ''
  try {
    const d = await acctApi.detail(id)
    Object.assign(info, d)
    await loadFlows()
  } catch (e) {
    // 学生看别人的卡户会被后端 AcctGuard 拦成 4001，这里给一个可读页面而不是空白
    err.value = (e && e.msg) ? e.msg : '加载失败，请稍后重试'
  } finally {
    busy.value = false
  }
}

async function loadFlows() {
  if (!range.value || range.value.length !== 2) return
  try {
    const list = await acctApi.flows(id, range.value[0], range.value[1])
    flows.value = Array.isArray(list) ? list : []
  } catch (e) {
    flows.value = []
  }
}

onMounted(load)
</script>

<style scoped>
.page { background: #fff; border-radius: 8px; padding: 18px 20px; }
.head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.head .left { display: flex; align-items: center; gap: 12px; }
h3 { font-size: 15px; margin: 0; color: #303133; }
.card { margin-bottom: 16px; }
.row { display: flex; align-items: flex-end; justify-content: space-between; }
.who { display: flex; align-items: center; gap: 10px; }
.name { font-size: 19px; font-weight: 600; color: #303133; }
.no { font-size: 13px; color: #909399; }
.bal { text-align: right; }
.bal .lbl { display: block; font-size: 12.5px; color: #909399; }
.bal .num { font-size: 26px; font-weight: 700; color: #303133; font-variant-numeric: tabular-nums; }
.meta { display: flex; gap: 26px; margin-top: 12px; font-size: 13px; color: #606266; }
.faces { margin-top: 12px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.faces .lbl { font-size: 13px; color: #909399; }
.facetag .fstate { margin-left: 5px; opacity: .75; font-size: 11.5px; }
.bar { display: flex; align-items: center; justify-content: space-between; margin: 6px 0 10px; }
.bar .lbl { font-size: 14px; color: #303133; font-weight: 600; }
.minus { color: #f56c6c; font-variant-numeric: tabular-nums; }
.plus { color: #67c23a; font-variant-numeric: tabular-nums; }
.dim { color: #c0c4cc; font-size: 13px; }
.tip { font-size: 12.5px; color: #909399; margin: 12px 0 0; }
</style>
