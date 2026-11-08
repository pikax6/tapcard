// 对账：生成批次、查批次、查差错
// 约定：一个后端 Controller 一个文件，导出具名函数，返回的是解包后的 data
import http from './http'


export const settleApi = {
  generate: (day, reported) => http.post('/settle/generate', { day, reported }),
  list: (day) => http.get('/settle/list', { params: { day } })
}
