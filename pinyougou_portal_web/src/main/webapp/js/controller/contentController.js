app.controller("contentController",function ($scope,contentService,$location) {
    $scope.contentList=[];//广告列表
    $scope.findByCategoryId=function (categoryId) {
        contentService.findByCategoryId(categoryId).success(
            function (response) {
                $scope.contentList[categoryId]=response;
            }
        );
    }
    //搜索跳转到search web,页面
    $scope.search=function () {
        location.href="http://localhost:9104/search.html#?keywords="+$scope.keywords;
    }
});