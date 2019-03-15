package com.pinyougou.page.service;

public interface ItemPageService {
    /**
     * 产生静态商品详情页
     * @param goodsId
     * @return
     */
    public boolean genItemHtml(Long goodsId);

    /**
     * 删除静态商品详情页
     * @param goodsIds
     * @return
     */
    public boolean deleteItemHtml(Long[]goodsIds );

}
