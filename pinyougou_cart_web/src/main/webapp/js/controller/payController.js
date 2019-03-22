app.controller("payController", function ($scope, payService,$location) {

    //生成付款码
    $scope.createNative = function () {
        payService.createNative().success(
            function (response) {
                $scope.money = (response.total_fee / 100).toFixed(2);//1分转为0.01元
                $scope.out_trade_no = response.out_trade_no;//获取订单号

                //生成二维码
                var qr = new QRious({
                    element: document.getElementById('qrious'),
                    size: 250,
                    level: 'H',
                    value: response.code_url
                });
                queryPayStatus();//验证支付状态
            }
        );
    }

    //验证支付状态
    queryPayStatus = function () {
        payService.queryPayStatus($scope.out_trade_no).success(
            function (response) {
                if (response.success) {
                    location.href = "paysuccess.html#?money="+$scope.money;
                } else {
                    if(response.message=="支付超时!"){
                        $scope.createNative();//重新生成二维码
                    }else {
                        location.href = "payfail.html";
                    }
                }
            }
        );
    }

    //显示支付成功金额
    $scope.getMoney=function () {
        return $location.search()['money'];
    }

});