import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http from '@/api/http'

/**
 * 当前登录人。后端 /auth/login 返回的是：
 *   { token, uid, empName, role, acctId, devNo, stallId }
 * 注意 role 是数字（1 学生 / 2 商户 / 3 财务 / 9 管理员），不是字符串。
 */
const ROLE_LABEL = { 1: '学生', 2: '商户操作员', 3: '财务', 9: '管理员' }

export const useWhoami = defineStore('whoami', () => {
  const token = ref(localStorage.getItem('tapcard_token') || '')
  const uid = ref(Number(localStorage.getItem('tapcard_uid') || 0))
  const name = ref(localStorage.getItem('tapcard_name') || '')
  const role = ref(Number(localStorage.getItem('tapcard_role') || 0))
  const acctId = ref(localStorage.getItem('tapcard_acct') || '')
  const devNo = ref(localStorage.getItem('tapcard_dev') || '')
  const stallId = ref(localStorage.getItem('tapcard_stall') || '')

  const isStudent = computed(() => role.value === 1)
  const isMerchant = computed(() => role.value === 2)
  const isFinance = computed(() => role.value === 3)
  const isAdmin = computed(() => role.value === 9)

  /** 财务与管理员在很多接口上是同权限，用这个判断省得写两遍 */
  const isBackOffice = computed(() => role.value === 3 || role.value === 9)

  const roleLabel = computed(() => ROLE_LABEL[role.value] || '未登录')

  function save() {
    localStorage.setItem('tapcard_token', token.value || '')
    localStorage.setItem('tapcard_uid', String(uid.value || ''))
    localStorage.setItem('tapcard_name', name.value || '')
    localStorage.setItem('tapcard_role', String(role.value || ''))
    localStorage.setItem('tapcard_acct', acctId.value || '')
    localStorage.setItem('tapcard_dev', devNo.value || '')
    localStorage.setItem('tapcard_stall', stallId.value || '')
  }

  async function login(empNo, pwd) {
    const d = await http.post('/auth/login', { empNo, pwd })

    token.value = d.token
    uid.value = Number(d.uid || 0)
    name.value = d.empName || ''          // 后端字段是 empName，不是 name
    role.value = Number(d.role || 0)      // 数字，别转字符串
    acctId.value = d.acctId || ''
    devNo.value = d.devNo || ''
    stallId.value = d.stallId || ''
    save()

    return d
  }

  function logout() {
    token.value = ''
    uid.value = 0
    name.value = ''
    role.value = 0
    acctId.value = ''
    devNo.value = ''
    stallId.value = ''
    ;['token', 'uid', 'name', 'role', 'acct', 'dev', 'stall'].forEach((k) =>
      localStorage.removeItem('tapcard_' + k)
    )
  }

  return {
    token, uid, name, role, acctId, devNo, stallId,
    isStudent, isMerchant, isFinance, isAdmin, isBackOffice, roleLabel,
    login, logout
  }
})
