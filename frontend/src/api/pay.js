// 支付：消费扣款、充值、冲正
// 约定：一个后端 Controller 一个文件，导出具名函数，返回的是解包后的 data

import http from './http'

export const payApi = {
  consume: (devNo, raw) => http.post('/pay/consume', { devNo, raw }),
  recharge: (data) => http.post('/pay/recharge', data),      // {cardNo, amtYuan, reqSn?}
  reverse: (billId, reason) => http.post('/pay/reverse', { billId, reason }),
  bills: (params) => http.get('/pay/bills', { params })      // {page,size,keyword,from,to}
}
