package com.pinyuogou.cart.service;

import com.pinyougou.pojogroup.Cart;

import java.util.List;

 public interface  CartService {
     /**
      *  购物车服务接口
      * @return
      */
     //参数页面购物车集合.SKU Id,商品数量
     List<Cart> addGoodsToCartList(List<Cart>cartList,Long itemId,Integer num);

     /**
      * 从缓存中获取购物车集合
      * @param username
      * @return
      */
     public List<Cart> findCartListFromRedis(String username);

     /**
      * 将购物车集合存入缓存
      * @param username
      * @param cartList
      */
     public void saveCartListToRedis(String username,List<Cart>cartList);

     /**
      * 合并购物车
      * @param cartList1
      * @param cartList2
      * @return
      */
     public List<Cart> mergeCartList(List<Cart>cartList1,List<Cart>cartList2);
}
