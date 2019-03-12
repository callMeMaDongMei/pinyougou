app.controller("searchController", function ($scope, searchService,$location) {
    //定义搜索对象的结构
    $scope.searchMap = {
        "keywords": "",
        "category": "",
        "brand": "",
        "spec": {},
        "price": "",
        "pageNo": 1,
        "pageSize": 40,
        "sort": "",
        "sortField": ""
    };

    //搜索
    $scope.search = function () {
        $scope.searchMap.pageNo = parseInt($scope.searchMap.pageNo);//指定页面字符转数字
        searchService.search($scope.searchMap).success(
            function (response) {
                $scope.resultMap = response;//搜索返回的结果
                buildPageLabel();//构建分页栏
            }
        );
    }

    //构建分页栏
    buildPageLabel = function () {
        $scope.pageLabel = [];
        var firstPage = 1;//初始第一页
        var lastPage = $scope.resultMap.totalPages;//最后一页
        //省略号处理
        $scope.firstDot = true;//前面有点
        $scope.lastDot = true; //后面有点

        //构建分页标签(totalPages 为总页数)
        //总页码totalPages有以下情况 比方:总100页,我想分页条每次只显示5页,
        // 考虑当前页码与总页码数的关系
        // 1__totalPages<=5,显示全部即可,不作处理
        // 2__totalPages>5,情况下处理分页条if(totalPages>5){
        // 2.1,中心页效果是前2后2 情况一当前页码小于等于3即是12345,就显示12345,:if()
        // 2.2,中心页>=98(totalPages-2)   96,97,98,99,100 ,firstPage=100-4;
        // 2.3,正常情况,前后页码够数
        // }
        if ($scope.resultMap.totalPages > 5) {
            if ($scope.searchMap.pageNo <= 3) {
                lastPage = 5;
                $scope.firstDot = false;//前面没点
            } else if ($scope.searchMap.pageNo >= $scope.resultMap.totalPages - 2) {
                firstPage = $scope.resultMap.totalPages - 4;
                $scope.lastDot = false;//后面没点

            } else {
                firstPage = $scope.searchMap.pageNo - 2;
                lastPage = $scope.searchMap.pageNo + 2;
            }
        } else {
            $scope.firstDot = false;//前面无点
            $scope.lastDot = false;//后边无点
        }
        for (var i = firstPage; i <= lastPage; i++) {
            $scope.pageLabel.push(i);
        }
    }

    //添加搜索项,改变resultMap值
    $scope.addSearchItem = function (key, value) {
        if (key == "category" || key == "brand" || key == "price") {  //表示用户点击的是分类或者,品牌
            $scope.searchMap[key] = value;
        } else {
            $scope.searchMap.spec[key] = value;
        }
        $scope.search();//查询(后端需要过滤筛选查询)
    }

    //移除搜索项,改变resultMap值
    $scope.removeSearchItem = function (key, value) {
        if (key == "category" || key == "brand" || key == "price") {  //表示用户点击的是分类或者,品牌
            $scope.searchMap[key] = "";
        } else {
            delete $scope.searchMap.spec[key];//直接把key:null 的key整个给删除
        }
        $scope.search();//查询
    }

    //分页查询  改变当前页,并重新查询
    $scope.queryByPage = function (pageNo) {
        if (pageNo < 1 || pageNo > $scope.resultMap.totalPages) {
            return;
        }
        $scope.searchMap.pageNo = pageNo;
        $scope.search();
    }

    //判断当前页是否为第一页,实现上下页特效禁用效果
    $scope.isTopPage = function () {
        if ($scope.searchMap.pageNo == 1) {
            return true;
        } else {
            return false;
        }
    }
    //判断当前页是否为最后页,实现上下页特效禁用效果
    $scope.isEndPage = function () {
        if ($scope.searchMap.pageNo == $scope.resultMap.totalPages) {
            return true; //为最后页则返回true disable 禁用
        } else {
            return false;
        }
    }
    //排序查询,方法影响searchMap值
    $scope.sortSearch=function (sortField,sort) {
        $scope.searchMap.sortField=sortField;
        $scope.searchMap.sort=sort;
        $scope.search();//赋值后重新查询
    }

    //用户输入关键词就是品牌,应该隐藏品牌选项
    $scope.keywordsBrand=function () {
        for(var i=0;i<$scope.resultMap.brandList.length;i++){
            if($scope.searchMap.keywords.indexOf($scope.resultMap.brandList[i].text)>=0) {
                return true;//说明用户输的是品牌名称,应该隐藏
            }
        }
        return false;//反之这是显示
    }

    $scope.loadkeywords=function () {
        $scope.searchMap.keywords=$location.search()['keywords'];
        $scope.search();//赋值后重新查询
    }

});