<template>
  <div class="page">
    <div class="head">
      <h3>机具台</h3>
    </div>

    <!-- 机具选择 -->
    <el-card shadow="never" class="dev">
      <div class="devrow">
        <span class="lbl">当前机具</span>
        <el-select v-model="devNo" style="width: 300px" @change="onDevChange">
          <el-option
            v-for="d in devs"
            :key="d.devNo"
            :label="d.devNo + '　' + d.name"
            :value="d.devNo"
          />
        </el-select>
        <el-tag :type="dev.kindType" effect="plain">{{ dev.kindName }}</el-tag>
        <span class="dim" v-if="dev.stallName">档口：{{ dev.stallName }}</span>
      </div>
      <div class="fmt">
        <span class="lbl">报文格式</span>
        <code>{{ dev.sample }}</code>
      </div>
    </el-card>

    <el-tabs v-model="tab" class="tabs">
      <!-- 刷卡 -->
      <el-tab-pane label="刷卡消费" name="consume">
        <el-form :model="cf" label-width="90px" class="form">
          <el-form-item label="卡号">
            <el-input v-model="cf.cardNo" placeholder="如 0810840001" style="width: 240px" />
          </el-form-item>
          <el-form-item label="金额（元）">
            <el-input-number v-model="cf.amtYuan" :min="0.01" :precision="2" :step="1" style="width: 180px" />
          </el-form-item>
          <el-form-item label="卡密码">
            <el-input v-model="cf.pwd" placeholder="30 元以上才需要，6 位数字" style="width: 240px" />
          </el-form-item>
          <el-form-item label="报文预览">
            <code class="raw">{{ rawPreview }}</code>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="busy" @click="doConsume">刷卡</el-button>
          </el-form-item>
          <el-form-item label=" ">
            <span class="dim">
              同一张卡 1 分钟内最多刷 3 笔，第 4 笔会被风控拦下；如果遇到提示，请稍等一分钟再办理，或提醒持卡人换一张卡。
            </span>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 充值 -->
      <el-tab-pane label="充值" name="recharge">
        <el-form :model="rf" label-width="90px" class="form">
          <el-form-item label="卡号">
            <el-input v-model="rf.cardNo" placeholder="如 0810840001" style="width: 240px" />
          </el-form-item>
          <el-form-item label="充值金额">
            <el-input-number v-model="rf.amtYuan" :min="0.01" :precision="2" :step="10" style="width: 180px" />
          </el-form-item>
          <el-form-item label="请求号">
            <el-input v-model="rf.reqSn" placeholder="可留空，留空由后端生成" style="width: 240px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="busy" @click="doRecharge">充值</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 冲正 -->
      <el-tab-pane label="冲正" name="reverse">
        <el-form :model="vf" label-width="90px" class="form">
          <el-form-item label="原单 ID">
            <el-input v-model.number="vf.billId" placeholder="账单列表里的 id" style="width: 200px" />
          </el-form-item>
          <el-form-item label="冲正原因">
            <el-input v-model="vf.reason" placeholder="如 学生投诉重复扣款" style="width: 320px" />
          </el-form-item>
          <el-form-item>
            <el-button type="warning" :loading="busy" @click="doReverse">提交冲正</el-button>
            <span class="dim">冲正会新开一张负向单，原单标记为「已冲正」，不是把余额加回去</span>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <!-- 结果 -->
    <el-alert
      v-if="err"
      :title="err"
      type="error"
      show-icon
      :closable="false"
      class="alert"
    />

    <el-card v-if="bill" shadow="never" class="result">
      <div class="rhead">
        <span class="rname">交易成功</span>
        <el-tag :type="bill.state === 0 ? 'warning' : 'success'" size="small">{{ bill.stateName }}</el-tag>
        <span class="dim">{{ bill.billNo }}</span>
      </div>
      <div class="nums">
        <div class="n">
          <span class="k">原价</span>
          <span class="v">{{ showYuan(bill.amt) }}</span>
        </div>
        <div class="n">
          <span class="k">减免</span>
          <span class="v rebate">-{{ showYuan(bill.rebateAmt) }}</span>
        </div>
        <div class="n pay">
          <span class="k">实付（元）</span>
          <span class="v">{{ showYuan(bill.payAmt) }}</span>
        </div>
      </div>
      <div class="rmeta">
        <span>类型：{{ bill.kindName }}</span>
        <span>机具：{{ bill.devNo || '—' }}</span>
        <span>卡户：{{ bill.stuName || '#' + bill.acctId }}</span>
        <span v-if="bill.oriBillId">原单：{{ bill.oriBillId }}</span>
        <span>请求号：{{ bill.reqSn }}</span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { payApi } from '@/api/pay'
import { showYuan } from '@/utils/money'
import { ALL_DEVS } from '@/utils/devs'
import { useWhoami } from '@/stores/whoami'

const who = useWhoami()


// 商户只能看见自己那台；财务、管理员、学生（只看不刷）看全部
const devs = computed(() => {
  if (who.isMerchant && who.devNo) return ALL_DEVS.filter((d) => d.devNo === who.devNo)
  return ALL_DEVS
})

const devNo = ref(devs.value[0].devNo)
const dev = computed(() => ALL_DEVS.find((d) => d.devNo === devNo.value) || ALL_DEVS[0])

const tab = ref('consume')
const busy = ref(false)
const bill = ref(null)
const err = ref('')

const cf = reactive({ cardNo: '', amtYuan: 8, pwd: '' })
const rf = reactive({ cardNo: '', amtYuan: 50, reqSn: '' })
const vf = reactive({ billId: null, reason: '' })

function onDevChange() {
  bill.value = null
  err.value = ''
}

/** 时间戳取 12 位，够唯一且短；末尾补 4 位随机，避免同一秒连点两次撞幂等键 */
function nonce() {
  return String(Date.now()).slice(-12) + String(Math.floor(Math.random() * 10000)).padStart(4, '0')
}

/** 前端扮演机具固件：按机具类型拼出各自的报文 */
function buildRaw(kind, cardNo, amtYuan, sn, pwd) {
  const ts = String(Date.now()).slice(-12)
  const fen = Math.round(Number(amtYuan) * 100)
  if (kind === 1) return cardNo + ',' + fen + ',' + ts + ',' + sn + (pwd ? ',' + pwd : '')
  if (kind === 2) return 'card_id=' + cardNo + '&total_fee=' + fen + '&trade_time=' + ts + '&out_trade_no=' + sn + (pwd ? '&pay_pwd=' + pwd : '')
  if (kind === 3) return cardNo + '|' + amtYuan + '|' + ts + '|' + sn
  return cardNo + ';' + amtYuan + ';' + ts + ';' + sn
}

const rawPreview = computed(() =>
  buildRaw(dev.value.kind, cf.cardNo || '<卡号>', cf.amtYuan, '<流水号>', cf.pwd)
)

function ok(d) {
  bill.value = d
  err.value = ''
  ElMessage.success('交易成功：' + d.billNo)
}

function bad(e) {
  bill.value = null
  err.value = (e && e.msg) ? e.msg : '交易失败'
}

async function doConsume() {
  if (!cf.cardNo) {
    ElMessage.warning('请填写卡号')
    return
  }
  busy.value = true
  try {
    const sn = nonce()
    const raw = buildRaw(dev.value.kind, cf.cardNo.trim(), cf.amtYuan, sn, cf.pwd || '')
    const d = await payApi.consume(devNo.value, raw)
    ok(d)
  } catch (e) {
    bad(e)
  } finally {
    busy.value = false
  }
}

async function doRecharge() {
  if (!rf.cardNo) {
    ElMessage.warning('请填写卡号')
    return
  }
  busy.value = true
  try {
    const d = await payApi.recharge({
      cardNo: rf.cardNo.trim(),
      amtYuan: rf.amtYuan,
      reqSn: rf.reqSn || null
    })
    ok(d)
  } catch (e) {
    bad(e)
  } finally {
    busy.value = false
  }
}

async function doReverse() {
  if (!vf.billId) {
    ElMessage.warning('请填写原单 ID')
    return
  }
  busy.value = true
  try {
    const d = await payApi.reverse(vf.billId, vf.reason || '柜面冲正')
    ok(d)
  } catch (e) {
    bad(e)
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.page { background: #fff; border-radius: 8px; padding: 18px 20px; }
.head { display: flex; align-items: baseline; gap: 12px; margin-bottom: 14px; }
h3 { font-size: 15px; margin: 0; color: #303133; }
.dim { font-size: 12.5px; color: #909399; }
.dev { margin-bottom: 14px; background: #fafbfc; }
.devrow { display: flex; align-items: center; gap: 10px; }
.fmt { display: flex; align-items: center; gap: 10px; margin-top: 10px; }
.lbl { font-size: 13px; color: #606266; }
code { background: #f2f3f5; padding: 2px 7px; border-radius: 4px; font-size: 12.5px; color: #c7254e; }
.raw { display: inline-block; max-width: 100%; word-break: break-all; }
.tabs { margin-top: 6px; }
.form { padding-top: 6px; }
.alert { margin-top: 6px; }
.result { margin-top: 16px; background: #fafbfc; }
.rhead { display: flex; align-items: center; gap: 10px; }
.rname { font-size: 15px; font-weight: 600; color: #303133; }
.nums { display: flex; gap: 44px; margin: 16px 0 12px; }
.n .k { display: block; font-size: 12.5px; color: #909399; }
.n .v { font-size: 22px; font-weight: 600; font-variant-numeric: tabular-nums; color: #303133; }
.n .v.rebate { color: #e6a23c; }
.n.pay .v { font-size: 30px; color: #f56c6c; }
.rmeta { display: flex; gap: 22px; flex-wrap: wrap; font-size: 12.5px; color: #606266; }
</style>
