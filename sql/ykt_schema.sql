-- ============================================================================
--  校园一卡通消费与结算系统 · 建库建表脚本
--  文件：sql/ykt_schema.sql          版本：v1.0
--  依据：「字段决策清单」30 项建议全部采纳后的落地结果
-- ============================================================================
--
--  ■ 本文件分四部分，可以从头到尾整份执行
--     Part 0  建库之前：环境检查 / 三种建库方式 / 执行顺序 / 自检 / 报错对照
--     Part 1  建库 + 11 张表 DDL（可重复执行）
--     Part 2  逻辑外键关系总表（报告 3.2 的 E-R 关系来源）
--     Part 3  报告 3.2「每张表字段表」五列素材（可直接抄进 Word）
--
--  ■ 环境要求
--     MySQL 8.0 及以上（必须，理由见 Part 0 第 4 条）
--     字符集 utf8mb4 / 排序规则 utf8mb4_0900_ai_ci
--     存储引擎 InnoDB
--
--  ■ 表清单（11 张）
--     主体类：stall 档口 / card_acct 卡户 / card_face 卡面 / pos_dev 机具 / op_user 操作员
--     业务类：rebate_rule 减免规则 / pay_bill 账单 / bill_line 账单行 / acct_flow 流水
--     统计类：settle_bill 结算单 / notify_log 通知日志
-- ============================================================================


-- ############################################################################
-- Part 0 · 建库之前
-- ############################################################################
--
-- ── 0.1 先做三件事：确认版本、字符集、引擎 ──────────────────────────────────
--   在 Navicat / MySQL Workbench / 命令行里执行下面三条，逐条看结果：
--
--     SELECT VERSION();                                  -- 必须 8.0.x 及以上
--     SHOW VARIABLES LIKE 'character_set_server';        -- 期望 utf8mb4
--     SHOW VARIABLES LIKE 'default_storage_engine';      -- 期望 InnoDB
--
--   若 character_set_server 不是 utf8mb4，本脚本在建库语句里已显式指定，
--   仍然能建出正确的库；只是将来手工新建别的库时要自己带字符集。
--
-- ── 0.2 三种建库方式，选一种 ────────────────────────────────────────────────
--
--   【方式一】命令行（推荐。最不容易出问题，报错信息也最清楚）
--     1) 打开 cmd 或 PowerShell（不要用 Git Bash，中文路径容易被转义）
--     2) 逐条执行，路径按你本机实际情况改：
--
--        cd /d "D:\27543\A-软件设计模式\期末考察\tapcard\sql"
--
--        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" ^
--            -u root -p --default-character-set=utf8mb4 < ykt_schema.sql
--
--        （PowerShell 不支持 ^ 换行，写成一行即可）
--
--     3) 回车后输入 root 密码。没有输出＝执行成功（MySQL 的哲学：没消息就是好消息）
--     4) 验证：
--
--        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" ^
--            -u root -p --default-character-set=utf8mb4 ^
--            -e "USE tapcard; SHOW TABLES;"
--
--        应该列出 11 张表。
--
--   【方式二】Navicat（图形化，适合核对结果）
--     1) 新建连接 → 主机 localhost，端口 3306，用户名 root，密码你的
--        ★ 连接属性里把「编码」设为 utf8mb4 / 65001 (UTF-8)，
--          否则中文注释会变成问号「???」——这一步最常被漏掉
--     2) 连接上之后，不要选任何库，直接在连接名上右键
--        →「运行 SQL 文件」→ 选中 ykt_schema.sql → 编码选 UTF-8 → 开始
--     3) 刷新左侧列表，tapcard 库下应有 11 张表
--
--   【方式三】MySQL Workbench / IDEA 的 Database 面板
--     打开 ykt_schema.sql → 选中整个文件 → 点闪电图标（Execute）
--     Workbench 若提示「Cannot connect」，先检查 MySQL80 服务是否在运行。
--
-- ── 0.3 执行顺序与「可重复执行」 ────────────────────────────────────────────
--   本脚本自带 CREATE DATABASE IF NOT EXISTS，并对每张表先 DROP TABLE IF EXISTS。
--   所以：重复执行不会报错，但会清空已有数据。演示数据请用 ykt_seed.sql 重新灌。
--   ⚠ 一旦库里有了你手工造的对账数据，再跑本文件前先想清楚。
--
-- ── 0.4 为什么必须 MySQL 8.0 ────────────────────────────────────────────────
--   两处硬依赖，5.7 会直接报错：
--     ① rebate_rule.params 用了 JSON 类型（5.7 也有，但校验行为弱）
--     ② 排序规则 utf8mb4_0900_ai_ci 是 8.0 才有的
--   如果你的机器上只有 5.7，改两处即可运行（但报告里请老实写 5.7）：
--     · 全文替换 utf8mb4_0900_ai_ci → utf8mb4_general_ci
--     · rebate_rule.params JSON → params TEXT
--
-- ── 0.5 执行完的 5 条自检（建议逐条跑，报告第 5 章要用）────────────────────
--   1) SHOW TABLES;                         -- 11 张
--   2) DESC pay_bill;                       -- Key 列里 uk_req_sn / uk_bill_no 应显示 UNI
--   3) SHOW INDEX FROM pay_bill;            -- 5 个索引，看 Key_name 与 Seq_in_index
--   4) SHOW FULL COLUMNS FROM card_acct;    -- Comment 列必须是中文，不能是 ???
--   5) SHOW TABLE STATUS FROM tapcard;      -- Engine 全 InnoDB，Collation 全 utf8mb4_0900_ai_ci
--
-- ── 0.6 常见报错对照 ────────────────────────────────────────────────────────
--   ERROR 1045 (28000) Access denied
--       → 密码不对，或该用户没有建库权限。换 root，或 GRANT ALL ON *.* TO 'user'@'localhost';
--   ERROR 1064 (42000) You have an error in your SQL syntax
--       → 八成是复制时被 Word / 微信转成了全角引号或全角分号。
--         本文件请直接用编辑器打开，不要经手 Word。
--   ERROR 1115 (42000) Unknown character set: 'utf8mb4'
--       → MySQL 版本低于 5.5，或客户端太老。换 8.0 客户端。
--   ERROR 1273 (HY000) Unknown collation: 'utf8mb4_0900_ai_ci'
--       → 版本低于 8.0，按 0.4 改排序规则。
--   ERROR 1366 (HY000) Incorrect string value: '\xE5\x8D\xA1...'
--       → 客户端连接编码不是 utf8mb4。命令行加 --default-character-set=utf8mb4；
--         Navicat 按 0.2 方式二第 1 步改连接编码。
--   ERROR 1050 (42S01) Table 'xxx' already exists
--       → 你没有跑完整的文件（只选了某几行），或者 DROP 被注释掉了。整份重跑。
--   ERROR 2003 (HY000) Can't connect to MySQL server on 'localhost'
--       → 服务没起。Windows：服务管理器里启动 MySQL80。
-- ############################################################################


-- ############################################################################
-- Part 1 · 建库 + 建表
-- ############################################################################

SET NAMES utf8mb4;
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- 建库。字符集与排序规则在这里定死，后面 11 张表继承。
CREATE DATABASE IF NOT EXISTS tapcard
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE tapcard;


-- ============================================================================
-- 表 1 / 11　stall　档口
--   职责：消费发生的商户主体。食堂窗口、超市柜台都是档口。
--   实体：com.zxy.tapcard.model.dev.Stall
--   为何单独成表：结算要按档口出报表，机具只是档口的采集终端。
-- ============================================================================
DROP TABLE IF EXISTS stall;
CREATE TABLE stall (
  id          BIGINT      NOT NULL AUTO_INCREMENT              COMMENT '主键',
  stall_name  VARCHAR(64) NOT NULL                             COMMENT '档口名称，如「三食堂二楼 07 号窗口」',
  stall_type  TINYINT     NOT NULL DEFAULT 1                   COMMENT '档口类型：1 食堂 / 2 超市 / 3 其他',
  contact     VARCHAR(32)          DEFAULT NULL                COMMENT '联系人及电话',
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                   ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='档口：消费发生的商户主体';


-- ============================================================================
-- 表 2 / 11　card_acct　卡户
--   职责：人（学号）+ 钱（余额）的金融主体。补卡不换卡户，所以卡号不在本表。
--   实体：com.zxy.tapcard.model.acct.CardAcct
--   关键设计：
--     · bal 不加 CHECK(>=0)。余额不足由扣款时的条件更新 SQL 拦截，
--       目的是让业务异常统一走 ErrCode.BAL_LACK，而不是抛 SQL 完整性异常。
--     · ver 是乐观锁版本号，扣款必须带 where id=? and ver=?。
-- ============================================================================
DROP TABLE IF EXISTS card_acct;
CREATE TABLE card_acct (
  id          BIGINT        NOT NULL AUTO_INCREMENT            COMMENT '主键',
  stu_no      VARCHAR(16)   NOT NULL                           COMMENT '学号，与教务系统一致',
  stu_name    VARCHAR(32)   NOT NULL                           COMMENT '学生姓名',
  dept        VARCHAR(64)            DEFAULT NULL              COMMENT '院系班级',
  bal         DECIMAL(12,2) NOT NULL DEFAULT 0.00              COMMENT '卡内余额（元）。不加 CHECK，余额不足由条件更新拦截',
  state       TINYINT       NOT NULL DEFAULT 1                 COMMENT '卡户状态：1 正常 / 2 冻结 / 3 注销',
  ver         INT           NOT NULL DEFAULT 0                 COMMENT '乐观锁版本号，每次成功更新 +1',
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_stu_no (stu_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='卡户：学生的账户主体与余额';


-- ============================================================================
-- 表 3 / 11　card_face　卡面
--   职责：物理卡凭证。挂失、解挂、补卡都作用在卡面上，不动余额。
--   实体：com.zxy.tapcard.model.acct.CardFace
--   为何与卡户分表：一个卡户可以先后拥有多张卡面（丢卡补卡），
--                   挂失的是一张卡，不是一个人。
-- ============================================================================
DROP TABLE IF EXISTS card_face;
CREATE TABLE card_face (
  id          BIGINT      NOT NULL AUTO_INCREMENT              COMMENT '主键',
  card_no     VARCHAR(20) NOT NULL                             COMMENT '卡号，物理卡唯一标识，独立于学号',
  acct_id     BIGINT      NOT NULL                             COMMENT '所属卡户 ID',
  state       TINYINT     NOT NULL DEFAULT 1                   COMMENT '卡面状态：1 正常 / 2 挂失 / 3 作废',
  issue_at    DATETIME             DEFAULT NULL                COMMENT '发卡时间',
  lose_at     DATETIME             DEFAULT NULL                COMMENT '挂失时间，未挂失为 NULL',
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                   ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_card_no (card_no),
  KEY idx_face_acct (acct_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='卡面：物理卡凭证，挂失与补卡的载体';


-- ============================================================================
-- 表 4 / 11　pos_dev　机具
--   职责：接入的终端。四类报文差异靠适配器抹平，本表只存"设备是谁"。
--   实体：com.zxy.tapcard.model.dev.PosDev
--   关键设计：
--     · stall_id 可空 —— 门禁、水控挂不到档口上，硬要求非空会逼出一个假档口。
--     · 在线状态不存布尔列，只存 last_beat_at。
--       布尔状态得靠定时任务翻转，任务一挂就永久错；时间戳是事实数据。
--       判断在线：NOW() - last_beat_at < 60 秒。
-- ============================================================================
DROP TABLE IF EXISTS pos_dev;
CREATE TABLE pos_dev (
  id            BIGINT      NOT NULL AUTO_INCREMENT            COMMENT '主键',
  dev_no        VARCHAR(16) NOT NULL                           COMMENT '机具号，唯一',
  dev_name      VARCHAR(64) NOT NULL                           COMMENT '机具名称或安装位置',
  kind          TINYINT     NOT NULL                           COMMENT '机具类型：1 食堂POS / 2 超市收银 / 3 门禁 / 4 水控',
  stall_id      BIGINT               DEFAULT NULL              COMMENT '所属档口 ID，门禁与水控为 NULL',
  state         TINYINT     NOT NULL DEFAULT 1                 COMMENT '机具状态：1 可用 / 2 停用',
  last_beat_at  DATETIME             DEFAULT NULL              COMMENT '最后一次心跳时间，在线状态由它实时计算',
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dev_no (dev_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='机具：接入系统的各类刷卡终端';


-- ============================================================================
-- 表 5 / 11　op_user　操作员
--   职责：登录主体。三种身份（学生 / 商户操作员 / 财务）靠 role 区分，
--         不建 role、permission 关联表 —— 课程项目里那是过度设计。
--   实体：com.zxy.tapcard.model.auth.OpUser
--   关键设计：
--     · acct_id / dev_no / stall_id 三列可空，按 role 填其中一个。
--       这三列就是代理模式 AcctGuard 做归属校验的数据基础：
--       学生只能查自己 acct_id 的余额流水，商户只能查自己机具的账单。
--       没有它们，"越权访问"这条测试用例根本造不出来。
--     · dev_no 按业务键关联 pos_dev（未用 dev_id）：商户账号是运维
--       拿着机具号人工配置的，用机具号更直观，且 dev_no 上有唯一索引。
-- ============================================================================
DROP TABLE IF EXISTS op_user;
CREATE TABLE op_user (
  id          BIGINT      NOT NULL AUTO_INCREMENT              COMMENT '主键',
  emp_no      VARCHAR(16) NOT NULL                             COMMENT '工号或学号，登录名',
  emp_name    VARCHAR(32) NOT NULL                             COMMENT '姓名',
  pwd_hash    VARCHAR(100) NOT NULL                            COMMENT 'BCrypt 密文，约 60 字符，禁止存明文',
  role        TINYINT     NOT NULL                             COMMENT '角色：1 学生 / 2 商户操作员 / 3 财务 / 9 管理员',
  acct_id     BIGINT               DEFAULT NULL                COMMENT '学生账号绑定的卡户 ID',
  dev_no      VARCHAR(16)          DEFAULT NULL                COMMENT '商户账号绑定的机具号',
  stall_id    BIGINT               DEFAULT NULL                COMMENT '商户账号绑定的档口 ID',
  state       TINYINT     NOT NULL DEFAULT 1                   COMMENT '账号状态：1 可用 / 2 停用',
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                   ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_emp_no (emp_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='操作员：登录主体与角色归属';


-- ============================================================================
-- 表 6 / 11　rebate_rule　减免规则
--   职责：优惠策略的数据源。策略模式在数据库层的落点。
--   实体：com.zxy.tapcard.model.rebate.RebateRule
--   关键设计（这一段是全文最硬的模式呼应，报告 3.1 可直接引）：
--     · params 用 JSON，不拆列、不一类规则一张表。
--       好处：新增一种优惠方式不改表结构，只加一个 RebateCalc 实现 + 一条记录。
--       即「变化的是优惠算法，稳定的是规则表结构」。
--     · JSON 里的金额一律用整数分（cutFen / capFen），不用小数。
--       因为 JSON 的 number 落到 Java 会经过 double，0.1 会变成 0.1000000000000000055。
--     · prio 升序取第一条命中，不做叠加。叠加会资损：
--       9 折 + 每日补贴 + 积分抵扣同时生效，一笔 5 元早餐能减到 0 还倒欠。
--     · 生效靠时间窗 + enabled 开关，不靠人工记得去关。
--   四种 params 结构（写死在 RebateCalc 各实现类里）：
--     折扣率    {"rate":0.90}
--     立减      {"cutFen":100}
--     每日补贴  {"capFen":300,"rate":0.5}
--     积分抵扣  {"pointPerYuan":100}
-- ============================================================================
DROP TABLE IF EXISTS rebate_rule;
CREATE TABLE rebate_rule (
  id          BIGINT      NOT NULL AUTO_INCREMENT              COMMENT '主键',
  rule_name   VARCHAR(64) NOT NULL                             COMMENT '规则名称，如「食堂早餐 9 折」',
  kind        TINYINT     NOT NULL                             COMMENT '规则类型：1 不打折 / 2 折扣率 / 3 立减 / 4 每日补贴 / 5 积分抵扣',
  params      JSON        NOT NULL                             COMMENT '规则参数 JSON，金额一律用整数分',
  prio        INT         NOT NULL                             COMMENT '优先级，升序取第一条命中，同组内不重复',
  dev_kind    TINYINT              DEFAULT NULL                COMMENT '限定机具类型，NULL 表示不限',
  stall_id    BIGINT               DEFAULT NULL                COMMENT '限定档口 ID，NULL 表示不限',
  start_at    DATETIME    NOT NULL                             COMMENT '生效开始时间',
  end_at      DATETIME             DEFAULT NULL                COMMENT '生效结束时间，NULL 表示长期有效',
  enabled     TINYINT     NOT NULL DEFAULT 1                   COMMENT '启用开关：1 启用 / 0 停用',
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                   ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_rule_pick (enabled, prio, start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='减免规则：优惠策略的参数来源';


-- ============================================================================
-- 表 7 / 11　pay_bill　账单
--   职责：全系统的凭据中心。每一次充值、消费、冲正都是一张账单。
--   实体：com.zxy.tapcard.model.pay.PayBill
--   关键设计（这张表承载了三个模式，报告里的分量最重）：
--     · kind 区分三种单：1 充值 / 2 消费 / 3 冲正。工厂模式按 kind 找 Maker。
--     · amt / rebate_amt / pay_amt 三列都存，不现算。
--       交易额（amt）与实收额（pay_amt）性质不同：给档口结的是 pay_amt，
--       学校补贴出去的是 rebate_amt，两笔钱走两个账。不分开存，
--       "补贴预算池"这个加分功能就做不出来。
--     · req_sn + UNIQUE 是幂等键。重复请求撞唯一索引 → 回查原单返回，
--       保证"同一个请求号只可能成功一次"。
--       ⚠ req_sn 必须由客户端（机具/前端）生成。后端生成等于没有幂等。
--     · 冲正不改余额了事，而是另生成一条负金额账单，ori_bill_id 指向原单。
--       否则余额变了、账本上却没有对应记录，对账时"账单总额"和
--       "流水变动总额"永远对不平。冲正单：amt 与 pay_amt 为负，rebate_amt 为 0。
--     · state 只有三个值，不做"已撤销"—— 撤销是"没发生"，冲正是"发生了再对冲"，
--       两个概念混在一张表里状态机会失控。
-- ============================================================================
DROP TABLE IF EXISTS pay_bill;
CREATE TABLE pay_bill (
  id          BIGINT        NOT NULL AUTO_INCREMENT            COMMENT '主键',
  bill_no     VARCHAR(32)   NOT NULL                           COMMENT '账单号，日期8位+机具号4位+序号6位',
  kind        TINYINT       NOT NULL                           COMMENT '账单类型：1 充值 / 2 消费 / 3 冲正',
  acct_id     BIGINT        NOT NULL                           COMMENT '卡户 ID',
  dev_no      VARCHAR(16)            DEFAULT NULL              COMMENT '机具号，充值单为 NULL',
  stall_id    BIGINT                 DEFAULT NULL              COMMENT '档口 ID，门禁/水控/充值为 NULL',
  amt         DECIMAL(12,2) NOT NULL                           COMMENT '原价金额（元），冲正单为负数',
  rebate_amt  DECIMAL(12,2) NOT NULL DEFAULT 0.00              COMMENT '减免金额（元），冲正单为 0',
  pay_amt     DECIMAL(12,2) NOT NULL                           COMMENT '实付金额（元）=amt-rebate_amt，冲正单为负数',
  state       TINYINT       NOT NULL DEFAULT 0                 COMMENT '账单状态：0 待结算 / 1 已结算 / 2 已冲正',
  req_sn      VARCHAR(40)   NOT NULL                           COMMENT '请求号，客户端生成，幂等键',
  ori_bill_id BIGINT                 DEFAULT NULL              COMMENT '冲正时指向被冲正的原账单 ID',
  remark      VARCHAR(128)           DEFAULT NULL              COMMENT '备注或冲正原因',
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_bill_no   (bill_no),
  UNIQUE KEY uk_req_sn    (req_sn),
  KEY idx_bill_acct   (acct_id, created_at),
  KEY idx_bill_dev    (dev_no, created_at),
  KEY idx_bill_settle (state, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='账单：充值、消费、冲正的统一凭据';


-- ============================================================================
-- 表 8 / 11　bill_line　账单行
--   职责：一张账单买了什么。建造者模式的落点 —— PayBillDraft 在内存里攒
--         一组 BillLine，seal() 时随主单一起落库。
--   实体：com.zxy.tapcard.model.pay.BillLine
-- ============================================================================
DROP TABLE IF EXISTS bill_line;
CREATE TABLE bill_line (
  id          BIGINT        NOT NULL AUTO_INCREMENT            COMMENT '主键',
  bill_id     BIGINT        NOT NULL                           COMMENT '所属账单 ID',
  item_name   VARCHAR(64)   NOT NULL                           COMMENT '品名，如「大排套餐」',
  qty         INT           NOT NULL DEFAULT 1                 COMMENT '数量',
  amt         DECIMAL(12,2) NOT NULL                           COMMENT '小计原价（元）',
  rebate_amt  DECIMAL(12,2) NOT NULL DEFAULT 0.00              COMMENT '小计减免（元）',
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_line_bill (bill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='账单行：单笔消费的明细';


-- ============================================================================
-- 表 9 / 11　acct_flow　流水
--   职责：每一分钱变动的凭证。与账单是一对一，但职责不同：
--         账单答"为什么扣"，流水答"余额从多少变成多少"。
--   实体：com.zxy.tapcard.model.acct.AcctFlow
--   关键设计：chg_amt 允许为负。出问题时靠 bal_before / bal_after 定位
--             是哪一步算错的，所以这两列不是冗余。
-- ============================================================================
DROP TABLE IF EXISTS acct_flow;
CREATE TABLE acct_flow (
  id          BIGINT        NOT NULL AUTO_INCREMENT            COMMENT '主键',
  acct_id     BIGINT        NOT NULL                           COMMENT '卡户 ID',
  bill_id     BIGINT                 DEFAULT NULL              COMMENT '关联账单 ID',
  kind        TINYINT       NOT NULL                           COMMENT '流水类型：1 充值 / 2 消费 / 3 冲正',
  chg_amt     DECIMAL(12,2) NOT NULL                           COMMENT '变动金额（元），支出与冲正为负数',
  bal_before  DECIMAL(12,2) NOT NULL                           COMMENT '变动前余额（元）',
  bal_after   DECIMAL(12,2) NOT NULL                           COMMENT '变动后余额（元）',
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_flow_acct (acct_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='流水：卡户余额的每一次变动';


-- ============================================================================
-- 表 10 / 11　settle_bill　结算单
--   职责：机具日结。对账大屏读的就是这张表。
--   实体：com.zxy.tapcard.model.settle.SettleBill
--   关键设计：
--     · 按「日期 + 机具」唯一，不按档口。对账是拿机具上报的笔数对系统记录的
--       笔数，机具才是天然的对账单元；档口是上卷，group by 就行。
--     · dev_cnt 存机具上报笔数，差异查询时算 dev_cnt - cnt，不落冗余列。
--       种子数据里故意让某台机具差 2 笔 —— "发现差异"比"全部平账"更能证明系统在工作。
-- ============================================================================
DROP TABLE IF EXISTS settle_bill;
CREATE TABLE settle_bill (
  id          BIGINT        NOT NULL AUTO_INCREMENT            COMMENT '主键',
  settle_date DATE          NOT NULL                           COMMENT '结算日期',
  dev_no      VARCHAR(16)   NOT NULL                           COMMENT '机具号',
  cnt         INT           NOT NULL DEFAULT 0                 COMMENT '系统记录笔数',
  dev_cnt     INT           NOT NULL DEFAULT 0                 COMMENT '机具上报笔数',
  amt         DECIMAL(12,2) NOT NULL DEFAULT 0.00              COMMENT '系统记录金额合计（元）',
  rebate_amt  DECIMAL(12,2) NOT NULL DEFAULT 0.00              COMMENT '减免合计（元），即补贴支出',
  state       TINYINT       NOT NULL DEFAULT 0                 COMMENT '结算状态：0 待核对 / 1 已平账 / 2 有差异',
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_settle (settle_date, dev_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='结算单：按机具日结的对账结果';


-- ============================================================================
-- 表 11 / 11　notify_log　通知日志
--   职责：观察者模式的可观测性证据。账单完成后每通知到一个观察者写一条。
--         报告里贴一条"扣款成功后三个观察者各写一条日志"的截图，
--         比空讲"我用了观察者模式"有力得多。
--   实体：com.zxy.tapcard.model.notify.NotifyLog
--   关键设计：本表对 pay_bill 是弱引用（存单号不存 id），
--             因为日志允许在账单归档后仍然可查，不追求引用完整性。
-- ============================================================================
DROP TABLE IF EXISTS notify_log;
CREATE TABLE notify_log (
  id          BIGINT      NOT NULL AUTO_INCREMENT              COMMENT '主键',
  evt_type    TINYINT     NOT NULL                             COMMENT '事件类型：1 账单完成 / 2 结算完成',
  bill_no     VARCHAR(32)          DEFAULT NULL                COMMENT '关联账单号，弱引用',
  watcher     VARCHAR(32) NOT NULL                             COMMENT '观察者类名，如 SmsWatcher',
  ok          TINYINT     NOT NULL DEFAULT 1                   COMMENT '执行结果：1 成功 / 0 失败',
  err_msg     VARCHAR(255)         DEFAULT NULL                COMMENT '失败原因',
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_notify_bill (bill_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='通知日志：观察者回调的执行留痕';


-- ============================================================================
-- 建表完成自检（在客户端里单跑这一段即可）
-- ============================================================================
-- SHOW TABLES;                        -- 期望 11 行
-- DESC pay_bill;                      -- uk_req_sn 出现在 Key 列，值为 UNI
-- SHOW INDEX FROM pay_bill;           -- 5 个索引
-- SHOW FULL COLUMNS FROM rebate_rule; -- params 类型为 json，Comment 为中文
-- SHOW TABLE STATUS FROM tapcard;     -- 11 张表 Engine=InnoDB


-- ############################################################################
-- Part 2 · 逻辑外键关系总表
-- ############################################################################
--
-- 本库不建物理 FOREIGN KEY，关系由应用层保证（决策 C6）。原因三条：
--   ① 种子数据导入不受表创建顺序限制；
--   ② 清表、重建演示数据时不用先解约束；
--   ③ 后续若分库，不必先拆约束。
-- 代价是数据库不再兜底引用完整性 —— 所以报告 3.2 必须补这一段说明，
-- 否则评审看到 E-R 图画了关系、DDL 里没有约束，会认为是漏写。
--
-- 全部 15 条关系（E-R 图的连线来源）：
--
--  序号  子表.列                 父表.列              基数      说明
--  ----  ----------------------  -------------------  --------  --------------------------------
--   1    card_face.acct_id       card_acct.id         N : 1     一个卡户可先后有多张卡面
--   2    pos_dev.stall_id        stall.id             N : 1 空   门禁与水控不属于任何档口
--   3    op_user.acct_id         card_acct.id         N : 1 空   学生账号
--   4    op_user.dev_no          pos_dev.dev_no       N : 1 空   商户账号（按业务键关联）
--   5    op_user.stall_id        stall.id             N : 1 空   商户账号
--   6    pay_bill.acct_id        card_acct.id         N : 1      谁被扣的钱
--   7    pay_bill.dev_no         pos_dev.dev_no       N : 1 空   在哪台机具上发生的
--   8    pay_bill.stall_id       stall.id             N : 1 空   钱最终归哪个档口
--   9    pay_bill.ori_bill_id    pay_bill.id          1 : 1 空   冲正单指向被冲正的原单（自关联）
--  10    bill_line.bill_id       pay_bill.id          N : 1      账单行属于哪张账单
--  11    acct_flow.acct_id       card_acct.id         N : 1      谁的余额动了
--  12    acct_flow.bill_id       pay_bill.id          1 : 1 空   因哪张账单而动的
--  13    rebate_rule.stall_id    stall.id             N : 1 空   规则限定档口
--  14    settle_bill.dev_no      pos_dev.dev_no       N : 1      哪台机具的日结
--  15    notify_log.bill_no      pay_bill.bill_no     N : 1 弱   日志弱引用，允许失联
--
-- 画 E-R 图时的实体与关系（顺时针摆放，中心放 pay_bill）：
--   卡户 1—N 卡面        卡户 1—N 账单        卡户 1—N 流水
--   档口 1—N 机具        档口 1—N 账单        档口 1—N 减免规则
--   机具 1—N 账单        机具 1—N 结算单
--   账单 1—N 账单行      账单 1—1 流水        账单 1—1 账单（冲正自关联）
--   操作员 N—1 卡户 / 机具 / 档口（三者按角色取其一）
-- ############################################################################


-- ############################################################################
-- Part 3 · 报告 3.2「每张表字段表」素材（五列）
-- ############################################################################
--
-- 用法：报告 3.2 要求「字段 / 类型 / 注释 / 外键关系 / 备注」五列。
--       下面每张表都已按这五列排好，直接照抄进 Word 转表格即可。
--       类型省略了 NULL/NOT NULL 之外的修饰，可空性写在「备注」里。
--
-- ---------------------------------------------------------------------------
-- 表 3-1　stall　档口
-- 职责：消费发生的商户主体，结算报表的归集维度。
-- ---------------------------------------------------------------------------
-- | 字段       | 类型        | 注释                          | 外键关系 | 备注                        |
-- |------------|-------------|-------------------------------|----------|-----------------------------|
-- | id         | BIGINT      | 主键                          | —        | 自增                        |
-- | stall_name | VARCHAR(64) | 档口名称                      | —        | 非空                        |
-- | stall_type | TINYINT     | 档口类型 1食堂/2超市/3其他    | —        | 非空，默认 1                |
-- | contact    | VARCHAR(32) | 联系人及电话                  | —        | 可空                        |
-- | created_at | DATETIME    | 创建时间                      | —        | 非空，默认当前时间          |
-- | updated_at | DATETIME    | 更新时间                      | —        | 非空，更新时自动刷新        |
--
-- ---------------------------------------------------------------------------
-- 表 3-2　card_acct　卡户
-- 职责：学生账户主体，承载余额与乐观锁版本。
-- ---------------------------------------------------------------------------
-- | 字段       | 类型          | 注释                             | 外键关系 | 备注                          |
-- |------------|---------------|----------------------------------|----------|-------------------------------|
-- | id         | BIGINT        | 主键                             | —        | 自增                          |
-- | stu_no     | VARCHAR(16)   | 学号                             | —        | 非空，唯一索引 uk_stu_no      |
-- | stu_name   | VARCHAR(32)   | 学生姓名                         | —        | 非空                          |
-- | dept       | VARCHAR(64)   | 院系班级                         | —        | 可空                          |
-- | bal        | DECIMAL(12,2) | 卡内余额（元）                   | —        | 非空，默认 0.00，不加 CHECK   |
-- | state      | TINYINT       | 卡户状态 1正常/2冻结/3注销       | —        | 非空，默认 1                  |
-- | ver        | INT           | 乐观锁版本号                     | —        | 非空，默认 0，每次更新 +1     |
-- | created_at | DATETIME      | 创建时间                         | —        | 非空，默认当前时间            |
-- | updated_at | DATETIME      | 更新时间                         | —        | 非空，更新时自动刷新          |
--
-- ---------------------------------------------------------------------------
-- 表 3-3　card_face　卡面
-- 职责：物理卡凭证，挂失与补卡的作用对象。
-- ---------------------------------------------------------------------------
-- | 字段       | 类型        | 注释                        | 外键关系            | 备注                        |
-- |------------|-------------|-----------------------------|---------------------|-----------------------------|
-- | id         | BIGINT      | 主键                        | —                   | 自增                        |
-- | card_no    | VARCHAR(20) | 卡号                        | —                   | 非空，唯一索引 uk_card_no   |
-- | acct_id    | BIGINT      | 所属卡户 ID                 | → card_acct.id      | 非空，索引 idx_face_acct    |
-- | state      | TINYINT     | 卡面状态 1正常/2挂失/3作废   | —                   | 非空，默认 1                |
-- | issue_at   | DATETIME    | 发卡时间                    | —                   | 可空                        |
-- | lose_at    | DATETIME    | 挂失时间                    | —                   | 可空，未挂失为 NULL         |
-- | created_at | DATETIME    | 创建时间                    | —                   | 非空，默认当前时间          |
-- | updated_at | DATETIME    | 更新时间                    | —                   | 非空，更新时自动刷新        |
--
-- ---------------------------------------------------------------------------
-- 表 3-4　pos_dev　机具
-- 职责：接入终端，四类报文差异由适配器抹平。
-- ---------------------------------------------------------------------------
-- | 字段         | 类型        | 注释                                  | 外键关系      | 备注                     |
-- |--------------|-------------|---------------------------------------|---------------|--------------------------|
-- | id           | BIGINT      | 主键                                  | —             | 自增                     |
-- | dev_no       | VARCHAR(16) | 机具号                                | —             | 非空，唯一索引 uk_dev_no |
-- | dev_name     | VARCHAR(64) | 机具名称或安装位置                    | —             | 非空                     |
-- | kind         | TINYINT     | 机具类型 1食堂POS/2超市/3门禁/4水控   | —             | 非空                     |
-- | stall_id     | BIGINT      | 所属档口 ID                           | → stall.id    | 可空，门禁水控为 NULL    |
-- | state        | TINYINT     | 机具状态 1可用/2停用                  | —             | 非空，默认 1             |
-- | last_beat_at | DATETIME    | 最后一次心跳时间                      | —             | 可空，在线状态实时计算   |
-- | created_at   | DATETIME    | 创建时间                              | —             | 非空，默认当前时间       |
-- | updated_at   | DATETIME    | 更新时间                              | —             | 非空，更新时自动刷新     |
--
-- ---------------------------------------------------------------------------
-- 表 3-5　op_user　操作员
-- 职责：登录主体与角色归属，越权校验的数据基础。
-- ---------------------------------------------------------------------------
-- | 字段       | 类型         | 注释                                  | 外键关系       | 备注                     |
-- |------------|--------------|---------------------------------------|----------------|--------------------------|
-- | id         | BIGINT       | 主键                                  | —              | 自增                     |
-- | emp_no     | VARCHAR(16)  | 工号或学号，登录名                    | —              | 非空，唯一索引 uk_emp_no |
-- | emp_name   | VARCHAR(32)  | 姓名                                  | —              | 非空                     |
-- | pwd_hash   | VARCHAR(100) | BCrypt 密文                           | —              | 非空，禁止明文           |
-- | role       | TINYINT      | 角色 1学生/2商户/3财务/9管理员        | —              | 非空                     |
-- | acct_id    | BIGINT       | 学生账号绑定的卡户 ID                 | → card_acct.id | 可空，按角色填           |
-- | dev_no     | VARCHAR(16)  | 商户账号绑定的机具号                  | → pos_dev.dev_no | 可空，按角色填         |
-- | stall_id   | BIGINT       | 商户账号绑定的档口 ID                 | → stall.id     | 可空，按角色填           |
-- | state      | TINYINT      | 账号状态 1可用/2停用                  | —              | 非空，默认 1             |
-- | created_at | DATETIME     | 创建时间                              | —              | 非空，默认当前时间       |
-- | updated_at | DATETIME     | 更新时间                              | —              | 非空，更新时自动刷新     |
--
-- ---------------------------------------------------------------------------
-- 表 3-6　rebate_rule　减免规则
-- 职责：优惠策略的数据源，策略模式在数据库层的落点。
-- ---------------------------------------------------------------------------
-- | 字段       | 类型        | 注释                                          | 外键关系   | 备注                        |
-- |------------|-------------|-----------------------------------------------|------------|-----------------------------|
-- | id         | BIGINT      | 主键                                          | —          | 自增                        |
-- | rule_name  | VARCHAR(64) | 规则名称                                      | —          | 非空                        |
-- | kind       | TINYINT     | 类型 1不打折/2折扣率/3立减/4每日补贴/5积分抵扣 | —          | 非空                        |
-- | params     | JSON        | 规则参数，金额用整数分                        | —          | 非空，结构由 kind 决定      |
-- | prio       | INT         | 优先级                                        | —          | 非空，升序取第一条命中      |
-- | dev_kind   | TINYINT     | 限定机具类型                                  | —          | 可空，NULL 表示不限         |
-- | stall_id   | BIGINT      | 限定档口 ID                                   | → stall.id | 可空，NULL 表示不限         |
-- | start_at   | DATETIME    | 生效开始时间                                  | —          | 非空                        |
-- | end_at     | DATETIME    | 生效结束时间                                  | —          | 可空，NULL 表示长期有效     |
-- | enabled    | TINYINT     | 启用开关 1启用/0停用                          | —          | 非空，默认 1                |
-- | created_at | DATETIME    | 创建时间                                      | —          | 非空，默认当前时间          |
-- | updated_at | DATETIME    | 更新时间                                      | —          | 非空，更新时自动刷新        |
--
-- ---------------------------------------------------------------------------
-- 表 3-7　pay_bill　账单
-- 职责：全系统凭据中心，充值、消费、冲正统一记录。
-- ---------------------------------------------------------------------------
-- | 字段        | 类型          | 注释                                | 外键关系         | 备注                          |
-- |-------------|---------------|-------------------------------------|------------------|-------------------------------|
-- | id          | BIGINT        | 主键                                | —                | 自增                          |
-- | bill_no     | VARCHAR(32)   | 账单号                              | —                | 非空，唯一索引 uk_bill_no     |
-- | kind        | TINYINT       | 账单类型 1充值/2消费/3冲正          | —                | 非空                          |
-- | acct_id     | BIGINT        | 卡户 ID                             | → card_acct.id   | 非空                          |
-- | dev_no      | VARCHAR(16)   | 机具号                              | → pos_dev.dev_no | 可空，充值单为 NULL           |
-- | stall_id    | BIGINT        | 档口 ID                             | → stall.id       | 可空，门禁水控为 NULL         |
-- | amt         | DECIMAL(12,2) | 原价金额（元）                      | —                | 非空，冲正单为负数            |
-- | rebate_amt  | DECIMAL(12,2) | 减免金额（元）                      | —                | 非空，默认 0.00               |
-- | pay_amt     | DECIMAL(12,2) | 实付金额（元）                      | —                | 非空，=amt-rebate_amt         |
-- | state       | TINYINT       | 账单状态 0待结算/1已结算/2已冲正    | —                | 非空，默认 0                  |
-- | req_sn      | VARCHAR(40)   | 请求号，幂等键                      | —                | 非空，唯一索引 uk_req_sn      |
-- | ori_bill_id | BIGINT        | 被冲正的原账单 ID                   | → pay_bill.id    | 可空，自关联                  |
-- | remark      | VARCHAR(128)  | 备注或冲正原因                      | —                | 可空                          |
-- | created_at  | DATETIME      | 创建时间                            | —                | 非空，默认当前时间            |
-- | updated_at  | DATETIME      | 更新时间                            | —                | 非空，更新时自动刷新          |
--
-- ---------------------------------------------------------------------------
-- 表 3-8　bill_line　账单行
-- 职责：一张账单买了什么，建造者模式的落点。
-- ---------------------------------------------------------------------------
-- | 字段       | 类型          | 注释            | 外键关系      | 备注                     |
-- |------------|---------------|-----------------|---------------|--------------------------|
-- | id         | BIGINT        | 主键            | —             | 自增                     |
-- | bill_id    | BIGINT        | 所属账单 ID     | → pay_bill.id | 非空，索引 idx_line_bill |
-- | item_name  | VARCHAR(64)   | 品名            | —             | 非空                     |
-- | qty        | INT           | 数量            | —             | 非空，默认 1             |
-- | amt        | DECIMAL(12,2) | 小计原价（元）  | —             | 非空                     |
-- | rebate_amt | DECIMAL(12,2) | 小计减免（元）  | —             | 非空，默认 0.00          |
-- | created_at | DATETIME      | 创建时间        | —             | 非空，默认当前时间       |
--
-- ---------------------------------------------------------------------------
-- 表 3-9　acct_flow　流水
-- 职责：余额每一次变动的凭证，与账单一一对应。
-- ---------------------------------------------------------------------------
-- | 字段       | 类型          | 注释                       | 外键关系      | 备注                        |
-- |------------|---------------|----------------------------|---------------|-----------------------------|
-- | id         | BIGINT        | 主键                       | —             | 自增                        |
-- | acct_id    | BIGINT        | 卡户 ID                    | → card_acct.id | 非空，索引 idx_flow_acct   |
-- | bill_id    | BIGINT        | 关联账单 ID                | → pay_bill.id | 可空                        |
-- | kind       | TINYINT       | 类型 1充值/2消费/3冲正     | —             | 非空                        |
-- | chg_amt    | DECIMAL(12,2) | 变动金额（元）             | —             | 非空，支出与冲正为负数      |
-- | bal_before | DECIMAL(12,2) | 变动前余额（元）           | —             | 非空                        |
-- | bal_after  | DECIMAL(12,2) | 变动后余额（元）           | —             | 非空                        |
-- | created_at | DATETIME      | 创建时间                   | —             | 非空，默认当前时间          |
--
-- ---------------------------------------------------------------------------
-- 表 3-10　settle_bill　结算单
-- 职责：机具日结结果，对账大屏的数据源。
-- ---------------------------------------------------------------------------
-- | 字段        | 类型          | 注释                            | 外键关系         | 备注                        |
-- |-------------|---------------|---------------------------------|------------------|-----------------------------|
-- | id          | BIGINT        | 主键                            | —                | 自增                        |
-- | settle_date | DATE          | 结算日期                        | —                | 非空，与 dev_no 组唯一      |
-- | dev_no      | VARCHAR(16)   | 机具号                          | → pos_dev.dev_no | 非空                        |
-- | cnt         | INT           | 系统记录笔数                    | —                | 非空，默认 0                |
-- | dev_cnt     | INT           | 机具上报笔数                    | —                | 非空，默认 0                |
-- | amt         | DECIMAL(12,2) | 系统记录金额合计（元）          | —                | 非空，默认 0.00             |
-- | rebate_amt  | DECIMAL(12,2) | 减免合计（元），即补贴支出      | —                | 非空，默认 0.00             |
-- | state       | TINYINT       | 结算状态 0待核对/1已平账/2有差异 | —               | 非空，默认 0                |
-- | created_at  | DATETIME      | 创建时间                        | —                | 非空，默认当前时间          |
-- | updated_at  | DATETIME      | 更新时间                        | —                | 非空，更新时自动刷新        |
--
-- ---------------------------------------------------------------------------
-- 表 3-11　notify_log　通知日志
-- 职责：观察者回调的执行留痕，观察者模式的可观测性证据。
-- ---------------------------------------------------------------------------
-- | 字段       | 类型         | 注释                     | 外键关系          | 备注                    |
-- |------------|--------------|--------------------------|-------------------|-------------------------|
-- | id         | BIGINT       | 主键                     | —                 | 自增                    |
-- | evt_type   | TINYINT      | 事件类型 1账单完成/2结算完成 | —             | 非空                    |
-- | bill_no    | VARCHAR(32)  | 关联账单号               | → pay_bill.bill_no | 可空，弱引用           |
-- | watcher    | VARCHAR(32)  | 观察者类名               | —                 | 非空                    |
-- | ok         | TINYINT      | 执行结果 1成功/0失败     | —                 | 非空，默认 1            |
-- | err_msg    | VARCHAR(255) | 失败原因                 | —                 | 可空                    |
-- | created_at | DATETIME     | 创建时间                 | —                 | 非空，默认当前时间      |
--
-- ############################################################################
-- 文件结束。建表完成后，下一步是 sql/ykt_seed.sql（演示数据）。
-- ############################################################################
