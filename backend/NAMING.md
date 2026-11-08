# tapcard · 命名与建类清单

> 这份文件有两个用途：**① 命名唯一依据**，② **建类勾选清单**。
> 每建一个类来这里打个勾，别凭记忆写名字。名字一乱，"类图与代码不一致"直接扣到 60%。

---

## 1. 包结构约定

```
com.zxy.tapcard
├── TapcardApp          启动入口（已建）
├── common              公共件：返回体、异常、错误码、金额、单号
├── config              装配：跨域、MyBatis、系统参数
├── auth                身份：JWT、登录拦截、当前操作人
├── model               实体与枚举（acct / pay / dev / rebate）
├── biz                 业务层，事务边界只在这里
├── dao                 数据访问层（含 stat 聚合行对象）
├── ext                 外部接入：厂商 SDK + 机具驱动
└── web                 表示层：REST + 入参 + 出参
```

**依赖方向只能单向**：`web → biz → dao`；`model` 三层共用；`ext` 只许 `biz` 调。

| 层 | 允许 | 禁止 |
|---|---|---|
| `web` | 收参校验、调 biz、装 vo | 业务判断、SQL、直接引用 `ext` 的 SDK |
| `biz` | `@Transactional`、编排 | 直接拼 SQL 字符串 |
| `dao` | SQL / XML、聚合 | 业务分支 |
| `model` | 字段、简单判断 | `@Service`、`@Autowired` |

---

## 2. 建类清单

标记说明：`[单例]` `[工厂]` `[建造者]` `[策略]` `[责任链]` `[观察者]` `[适配器]` `[装饰]` `[代理]` 是模式角色，建完类记得在类注释里也写一句。

> 下面共 99 条勾选项，对应 106 个类——`param/` 和 `vo/` 两组按行并列，一行里含多个类。
>
> **分两批建**：2.1 ~ 2.3（common / config / auth，共 12 个类）不碰数据库，可以立刻写；
> 2.4 之后（实体、枚举、DAO、biz、web，共 94 个）**对齐 `sql/ykt_schema.sql` 之后再动手**。
>
> **本版已按建表结果校正过一轮**（`sql/ykt_schema.sql` 定稿时补齐了 10 个类）：
> 新增实体/枚举 `OpUser` `SettleBill` `SettleState` `NotifyLog` `EvtKind` `FaceState` `StallKind`、
> DAO `OpUserDao` `NotifyLogDao`、策略实现 `CutCalc`。

### 2.1 common —— 先建这一层，后面全要用

- [ ] `R` 统一返回体 `{code, msg, data}`，静态方法 `ok()` / `fail(ErrCode)`
- [ ] `BizErr` 业务异常，携带 `ErrCode`
- [ ] `ErrCode` 错误码枚举：`BAL_LACK` `FACE_LOST` `FACE_DEAD` `REQ_REPEAT` `NO_RIGHT` `DEV_OFFLINE` `AMT_ILLEGAL` `QUOTA_OVER` `NO_ROLE`
- [ ] `ErrCatcher` `@RestControllerAdvice` 全局兜底，把 `BizErr` 翻成 `R`
- [ ] `Amt` 元分互转 + `fix2(BigDecimal)`，全项目禁 double
- [ ] `BillNoGen` `[单例]` 单号生成，静态内部类写法，格式 `日期(8) + 机具号(4) + 序号(6)`

### 2.2 config

- [ ] `WebCfg` 跨域 + 注册 `LoginWall`
- [ ] `MybatisCfg` 分页插件、乐观锁插件
- [ ] `SysParamBag` `[单例]` 系统参数袋，DCL 写法（报告第 4 章要贴这 10 行）

### 2.3 auth

- [ ] `TokenKit` 签发 / 解析 JWT，载荷带 `uid` `role` `acctId` 或 `devNo`
- [ ] `LoginWall` 登录拦截器
- [ ] `WhoIsIt` ThreadLocal 当前操作人

### 2.4 model —— 实体与枚举（子包与表一一对应）

- [ ] `acct/CardAcct` 卡户（含 `ver` 乐观锁字段）→ `card_acct`
- [ ] `acct/CardFace` 卡面 → `card_face`
- [ ] `acct/AcctFlow` 流水 → `acct_flow`
- [ ] `acct/AcctState` 枚举：1 正常 / 2 冻结 / 3 注销
- [ ] `acct/FaceState` 枚举：1 正常 / 2 挂失 / 3 作废
- [ ] `pay/PayBill` 账单 → `pay_bill`
- [ ] `pay/BillLine` 账单行 → `bill_line`
- [ ] `pay/BillKind` 枚举：1 充值 / 2 消费 / 3 冲正
- [ ] `pay/BillState` 枚举：0 待结算 / 1 已结算 / 2 已冲正
- [ ] `dev/PosDev` 机具 → `pos_dev`
- [ ] `dev/DevKind` 枚举：1 食堂POS / 2 超市收银 / 3 门禁 / 4 水控
- [ ] `dev/Stall` 档口 → `stall`
- [ ] `dev/StallKind` 枚举：1 食堂 / 2 超市 / 3 其他
- [ ] `dev/DevChargeCmd` `record`，统一刷卡指令 `{cardNo, devNo, amtYuan, ts, nonce}`（不落表）
- [ ] `rebate/RebateRule` 减免规则 → `rebate_rule`
- [ ] `rebate/RebateKind` 枚举：1 不打折 / 2 折扣率 / 3 立减 / 4 每日补贴 / 5 积分抵扣
- [ ] `auth/OpUser` 操作员（`role` + 三个归属字段）→ `op_user`
- [ ] `settle/SettleBill` 结算单 → `settle_bill`
- [ ] `settle/SettleState` 枚举：0 待核对 / 1 已平账 / 2 有差异
- [ ] `notify/NotifyLog` 通知日志 → `notify_log`
- [ ] `notify/EvtKind` 枚举：1 账单完成 / 2 结算完成

> 实体类名驼峰、表名下划线：`CardAcct` ↔ `card_acct`，一一对应，别多也别少。
> 枚举一律按 `TINYINT` 存，**序号必须与 `ykt_schema.sql` 列注释里的编号一致**——
> 差一位，账单状态就会全线错乱，而且这种错不会报错，只会算错。

### 2.5 dao

- [ ] `CardAcctDao`（含扣款用它做条件更新：`where id=? and ver=?`）
- [ ] `CardFaceDao`
- [ ] `AcctFlowDao`
- [ ] `PayBillDao`
- [ ] `BillLineDao`
- [ ] `PosDevDao`
- [ ] `StallDao`
- [ ] `RebateRuleDao`
- [ ] `SettleBillDao` → `settle_bill`
- [ ] `OpUserDao` 登录查账号，按 `emp_no` 命中 `uk_emp_no`
- [ ] `NotifyLogDao` 观察者写日志
- [ ] `stat/AcctStatRow` 聚合结果行对象（按档口/日期汇总）
- [ ] `stat/DevDayRow` 按机具汇总

### 2.6 ext —— 第三方的东西只能住这里

- [ ] `XinYeSdk` 模拟厂商 SDK（桩），报文字段名与我们的不同、金额单位是"分"
- [ ] `HuiTongSdk` 模拟厂商 SDK（桩），报文用 GBK、字段名又一套
- [ ] `dev/DevDriver` `[适配器]` 目标接口：`DevChargeCmd adapt(String rawMsg)`
- [ ] `dev/CanteenPosDriver` 食堂 POS 报文 → 统一指令
- [ ] `dev/MarketDriver` 超市收银
- [ ] `dev/GateDriver` 门禁
- [ ] `dev/WaterDriver` 水控

### 2.7 biz/acct

- [ ] `AcctOpSvc` 开户 / 挂失 / 解挂 / 销户
- [ ] `AcctQuery` `[代理]` 查询接口（真实主题与代理的共同接口）
- [ ] `AcctQuerySvc` `[代理]` 真实主题
- [ ] `AcctGuard` `[代理]` 归属校验 + 耗时日志 + 403

### 2.8 biz/pay

- [ ] `PaySvc` 扣款主流程编排（唯一入口）

**maker 包 —— 工厂**
- [ ] `PayBillMaker` `[工厂]` 接口：`boolean mine(BillKind k)` + `PayBill make(PayBillDraft d)`
- [ ] `BillMakerRegistry` `[工厂]` 注册表，Spring 注入后按 BillKind 建索引
- [ ] `RechargeMaker` 充值单
- [ ] `ConsumeMaker` 消费单
- [ ] `ReverseMaker` 冲正单

**draft 包 —— 建造者**
- [ ] `PayBillDraft` `[建造者]` 链式填单，`seal()` 收口校验并成单

**check 包 —— 责任链**
- [ ] `PayCheck` `[责任链]` 抽象：`next()` + 模板方法 `run(Ctx)` + 抽象 `verify(Ctx)`
- [ ] `PayCheckLine` `[责任链]` 按 `devKind` 装配整条链
- [ ] `CardStateCheck` 卡状态（挂失/注销直接拒）
- [ ] `PwdCheck` 密码（门禁跳过）
- [ ] `QuotaCheck` 单笔限额（超市 200 元）
- [ ] `BalCheck` 余额
- [ ] `RiskCheck` `[加分]` 风控：同卡 1 分钟 &gt; 3 笔、单日累计超限
- [ ] `DeductCheck` 链尾，真正落账

**rebate 包 —— 策略**
- [ ] `RebateCalc` `[策略]` 接口：`BigDecimal calc(RebateBox box)` + `boolean mine(RebateKind k)`
- [ ] `RebateBox` `[策略]` 上下文：当前规则 + 待算金额 + 已减累计
- [ ] `NoCutCalc` 不打折
- [ ] `RateCutCalc` 折扣率
- [ ] `CutCalc` 立减（满多少减多少，`{"cutFen":100}`）
- [ ] `DailySubsidyCalc` 每日补贴（每人每日 3 元上限）
- [ ] `PointCutCalc` 积分抵扣

### 2.9 biz/settle

- [ ] `SettleSvc` 对账批次生成
- [ ] `sheet/SettleSheet` `[装饰]` 抽象结算单
- [ ] `sheet/BaseSheet` 基础单
- [ ] `sheet/PointSticker` 贴积分累计
- [ ] `sheet/BudgetSticker` 贴补贴预算扣减
- [ ] `sheet/TraceSticker` 贴操作留痕

### 2.10 biz/notify —— 观察者

- [ ] `PayBillSignal` `[观察者]` 被观察者：维护 Watcher 列表并按事件分发
- [ ] `PayBillWatcher` `[观察者]` 观察者接口：`void onPaid(PayBillEvent e)`
- [ ] `SettleWatcher` 写待对账
- [ ] `StatWatcher` 更新当日汇总
- [ ] `SmsWatcher` 推到账消息
- [ ] `PayBillEvent` 事件对象（放 `biz/notify` 或 `model/pay`，二选一，别两处都建）

### 2.11 web

- [ ] `auth/AuthApi` 登录、当前用户
- [ ] `acct/AcctApi` 开户 / 挂失 / 解挂 / 销户 / 卡户详情 / 流水
- [ ] `pay/PayApi` 消费扣款 / 充值 / 冲正
- [ ] `settle/SettleApi` 批次生成 / 批次详情
- [ ] `report/StatApi` 按档口、按日期、按机具统计
- [ ] `param/AcctOpenParam` `RechargeParam` `ConsumeParam` `ReverseParam` `PageParam`
- [ ] `vo/AcctVo` `BillVo` `SettleVo` `StatVo`
- [ ] `PingApi` 已建，是骨架自检用的，正式功能齐了可以留也可以删

---

## 3. 表名对照

| 类 | 表 | 关键字段 |
|---|---|---|
| `CardAcct` | `card_acct` | `bal DECIMAL(12,2)`、`state TINYINT`、`ver INT` |
| `CardFace` | `card_face` | `card_no`、`acct_id`、`state` |
| `AcctFlow` | `acct_flow` | `acct_id`、`bal_before`、`bal_after`、`chg_amt` |
| `PayBill` | `pay_bill` | `kind`、`amt`、`state`、`req_sn`(唯一)、`ori_bill_id` |
| `BillLine` | `bill_line` | `bill_id`、`item_name`、`amt`、`rebate_amt` |
| `PosDev` | `pos_dev` | `dev_no`、`kind`、`stall_id`、`state` |
| `Stall` | `stall` | `stall_name`、`stall_type` |
| `RebateRule` | `rebate_rule` | `kind`、`params`(JSON)、`prio`、`start_at`、`end_at` |
| `SettleBill` | `settle_bill` | `settle_date`、`dev_no`、`cnt`、`amt`、`diff_cnt` |
| — | `notify_log` | `evt_type`、`watcher`、`ok`、`created_at` |
| — | `op_user` | `emp_no`、`pwd_hash`、`role`、`acct_id` / `dev_no` |

金额一律 `DECIMAL(12,2)`；`req_sn` 建唯一索引 `uk_req_sn`；`card_acct.ver` 乐观锁。

---

## 4. 统一动词（比改名更管用）

| 动词 | 含义 | 不要用 |
|---|---|---|
| `mine(kind)` | 这个类型归我管吗 | `supports()` |
| `calc(box)` | 算一下金额 | `compute()` / `calculate()` |
| `seal()` | 收口校验、正式成单 | `build()` |

九个类统一用同一批动词，是"人写的"最强信号。

---

## 5. 三条纪律

1. **改就全局改**：类名 / 表名 / 字段名 / 前端 api 文件名 / 报告类图 / 报告 3.3 职责表，六处同步。
2. **类名自由，构造型死板**：类图上 `«Strategy»` `«Handler»` `«Observer»` `«Adapter»` `«Decorator»` `«Proxy»` 一个都不能省，否则模式角色不可辨。
3. **报告用业务词，代码用工件词**：报告写"交易""结算"（对得上题目），代码写 `PayBill`。这是分工，不是混乱——3.3 职责表里列一次对照即可。
