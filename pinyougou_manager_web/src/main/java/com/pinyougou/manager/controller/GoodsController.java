package com.pinyougou.manager.controller;

import java.util.List;

import com.alibaba.fastjson.JSON;
//import com.pinyougou.page.service.ItemPageService;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojogroup.Goods;
//import com.pinyougou.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbGoods;
import com.pinyougou.sellergoods.service.GoodsService;

import entity.PageResult;
import entity.Result;

import javax.jms.*;

/**
 * controller
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Reference(timeout = 30000)
    private GoodsService goodsService;

    /**
     * 返回全部列表
     *
     * @return
     */
    @RequestMapping("/findAll")
    public List<TbGoods> findAll() {
        return goodsService.findAll();
    }


    /**
     * 返回全部列表
     *
     * @return
     */
    @RequestMapping("/findPage")
    public PageResult findPage(int page, int rows) {
        return goodsService.findPage(page, rows);
    }

    /**
     * 增加
     *
     * @param goods
     * @return
     */
    @RequestMapping("/add")
    public Result add(@RequestBody Goods goods) {
        try {
            goodsService.add(goods);
            return new Result(true, "增加成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "增加失败");
        }
    }

    /**
     * 修改
     *
     * @param goods
     * @return
     */
    @RequestMapping("/update")
    public Result update(@RequestBody Goods goods) {
        try {
            goodsService.update(goods);
            return new Result(true, "修改成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "修改失败");
        }
    }

    /**
     * 获取实体
     *
     * @param id
     * @return
     */
    @RequestMapping("/findOne")
    public Goods findOne(Long id) {

        return goodsService.findOne(id);
    }

    @Autowired
    //删除索引库的目标队列
    private Destination queueSolrDeleteDestination;
    @Autowired
    //删除生成的静态商品详情页
    private Destination topicPageDeleteDestination;

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @RequestMapping("/delete")
    public Result delete(final Long[] ids) {
        try {
            goodsService.delete(ids);
            //从索引库中删除
            jmsTemplate.send(queueSolrDeleteDestination, new MessageCreator() {
                @Override
                public Message createMessage(Session session) throws JMSException {
                    return session.createObjectMessage(ids);//Long实现了序列化接口,直接用对象的方式返回消息
                }
            });
            jmsTemplate.send(topicPageDeleteDestination, new MessageCreator() {
                @Override
                public Message createMessage(Session session) throws JMSException {

                    return session.createObjectMessage(ids);//删除生成的静态页面
                }
            });
//            itemSearchService.deleteByGoodsIds(Arrays.asList(ids));
            return new Result(true, "删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "删除失败");
        }
    }

    /**
     * 查询+分页
     *
     * @param
     * @param page
     * @param rows
     * @return
     */
    @RequestMapping("/search")
    public PageResult search(@RequestBody TbGoods goods, int page, int rows) {
        return goodsService.findPage(goods, page, rows);
    }

    /**
     * 商品审核
     *
     * @param ids
     * @param status
     * @return
     */
    // @Reference(timeout = 200000)//20s超时设置
//    private ItemSearchService itemSearchService;

//模块间依赖由activeMQ消息中间件来传递,审核通过需要追加索引库,以及生成静态页面
    @Autowired
    private JmsTemplate jmsTemplate;
    @Autowired
    private Destination queueSolrDestination; //队列目的地(终点)对象
    @Autowired
    private Destination topicPageDestination; //这是订阅模式审核通过批量产生静态商品详情html

    @RequestMapping("/updateStatus")
    public Result updateStatus(Long[] ids, String status) {
        try {
            //System.out.println(status);
            goodsService.updateStatus(ids, status);
            if ("1".equals(status)) {
                //*************导入到索引库 审核通过才去维护索引库
                final List<TbItem> itemList = goodsService.findItemListByGoodsIdListAndStatus(ids, status);
                final String jsonString = JSON.toJSONString(itemList);
                //再调用search中的importList(list)方法,完成对索引库的维护
                jmsTemplate.send(queueSolrDestination, new MessageCreator() {
                    @Override
                    public Message createMessage(Session session) throws JMSException {
                        System.out.println("jsonString:" + jsonString);
                        return session.createTextMessage(jsonString);
                    }
                });
                //System.out.println(ids + status + "  itemList  " + itemList);

                // if (itemList.size() > 0) {
                //     itemSearchService.importList(itemList);
                // } else {
                //     System.out.println("索引维护失败");
                // }

                //*************生成商品详情页
                for (final Long goodsId : ids) {
                    // itemPageService.genItemHtml(goodsId);
                    jmsTemplate.send(topicPageDestination, new MessageCreator() {
                        @Override
                        public Message createMessage(Session session) throws JMSException {

                            return session.createTextMessage(goodsId + "");

                        }
                    });
                }

            }
            return new Result(true, "成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "失败");
        }
    }

    // @Reference(timeout = 20000)
    // private ItemPageService itemPageService;

    @RequestMapping("/genHtml")
    public void genHtml(Long goodsId) {

        //itemPageService.genItemHtml(goodsId);

    }
}
