<template>
  <el-dialog v-model="open" title="账单冲正" width="420px" @close="reset">
    <p class="ori">
      原单号：<b>{{ ori }}</b>
    </p>
    <el-form label-width="80px">
      <el-form-item label="冲正金额">
        <el-input v-model="amt" placeholder="不超过原单金额" />
      </el-form-item>
      <el-form-item label="原因">
        <el-input v-model="memo" type="textarea" :rows="2" placeholder="如：重复刷卡、机具多扣" />
      </el-form-item>
    </el-form>
    <p class="hint">冲正会新生成一张负向账单并关联原单，不是把余额加回去。</p>
    <template #footer>
      <el-button @click="open = false">取消</el-button>
      <el-button type="primary" @click="submit">确认冲正</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const open = ref(false)
const ori = ref('')
const amt = ref('')
const memo = ref('')

function show(billNo) {
  ori.value = billNo
  amt.value = ''
  memo.value = ''
  open.value = true
}

function reset() {
  ori.value = ''
  amt.value = ''
  memo.value = ''
}

function submit() {
  // 待实现：调 pay.js 里的冲正接口，成功后 emit 刷新列表
  ElMessage.info('待接后端：POST /api/pay/reverse')
  open.value = false
}

defineExpose({ show })
</script>

<style scoped>
.ori { font-size: 13px; color: #606266; margin: 0 0 14px; }
.hint { font-size: 12px; color: #909399; margin: 4px 0 0; }
</style>
