//搜索服务层
app.service("searchService",function ($http) {
    this.search=function (searchMap) {
        return $http.post("itemserach/search.do",searchMap);
    }
});