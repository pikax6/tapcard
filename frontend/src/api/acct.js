// 卡户：分页、详情、开户、挂失、解挂、销户、流水
// 约定：一个后端 Controller 一个文件，导出具名函数，返回的是解包后的 data
import http from './http'


export const acctApi = {
  list: (keyword) => http.get('/acct/list', { params: { keyword } }),
  detail: (id) => http.get(`/acct/${id}`),
  flows: (id, from, to) => http.get(`/acct/${id}/flows`, { params: { from, to } }),

  // 下面四个接口的参数在 query 上，不是请求体
  open: (data) => http.post('/acct/open', data),
  lose: (cardNo) => http.post('/acct/lose', null, { params: { cardNo } }),
  unlock: (cardNo) => http.post('/acct/unlock', null, { params: { cardNo } }),
  close: (acctId) => http.post('/acct/close', null, { params: { acctId } })
}
