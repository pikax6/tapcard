// 金额只在前端做展示与输入转换，真正的计算一律在后端 BigDecimal
export const yuanToFen = (y) => Math.round(Number(y || 0) * 100)

export const fenToYuan = (f) => (Number(f || 0) / 100).toFixed(2)

export const showYuan = (v) => (v === null || v === undefined ? '-' : Number(v).toFixed(2))
