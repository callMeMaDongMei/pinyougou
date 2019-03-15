package com.pinyougou.page.service.impl;

import com.pinyougou.page.service.ItemPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

@Component
public class PageDeleteListener implements MessageListener {
    @Autowired
    private ItemPageService itemPageService;
    @Override
    public void onMessage(Message message) {
        ObjectMessage objectMessage= (ObjectMessage) message;
        try {
            System.out.println("接收提供者goodsIds,开始删除静态页面...");
            Long [] goodsIds = (Long[]) objectMessage.getObject();
            itemPageService.deleteItemHtml(goodsIds);
            System.out.println("删除完成...");
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
