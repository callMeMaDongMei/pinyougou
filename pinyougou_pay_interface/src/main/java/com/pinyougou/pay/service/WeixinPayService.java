package com.pinyougou.pay.service;

import java.util.Map;

public interface WeixinPayService {
    /**
     * 接入微信支付生成二维码
     * @param out_trade_no 订单号
     * @param total_fee     金额(分)
     * @return
     */
    public Map createNative(String out_trade_no,String total_fee);//total_fee 单位为分

    /**
     * 查询用户支付状态
     * @param out_trade_no
     * @return
     */
    public Map queryPayStatus(String out_trade_no);



}
