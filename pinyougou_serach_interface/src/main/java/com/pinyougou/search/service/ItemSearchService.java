package com.pinyougou.search.service;

import com.pinyougou.pojo.TbItem;

import java.util.List;
import java.util.Map;

public interface ItemSearchService {
    /**
     * 搜索方法
     * @param searchMap
     * @return
     */
    public Map<String,Object> search(Map searchMap);

    //导入列表,准备维护索引库
    public void  importList(List list);

    //删除商品数据,维护索引库
    public void deleteByGoodsIds(List goodsIdList);
}
