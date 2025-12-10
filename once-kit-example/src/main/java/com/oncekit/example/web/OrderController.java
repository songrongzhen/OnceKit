package com.oncekit.example.web;

import com.oncekit.core.annotation.Idempotent;
import com.oncekit.example.dto.Candidate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    @PostMapping("/order")
    @Idempotent(key = "'order:' + #userId + ':' + #goodsId", expire = 300)
    public String createOrder(@RequestParam Long userId, @RequestParam Long goodsId) {
        return "订单创建成功: " + userId + "-" + goodsId;
    }

    /**
     * 用身份证号做幂等
     * @param candidate
     * @return
     */
    @PostMapping("/enroll1")
    @Idempotent(key = "'enroll:idcard:' + #candidate.idCard", expire = 300)
    public String enroll1(@RequestBody Candidate candidate) {
        return "报名成功: " + candidate.getName();
    }

    /**
     * 组合唯一键（身份证+手机号）
     * @param candidate
     * @return
     */
    @PostMapping("/enroll2")
    @Idempotent(key = "'enroll:' + #candidate.idCard + ':' + #candidate.phone", expire = 300)
    public String enroll2(@RequestBody Candidate candidate) {
        return "报名成功: " + candidate.getName();
    }


}