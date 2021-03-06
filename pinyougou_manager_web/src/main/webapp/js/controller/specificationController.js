 //控制层 
app.controller('specificationController' ,function($scope,$controller   ,specificationService) {

    $controller('baseController', {$scope: $scope});//继承

    //读取列表数据绑定到表单中  
    $scope.findAll = function () {
        specificationService.findAll().success(
            function (response) {
                $scope.list = response;
            }
        );
    }

    //分页
    $scope.findPage = function (page, rows) {
        specificationService.findPage(page, rows).success(
            function (response) {
                $scope.list = response.rows;
                $scope.paginationConf.totalItems = response.total;//更新总记录数
            }
        );
    }

    //查询实体
    $scope.findOne = function (id) {
        specificationService.findOne(id).success(
            function (response) {
                $scope.entity = response;
            }
        );
    }

    //保存
    $scope.save = function () {
        var serviceObject;//服务层对象
        if ($scope.entity.specification.id != null) {//如果有ID
            serviceObject = specificationService.update($scope.entity); //修改
        } else {
            serviceObject = specificationService.add($scope.entity);//增加
        }
        serviceObject.success(
            function (response) {
                if (response.success) {
                    //重新查询
                    $scope.reloadList();//重新加载
                } else {
                    alert(response.message);
                }
            }
        );
    }


    //批量删除
    $scope.dele = function () {
        //获取选中的复选框
        if(confirm('确定要删除吗？')){
        specificationService.dele($scope.selectIds).success(
            function (response) {
                if (response.success) {
                    $scope.reloadList();//刷新列表
                    $scope.selectIds = [];
                }
            }
        );
    }
}
	
	$scope.searchEntity={};//定义搜索对象 
	
	//搜索
	$scope.search=function(page,rows){			
		specificationService.search(page,rows,$scope.searchEntity).success(
			function(response){
				$scope.list=response.rows;	
				$scope.paginationConf.totalItems=response.total;//更新总记录数
			}			
		);
	}

	//specificationController新建是同时两张表操作,有一个新建功能,里面的子选项有个新建规格选项
	//就是新增一行表,显示的效果
	$scope.entity={specificationOptionList:[]};//初始化参数
	$scope.addTableRow=function () {
		//数据结构
		//$scope.entity={specification:[],specificationOptionList:[]};
        $scope.entity.specificationOptionList.push({});

    }
    $scope.deleTableRow=function (index) {
        //删除一行
        $scope.entity.specificationOptionList.splice(index,1);

    }


    
});	
