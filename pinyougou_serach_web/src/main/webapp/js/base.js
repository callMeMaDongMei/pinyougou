//没有引入其他模块
var app = angular.module('pinyougou', []);
/*$sce 服务写成过滤器*/
app.filter("trustHtml",["$sce",function ($sce) {
    return function (data) {//传入要被过滤的内容
        return $sce.trustAsHtml(data);//返回的则是或滤后的内容
    }
}]);