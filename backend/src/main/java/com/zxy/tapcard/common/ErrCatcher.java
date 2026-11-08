package com.zxy.tapcard.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ErrCatcher {

    private static final Logger log = LoggerFactory.getLogger(ErrCatcher.class);

    @ExceptionHandler(BizErr.class)
    public R<Void> onBiz(BizErr e) {
        log.warn("业务失败 code={} msg={} detail={}", e.ec().code(), e.ec().msg(), e.detail());
        return R.fail(e.ec());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> onValid(MethodArgumentNotValidException e) {
        String first = e.getBindingResult().getFieldErrors().stream()
                .findFirst().map(f -> f.getField() + f.getDefaultMessage()).orElse("");
        return R.fail(ErrCode.PARAM_MISS, first);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public R<Void> onDup(DuplicateKeyException e) {
        log.warn("唯一键冲突", e);
        return R.fail(ErrCode.REQ_REPEAT);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public R<Void> onNotFound(NoResourceFoundException e) {
        return R.fail(ErrCode.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> onAny(Exception e) {
        log.error("未预料的异常", e);
        return R.fail(ErrCode.SYS_ERR);
    }
}
