package com.pinyougou.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.List;

@Component
public class ItemSearchListener implements MessageListener {
    @Autowired
    private ItemSearchService searchService;
    @Override
    public void onMessage(Message message) {
        TextMessage textMessage =(TextMessage) message;
        try {
            String items = textMessage.getText();
            System.out.println("监听到提供者..."+items);
            List<TbItem> itemList = JSON.parseArray(items, TbItem.class);
            searchService.importList(itemList);
            System.out.println("成功接收提供者的itemList,完成追加solr索引库操作...");

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
