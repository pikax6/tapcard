-- ############################################################################
-- ykt_er_only.sql  ·  仅用于「自动生成 E-R 图」，不是正式建库脚本
-- ############################################################################
--
-- 为什么要单独这个文件：
--   正式库 tapcard 刻意不建物理外键（见 ykt_schema.sql Part 2 的三条理由），
--   而 DataGrip / Navicat / PowerDesigner 的「自动生成 E-R 图」是靠
--   数据库里的 FOREIGN KEY 元数据来画连线的 —— 没有外键，导出的图会是
--   11 个互不相连的孤立方块。
--
-- 这个脚本做的事：
--   ① 建一个独立的库 tapcard_er（不碰你的 tapcard 正式库）；
--   ② 表结构与正式库完全一致（11 张表，字段、注释、索引都一样）；
--   ③ 额外补上 15 条物理外键，只为让建模工具能把连线画出来。
--
-- 用法：
--   mysql -u root -p --default-character-set=utf8mb4 < ykt_er_only.sql
--   然后在该库里生成 E-R 图（步骤见《类图与报告素材 · 执行手册》第 3 节）。
--
-- 用完可以整库删掉：
--   DROP DATABASE IF EXISTS tapcard_er;
--
-- 注意：库里不放演示数据。E-R 图画的是结构，不是数据，空表导出的图更干净。
-- ############################################################################

DROP DATABASE IF EXISTS tapcard_er;
CREATE DATABASE tapcard_er DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE tapcard_er;

-- ---------------------------------------------------------------- 11 张表
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

-- ---------------------------------------------------------------- 15 条物理外键（仅为本文件而加）

-- 一个卡户可先后有多张卡面（挂失补卡）
ALTER TABLE card_face ADD CONSTRAINT fk_face_acct FOREIGN KEY (acct_id) REFERENCES card_acct(id);
-- 门禁与水控不属于任何档口，此列为 NULL
ALTER TABLE pos_dev ADD CONSTRAINT fk_dev_stall FOREIGN KEY (stall_id) REFERENCES stall(id);
-- 学生账号绑定卡户
ALTER TABLE op_user ADD CONSTRAINT fk_user_acct FOREIGN KEY (acct_id) REFERENCES card_acct(id);
-- 商户账号绑定机具（按业务键关联）
ALTER TABLE op_user ADD CONSTRAINT fk_user_dev FOREIGN KEY (dev_no) REFERENCES pos_dev(dev_no);
-- 商户账号绑定档口
ALTER TABLE op_user ADD CONSTRAINT fk_user_stall FOREIGN KEY (stall_id) REFERENCES stall(id);
-- 谁被扣的钱
ALTER TABLE pay_bill ADD CONSTRAINT fk_bill_acct FOREIGN KEY (acct_id) REFERENCES card_acct(id);
-- 在哪台机具上发生；充值单为 NULL
ALTER TABLE pay_bill ADD CONSTRAINT fk_bill_dev FOREIGN KEY (dev_no) REFERENCES pos_dev(dev_no);
-- 钱最终归哪个档口
ALTER TABLE pay_bill ADD CONSTRAINT fk_bill_stall FOREIGN KEY (stall_id) REFERENCES stall(id);
-- 冲正单指向被冲正的原单（自关联 1:1）
ALTER TABLE pay_bill ADD CONSTRAINT fk_bill_ori FOREIGN KEY (ori_bill_id) REFERENCES pay_bill(id);
-- 账单行属于哪张账单
ALTER TABLE bill_line ADD CONSTRAINT fk_line_bill FOREIGN KEY (bill_id) REFERENCES pay_bill(id);
-- 谁的余额动了
ALTER TABLE acct_flow ADD CONSTRAINT fk_flow_acct FOREIGN KEY (acct_id) REFERENCES card_acct(id);
-- 因哪张账单而动的（1:1）
ALTER TABLE acct_flow ADD CONSTRAINT fk_flow_bill FOREIGN KEY (bill_id) REFERENCES pay_bill(id);
-- 规则限定档口，NULL 表示全场通用
ALTER TABLE rebate_rule ADD CONSTRAINT fk_rule_stall FOREIGN KEY (stall_id) REFERENCES stall(id);
-- 哪台机具的日结
ALTER TABLE settle_bill ADD CONSTRAINT fk_settle_dev FOREIGN KEY (dev_no) REFERENCES pos_dev(dev_no);
-- 按账单号弱引用
ALTER TABLE notify_log ADD CONSTRAINT fk_log_bill FOREIGN KEY (bill_no) REFERENCES pay_bill(bill_no);
