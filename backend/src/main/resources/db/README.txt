建表脚本不放在这里，统一放工程根目录的 sql/ 下。

原因：交付要求 zip 里要有 sql/ 目录（schema + seed），
如果这里再放一份，两份一旦不同步，答辩时会被问"到底以哪份为准"。

所以：
  sql/ykt_schema.sql  建库建表
  sql/ykt_seed.sql    演示数据

执行顺序见根目录 README.md。
