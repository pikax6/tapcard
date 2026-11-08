package com.zxy.tapcard.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import com.zxy.tapcard.common.R;
import com.zxy.tapcard.dao.CardAcctDao;

/**
 * 骨架自检用：起来先打这个，能返回就说明 Web 层通了。
 * 后面 common.R 建好之后，把返回改成 R.ok(...) 统一格式。
 */
@RestController
@RequestMapping("/api")
public class PingApi {
    private final CardAcctDao cardAcctDao;

    public PingApi(CardAcctDao cardAcctDao) {
        this.cardAcctDao = cardAcctDao;
    }


    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 0);
        body.put("msg", "pong");
        body.put("data", LocalDateTime.now().toString());
        return body;
    }
    @GetMapping("/ping/db")
    public R<String> db() {
        Long cnt = cardAcctDao.selectCount(null);
        return R.ok("card_acct 行数 = " + cnt);
    }

}
