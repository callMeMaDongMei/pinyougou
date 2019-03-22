package com.pinyougou.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojogroup.Cart;
import com.pinyuogou.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private TbItemMapper itemMapper;

    /**
     * 添加购物车
     * @param cartList
     * @param itemId
     * @param num
     * @return
     */
    @Override
    public List<Cart> addGoodsToCartList(List<Cart> cartList, Long itemId, Integer num) {
        //1.根据商品 SKU ID 查询 SKU 商品信息
        TbItem item = itemMapper.selectByPrimaryKey(itemId);
        if (item == null) {
            throw new RuntimeException("不存在商品!");
        }
        if (!item.getStatus().equals("1")) {
            throw new RuntimeException("状态不合法!!");
        }

        //2.获取商家 ID
        String sellerId = item.getSellerId();

        //3.根据商家 ID 判断购物车列表中是否存在该商家的购物车
        Cart cart = searchCartBySellerId(cartList, sellerId);

        if (cart == null) {//4.如果购物车列表中不存在该商家的购物车
            //4.1 新建购物车对象
            cart = new Cart();
            //4.2 将新建的购物车对象添加到购物车列表
            cart.setSellerId(sellerId);
            cart.setSellerName(item.getSeller());
            System.out.println("cart.SellerName:  " + cart.getSellerName());
            List<TbOrderItem> orderItemList = new ArrayList<>(); //购物车明细列表(商家下面的商品详情内容)
            TbOrderItem orderItem = createOrderItem(item, num);
            orderItemList.add(orderItem);
            cart.setOrderItemList(orderItemList);
            cartList.add(cart);//把新添加进购物车对象添加进购物车集合中

        } else { //5.如果购物车列表中存在该商家的购物车

            // 查询购物车明细列表中是否存在该商品
            TbOrderItem orderItem = searchOrderItemByItemId(cart.getOrderItemList(), itemId);

            if (orderItem == null) {//5.1. 如果没有，新增购物车明细
                orderItem = createOrderItem(item, num);
                cart.getOrderItemList().add(orderItem);

            } else {         //5.2. 如果有，在原购物车明细上添加数量，更改金额

                orderItem.setNum(orderItem.getNum() + num);//目前数量等于:原列表数量加上页面传回的数量和orderItem.getNum()+num
                orderItem.setTotalFee(new BigDecimal(orderItem.getNum() * orderItem.getPrice().doubleValue()));//目前总价格等于目前数量乘以价格

                System.out.println(orderItem.getNum() * orderItem.getPrice().doubleValue());

                if (orderItem.getNum() <= 0) { //如果添加减少购物车商品数量为0了,意味着需要移除当前table显示即从集合中移除
                    cart.getOrderItemList().remove(orderItem);//从购物车中移除
                }
                if (cart.getOrderItemList().size() == 0) { //意味着购物车商家下不存在商品,所以直接移除该商家的整个购物车显示
                    cartList.remove(cart);
                }
            }
        }
        return cartList;
    }


    /**
     * 根据商家id在购物车列表中查询购物车对象
     * 就是判断当前添加购物车操作过程中购物车列表里是否已经存在当前商家商品,有就返回当前的购物车对象
     *
     * @param cartList
     * @param sellerId
     * @return
     */
    private Cart searchCartBySellerId(List<Cart> cartList, String sellerId) {
        for (Cart cart : cartList) {
            if (cart.getSellerId().equals(sellerId)) {
                return cart;
            }
        }
        return null;
    }

    /**
     * 新增商品至购物车,判断该商品是否已经存在,存在则只需追加数量+1
     *
     * @param orderItems
     * @param itemId
     * @return
     */
    private TbOrderItem searchOrderItemByItemId(List<TbOrderItem> orderItems, Long itemId) {
        for (TbOrderItem orderItem : orderItems) {
            if (orderItem.getItemId().longValue() == itemId) {//说明添加的商品已经在购物车中存在,只需要追加数量
                return orderItem;
            }
        }
        return null;
    }

    /**
     * 创建新的购物车明细对象
     *
     * @param item
     * @param num
     * @return
     */
    private TbOrderItem createOrderItem(TbItem item, Integer num) {
        TbOrderItem orderItem = new TbOrderItem();
        orderItem.setGoodsId(item.getGoodsId());
        orderItem.setItemId(item.getId());
        orderItem.setNum(num);
        orderItem.setPicPath(item.getImage());
        orderItem.setPrice(item.getPrice());
        orderItem.setSellerId(item.getSellerId());
        orderItem.setTitle(item.getTitle());
        orderItem.setTotalFee(new BigDecimal(item.getPrice().doubleValue() * num));
        return orderItem;
    }

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 从缓存中获取购物车集合
     *
     * @param username
     * @return
     */
    @Override
    public List<Cart> findCartListFromRedis(String username) {
        System.out.println("从redis中获取购物车..."+username);
        List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(username);//根据登录用户确定从缓存中获取购物车集合(是已经登录的用户才会有缓存购物车集合)
        if (cartList == null) {
            cartList = new ArrayList<>();
        }
        return cartList;
    }

    /**
     * 把已经登录用户的购物车存入redis
     *
     * @param username
     * @param cartList
     */
    @Override
    public void saveCartListToRedis(String username, List<Cart> cartList) {
        System.out.println("把购物车存入redis..."+username);
        List<TbOrderItem>items=new ArrayList<>();
        for (Cart cart : cartList) {
            items=cart.getOrderItemList();
        }
        for (TbOrderItem item : items) {
            System.out.println("TbOrderItem,TotalFee:"+item.getTotalFee());
        }
        redisTemplate.boundHashOps("cartList").put(username,cartList);
    }

    /**
     * 合并cookie和redis中的购物车集合
     * @param cartList1
     * @param cartList2
     * @return
     */
    @Override
    public List<Cart> mergeCartList(List<Cart> cartList1, List<Cart> cartList2) {

        for (Cart cart : cartList2) {
            for (TbOrderItem orderItem : cart.getOrderItemList()) {
                cartList1=addGoodsToCartList(cartList1,orderItem.getItemId(),orderItem.getNum());
            }
        }
        return cartList1;
    }
}
