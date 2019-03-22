package com.pinyougou.pay.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.wxpay.sdk.WXPayUtil;
import com.pinyougou.pay.service.WeixinPayService;
import org.springframework.beans.factory.annotation.Value;
import util.HttpClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class WeixinPayServiceImpl implements WeixinPayService {

    @Value("${appid}")
    private String appid;//公众号id
    @Value("${partner}")
    private String partner;//
    @Value("${partnerkey}")
    private String partnerkey;

    /**
     * 生成二维码
     *
     * @param out_trade_no 订单号
     * @param total_fee    金额(分)
     * @return
     */
    @Override
    public Map createNative(String out_trade_no, String total_fee) {
        /*交易类型	trade_type          是
         *通知地址	notify_url	是
         * 终端IP	spbill_create_ip    是
         * 标价金额	total_fee           是
         * 商户订单号  out_trade_no  是
         * 商品描述	body            是
         * 签名	sign       暂未       是
         * 随机字符串	nonce_str	是
         * 商户号	mch_id	是
         * 公众账号ID	appid	是*/
        Map<String, String> param = new HashMap();
        param.put("appid", appid); //公众账号ID
        param.put("mch_id", partner);//商户号
        param.put("nonce_str", WXPayUtil.generateNonceStr());//随机字符串
        param.put("body", "品优购"); //商品描述
        param.put("total_fee", total_fee);//标价金额
        param.put("out_trade_no", out_trade_no);//微信流水订单号
        param.put("spbill_create_ip", "127.0.0.1");//终端IP
        param.put("notify_url", "http://www.baidu.com");//通知地址
        param.put("trade_type", "NATIVE");//交易类型
        try {
            String xmlParam = WXPayUtil.generateSignedXml(param, partnerkey);
            System.out.println("请求的参数:" + xmlParam);
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            httpClient.setHttps(true);
            httpClient.setXmlParam(xmlParam);
            httpClient.post();

            //获得结果
            String result = httpClient.getContent();//向微信支付发起请求,这是请求结果,有二维码,价格,商户id
            System.out.println("**********************************");
            System.out.println("响应回的数据:" + result);
            Map<String, String> resultMap = WXPayUtil.xmlToMap(result);

            //设置返回的 结果Map集合内容
            Map<String, String> map = new HashMap();
            map.put("code_url", resultMap.get("code_url"));//生成的二维码
            map.put("total_fee", total_fee);//设置的支付金额
            map.put("out_trade_no", out_trade_no);//订单号
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    @Override
    public Map queryPayStatus(String out_trade_no) {
        Map<String, String> param = new HashMap<>();
        param.put("appid", appid); //公众账号ID
        param.put("mch_id", partner);//商户号
        param.put("nonce_str", WXPayUtil.generateNonceStr());//随机字符串
        param.put("out_trade_no", out_trade_no);//商户订单号
        String url = "https://api.mch.weixin.qq.com/pay/orderquery";
        try {
            HttpClient httpClient = new HttpClient(url);
            String xmlParam = WXPayUtil.generateSignedXml(param, partnerkey);
            httpClient.setXmlParam(xmlParam);
            httpClient.setHttps(true);
            httpClient.post();

            String result = httpClient.getContent();
            System.out.println("************************************************************");
            System.out.println("响应回的数据:" + result);
            Map<String, String> map = WXPayUtil.xmlToMap(result);
            System.out.println("map:" + map);
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
