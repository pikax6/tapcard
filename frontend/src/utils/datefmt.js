export const pad2 = (n) => (n < 10 ? '0' + n : '' + n)

export const today = () => {
  const d = new Date()
  return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate())
}

export const hhmm = (ts) => {
  const d = new Date(ts)
  return pad2(d.getHours()) + ':' + pad2(d.getMinutes())
}
