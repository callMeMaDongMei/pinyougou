app.controller("searchController",function ($scope, searchService) {
    //定义搜索对象的结构
    $scope.searchMap={"keywords":"","category":"","brand":"","spec":{}};

    //搜索
    $scope.search=function () {
        searchService.search($scope.searchMap).success(
            function (response) {
                $scope.resultMap=response;//搜索返回的结果
            }
        );
    }

    //添加搜索项,改变resultMap值
    $scope.addSearchItem=function (key, value) {
        if(key=="category"||key=="brand"){  //表示用户点击的是分类或者,品牌
            $scope.searchMap[key]=value;
        }else {
            $scope.searchMap.spec[key]=value;
        }
        $scope.search();//查询(后端需要过滤筛选查询)
    }

    //移除搜索项,改变resultMap值
    $scope.removeSearchItem=function (key, value) {
        if(key=="category"||key=="brand"){  //表示用户点击的是分类或者,品牌
            $scope.searchMap[key]="";
        }else {
           delete $scope.searchMap.spec[key];//直接把key:null 的key整个给删除
        }
        $scope.search();//查询
    }
});