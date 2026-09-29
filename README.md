# abcmysqldatainit

通过 Java 代码为 MySQL 三张表批量生成初始化测试数据，用于构造大数据量环境，测试 SQL 性能优化、索引设计、分页查询等场景。

## 技术栈

- JDK 17 + Maven
- mysql-connector-java 8.0.33（纯 JDBC，无 ORM 框架）
- 批量插入 + 多线程分片 + `rewriteBatchedStatements` 加速写入

## 数据表

DDL 位于 `src/main/resources/db.sql`，目标库为 `my_test`：

| 表名 | 说明 | 数据量 |
|---|---|---|
| `abc_clinic` | 诊所（id, name） | 1 万条 |
| `abc_employee` | 雇员（name, type：1=医生 / 2=收费员, clinic_id） | 1 万条 |
| `abc_charge` | 收费记录（clinic_id, employ_id, patient_id, amount, charge_time） | 3000 万条（可调至 1 亿） |

## 使用方式

1. 修改 `src/main/java/com/gang/TableDataGenerator.java` 中的数据库连接配置：

   ```java
   private static final String URL = "jdbc:mysql://127.0.0.1:3306/my_test?rewriteBatchedStatements=true&useSSL=false&serverTimezone=Asia/Shanghai";
   private static final String USER = "root";
   private static final String PWD = "your_password";
   ```

2. 在 `my_test` 库中执行 `src/main/resources/db.sql` 创建三张表。

3. 运行 `TableDataGenerator#main`，程序将按以下流程执行：

   1. **清空表**：`TRUNCATE` 三张表并重置自增 ID
   2. **插入诊所**：1 万条（诊所_1 ~ 诊所_10000），每 1000 条一批提交
   3. **插入雇员**：1 万条，随机分配诊所，约 40% 为收费员（type=2），收费员缓存到内存
   4. **多线程生成收费记录**：20 个线程分片插入 3000 万条，每条记录保证：
      - 雇员一定是收费员，且诊所与该雇员一致（数据业务关联合理）
      - 患者 ID 随机（100 万 ~ 200 万）
      - 金额为 0 ~ 9999.99（两位小数）
      - 收费时间随机落在 2025 年内（8~18 点工作时段）
   - 运行过程中每 5 秒打印一次插入进度

## 可调参数

均在 `TableDataGenerator` 顶部常量中配置：

| 常量 | 说明 | 默认值 |
|---|---|---|
| `CLINIC_COUNT` | 诊所数量 | 10000 |
| `EMPLOYEE_COUNT` | 雇员数量 | 10000 |
| `CHARGE_TOTAL` | 收费记录总数 | 3000 万 |
| `BATCH_SIZE` | 每批提交条数 | 1000 |
| `THREAD_NUM` | charge 插入线程数 | 20 |

## 注意事项

- 数据库密码目前硬编码在源码中，若仓库公开请注意泄露风险，建议改为配置文件或环境变量注入。
- 生成 3000 万条数据耗时较长，请预留磁盘空间并耐心等待（控制台有进度输出）。
