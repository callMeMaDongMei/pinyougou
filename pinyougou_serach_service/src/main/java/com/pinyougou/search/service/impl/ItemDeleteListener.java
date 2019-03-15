package com.pinyougou.search.service.impl;

import com.pinyougou.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.util.Arrays;

@Component
public class ItemDeleteListener implements MessageListener {
    @Autowired
    private ItemSearchService searchService;

    @Override
    public void onMessage(Message message) {
        ObjectMessage objectMessage = (ObjectMessage) message;
        try {
            Long[] goodsIds = (Long[]) objectMessage.getObject();
            System.out.println("删除索引库-消费者-接收到生产者数据:"+goodsIds);
            searchService.deleteByGoodsIds(Arrays.asList(goodsIds));//调用方法完成索引删除
            System.out.println("执行索引库删除");
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
