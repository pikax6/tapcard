// 机具清单：与 sql/ykt_seed.sql 的 pos_dev 一一对应，改种子时同步改这里
// 项目没有"机具管理"页面，机具台与对账盘共用这一份，避免两处写岔
export const ALL_DEVS = [
  { devNo: 'D001', name: '三食堂二楼 07 号窗口 POS', kind: 1, kindName: '食堂 POS', kindType: 'success', stallName: '三食堂二楼 07 号窗口' },
  { devNo: 'D002', name: '三食堂二楼 08 号窗口 POS', kind: 1, kindName: '食堂 POS', kindType: 'success', stallName: '三食堂二楼 08 号窗口' },
  { devNo: 'D003', name: '校园超市 A 区收银机', kind: 2, kindName: '超市收银', kindType: 'warning', stallName: '校园超市 A 区' },
  { devNo: 'D004', name: '6 号公寓门禁', kind: 3, kindName: '门禁', kindType: 'info', stallName: '' },
  { devNo: 'D005', name: '澡堂水控终端', kind: 4, kindName: '水控', kindType: 'info', stallName: '' }
]

export const devNameOf = (devNo) => {
  const d = ALL_DEVS.find((x) => x.devNo === devNo)
  return d ? d.name : devNo
}
