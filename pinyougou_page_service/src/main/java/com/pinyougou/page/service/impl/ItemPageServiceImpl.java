package com.pinyougou.page.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.mapper.TbGoodsDescMapper;
import com.pinyougou.mapper.TbGoodsMapper;
import com.pinyougou.mapper.TbItemCatMapper;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.page.service.ItemPageService;
import com.pinyougou.pojo.TbGoods;
import com.pinyougou.pojo.TbGoodsDesc;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbItemExample;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemPageServiceImpl implements ItemPageService {
    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;
    @Value("${pagedir}")
    private String pagedir;
    @Autowired
    private TbGoodsMapper goodsMapper;
    @Autowired
    private TbGoodsDescMapper goodsDescMapper;
    @Autowired
    private TbItemCatMapper itemCatMapper;
    @Autowired
    private TbItemMapper itemMapper;

    /**
     * freeMarker,模板生成静态页面(测试)
     * @param goodsId
     * @return
     */
    @Override
    public boolean genItemHtml(Long goodsId) {
        Configuration configuration = freeMarkerConfigurer.getConfiguration();
        try {
            Template template = configuration.getTemplate("item.ftl");
            //创建数据模型
            Map dataModel = new HashMap<>();

            //1_读取goods表
            TbGoods goods = goodsMapper.selectByPrimaryKey(goodsId); //获取商品对象
            //2_读取goodsDesc表
            TbGoodsDesc goodsDesc = goodsDescMapper.selectByPrimaryKey(goodsId); //获取商品扩展表对象

            dataModel.put("goods", goods);
            dataModel.put("goodsDesc", goodsDesc);

            //3_读取itemCat表,获取itemCat商品分类名称
            String itemCat1 = itemCatMapper.selectByPrimaryKey(goods.getCategory1Id()).getName(); //获取itemCat商品分类名称
            String itemCat2 = itemCatMapper.selectByPrimaryKey(goods.getCategory2Id()).getName(); //获取itemCat商品分类名称
            String itemCat3 = itemCatMapper.selectByPrimaryKey(goods.getCategory3Id()).getName(); //获取itemCat商品分类名称

            dataModel.put("itemCat1",itemCat1);
            dataModel.put("itemCat2",itemCat2);
            dataModel.put("itemCat3",itemCat3);

            //4_读取SKU表及item表内容
            TbItemExample example=new TbItemExample();
            TbItemExample.Criteria criteria = example.createCriteria();
            criteria.andGoodsIdEqualTo(goodsId);
            criteria.andStatusEqualTo("1"); //状态有效
            example.setOrderByClause("is_default DESC"); //根据是否默认排序,目的是返回的结果第一条是默认选中状态
            List<TbItem> itemList = itemMapper.selectByExample(example);
            dataModel.put("itemList",itemList);//把sku信息存入模板中



//            Writer out = new FileWriter(new FileOutputStream(pagedir + goodsId + ".html"),"utf-8");
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pagedir + goodsId + ".html"),"UTF-8"));
            template.process(dataModel, out);// 输出生成的静态页面

            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }
}
