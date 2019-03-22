package com.pinyougou.order.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.pinyougou.mapper.TbOrderItemMapper;
import com.pinyougou.mapper.TbPayLogMapper;
import com.pinyougou.order.service.OrderService;
import com.pinyougou.pojo.*;
import com.pinyougou.pojogroup.Cart;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.TbOrderMapper;
import com.pinyougou.pojo.TbOrderExample.Criteria;

import entity.PageResult;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import util.IdWorker;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    @Autowired
    private TbOrderMapper orderMapper;

    @Autowired
    private TbPayLogMapper payLogMapper;


    /**
     * 查询全部
     */
    @Override
    public List<TbOrder> findAll() {
        return orderMapper.selectByExample(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbOrder> page = (Page<TbOrder>) orderMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 增加
     */
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private TbOrderItemMapper orderItemMapper;

    @Override
    public void add(TbOrder order) {
        /**
         * 1.补全表数据
         * 2.合计数
         * 3.清空redis的购物车内容
         */
        //一个商家就会对应产生一份订单
        List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(order.getUserId());//从缓存中获取购物车列表
        double total_money = 0;//支付日志的订单总价初始化
        List<String> orderIdList = new ArrayList<>();//初始化支付日志的订单id集合
        for (Cart cart : cartList) {
            TbOrder tbOrder = new TbOrder();
            long orderId = idWorker.nextId();
            //************需要后台填充的属性数据
            tbOrder.setOrderId(orderId);
            tbOrder.setStatus("1"); //状态：1、未付款，2、已付款，3、未发货，4、已发货，5、交易成功，6、交易关闭,7、待评价
            tbOrder.setCreateTime(new Date());//订单创建日期
            tbOrder.setUpdateTime(new Date());//订单更新日期
            tbOrder.setSellerId(cart.getSellerId());//商家 ID
            //************从浏览器携带的数据
            tbOrder.setReceiverAreaName(order.getReceiverAreaName());//地址
            tbOrder.setReceiverMobile(order.getReceiverMobile());//手机号
            tbOrder.setReceiver(order.getReceiver());//收货人
            tbOrder.setSourceType(order.getSourceType());//订单来源
            tbOrder.setUserId(order.getUserId());//用户名
            tbOrder.setPaymentType(order.getPaymentType());//支付类型

            double money = 0;//合计数
            //循环购物车明细,追加订单明细表数据
            for (TbOrderItem orderItem : cart.getOrderItemList()) {
                //补全订单明细表数据
                orderItem.setId(idWorker.nextId());
                orderItem.setOrderId(orderId);//与订单表id保持一致
                orderItem.setSellerId(cart.getSellerId());
                //追加订单明细表数据
                orderItemMapper.insert(orderItem);
                money += orderItem.getTotalFee().doubleValue();//把每个购物车中的商品价格综合累加
            }
            tbOrder.setPayment(new BigDecimal(money));//设置总价格
            orderMapper.insert(tbOrder);//追加订单表数据

            //支付日志表赋值
            total_money += money;//累加订单金额到支付日志表中
            orderIdList.add(orderId + "");//把订单id添加到集合中
        }

        //存储支付日志
        if ("1".equals(order.getPaymentType())) {  //如果选择的是微信付款
            //订单生成的同时完成支付日志的初始化
            TbPayLog payLog = new TbPayLog();

            payLog.setCreateTime(new Date());//创建时间
            payLog.setOutTradeNo(idWorker.nextId() + "");//支付订单号
            payLog.setUserId(order.getUserId());//登录用户名
            payLog.setPayType("1");//支付类型支付类型：1:微信 2:支付宝 3:网银
            payLog.setTradeState("0");//支付状态,初始未支付
            payLog.setTotalFee((long) (total_money * 100));//总金额(分)
            String ids = orderIdList.toString().replace("[", "").replace("]", "").replace("", "");
            payLog.setOrderList(ids);//订单号列表，逗号分隔
            payLogMapper.insert(payLog);
            //把支付日志存入缓存
            redisTemplate.boundHashOps("payLog").put(order.getUserId(), payLog);

        }

        //清空缓存
        redisTemplate.boundHashOps("cartList").delete(order.getUserId());

    }


    /**
     * 修改
     */
    @Override
    public void update(TbOrder order) {
        orderMapper.updateByPrimaryKey(order);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbOrder findOne(Long id) {
        return orderMapper.selectByPrimaryKey(id);
    }

    /**
     * 批量删除
     */
    @Override
    public void delete(Long[] ids) {
        for (Long id : ids) {
            orderMapper.deleteByPrimaryKey(id);
        }
    }


    @Override
    public PageResult findPage(TbOrder order, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbOrderExample example = new TbOrderExample();
        Criteria criteria = example.createCriteria();

        if (order != null) {
            if (order.getPaymentType() != null && order.getPaymentType().length() > 0) {
                criteria.andPaymentTypeLike("%" + order.getPaymentType() + "%");
            }
            if (order.getPostFee() != null && order.getPostFee().length() > 0) {
                criteria.andPostFeeLike("%" + order.getPostFee() + "%");
            }
            if (order.getStatus() != null && order.getStatus().length() > 0) {
                criteria.andStatusLike("%" + order.getStatus() + "%");
            }
            if (order.getShippingName() != null && order.getShippingName().length() > 0) {
                criteria.andShippingNameLike("%" + order.getShippingName() + "%");
            }
            if (order.getShippingCode() != null && order.getShippingCode().length() > 0) {
                criteria.andShippingCodeLike("%" + order.getShippingCode() + "%");
            }
            if (order.getUserId() != null && order.getUserId().length() > 0) {
                criteria.andUserIdLike("%" + order.getUserId() + "%");
            }
            if (order.getBuyerMessage() != null && order.getBuyerMessage().length() > 0) {
                criteria.andBuyerMessageLike("%" + order.getBuyerMessage() + "%");
            }
            if (order.getBuyerNick() != null && order.getBuyerNick().length() > 0) {
                criteria.andBuyerNickLike("%" + order.getBuyerNick() + "%");
            }
            if (order.getBuyerRate() != null && order.getBuyerRate().length() > 0) {
                criteria.andBuyerRateLike("%" + order.getBuyerRate() + "%");
            }
            if (order.getReceiverAreaName() != null && order.getReceiverAreaName().length() > 0) {
                criteria.andReceiverAreaNameLike("%" + order.getReceiverAreaName() + "%");
            }
            if (order.getReceiverMobile() != null && order.getReceiverMobile().length() > 0) {
                criteria.andReceiverMobileLike("%" + order.getReceiverMobile() + "%");
            }
            if (order.getReceiverZipCode() != null && order.getReceiverZipCode().length() > 0) {
                criteria.andReceiverZipCodeLike("%" + order.getReceiverZipCode() + "%");
            }
            if (order.getReceiver() != null && order.getReceiver().length() > 0) {
                criteria.andReceiverLike("%" + order.getReceiver() + "%");
            }
            if (order.getInvoiceType() != null && order.getInvoiceType().length() > 0) {
                criteria.andInvoiceTypeLike("%" + order.getInvoiceType() + "%");
            }
            if (order.getSourceType() != null && order.getSourceType().length() > 0) {
                criteria.andSourceTypeLike("%" + order.getSourceType() + "%");
            }
            if (order.getSellerId() != null && order.getSellerId().length() > 0) {
                criteria.andSellerIdLike("%" + order.getSellerId() + "%");
            }

        }

        Page<TbOrder> page = (Page<TbOrder>) orderMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }


    /**
     * 从登陆用户的缓存中查找支付日志信息
     *
     * @param userId
     * @return
     */
    @Override
    public TbPayLog searchPayLogFromRedis(String userId) {
        return (TbPayLog) redisTemplate.boundHashOps("payLog").get(userId);
    }

    /**
     * 支付成功修改状态码(订单表和支付日志表)
     *
     * @param out_trade_no
     * @param transaction_id
     */
    @Override
    public void updateOrderStatus(String out_trade_no, String transaction_id) {

        //1.支付成功后更新支付日志表
        TbPayLog payLog = payLogMapper.selectByPrimaryKey(out_trade_no);//根据主键id查找
        payLog.setPayTime(new Date());//支付成功时间
        payLog.setTradeState("1");//改变支付状态,已经成功支付 "0"未支付,"1"已支付
        payLog.setTransactionId(transaction_id);//微信的交易流水号
        payLogMapper.updateByPrimaryKey(payLog);//更新支付日志表

        //2.修改订单状态
        String orderList = payLog.getOrderList();
        String[] orderIds = orderList.split(",");//根据逗号分割
        for (String orderId : orderIds) {
            TbOrder order = orderMapper.selectByPrimaryKey(Long.valueOf(orderId));
            order.setStatus("2");//设置为已付款状态
            orderMapper.updateByPrimaryKey(order);//更新表
        }

        //3.清空paylog缓存
        redisTemplate.boundHashOps("payLog").delete(payLog.getUserId());


    }
}
