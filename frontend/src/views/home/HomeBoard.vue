<template>
  <div class="page">
    <div class="head">
      <h3>概览</h3>
      <span class="dim">{{ who.roleLabel }} · {{ todayStr }}</span>
      <div class="sp" />
      <el-button size="small" :loading="busy" @click="load">刷新</el-button>
    </div>

    <div class="cards">
      <div
        v-for="c in cards"
        :key="c.label"
        class="card"
        :class="[c.tone, c.to ? 'clickable' : '']"
        @click="c.to && router.push(c.to)"
      >
        <i>{{ c.label }}</i>
        <b>{{ c.value }}<em v-if="c.unit">{{ c.unit }}</em></b>
        <span class="sub">{{ c.sub || '' }}</span>
      </div>
    </div>

    <div class="sec">
      <h4>最近账单</h4>
      <span class="dim" v-if="hint">{{ hint }}</span>
    </div>

    <el-empty v-if="!rows.length" :description="emptyText" :image-size="76" />
    <el-table v-else :data="rows" size="small">
      <el-table-column label="单号" min-width="176">
        <template #default="{ row }">
          <code class="bn">{{ row.billNo }}</code>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="76">
        <template #default="{ row }">
          <el-tag :type="kindType(row.kind)" size="small" effect="plain">{{ row.kindName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="卡户" width="88">
        <template #default="{ row }">{{ row.stuName || '—' }}</template>
      </el-table-column>
      <el-table-column label="机具" width="74">
        <template #default="{ row }">{{ row.devNo || '—' }}</template>
      </el-table-column>
      <el-table-column label="实付" width="94" align="right">
        <template #default="{ row }">
          <b>{{ showYuan(row.payAmt) }}</b>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="88">
        <template #default="{ row }">
          <el-tag :type="stateType(row.state)" size="small">{{ row.stateName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="150">
        <template #default="{ row }">
          <span class="dim">{{ fmtTime(row.createdAt) }}</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { acctApi } from '@/api/acct'
import { payApi } from '@/api/pay'
import { settleApi } from '@/api/settle'
import { statApi } from '@/api/stat'
import { showYuan } from '@/utils/money'
import { today, pad2 } from '@/utils/datefmt'
import { useWhoami } from '@/stores/whoami'

const who = useWhoami()
const router = useRouter()

const busy = ref(false)
const cards = ref([])
const rows = ref([])
const hint = ref('')
const emptyText = ref('还没有账单')
const todayStr = today()

/** 金额一律按整数分累加，最后再除以 100 */
const fen = (v) => Math.round(Number(v || 0) * 100)
const yuan = (f) => showYuan(f / 100)

function firstDayOfMonth() {
  const d = new Date()
  return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-01'
}

function fmtTime(s) {
  if (!s) return ''
  return String(s).replace('T', ' ').slice(0, 19)
}

function kindType(k) {
  return { 1: 'success', 2: 'primary', 3: 'warning' }[k] || 'info'
}

function stateType(s) {
  return { 0: 'warning', 1: 'success', 2: 'info' }[s] || 'info'
}

/** 学生：只关心自己的余额和本月花销 */
async function loadStudent() {
  const acct = await acctApi.detail(who.acctId)
  const page = await payApi.bills({ page: 1, size: 200, from: firstDayOfMonth(), to: todayStr })
  const list = page.rows || []

  let eatCnt = 0
  let eatFen = 0
  let inCnt = 0
  let inFen = 0
  for (const b of list) {
    if (b.kind === 2) {
      eatCnt++
      eatFen += fen(b.payAmt)
    } else if (b.kind === 1) {
      inCnt++
      inFen += fen(b.payAmt)
    }
  }

  const stateText = { 1: '正常', 2: '冻结', 3: '已注销' }[acct.state] || '未知'
  cards.value = [
    { label: '卡内余额', value: showYuan(acct.bal), unit: '元', sub: '卡户状态 ' + stateText, tone: 'main' },
    { label: '本月消费', value: eatCnt, unit: '笔', sub: '合计 ' + yuan(eatFen) + ' 元' },
    { label: '本月充值', value: inCnt, unit: '笔', sub: '合计 ' + yuan(inFen) + ' 元' },
    { label: '名下卡面', value: (acct.cards || []).length, unit: '张', sub: (acct.cards || []).map((c) => c.cardNo).join(' / ') || '无卡' }
  ]
  rows.value = list.slice(0, 8)
  hint.value = '本月共 ' + list.length + ' 条账单'
  emptyText.value = '本月还没有账单'
}

/** 商户：只看自己机具当天的流水 */
async function loadMerchant() {
  const page = await payApi.bills({ page: 1, size: 200, from: todayStr, to: todayStr })
  const list = (page.rows || []).filter((b) => b.kind === 2)
  let cnt = 0
  let amt = 0
  let pay = 0
  for (const b of list) {
    cnt++
    amt += fen(b.amt)
    pay += fen(b.payAmt)
  }
  const avg = cnt ? Math.round(pay / cnt) : 0

  cards.value = [
    { label: '今日笔数', value: cnt, unit: '笔', tone: 'main' },
    { label: '今日实付', value: yuan(pay), unit: '元', sub: '原价 ' + yuan(amt) + ' 元' },
    { label: '今日减免', value: yuan(amt - pay), unit: '元', sub: '补贴支出' },
    { label: '笔均消费', value: yuan(avg), unit: '元', sub: '机具 ' + (who.devNo || '—') }
  ]
  rows.value = list.slice(0, 8)
  hint.value = cnt ? '' : '今天还没有交易'
  emptyText.value = '今天还没有交易'
}

/** 财务 / 管理员：全局视角 */
async function loadBackOffice() {
  const dayStat = (await statApi.byDay(todayStr, todayStr)) || []
  const accts = (await acctApi.list()) || []
  const settle = (await settleApi.list(todayStr)) || []
  const page = await payApi.bills({ page: 1, size: 8 })

  let cnt = 0
  let pay = 0
  let amt = 0
  for (const r of dayStat) {
    cnt += r.cnt || 0
    pay += fen(r.payAmt)
    amt += fen(r.amt)
  }
  const diffDev = settle.filter((s) => Number(s.diff) !== 0).length
  const frozen = accts.filter((a) => a.state === 2).length

  cards.value = [
    { label: '今日笔数', value: cnt, unit: '笔', sub: '原价 ' + yuan(amt) + ' 元', tone: 'main' },
    { label: '今日实付', value: yuan(pay), unit: '元', sub: '在校生实扣金额' },
    { label: '补贴支出', value: yuan(amt - pay), unit: '元', sub: '在册卡户 ' + accts.length + ' 人 · 冻结 ' + frozen + ' 人' },
    {
      label: '差异机具',
      value: diffDev,
      unit: '台',
      sub: settle.length ? '今日已对账 ' + settle.length + ' 台，点此查看' : '今日未生成批次，点此对账',
      tone: diffDev ? 'bad' : '',
      to: '/settle'
    }
  ]
  rows.value = page.rows || []
  hint.value = cnt ? '' : '今天还没有交易'
  emptyText.value = '今天还没有交易'
}

async function load() {
  busy.value = true
  try {
    if (who.isStudent) await loadStudent()
    else if (who.isMerchant) await loadMerchant()
    else await loadBackOffice()
  } catch (e) {
    cards.value = []
    rows.value = []
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { background: #fff; border-radius: 8px; padding: 18px 20px; }
.head { display: flex; align-items: baseline; gap: 12px; margin-bottom: 16px; }
h3 { font-size: 15px; margin: 0; color: #303133; }
.sp { flex: 1; }
.dim { font-size: 12.5px; color: #909399; }
.cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 22px; }
.card { background: #fafbfc; border-radius: 8px; padding: 14px 16px; border-left: 3px solid #e4e7ed; }
.card.main { border-left-color: #409eff; }
.card.bad { border-left-color: #f56c6c; }
.card.clickable { cursor: pointer; transition: background 0.15s; }
.card.clickable:hover { background: #f2f6fc; }
.card i { display: block; font-style: normal; font-size: 12.5px; color: #909399; margin-bottom: 8px; }
.card b { font-size: 26px; color: #303133; font-variant-numeric: tabular-nums; line-height: 1.2; }
.card b em { font-size: 13px; color: #909399; font-style: normal; margin-left: 3px; }
.card .sub { display: block; margin-top: 6px; font-size: 12px; color: #909399; }
.card.bad b { color: #f56c6c; }
.sec { display: flex; align-items: baseline; gap: 12px; margin-bottom: 10px; }
h4 { font-size: 13.5px; margin: 0; color: #303133; font-weight: 600; }
.bn { background: #f2f3f5; padding: 1px 6px; border-radius: 4px; font-size: 12px; color: #606266; }
</style>
