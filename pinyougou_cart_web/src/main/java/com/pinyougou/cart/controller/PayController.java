package com.pinyougou.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.order.service.OrderService;
import com.pinyougou.pay.service.WeixinPayService;
import com.pinyougou.pojo.TbPayLog;
import entity.Result;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import util.IdWorker;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/pay")
public class PayController {
    @Reference(timeout = 50000)
    private WeixinPayService weixinPayService;

    @Reference(timeout = 50000)
    private OrderService orderService;

    /**
     * 生成二维码
     *
     * @param
     * @param
     */
    @RequestMapping("/createNative")
    public Map createNative() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        TbPayLog payLog = orderService.searchPayLogFromRedis(userId);
        if (payLog!=null) {

            return weixinPayService.createNative(payLog.getOutTradeNo(), payLog.getTotalFee()+"");//订单号,和总价
            //{"out_trade_no":"1108697960850980864","code_url":"weixin://wxpay/bizpayurl?pr=ZEWTwzd","total_fee":"1"}
        } else{
            return new HashMap();
        }
    }

    @RequestMapping("/queryPayStatus")
    public Result queryPayStatus(String out_trade_no) {
        Result result = null;
        int x = 0;
        while (true) {
            Map<String, String> map = weixinPayService.queryPayStatus(out_trade_no);
            if (map == null) {
                result = new Result(false, "支付出错!");
                break;
            }
            if (map.get("trade_state").equals("SUCCESS")) {
                result = new Result(true, "支付成功!");
                orderService.updateOrderStatus(out_trade_no,map.get("transaction_id"));
                break;
            }
            try {
                Thread.sleep(3000);//每3s刷新一次状态请求
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            x++;
            if (x > 100) {//超过5分钟
                result = new Result(false, "支付超时!");
                break;
            }
        }
        return result;
    }
}
