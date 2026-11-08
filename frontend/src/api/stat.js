// 统计：按档口、按机具、按小时
// 约定：一个后端 Controller 一个文件，导出具名函数，返回的是解包后的 data

import http from './http'

export const statApi = {
  byStall: (day) => http.get('/stat/stall', { params: { day } }),
  byDev: (day) => http.get('/stat/dev', { params: { day } }),
  byDay: (from, to) => http.get('/stat/day', { params: { from, to } })
}
