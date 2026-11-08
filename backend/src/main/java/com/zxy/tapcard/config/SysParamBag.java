package com.zxy.tapcard.config;

public final class SysParamBag {

    private static volatile SysParamBag IT;

    private volatile int  singleQuotaFen = 20000;   // 超市单笔限额 200 元
    private volatile int  beatTimeoutSec = 60;      // 心跳超时
    private volatile boolean settleRunning = false; // 结算任务是否在跑
    private volatile int  noPwdFen = 3000;          // 小额免密额度 30 元

    private SysParamBag() {}

    public static SysParamBag me() {
        if (IT == null) {
            synchronized (SysParamBag.class) {
                if (IT == null) {
                    IT = new SysParamBag();
                }
            }
        }
        return IT;
    }

    public int  singleQuotaFen() { return singleQuotaFen; }
    public void singleQuotaFen(int fen) { this.singleQuotaFen = fen; }

    public int  beatTimeoutSec() { return beatTimeoutSec; }
    public int  noPwdFen() { return noPwdFen; }

    /** 结算任务互斥：抢到才跑，跑完必须还回去 */
    public boolean tryLockSettle() {
        synchronized (this) {
            if (settleRunning) return false;
            settleRunning = true;
            return true;
        }
    }

    public void unlockSettle() {
        synchronized (this) { settleRunning = false; }
    }
}
