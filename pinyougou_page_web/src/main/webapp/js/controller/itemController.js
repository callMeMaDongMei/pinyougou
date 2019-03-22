app.controller("itemController", function ($scope, $http) {

    //购物车数量加减
    //$scope.num=1;,页面初始化设置值为1
    $scope.addNum = function (x) {
        $scope.num = parseInt($scope.num);
        $scope.num += x;
        if ($scope.num < 1) {
            $scope.num = 1;
        }
    }

    //页面规格选中特效处理

    //储存用户点击选中的Spec
    $scope.specificationItems = {};//储存用户点击选中的规格信息{"网络":"4G","内存","128G"}
    //用户选择规格
    $scope.selectSpecification = function (key, value) {
        $scope.specificationItems[key] = value;
        searchSku(); //查询SKU
    }

    $scope.isSelected = function (key, value) {
        if ($scope.specificationItems[key] == value) {
            return true;
        } else {
            return false;
        }
    }


    //页面加载显示默认的商品规格
    $scope.sku = {}; //当前选择的sku,页面显示SKU信息

    $scope.loadSku = function () {
        $scope.sku = skuList[0];
        //深克隆,加载默认选中特效
        $scope.specificationItems = JSON.parse(JSON.stringify($scope.sku.spec));//默认选中
    }


    //匹配两个对象是否相等,实现改变规格显示	不同spec数据
    matchObject = function (map1, map2) {  //这两个map JSON对象一个表示的是用户选择的规格信息,一个是数据库中的skuList
        //对json对象循环   'spec':{"网络":"联通4G","机身内存":"128G"} k就是'网络' map就是spec JSON对象

        for (var k in map1) {
            if (map1[k] != map2[k]) {
                return false;  //说明不相等
            }
        }
        for (var k in map2) {
            if (map2[k] != map1[k]) {
                return false;  //说明不相等
            }
        }

        return true; //没进不相等的判断说明最后就是相等的
    }

    //进行比较
    searchSku = function () {

        for (var i = 0; i < skuList.length; i++) {
            //用户选择的$scope,specificationItems(通过单击绑定存储的),和全局skuList[i].spec比较
            if (matchObject(skuList[i].spec, $scope.specificationItems)) {
                $scope.sku = skuList[i];
                return;
            }
        }
        $scope.sku = {id: 0, title: '--------', price: 0};//如果没有匹配的
    }

    /* //点击加入购物车弹出商品id

     $scope.addToCart = function () {

         alert("您选择的SKUID为:" + $scope.sku.id);
     }
 */
    //添加商品到购物车

    $scope.addToCart = function () {

        //发起跨域请求
        $http.get("http://localhost:9107/cart/addGoodsToCartList.do?itemId=" + $scope.sku.id + "&num=" + $scope.num,{"withCredentials":true}).success(
            function (response) {
                if (response.success) {
                    location.href="http://localhost:9107/cart.html";
                }else {
                    alert(response.message);
                }
            }
        );
    }

});