//购物车控制层
app.controller("cartController", function ($scope, cartService) {
    //查询购物车列表
    $scope.findCartList = function () {
        cartService.findCartList().success(
            function (response) {
                $scope.cartList = response;
                $scope.totalValue = cartService.sum($scope.cartList);//求合计数
            }
        );
    }

    //加减购物车商品数量
    $scope.addGoodsToCartList = function (itemId, num) {
        cartService.addGoodsToCartList(itemId, num).success(
            function (response) {
                if (response.success) {
                    //alert(response.message);
                    $scope.findCartList();//重新刷新列表
                } else {
                    alert(response.message);
                }
            }
        );
    }


    //获取收货地址列表
    $scope.findAddressList = function () {
        cartService.findAddressList().success(
            function (response) {
                $scope.addressList = response;
                for (var i = 0; i < $scope.addressList.length; i++) {
                    if ($scope.addressList[i].isDefault=='1') {
                        $scope.address=$scope.addressList[i];
                        break;
                    }
                }
            }
        );
    }

    //存储选中的收货地址
    $scope.selectAddress = function (address) {
        $scope.address = address;
    }

    //判断是否被选中
    $scope.isSelectedAddress = function (address) {
        if ($scope.address == address) {
            return true;
        } else {
            return false;
        }
    }

    //选择支付方式
    $scope.order={paymentType:'1'};//初始订单对象支付方式选择为微信支付

    $scope.selectPayType=function (type) {
        $scope.order.paymentType=type;
    }


    //添加订单
    $scope.submitOrder=function () {
        $scope.order.receiverAreaName=$scope.address.address;//地址
        $scope.order.receiverMobile=$scope.address.mobile;//手机
        $scope.order.receiver=$scope.address.contact;//联系人
        cartService.submitOrder($scope.order).success(
            function (response) {
                if(response.success){
                    //判断是微信还是货到付款
                    if($scope.order.paymentType=='1'){ //是微信支付,跳转到微信支付
                        location.href="pay.html";
                    }else {
                        location.href="paysuccess.html";
                    }
                }else {
                    alert(response.message); //也可以跳转到提示页面
                }
            }
        );
    }


});


