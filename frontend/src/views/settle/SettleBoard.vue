<template>
  <div class="page">
    <div class="head">
      <h3>对账盘</h3>
      <span class="dim">{{ rows.length }} 台机具 · 差异 {{ diffCnt }} 台</span>
      <div class="sp" />
      <el-date-picker
        v-model="day"
        type="date"
        value-format="YYYY-MM-DD"
        :clearable="false"
        style="width: 150px"
        @change="load"
      />
      <el-button :loading="busy" @click="load">刷新</el-button>
      <el-button type="primary" @click="openGen">生成批次</el-button>
    </div>

    <div class="stats">
      <div class="st">
        <i>系统笔数</i>
        <b>{{ sum.cnt }}</b>
      </div>
      <div class="st">
        <i>机具笔数</i>
        <b>{{ sum.devCnt }}</b>
      </div>
      <div class="st">
        <i>差异</i>
        <b :class="diffCnt ? 'bad' : 'ok'">{{ diffCnt }} 台</b>
      </div>
      <div class="st">
        <i>实付合计</i>
        <b>{{ showYuan(sum.payFen / 100) }}</b>
      </div>
    </div>

    <el-table
      :data="rows"
      v-loading="busy"
      size="small"
      :row-class-name="rowClass"
      empty-text="这一天还没有结算批次"
    >
      <el-table-column type="expand" width="34">
        <template #default="{ row }">
          <div class="desc">
            <span class="k">对账描述</span>
            <code :class="{ over: String(row.desc || '').includes('超支') }">{{ row.desc }}</code>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="机具" min-width="210">
        <template #default="{ row }">
          <b>{{ row.devNo }}</b>
          <span class="dim">　{{ devNameOf(row.devNo) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="系统笔数" width="92" align="right">
        <template #default="{ row }">{{ row.cnt }}</template>
      </el-table-column>
      <el-table-column label="机具笔数" width="92" align="right">
        <template #default="{ row }">{{ row.devCnt }}</template>
      </el-table-column>
      <el-table-column label="差异" width="78" align="right">
        <template #default="{ row }">
          <b :class="Number(row.diff) ? 'bad' : 'ok'">
            {{ Number(row.diff) > 0 ? '+' + row.diff : row.diff }}
          </b>
        </template>
      </el-table-column>
      <el-table-column label="原价" width="92" align="right">
        <template #default="{ row }">{{ showYuan(row.amt) }}</template>
      </el-table-column>
      <el-table-column label="减免" width="92" align="right">
        <template #default="{ row }">
          <span class="rebate">{{ showYuan(row.rebateAmt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="实付" width="100" align="right">
        <template #default="{ row }"><b>{{ showYuan(row.payAmt) }}</b></template>
      </el-table-column>
      <el-table-column label="状态" width="92">
        <template #default="{ row }">
          <el-tag :type="stateType(row.state)" size="small">{{ row.stateName }}</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="gen.open" title="生成结算批次" width="520px">
      <div class="genrow">
        <span class="k">结算日期</span>
        <b>{{ day }}</b>
      </div>
      <div class="genrow">
        <span class="k">机具上报笔数</span>
        <span class="dim">留空表示按系统笔数平账</span>
      </div>
      <div class="devlist">
        <div v-for="d in ALL_DEVS" :key="d.devNo" class="devitem">
          <span class="dn">{{ d.devNo }}</span>
          <span class="dname">{{ d.name }}</span>
          <el-input v-model="gen.input[d.devNo]" placeholder="—" style="width: 84px" />
        </div>
      </div>
      <template #footer>
        <el-button @click="gen.open = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="doGen">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { settleApi } from '@/api/settle'
import { showYuan } from '@/utils/money'
import { today } from '@/utils/datefmt'
import { ALL_DEVS, devNameOf } from '@/utils/devs'

const busy = ref(false)
const rows = ref([])
const day = ref(today())
const gen = reactive({ open: false, input: {} })

/** 累加按整数分算，避免 0.1 + 0.2 这类浮点误差 */
const sum = computed(() =>
  rows.value.reduce(
    (a, r) => ({
      cnt: a.cnt + (r.cnt || 0),
      devCnt: a.devCnt + (r.devCnt || 0),
      payFen: a.payFen + Math.round(Number(r.payAmt || 0) * 100)
    }),
    { cnt: 0, devCnt: 0, payFen: 0 }
  )
)

const diffCnt = computed(() => rows.value.filter((r) => Number(r.diff) !== 0).length)

async function load() {
  busy.value = true
  try {
    rows.value = (await settleApi.list(day.value)) || []
  } catch (e) {
    rows.value = []
  } finally {
    busy.value = false
  }
}

function openGen() {
  gen.input = {}
  ALL_DEVS.forEach((d) => {
    gen.input[d.devNo] = ''
  })
  gen.open = true
}

async function doGen() {
  const rep = {}
  Object.keys(gen.input).forEach((k) => {
    const v = String(gen.input[k] ?? '').trim()
    if (v !== '' && /^\d+$/.test(v)) rep[k] = Number(v)
  })

  busy.value = true
  try {
    const list = await settleApi.generate(day.value, Object.keys(rep).length ? rep : null)
    gen.open = false
    await load()
    if (!list || !list.length) {
      ElMessage.warning('这一天没有待结算的消费单')
    } else {
      ElMessage.success('已生成 ' + list.length + ' 条批次')
    }
  } finally {
    busy.value = false
  }
}

function stateType(s) {
  return { 0: 'info', 1: 'success', 2: 'danger' }[s] || 'info'
}

function rowClass({ row }) {
  return Number(row.diff) !== 0 ? 'has-diff' : ''
}

onMounted(load)
</script>

<style scoped>
.page { background: #fff; border-radius: 8px; padding: 18px 20px; }
.head { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
h3 { font-size: 15px; margin: 0; color: #303133; }
.sp { flex: 1; }
.dim { font-size: 12.5px; color: #909399; }
.stats { display: flex; gap: 14px; margin-bottom: 14px; }
.st { flex: 1; background: #fafbfc; border-radius: 6px; padding: 12px 14px; }
.st i { display: block; font-style: normal; font-size: 12.5px; color: #909399; margin-bottom: 6px; }
.st b { font-size: 22px; color: #303133; font-variant-numeric: tabular-nums; }
.bad { color: #f56c6c; }
.ok { color: #67c23a; }
.rebate { color: #e6a23c; }
.desc { padding: 8px 16px 10px 48px; background: #fafbfc; }
.desc .k { font-size: 12px; color: #909399; margin-right: 10px; }
.desc code { font-size: 12.5px; color: #606266; background: transparent; }
.desc code.over { color: #f56c6c; font-weight: 600; }
:deep(.el-table__row.has-diff) td { background: #fef0f0 !important; }
.genrow { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; font-size: 13px; }
.genrow .k { color: #909399; width: 96px; }
.devlist { border: 1px solid #ebeef5; border-radius: 6px; overflow: hidden; }
.devitem { display: flex; align-items: center; gap: 10px; padding: 6px 12px; border-bottom: 1px solid #f2f3f5; }
.devitem:last-child { border-bottom: none; }
.dn { font-size: 12.5px; color: #303133; font-weight: 600; width: 44px; }
.dname { flex: 1; font-size: 12.5px; color: #606266; }
</style>
