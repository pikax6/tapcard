package com.zxy.tapcard.biz.acct;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zxy.tapcard.common.Amt;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.dao.CardAcctDao;
import com.zxy.tapcard.dao.CardFaceDao;
import com.zxy.tapcard.model.acct.CardAcct;
import com.zxy.tapcard.model.acct.CardFace;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 卡户生命周期：开户 / 挂失 / 解挂 / 销户。只有财务能用 */
@Service
public class AcctOpSvc {

    private final CardAcctDao cardAcctDao;
    private final CardFaceDao cardFaceDao;

    public AcctOpSvc(CardAcctDao cardAcctDao, CardFaceDao cardFaceDao) {
        this.cardAcctDao = cardAcctDao;
        this.cardFaceDao = cardFaceDao;
    }

    /** 开户：卡户与卡面必须同一个事务，否则会出现"有户无卡"的空账户 */
    @Transactional(rollbackFor = Exception.class)
    public Long open(String stuNo, String stuName, String dept) {
        if (stuNo == null || stuNo.isBlank() || stuName == null || stuName.isBlank()) {
            throw BizErr.of(ErrCode.PARAM_MISS, "学号与姓名不能为空");
        }
        if (cardAcctDao.countByStuNo(stuNo) > 0) {
            throw BizErr.of(ErrCode.PARAM_MISS, "该学号已开户: " + stuNo);
        }

        CardAcct a = new CardAcct();
        a.setStuNo(stuNo.trim());
        a.setStuName(stuName.trim());
        a.setDept(dept);
        a.setState(1);
        a.setBal(Amt.fix2(null));
        a.setVer(0);
        cardAcctDao.insert(a);

        CardFace f = new CardFace();
        f.setCardNo(cardNoOf(a.getId()));
        f.setAcctId(a.getId());
        f.setState(1);
        f.setIssueAt(LocalDateTime.now());
        cardFaceDao.insert(f);

        return a.getId();
    }

    /** 挂失：只改卡面，卡户不动 —— 学生人还在，只是这张卡不能刷了 */
    @Transactional(rollbackFor = Exception.class)
    public void lose(String cardNo) {
        CardFace f = mustFace(cardNo);
        if (f.getState() == 3) throw BizErr.of(ErrCode.FACE_DEAD, "卡号 " + cardNo);
        if (f.getState() == 2) throw BizErr.of(ErrCode.FACE_LOST, "卡号 " + cardNo + " 已挂失");

        int rows = cardFaceDao.update(null, new LambdaUpdateWrapper<CardFace>()
                .eq(CardFace::getCardNo, cardNo)
                .eq(CardFace::getState, 1)
                .set(CardFace::getState, 2)
                .set(CardFace::getLoseAt, LocalDateTime.now()));
        if (rows == 0) throw BizErr.of(ErrCode.FACE_LOST, "挂失失败，状态已变化");
    }

    /** 解挂：必须把 lose_at 一起清掉。updateById 不会把字段置 null，只能用 UpdateWrapper */
    @Transactional(rollbackFor = Exception.class)
    public void unlock(String cardNo) {
        CardFace f = mustFace(cardNo);
        if (f.getState() == 3) throw BizErr.of(ErrCode.FACE_DEAD, "卡号 " + cardNo);
        if (f.getState() == 1) throw BizErr.of(ErrCode.PARAM_MISS, "该卡未挂失，无需解挂");

        int rows = cardFaceDao.update(null, new LambdaUpdateWrapper<CardFace>()
                .eq(CardFace::getCardNo, cardNo)
                .eq(CardFace::getState, 2)
                .set(CardFace::getState, 1)
                .set(CardFace::getLoseAt, null));
        if (rows == 0) throw BizErr.of(ErrCode.PARAM_MISS, "解挂失败，状态已变化");
    }

    /** 销户：余额必须清零（先退费再销户），卡户作废，名下卡面全部作废 */
    @Transactional(rollbackFor = Exception.class)
    public void close(Long acctId) {
        CardAcct a = cardAcctDao.selectById(acctId);
        if (a == null) throw BizErr.of(ErrCode.FACE_NOT_FOUND, "卡户不存在: " + acctId);
        if (a.getState() == 3) throw BizErr.of(ErrCode.ACCT_CLOSED, "学号 " + a.getStuNo());
        if (Amt.nz(a.getBal()).signum() != 0) {
            throw BizErr.of(ErrCode.BILL_STATE_BAD, "余额 " + a.getBal() + " 元未清零，不能销户");
        }

        cardFaceDao.killByAcct(acctId);
        cardAcctDao.update(null, new LambdaUpdateWrapper<CardAcct>()
                .eq(CardAcct::getId, acctId)
                .set(CardAcct::getState, 3));
    }

    private CardFace mustFace(String cardNo) {
        if (cardNo == null || cardNo.isBlank()) throw BizErr.of(ErrCode.PARAM_MISS, "卡号不能为空");
        CardFace f = cardFaceDao.byCardNo(cardNo);
        if (f == null) throw BizErr.of(ErrCode.FACE_NOT_FOUND, "卡号 " + cardNo);
        return f;
    }

    /** 卡号 = 发卡网点 0810 + 卡户流水 6 位，与卡户一一对应，天然唯一 */
    private String cardNoOf(Long acctId) {
        return "0810" + String.format("%06d", acctId);
    }
}
