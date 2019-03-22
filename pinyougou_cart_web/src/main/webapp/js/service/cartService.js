//购物车服务层
app.service("cartService",function ($http) {
    //购物车列表
    this.findCartList=function () {
        return $http.get("cart/findCartList.do");
    }

//添加商品到购物车
    this.addGoodsToCartList=function (itemId, num) {
        return $http.get("cart/addGoodsToCartList.do?itemId="+itemId+"&num="+num);
    }


//合计数
    this.sum=function (cartList) {
        var totalValue={totalNum:0, totalMoney:0};//合计实体
        //给构造实体对象赋值,从当前页购物车集合中获取
        for(var i=0;i<cartList.length;i++){
            var cart=cartList[i];
            for(var j=0;j<cart.orderItemList.length;j++){ //获取每一个购物车内的商品明细
                var orderItem=cart.orderItemList[j];//购物车明细
                totalValue.totalNum+=orderItem.num;
                totalValue.totalMoney+=orderItem.totalFee;
            }
        }
        return totalValue;
    }


    //显示收货地址列表

    this.findAddressList=function () {
        $http.get("address/findListByLoginUser.do");
    }

    //获取地址列表
    this.findAddressList=function(){
        return $http.get('address/findListByLoginUser.do');
    }

    //添加订单
    this.submitOrder=function (order) {
        return $http.post("order/add.do",order);
    }



});



