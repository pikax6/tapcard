package com.zxy.tapcard.biz.pay.check;

import com.zxy.tapcard.biz.pay.draft.PayBillDraft;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.model.acct.CardAcct;
import com.zxy.tapcard.model.acct.CardFace;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class CardStateCheck extends PayCheck {

    @Override
    protected void verify(PayBillDraft d) {
        // 冲正是系统发起的修正，手上没有卡，所以只校验卡户
        if (d.kind() != 3) {
            CardFace f = d.face();
            if (f == null) throw BizErr.of(ErrCode.FACE_NOT_FOUND);
            if (f.getState() == 3) throw BizErr.of(ErrCode.FACE_DEAD, "卡号 " + f.getCardNo());
            if (f.getState() == 2) throw BizErr.of(ErrCode.FACE_LOST, "卡号 " + f.getCardNo());
        }

        CardAcct a = d.acct();
        if (a == null) throw BizErr.of(ErrCode.FACE_NOT_FOUND, "卡面无对应卡户");
        if (a.getState() == 2) throw BizErr.of(ErrCode.ACCT_FROZEN, "学号 " + a.getStuNo());
        if (a.getState() == 3) throw BizErr.of(ErrCode.ACCT_CLOSED, "学号 " + a.getStuNo());
    }
}
