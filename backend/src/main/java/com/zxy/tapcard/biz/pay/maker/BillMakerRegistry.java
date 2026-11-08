package com.zxy.tapcard.biz.pay.maker;

import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.model.pay.BillKind;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BillMakerRegistry {

    /** 类型编码 → 负责建这种单的 Maker。启动时建一次，运行期查表 */
    private final Map<Integer, PayBillMaker> box = new HashMap<>();

    public BillMakerRegistry(List<PayBillMaker> makers) {
        for (PayBillMaker m : makers) {
            // 遍历枚举拿到"总共有哪些类型"，再问每个 Maker 收不收
            for (BillKind k : BillKind.values()) {
                if (!m.mine(k.code())) continue;

                PayBillMaker old = box.put(k.code(), m);
                // 两个实现抢同一个类型是装配错误，必须启动就炸，不能后到者静默覆盖
                if (old != null) {
                    throw new IllegalStateException("账单类型 " + k.code() + "(" + k.name()
                            + ") 被重复注册: " + old.getClass().getSimpleName()
                            + " / " + m.getClass().getSimpleName());
                }
            }
        }
    }

    public PayBillMaker pick(int billKind) {
        PayBillMaker m = box.get(billKind);
        if (m == null) {
            throw BizErr.of(ErrCode.PARAM_MISS, "未知账单类型 " + billKind + "，已注册 " + box.keySet());
        }
        return m;
    }

    /** 调试用：当前注册了几种类型 */
    public int size() { return box.size(); }
}

