package com.gang;

import com.gang.entity.Employee;

import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class TableDataGenerator {
    // ==================== 配置项 ====================
    private static final int CLINIC_COUNT = 10000;
    private static final int EMPLOYEE_COUNT = 10000;
    //    private static final long CHARGE_TOTAL = 100_000_000L; // 收费记录 1亿
    private static final long CHARGE_TOTAL = 30_000_000L; // 收费记录 3千万
    private static final int BATCH_SIZE = 1000;
    private static final int THREAD_NUM = 20; // charge插入线程数，根据CPU调

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/my_test?rewriteBatchedStatements=true&useSSL=false&serverTimezone=Asia/Shanghai";
    private static final String USER = "root";
    private static final String PWD = "msq1615";

    // 全局缓存：所有收费员(type=2)，供charge多线程读取
    private static List<Employee> cashierList = new ArrayList<>();
    private static final AtomicLong progress = new AtomicLong(0);

    public static void main(String[] args) throws Exception {
        // 1. 清空表重置自增ID
        truncateAllTable();
        System.out.println("✅ 表清空完成");

        // 2. 插入诊所 1w
        insertClinic();
        System.out.println("✅ abc_clinic 10000 插入完成");

        //3. 插入雇员1w，同时收集收费员到内存
        insertEmployeeAndCacheCashier();
        System.out.println("✅ abc_employee 10000 插入完成，收费员数量：" + cashierList.size());
        if (cashierList.isEmpty()) {
            System.err.println("❌ 没有收费员，无法生成charge数据");
            return;
        }

        //4. 多线程插入1亿收费记录
        long perThread = CHARGE_TOTAL / THREAD_NUM;
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_NUM);
        for (int t = 0; t < THREAD_NUM; t++) {
            long start = t * perThread;
            long end = (t == THREAD_NUM - 1) ? CHARGE_TOTAL : (t + 1) * perThread;
            pool.submit(() -> batchInsertCharge(start, end));
        }
        pool.shutdown();
        while (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
            System.out.printf("⏳ charge进度：%d / %d%n", progress.get(), CHARGE_TOTAL);
        }
        System.out.println("🎉 全部数据插入完成");
    }

    /**
     * 清空三张表，重置自增
     */
    private static void truncateAllTable() throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PWD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            stmt.execute("TRUNCATE TABLE abc_charge");
            stmt.execute("TRUNCATE TABLE abc_employee");
            stmt.execute("TRUNCATE TABLE abc_clinic");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    /**
     * 插入诊所
     */
    private static void insertClinic() throws SQLException {
        String sql = "INSERT INTO abc_clinic(name) VALUES (?)";
        try (Connection conn = getConn();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 1; i <= CLINIC_COUNT; i++) {
                pstmt.setString(1, "诊所_" + i);
                pstmt.addBatch();
                if (i % BATCH_SIZE == 0) {
                    pstmt.executeBatch();
                    conn.commit();
                }
            }
            pstmt.executeBatch();
            conn.commit();
        }
    }

    /**
     * 插入雇员，并且缓存type=2收费员
     */
    private static void insertEmployeeAndCacheCashier() throws SQLException {
        String sql = "INSERT INTO abc_employee(name, type, clinic_id) VALUES (?,?,?)";
        List<Employee> tempCashier = new ArrayList<>();
        try (Connection conn = getConn();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 1; i <= EMPLOYEE_COUNT; i++) {
                // clinicId 1~10000
                long clinicId = random.nextLong(1, CLINIC_COUNT + 1);
                // 40%概率是收费员 type=2，60%医生type=1
                int type = random.nextDouble() < 0.4 ? 2 : 1;
                String empName = "雇员_" + i;

                pstmt.setString(1, empName);
                pstmt.setInt(2, type);
                pstmt.setLong(3, clinicId);
                pstmt.addBatch();

                // 缓存收费员，id是自增，插入顺序id=i
                if (type == 2) {
                    Employee emp = new Employee();
                    emp.setId((long) i);
                    emp.setName(empName);
                    emp.setType(type);
                    emp.setClinicId(clinicId);
                    tempCashier.add(emp);
                }

                if (i % BATCH_SIZE == 0) {
                    pstmt.executeBatch();
                    conn.commit();
                }
            }
            pstmt.executeBatch();
            conn.commit();
        }
        cashierList = Collections.unmodifiableList(tempCashier);
    }

    /**
     * 分片插入收费记录
     */
    private static void batchInsertCharge(long start, long end) {
        String sql = "INSERT INTO abc_charge(clinic_id, employ_id, patient_id, amount, charge_time) VALUES (?,?,?,?,?)";
        try (Connection conn = getConn();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int batchPending = 0; // 待提交计数（自己维护）
            for (long i = start; i < end; i++) {
                // 随机取收费员，保证employId是收费员，clinicId和雇员诊所一致
                Employee cashier = cashierList.get(random.nextInt(cashierList.size()));
                long clinicId = cashier.getClinicId();
                long employId = cashier.getId();
                long patientId = 1_000_000 + random.nextLong(1, 100_0000);

                // decimal(7,2) 0 ~ 9999.99 保留2位小数
                double raw = (random.nextInt(100000) / 100000.0) * 9999.99;
                BigDecimal amount = BigDecimal.valueOf(raw).setScale(2, BigDecimal.ROUND_HALF_UP);

                // 随机时间 2025全年
                LocalDateTime chargeTime = LocalDateTime.of(2025,
                        random.nextInt(1, 13),
                        random.nextInt(1, 28),
                        random.nextInt(8, 18),
                        random.nextInt(0, 60),
                        random.nextInt(0, 60));

                pstmt.setLong(1, clinicId);
                pstmt.setLong(2, employId);
                pstmt.setLong(3, patientId);
                pstmt.setBigDecimal(4, amount);
                pstmt.setTimestamp(5, Timestamp.valueOf(chargeTime));
                pstmt.addBatch();
                batchPending++;

                if (batchPending % BATCH_SIZE == 0) {
                    pstmt.executeBatch();
                    conn.commit();
                    progress.addAndGet(BATCH_SIZE);
                    batchPending = 0;
                }
            }
            if (batchPending > 0) {
                pstmt.executeBatch();
                conn.commit();
                progress.addAndGet(batchPending);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Connection getConn() throws SQLException {
        Connection conn = DriverManager.getConnection(URL, USER, PWD);
        conn.setAutoCommit(false);
        return conn;
    }
}
