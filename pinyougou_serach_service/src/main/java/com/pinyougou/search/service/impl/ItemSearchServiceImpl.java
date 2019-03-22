package com.pinyougou.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemSearchServiceImpl implements ItemSearchService {
    @Autowired
    private SolrTemplate solrTemplate;

    /**
     * 搜索功能
     *
     * @param searchMap
     * @return
     */
    @Override
    public Map<String, Object> search(Map searchMap) {
        Map map = new HashMap();
        //关键词空格处理

        String keywords = (String) searchMap.get("keywords");
        searchMap.put("keywords",keywords.replace(" ",""));//空串替换空格

        //1_查询列表(含高亮字符)
        map.putAll(searchList(searchMap));

        //2_查询分页列表
        List<String> categoryList = searchCategoryList(searchMap);
        map.put("categoryList", categoryList);

        //3.0根据选择的分类或品牌进行筛选查询
        String category = (String) searchMap.get("category");
        if (!category.equals("")) {
            map.putAll(searchBrandAndSpecList(category));
        } else {
            if (categoryList.size() > 0) {
                map.putAll(searchBrandAndSpecList(categoryList.get(0)));
            }
        }
        return map;
    }

    //查询列表的私有方法(根据用户搜索的关键词,后端处理返回页面带有高亮标签的字符列表map集合)
    private Map searchList(Map searchMap) {
        Map map = new HashMap();
        /**
         *h.getHighlights() 获取每条高亮记录的所有高亮域
         *在new HighlightOptions().addField(“item_title”)是添加的高亮域
         *h.getHighlights().get(0).getSnipplets()获取第一个高亮域的内容
         *h.getHighlights().get(0).getSnipplets().get(0) 一个高亮域中可能存在多值
         *取决于solr中的配置域的是否配置了multiValued是否为true
         ---------------------
         */
        HighlightQuery query = new SimpleHighlightQuery();//步骤2
        //搜索内容与标题匹配高亮显示
        HighlightOptions highlightOptions = new HighlightOptions().addField("item_title");//步骤5
        highlightOptions.setSimplePrefix("<em style='color:red'>");//高亮文字前缀 //步骤6
        highlightOptions.setSimplePostfix("</em>");//设置后缀      //步骤7
        query.setHighlightOptions(highlightOptions);//步骤4  //设置高亮

        //  1.1关键字查询
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));//步骤3
        query.addCriteria(criteria);//步骤3

        //  1.2根据分类信息筛选查询
        if (!"".equals(searchMap.get("category"))) {//分类信息不为空时,进行筛选查询
            Criteria filterCriteria = new Criteria("item_category").is(searchMap.get("category"));
            FilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
            query.addFilterQuery(filterQuery);
        }

        //  1.3根据品牌信息筛选查询
        if (!"".equals(searchMap.get("brand"))) {//品牌信息不为空时,进行筛选查询
            FilterQuery filterQuery = new SimpleFilterQuery();
            Criteria filterCriteria = new Criteria("item_brand").is(searchMap.get("brand"));
            filterQuery.addCriteria(filterCriteria);
            query.addFilterQuery(filterQuery);
        }

        //  1.4根据规格信息筛选查询
        if (searchMap.get("spec") != null) {//规格信息不为空时,进行筛选查询
            //System.out.println(searchMap.get("spec"));
            Map<String, String> specMap = (Map<String, String>) searchMap.get("spec");
            for (String key : specMap.keySet()) {
                //循环添加过滤查询条件
                FilterQuery filterQuery = new SimpleFilterQuery();
                Criteria filterCriteria = new Criteria("item_spec_" + key).is(specMap.get(key));
                filterQuery.addCriteria(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
        }

        // 1.5根据价格筛选
        if (!"".equals(searchMap.get("price"))) {//价格信息不为空时,进行筛选查询
            /*
              String price = (String) searchMap.get("price");
              String[] split = price.split("_");
              */

            String[] price = ((String) searchMap.get("price")).split("-");
            if (!price[0].equals("0")) {//最低价格不为0,查询条件为大于等于
                Criteria filterCriteria = new Criteria("item_price").greaterThanEqual(price[0]);
                FilterQuery filterQuery = new SimpleFilterQuery();
                filterQuery.addCriteria(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
            if (!price[1].equals("*")) {//3000-*的情况
                FilterQuery filterQuery = new SimpleFilterQuery();
                Criteria filterCriteria = new Criteria("item_price").lessThanEqual(price[1]);
                filterQuery.addCriteria(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
        }

        // 1.6分页查询
        Integer pageNo = (Integer) searchMap.get("pageNo");// 拿到pageNo
        if (pageNo == null) {
            pageNo = 1;   //没有当前页默认第一页
        }
        Integer pageSize = (Integer) searchMap.get("pageSize");// 拿到每页记录数pageSize
        if (pageSize == null) {
            pageSize = 20;//每页显示条数
        }
        query.setOffset((pageNo - 1) * pageSize);// 从第几条记录查询
        query.setRows(pageSize);//设置每页条数

        // 1.7价格排序
        String sortValue = (String) searchMap.get("sort");//升降序状态
        String sortField = (String) searchMap.get("sortField");//获取排序字段
        if(!"".equals(sortValue) && searchMap!=null){
            if(sortValue.equals("ASC")){
                Sort sort=new Sort(Sort.Direction.ASC,"item_"+sortField);//升序价格从低到高
                query.addSort(sort);
            }
            if(sortValue.equals("DESC")){
                Sort sort=new Sort(Sort.Direction.DESC,"item_"+sortField);//升序价格从低到高
                query.addSort(sort);
            }

        }


        //**********************   获取高亮结果集(之前写过滤查询)   ****************************
        //得到高亮页对象
        HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);//步骤1

        //得到高亮入口集合
        List<HighlightEntry<TbItem>> entryList = page.getHighlighted();
        for (HighlightEntry<TbItem> entry : entryList) {
            //得到高亮列表集合
            List<HighlightEntry.Highlight> highlightList = entry.getHighlights();
           /* for (HighlightEntry.Highlight h : highlightList) {
                System.out.println(h.getSnipplets());//打印验证highlightList是高亮列表集合
            }*/
            // highlightList.get(0).getSnipplets().get(0);//得到用户输入关键词的高亮文本(与title字段匹配高亮)

            //判断高亮列表是否空值(多值),以及高亮域中文本空值状态
            if (highlightList.size() > 0 && highlightList.get(0).getSnipplets().size() > 0) {
                TbItem item = entry.getEntity();
                item.setTitle(highlightList.get(0).getSnipplets().get(0));//设置高亮的文本内容
            }
        }
        map.put("rows", page.getContent());//返回分页内容
        map.put("totalPages",page.getTotalPages());//返回总页数
        map.put("total",page.getTotalElements());//返回总记录数
        return map;
    }

    /**
     * 分组查询(查询商品分类列表)
     *
     * @param searchMap
     * @return
     */
    private List<String> searchCategoryList(Map searchMap) {
        List<String> list = new ArrayList();

        Query query = new SimpleQuery("*:*");
        //复制域item_keywords的作用在于将某一个 Field 中的数据复制到另一个域中
        //根据关键字查询
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));//根据用户搜索的关键词作为条件
        query.addCriteria(criteria);

        //设置分组条件
        GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category");//group by 根据什么分组查询
        query.setGroupOptions(groupOptions);

        //得到分组页(可能有多个分组页)
        GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);

        //得到分组结果对象
        GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
        Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();

        //获取分页入口集合
        List<GroupEntry<TbItem>> entryList = groupEntries.getContent();
        for (GroupEntry<TbItem> entry : entryList) {
            //String value = entry.getGroupValue();//获取到根据category分组的结果
            list.add(entry.getGroupValue());
        }
        return list;
    }

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 从缓存中获取规格,品牌列表,
     *
     * @param category
     * @return
     */
    private Map searchBrandAndSpecList(String category) {
        Map map = new HashMap();

        Long templatetId = (Long) redisTemplate.boundHashOps("itemCat").get(category);//根据分类名称得到模板id

        if (templatetId != null) {

            List brandList = (List) redisTemplate.boundHashOps("brandList").get(templatetId);//根据模板id得到品牌列表

            List<Map> specList = (List<Map>) redisTemplate.boundHashOps("specList").get(templatetId);//根据模板id得到详细规格列表

            map.put("brandList", brandList);
            map.put("specList", specList);
        }
        return map;
    }

    //导入列表,准备维护索引库
    @Override
    public void importList(List list) {
        solrTemplate.saveBeans(list);
        solrTemplate.commit();
    }

    //删除商品数据,维护索引库

    @Override
    public void deleteByGoodsIds(List goodsIdList) {
        Query query=new SimpleQuery("*:*");
        Criteria criteria=new Criteria("item_goodsid").in(goodsIdList);
        query.addCriteria(criteria);
        solrTemplate.delete(query);
        solrTemplate.commit();
    }
}
