# tapcard · 校园一卡通消费与结算系统

软件设计模式课程期末考查（题目一）。后端 Spring Boot 3.3 + JDK 21，前端 Vue 3 + Vite。

## 一、环境要求

| 项 | 版本 |
|---|---|
| JDK | 21 |
| Maven | 3.9+ |
| MySQL | 8.0（字符集 utf8mb4） |
| Node.js | 18+（建议 20 LTS） |

## 二、建库

```sql
-- 方式一：命令行
mysql -uroot -p < sql/ykt_schema.sql
mysql -uroot -p < sql/ykt_seed.sql
```

或用 Navicat / DataGrip 打开 `sql/ykt_schema.sql` 整份执行，再执行 `sql/ykt_seed.sql`。

## 三、启动后端

1. 打开 `backend/src/main/resources/application.yml`，把 `spring.datasource.username` / `password` 改成你本机的；
2. 断言 `tapcard.jwt.secret` 换成你自己的串（至少 32 字节）；
3. 启动（三种方式，见下方「路径中文的坑」选一种）：

```bash
cd backend

# 方式 A：IDEA 里直接点 Run（推荐开发期用）
# 方式 B：打包后运行（跳板最稳，中文路径也不受影响）
mvn clean package -DskipTests
java -jar target/tapcard-1.0.0.jar

# 方式 C：mvn spring-boot:run（仅当工程路径全为英文时可用）
mvn spring-boot:run
```

自检：浏览器访问 <http://localhost:8080/api/ping>，返回 `{"code":0,"msg":"pong",...}` 即通。

### ⚠ 路径含中文的坑（本项目路径就含中文，必读）

工程的父目录是 `A-软件设计模式\期末考察\`，含中文。在这种路径下执行
`mvn spring-boot:run` 会报：

```
错误: 找不到或无法加载主类 com.zxy.tapcard.TapcardApp
原因: java.lang.ClassNotFoundException: com.zxy.tapcard.TapcardApp
```

**这不是代码没编译，也不是数据库连不上。** 原因：classpath 很长时
`spring-boot-maven-plugin` 会把它写进 `%TEMP%\spring-boot-*.argfile` 再交给
子 JVM（`-cp @argfile`），而 argfile 里的中文路径在子 JVM 读回来时编码对不上，
整条 classpath 失效，主类自然找不到。

已实测的对照结论：

| 场景 | 结果 |
|---|---|
| `mvn spring-boot:run`，中文路径 | ✗ ClassNotFoundException |
| `mvn spring-boot:run`，纯英文路径（如 `D:\tmp\tc`） | ✓ 正常启动 |
| 中文路径 + 手工 `java -cp "target\classes;依赖" com.zxy.tapcard.TapcardApp` | ✓ 正常启动 |
| 中文路径 + `java -jar target\tapcard-1.0.0.jar` | ✓ 正常启动，`/api/ping` 通 |
| `mvn -Dfile.encoding=GBK spring-boot:run` | ✗ 无效 |

**结论：开发期用 IDEA 的 Run 按钮，命令行验证用 `java -jar`。**
真要彻底解决就把整个 `tapcard` 目录搬到纯英文路径（如 `D:\work\tapcard`），
但会连带改 README 与报告里的路径，收益不高，不必为此折腾。

## 四、启动前端

```bash
cd frontend
npm install
npm run dev
```

默认 <http://localhost:5173>，`/api` 由 vite 代理到 `localhost:8080`，不需要额外配跨域。
**只认终端打印出来的地址**；`127.0.0.1:5173` 和 `8080` 都打不开前端页面。

### 七个页面各自做什么

| 路由 | 页面 | 内容 | 谁能进 |
|---|---|---|---|
| `/home` | 概览 | 按角色显示四张指标卡 + 最近账单；财务的「差异机具」卡可点进对账盘 | 全部 |
| `/acct` | 卡户册 | 搜索、开户、挂失/解挂、销户；卡号带状态标签（正常/挂失/作废） | 学生只看自己，财务/管理员看全部 |
| `/acct/:id` | 卡户详情 | 余额、名下卡面、流水表；越权访问显示错误页而非白屏 | 学生仅限自己 |
| `/dev` | 机具台 | 刷卡消费、充值、冲正；切换机具时按厂商格式拼报文并实时预览 | 商户/财务/管理员 |
| `/bill` | 账单簿 | 筛选、分页、行内冲正；冲正后原单变灰显示「已冲正」 | 学生看自己，商户看自己机具 |
| `/settle` | 对账盘 | 生成批次、填机具上报笔数制造差异；差异行标红，展开行显示装饰链描述 | 财务/管理员 |
| `/stat` | 统计 | 按档口（饼图）、按机具（柱状）、按日期趋势（双线折线） | 财务/管理员 |

登录后会按角色落到不同首页：学生进账单簿，商户进机具台，财务与管理员进概览。

## 五、演示账号

密码统一 `123456`（库里存的是 BCrypt 密文）。

| 角色 | 登录名 | 姓名 | 能做什么 |
|---|---|---|---|
| 学生 | `2024081084` | 张语瑄 | 查自己的余额与流水，主演示账号（有 4 条历史流水） |
| 学生 | `2024081085` | 李思远 | 低余额户，用来演「余额不足」 |
| 商户操作员 | `M0001` | 李建国 | 只能在 `D001` 发起扣款，只能查自己机具的账单 |
| 财务 | `F0001` | 赵敏 | 开户、挂失、冲正、出结算、看对账大屏 |
| 管理员 | `A0001` | 系统管理员 | 全部 |

三个归属字段（`acct_id` / `dev_no` / `stall_id`）就是越权校验的依据：学生账号只绑卡户，商户账号只绑机具，财务账号不绑——所以拿学生账号查别人流水的请求会被拦下。

## 六、接口基址

- 后端基址：`http://localhost:8080/api`
- 统一返回体：`{ "code": 0, "msg": "ok", "data": ... }`，`code != 0` 表示业务失败
- 鉴权：登录后把 token 放请求头 `Authorization: Bearer <token>`

## 七、目录

```
backend/    Spring Boot 工程（见 backend/NAMING.md 的建类清单）
frontend/   Vue 3 工程
sql/        建表与演示数据
```
