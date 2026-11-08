package com.zxy.tapcard.biz.acct;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.dao.AcctFlowDao;
import com.zxy.tapcard.dao.CardAcctDao;
import com.zxy.tapcard.dao.CardFaceDao;
import com.zxy.tapcard.model.acct.AcctFlow;
import com.zxy.tapcard.model.acct.CardAcct;
import com.zxy.tapcard.model.acct.CardFace;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 真实主题：只管查，不做任何权限判断 */
@Service
public class AcctQuerySvc implements AcctQuery {

    private final CardAcctDao cardAcctDao;
    private final CardFaceDao cardFaceDao;
    private final AcctFlowDao acctFlowDao;

    public AcctQuerySvc(CardAcctDao cardAcctDao, CardFaceDao cardFaceDao, AcctFlowDao acctFlowDao) {
        this.cardAcctDao = cardAcctDao;
        this.cardFaceDao = cardFaceDao;
        this.acctFlowDao = acctFlowDao;
    }

    @Override
    public AcctVo mine(Long acctId) {
        CardAcct a = cardAcctDao.selectById(acctId);
        if (a == null) throw BizErr.of(ErrCode.FACE_NOT_FOUND, "卡户不存在: " + acctId);
        return toVo(a);
    }

    @Override
    public List<AcctVo> list(String keyword) {
        LambdaQueryWrapper<CardAcct> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            w.and(q -> q.like(CardAcct::getStuNo, k).or().like(CardAcct::getStuName, k));
        }
        w.orderByAsc(CardAcct::getId);

        List<AcctVo> out = new ArrayList<>();
        for (CardAcct a : cardAcctDao.selectList(w)) {
            out.add(toVo(a));
        }
        return out;
    }

    @Override
    public List<AcctFlow> flows(Long acctId, LocalDateTime from, LocalDateTime to) {
        return acctFlowDao.pageByAcct(acctId, from, to);
    }

    /** 卡户 + 卡面列表拼成视图，这一层不做缓存，字段少、量小 */
    private AcctVo toVo(CardAcct a) {
        AcctVo v = new AcctVo();
        v.setId(a.getId());
        v.setStuNo(a.getStuNo());
        v.setStuName(a.getStuName());
        v.setDept(a.getDept());
        v.setBal(a.getBal());
        v.setState(a.getState());
        v.setCreatedAt(a.getCreatedAt());

        List<CardFace> faces = cardFaceDao.selectList(
                new LambdaQueryWrapper<CardFace>().eq(CardFace::getAcctId, a.getId())
                        .orderByAsc(CardFace::getId));
        for (CardFace f : faces) v.getCards().add(new AcctVo.CardVo(f.getCardNo(), f.getState()));
        return v;
    }
}
