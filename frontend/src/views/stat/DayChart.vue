<template>
  <div class="page">
    <div class="head">
      <h3>统计</h3>
      <span class="dim">{{ rangeLabel }}</span>
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
    </div>

    <el-radio-group v-model="tab" class="tabs" @change="load">
      <el-radio-button value="stall">按档口</el-radio-button>
      <el-radio-button value="dev">按机具</el-radio-button>
      <el-radio-button value="trend">按日期趋势</el-radio-button>
    </el-radio-group>

    <div ref="chartEl" v-loading="busy" class="chart" />

    <el-table :data="rows" size="small" class="tbl" empty-text="这一天没有消费数据">
      <el-table-column :label="firstCol" min-width="180">
        <template #default="{ row }">{{ row.label }}</template>
      </el-table-column>
      <el-table-column label="笔数" width="90" align="right">
        <template #default="{ row }">{{ row.cnt }}</template>
      </el-table-column>
      <el-table-column label="原价" width="100" align="right">
        <template #default="{ row }">{{ showYuan(row.amt) }}</template>
      </el-table-column>
      <el-table-column label="减免" width="100" align="right">
        <template #default="{ row }"><span class="rebate">{{ showYuan(row.rebateAmt) }}</span></template>
      </el-table-column>
      <el-table-column label="实付" width="110" align="right">
        <template #default="{ row }"><b>{{ showYuan(row.payAmt) }}</b></template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { computed, nextTick, onUnmounted, ref, onMounted } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { statApi } from '@/api/stat'
import { showYuan } from '@/utils/money'
import { today, pad2 } from '@/utils/datefmt'

echarts.use([BarChart, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const busy = ref(false)
const tab = ref('stall')
const day = ref(today())
const rows = ref([])
const chartEl = ref(null)

let inst = null
let lastOption = null

const firstCol = computed(() => (tab.value === 'trend' ? '日期' : '名称'))
const rangeLabel = computed(() => (tab.value === 'trend' ? minusDays(day.value, 6) + ' 至 ' + day.value : day.value))

function minusDays(iso, n) {
  const [y, m, d] = iso.split('-').map(Number)
  const dt = new Date(y, m - 1, d)
  dt.setDate(dt.getDate() - n)
  return dt.getFullYear() + '-' + pad2(dt.getMonth() + 1) + '-' + pad2(dt.getDate())
}

function toRow(r) {
  return {
    label: r.name || r.key,
    cnt: r.cnt || 0,
    amt: r.amt || 0,
    rebateAmt: r.rebateAmt || 0,
    payAmt: r.payAmt || 0
  }
}

function pieOption(list) {
  return {
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 笔（{d}%）' },
    legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12 } },
    series: [
      {
        type: 'pie',
        radius: ['42%', '66%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{c} 笔', fontSize: 12 },
        data: list.map((r) => ({ name: r.label, value: r.cnt }))
      }
    ]
  }
}

function barOption(list) {
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 52, right: 24, top: 34, bottom: 46 },
    xAxis: { type: 'category', data: list.map((r) => r.label), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', name: '实付（元）', nameTextStyle: { fontSize: 11 } },
    series: [
      {
        type: 'bar',
        barMaxWidth: 44,
        itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
        label: { show: true, position: 'top', fontSize: 11 },
        data: list.map((r) => r.payAmt)
      }
    ]
  }
}

function lineOption(list) {
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 52, right: 28, top: 34, bottom: 46 },
    xAxis: { type: 'category', boundaryGap: false, data: list.map((r) => r.label), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', name: '金额（元）', nameTextStyle: { fontSize: 11 } },
    legend: { top: 0, right: 10, itemWidth: 14, itemHeight: 8, textStyle: { fontSize: 12 } },
    series: [
      {
        name: '实付',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        itemStyle: { color: '#67c23a' },
        areaStyle: { opacity: 0.14 },
        label: { show: true, position: 'top', fontSize: 11 },
        data: list.map((r) => r.payAmt)
      },
      {
        name: '补贴',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        itemStyle: { color: '#e6a23c' },
        label: { show: true, position: 'bottom', fontSize: 11 },
        data: list.map((r) => r.rebateAmt)
      }
    ]
  }
}

function render() {
  if (!chartEl.value) return
  // 每次重建：切 tab 时容器尺寸会变，复用实例容易出现半张图
  if (inst) {
    inst.dispose()
    inst = null
  }
  inst = echarts.init(chartEl.value)
  if (lastOption) inst.setOption(lastOption)
}

async function load() {
  busy.value = true
  try {
    let list = []
    if (tab.value === 'stall') {
      list = ((await statApi.byStall(day.value)) || []).map(toRow)
      lastOption = pieOption(list)
    } else if (tab.value === 'dev') {
      list = ((await statApi.byDev(day.value)) || []).map(toRow)
      lastOption = barOption(list)
    } else {
      const from = minusDays(day.value, 6)
      list = ((await statApi.byDay(from, day.value)) || []).map(toRow)
      lastOption = lineOption(list)
    }
    rows.value = list
  } catch (e) {
    rows.value = []
    lastOption = null
  } finally {
    busy.value = false
  }
  await nextTick()
  render()
}

function onResize() {
  if (inst) inst.resize()
}

onMounted(() => {
  window.addEventListener('resize', onResize)
  load()
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  if (inst) {
    inst.dispose()
    inst = null
  }
})
</script>

<style scoped>
.page { background: #fff; border-radius: 8px; padding: 18px 20px; }
.head { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
h3 { font-size: 15px; margin: 0; color: #303133; }
.sp { flex: 1; }
.dim { font-size: 12.5px; color: #909399; }
.tabs { margin-bottom: 6px; }
.chart { width: 100%; height: 320px; }
.tbl { margin-top: 8px; }
.rebate { color: #e6a23c; }
</style>
