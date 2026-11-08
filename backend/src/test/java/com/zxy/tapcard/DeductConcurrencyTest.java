package com.zxy.tapcard;

import com.zxy.tapcard.dao.CardAcctDao;
import com.zxy.tapcard.model.acct.CardAcct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 报告 5.5「并发对照组」用的测试。
 *
 * 测的是什么：卡里只有 50 元，100 个线程同时各扣 1 元，会不会扣出负数。
 *
 * 为什么不走 PaySvc.consume()：
 *   RiskCheck 规定同一张卡 1 分钟内最多 3 笔，100 个并发请求里 97 个会先被
 *   风控 3006 拦下，根本到不了扣款那一步 —— 那样测出来的是风控，不是并发安全。
 *   所以这里直接打 CardAcctDao.deduct()，也就是 DeductCheck 真正执行的那条
 *   条件更新 SQL，把变量收敛到"版本条件在不在"这一件事上。
 *
 * 对照组怎么造（跑完务必改回来）：
 *   打开 dao/CardAcctDao.java，把 deduct() 的 @Update 里
 *     " and ver = #{ver} and bal >= #{amt}" 删掉，只留 "where id = #{id}"，
 *   重新编译后右键重跑本测试，第二次输出就是对照组。
 *
 * 注意：本测试直接改真实库 card_acct 表（不写账单、不写流水）。
 *   跑完用下面这条恢复（数值按你跑之前的实际值填）：
 *     update card_acct set bal = 165.70, ver = 8 where id = 1;
 */
@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=120")
class DeductConcurrencyTest {

    private static final Long ACCT_ID = 1L;
    private static final int THREADS = 100;
    private static final BigDecimal ONE_YUAN = new BigDecimal("1.00");

    @Autowired
    private CardAcctDao cardAcctDao;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void concurrentDeduct() throws Exception {
        // 重置到"卡里只有 50 元"。用 JdbcTemplate 直写，绕开 @Version 的自动条件
        jdbc.update("update card_acct set bal = 50.00, ver = 0 where id = ?", ACCT_ID);
        System.out.println("===== 并发扣款测试 =====");
        System.out.println("初始余额 = 50.00，线程数 = " + THREADS + "，每笔扣 1.00");

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger blocked = new AtomicInteger();

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    // 模拟一次真实请求：先读当前版本，再拿读到的版本去条件更新
                    CardAcct snapshot = cardAcctDao.selectById(ACCT_ID);
                    int rows = cardAcctDao.deduct(ACCT_ID, ONE_YUAN, snapshot.getVer());
                    if (rows == 1) {
                        ok.incrementAndGet();
                    } else {
                        // 影响行数 0 = 版本被别的线程改了，或者余额已经不够
                        blocked.incrementAndGet();
                    }
                } catch (Exception e) {
                    blocked.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();          // 一声令下，100 个线程同时开跑
        done.await(120, TimeUnit.SECONDS);   // 等全部结束，最多 2 分钟，避免卡死
        pool.shutdown();

        CardAcct now = cardAcctDao.selectById(ACCT_ID);
        System.out.println("成功 = " + ok.get() + "，被拦 = " + blocked.get());
        System.out.println("最终余额 = " + now.getBal());
        System.out.println("最终 ver  = " + now.getVer());
        System.out.println(now.getBal().signum() < 0
                ? ">>> 结论：出现超扣，并发保护失效"
                : ">>> 结论：未出现超扣，条件更新生效");
        System.out.println("========================");
    }
}
