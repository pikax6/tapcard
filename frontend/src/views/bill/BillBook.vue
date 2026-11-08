<template>
  <div class="page">
    <div class="head">
      <h3>账单簿</h3>
      <span class="dim">共 {{ total }} 条</span>
      <el-button size="small" :loading="busy" @click="load">刷新</el-button>
    </div>

    <el-form :inline="true" class="filters">
      <el-form-item label="关键字">
        <el-input
          v-model="q.keyword"
          placeholder="单号 / 机具号"
          clearable
          style="width: 190px"
          @keyup.enter="search"
        />
      </el-form-item>
      <el-form-item label="日期">
        <el-date-picker
          v-model="range"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="起始日"
          end-placeholder="结束日"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="q.kind" placeholder="全部" clearable style="width: 108px">
          <el-option label="充值" :value="1" />
          <el-option label="消费" :value="2" />
          <el-option label="冲正" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="q.state" placeholder="全部" clearable style="width: 108px">
          <el-option label="待结算" :value="0" />
          <el-option label="已结算" :value="1" />
          <el-option label="已冲正" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table
      :data="rows"
      v-loading="busy"
      size="small"
      empty-text="没有符合条件的账单"
      :row-class-name="rowClass"
    >
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
      <el-table-column label="卡户" width="90">
        <template #default="{ row }">{{ row.stuName || '—' }}</template>
      </el-table-column>
      <el-table-column label="机具" width="76">
        <template #default="{ row }">{{ row.devNo || '—' }}</template>
      </el-table-column>
      <el-table-column label="原价" width="86" align="right">
        <template #default="{ row }">{{ showYuan(row.amt) }}</template>
      </el-table-column>
      <el-table-column label="减免" width="86" align="right">
        <template #default="{ row }">
          <span class="rebate">{{ showYuan(row.rebateAmt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="实付" width="96" align="right">
        <template #default="{ row }">
          <b :class="Number(row.payAmt) < 0 ? 'back' : 'pay'">{{ showYuan(row.payAmt) }}</b>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="88">
        <template #default="{ row }">
          <el-tag :type="stateType(row.state)" size="small">{{ row.stateName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="148">
        <template #default="{ row }">
          <span class="dim">{{ fmtTime(row.createdAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="72" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="canReverse(row)"
            link
            type="warning"
            size="small"
            @click="openReverse(row)"
          >
            冲正
          </el-button>
          <span v-else class="dim">—</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="foot">
      <el-pagination
        layout="total, sizes, prev, pager, next"
        :total="total"
        :current-page="q.page"
        :page-size="q.size"
        :page-sizes="[10, 20, 50]"
        @current-change="onPage"
        @size-change="onSize"
      />
    </div>

    <el-dialog v-model="rv.open" title="冲正账单" width="470px">
      <div class="rvbox">
        <div class="rrow">
          <span class="k">原单号</span>
          <code>{{ rv.billNo }}</code>
        </div>
        <div class="rrow">
          <span class="k">卡户</span>
          <b>{{ rv.stuName }}</b>
        </div>
        <div class="nums">
          <div class="n">
            <i>原价</i>
            <b>{{ showYuan(rv.amt) }}</b>
          </div>
          <div class="n">
            <i>减免</i>
            <b class="rebate">{{ showYuan(rv.rebateAmt) }}</b>
          </div>
          <div class="n">
            <i>退回</i>
            <b class="back">{{ showYuan(rv.payAmt) }}</b>
          </div>
        </div>
      </div>
      <el-form label-width="80px" class="rvform">
        <el-form-item label="冲正原因">
          <el-input v-model="rv.reason" placeholder="如 学生投诉重复扣款" />
        </el-form-item>
      </el-form>
      <div class="rvhint">冲正会新开一张负向账单，并把原单标记为「已冲正」，原单不会被删除</div>
      <template #footer>
        <el-button @click="rv.open = false">取消</el-button>
        <el-button type="warning" :loading="busy" @click="doReverse">确认冲正</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { payApi } from '@/api/pay'
import { showYuan } from '@/utils/money'
import { useWhoami } from '@/stores/whoami'

const who = useWhoami()

const busy = ref(false)
const rows = ref([])
const total = ref(0)
const range = ref(null)

const q = reactive({ page: 1, size: 10, keyword: '', kind: null, state: null })

const rv = reactive({ open: false, id: null, billNo: '', stuName: '', amt: 0, rebateAmt: 0, payAmt: 0, reason: '' })

function params() {
  const p = {
    page: q.page,
    size: q.size,
    keyword: q.keyword || undefined,
    kind: q.kind === null || q.kind === '' ? undefined : q.kind,
    state: q.state === null || q.state === '' ? undefined : q.state
  }
  if (range.value && range.value.length === 2) {
    p.from = range.value[0]
    p.to = range.value[1]
  }
  return p
}

async function load() {
  busy.value = true
  try {
    const d = await payApi.bills(params())
    rows.value = d.rows || []
    total.value = d.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    busy.value = false
  }
}

function search() {
  q.page = 1
  load()
}

function reset() {
  q.keyword = ''
  q.kind = null
  q.state = null
  range.value = null
  q.page = 1
  load()
}

function onPage(p) {
  q.page = p
  load()
}

function onSize(s) {
  q.size = s
  q.page = 1
  load()
}

function kindType(k) {
  return { 1: 'success', 2: 'primary', 3: 'warning' }[k] || 'info'
}

function stateType(s) {
  return { 0: 'warning', 1: 'success', 2: 'info' }[s] || 'info'
}

function rowClass({ row }) {
  return row.state === 2 ? 'is-reversed' : ''
}

/** 与后端约束保持一致：只有消费单、且尚未冲正、且当前账号有权限时才出按钮 */
function canReverse(row) {
  return who.isBackOffice && row.kind === 2 && row.state !== 2
}

function fmtTime(s) {
  if (!s) return ''
  return String(s).replace('T', ' ').slice(0, 19)
}

function openReverse(row) {
  rv.open = true
  rv.id = row.id
  rv.billNo = row.billNo
  rv.stuName = row.stuName || '—'
  rv.amt = row.amt
  rv.rebateAmt = row.rebateAmt
  rv.payAmt = row.payAmt
  rv.reason = ''
}

async function doReverse() {
  busy.value = true
  try {
    await payApi.reverse(rv.id, rv.reason || '柜面冲正')
    rv.open = false
    // 不跳页：原单就在当前页，刷新后能直接看到它变成「已冲正」
    await load()
    ElMessage.success('已冲正，原单状态变为「已冲正」')
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { background: #fff; border-radius: 8px; padding: 18px 20px; }
.head { display: flex; align-items: baseline; gap: 12px; margin-bottom: 14px; }
h3 { font-size: 15px; margin: 0; color: #303133; }
.dim { font-size: 12.5px; color: #909399; }
.filters { margin-bottom: 4px; }
.filters :deep(.el-form-item) { margin-bottom: 12px; }
.bn { background: #f2f3f5; padding: 1px 6px; border-radius: 4px; font-size: 12px; color: #606266; }
.rebate { color: #e6a23c; }
.pay { color: #303133; }
.back { color: #67c23a; }
:deep(.is-reversed) { color: #c0c4cc; }
:deep(.is-reversed) .bn { color: #c0c4cc; }
.foot { display: flex; justify-content: flex-end; margin-top: 14px; }
.rvbox { background: #fafbfc; border-radius: 6px; padding: 12px 14px; }
.rrow { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; font-size: 13px; }
.rrow .k { color: #909399; width: 56px; }
.rrow code { background: #f2f3f5; padding: 1px 6px; border-radius: 4px; font-size: 12px; color: #606266; }
.nums { display: flex; gap: 36px; margin-top: 10px; }
.n i { display: block; font-size: 12px; color: #909399; font-style: normal; }
.n b { font-size: 20px; font-variant-numeric: tabular-nums; color: #303133; }
.rvform { margin-top: 14px; }
.rvhint { font-size: 12.5px; color: #909399; line-height: 1.6; }
</style>
