package com.pinyougou.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojogroup.Cart;
import com.pinyuogou.cart.service.CartService;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import util.CookieUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {
    @Reference(timeout = 100000)
    private CartService cartService;
    @Autowired
    private HttpServletRequest request;
    @Autowired
    private HttpServletResponse response;

    @RequestMapping("/findCartList")
    public List<Cart> findCartList() {//从cookie中查找购物车对象
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("用户名:  " + username);
        //从cookie中获取购物车集合
        String cartListString = CookieUtil.getCookieValue(request, "cartList", "UTF-8");

        if (cartListString == null || cartListString.equals("")) {
            cartListString = "[]";
        }
        List<Cart> cartList_cookie = JSON.parseArray(cartListString, Cart.class);

        if (username.equals("anonymousUser")) {//说明该用户未登录
            System.out.println("从cookie中获取购物车集合----");
                      return cartList_cookie;

        } else {//已经登录从redis中获取,进行合并购物车操作
            System.out.println("从redis中获取购物车集合---");
            List<Cart> cartList_redis = cartService.findCartListFromRedis(username);
            //合并缓存与cookie
            if(cartList_cookie.size()>0){//本地有cookie的购物车列表才执行合并
                cartList_redis = cartService.mergeCartList(cartList_redis, cartList_cookie);
                //把合并后的购物车往redis中存
                cartService.saveCartListToRedis(username,cartList_redis);
                //并且清除cookie
                util.CookieUtil.deleteCookie(request,response,"cartList");
            }
            return cartList_redis;
        }
    }


    @RequestMapping("/addGoodsToCartList")
   // @CrossOrigin(origins = "http://localhost:9105",allowCredentials="true") spring 4.2 才能用
    public Result addGoodsToCartList(Long itemId, Integer num) {
        response.setHeader("Access-Control-Allow-Origin","http://localhost:9105");//允许跨域请求
        response.setHeader("Access-Control-Allow-Credentials","true");//允许跨域请求携带cookie

        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            //（1）取出购物车
            List<Cart> cartList = findCartList();
            //（2）向购物车添加商品
            cartList = cartService.addGoodsToCartList(cartList, itemId, num);

            for (Cart cart : cartList) {
                List<TbOrderItem> orderItemList = cart.getOrderItemList();
                for (TbOrderItem orderItem : orderItemList) {
                    System.out.println("总价getTotalFee:"+orderItem.getTotalFee());
                }
            }

            if (name.equals("anonymousUser")) {//说明该用户未登录
                //（3）将购物车存入 cookie
                String cartListString = JSON.toJSONString(cartList);
                CookieUtil.setCookie(request, response, "cartList", cartListString, 3600 * 24, "UTF-8");
                System.out.println("向cookie中存储...");
                return new Result(true, "添加购物车成功");
            } else {//已经登录,把cookie中的购物车集合存入缓存
                cartService.saveCartListToRedis(name, cartList);
                return new Result(true,"添加购物车成功");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "添加购物车失败");
        }
    }
}
