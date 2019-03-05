//控制层
app.controller('goodsController', function ($scope, $controller, goodsService, uploadService, itemCatService, typeTemplateService) {

    $controller('baseController', {$scope: $scope});//继承

    //读取列表数据绑定到表单中  
    $scope.findAll = function () {
        goodsService.findAll().success(
            function (response) {
                $scope.list = response;
            }
        );
    }

    //分页
    $scope.findPage = function (page, rows) {
        goodsService.findPage(page, rows).success(
            function (response) {
                $scope.list = response.rows;
                $scope.paginationConf.totalItems = response.total;//更新总记录数
            }
        );
    }

    //查询实体
    $scope.findOne = function (id) {
        goodsService.findOne(id).success(
            function (response) {
                $scope.entity = response;
            }
        );
    }

    //新增商品
    $scope.add = function () {
        $scope.entity.goodsDesc.introduction = editor.html();
        goodsService.add($scope.entity).success(
            function (response) {
                if (response.success) {
                    //重新查询
                    alert("新增成功!")//重新加载
                    $scope.entity = {};
                    //清空富文本编辑器内容
                    editor.html("");
                } else {
                    alert(response.message);
                }
            }
        );
    }


    //批量删除
    $scope.dele = function () {
        //获取选中的复选框
        goodsService.dele($scope.selectIds).success(
            function (response) {
                if (response.success) {
                    $scope.reloadList();//刷新列表
                    $scope.selectIds = [];
                }
            }
        );
    }

    $scope.searchEntity = {};//定义搜索对象

    //搜索
    $scope.search = function (page, rows) {
        goodsService.search(page, rows, $scope.searchEntity).success(
            function (response) {
                $scope.list = response.rows;
                $scope.paginationConf.totalItems = response.total;//更新总记录数
            }
        );
    }

//上传
    $scope.uploadFile = function () {
        uploadService.uploadFile().success(
            function (response) {
                if (response.success) {
                    $scope.image_entity.url = response.message;
                } else {
                    alert(response.message);
                }
            }
        ).error(function () {

            alert("上传发生错误");
        });
    }

//绑定goodsdesc的itemImages字段
    $scope.entity = {goods: {}, goodsDesc: {itemImages: [], specificationItems: []}};
    //$scope.entity={goods:{},goodsDesc:{itemImages:[]}};
    $scope.add_image_entity = function () {

        $scope.entity.goodsDesc.itemImages.push($scope.image_entity);

    }

//删除添加图片
    $scope.remove_image_entity = function (index) {
        $scope.entity.goodsDesc.itemImages.splice(index, 1);
    }


//一级下拉选项
    $scope.selectItemCat1List = function () {
        itemCatService.findByParentId(0).success(
            function (response) {
                $scope.itemCat1List = response;
            }
        )
    }
//二级下拉列表
    $scope.$watch("entity.goods.category1Id", function (newValue, oldValue) {
        itemCatService.findByParentId(newValue).success(
            function (response) {
                $scope.itemCat2List = response;
            }
        )
    });

//三级下拉列表
    $scope.$watch("entity.goods.category2Id", function (newValue, oldValue) {
        itemCatService.findByParentId(newValue).success(
            function (response) {
                $scope.itemCat3List = response;
            }
        );
    });

//获取模板id
    $scope.$watch("entity.goods.category3Id", function (newValue, oldValue) {
        //先从tb_itemcast表中根据最后选择的parentId(就是newValue)查到实体,然后取出typeId
        itemCatService.findOne(newValue).success(
            function (response) {
                $scope.entity.goods.typeTemplateId = response.typeId;
            }
        );
    });

//获取品牌下拉列表(思路:选择三级联动后,根据第三级的typeId从模板表中获取商品名称)
    //监控对象是模板id
    $scope.$watch("entity.goods.typeTemplateId", function (newValue, oldValue) {
        typeTemplateService.findOne(newValue).success(
            function (response) {
                //定义模板变量接收模板实体
                $scope.typeTemplate = response;//最终目的是读取品牌列表
                $scope.typeTemplate.brandIds = JSON.parse($scope.typeTemplate.brandIds);//解析json字符串为json对象
                //扩展属性的添加功能 数据库存储的是json字符串,是集合类型 [{"text":"内存大小","value":"101M"},{"text":"颜色","value":"红色"}]
                $scope.entity.goodsDesc.customAttributeItems = JSON.parse($scope.typeTemplate.customAttributeItems);

            }
        );

        //获取规格选项list<Map>list
        typeTemplateService.findSpecList(newValue).success(
            function (response) {
                $scope.specList = response;
            }
        );
    });


    //在list集合中根据key值查询对象,规格名称与具体规格选项的存储
    $scope.updateSpecAttribute = function ($event, name, value) {
        //attributeName":"网络制式","attributeValue":["移动3G","移动4G"]
        //attributeName:name是"网络规格",attributeValue:value是网络样式
        var object = $scope.searchObjectByKey($scope.entity.goodsDesc.specificationItems, "attributeName", name);
        if (object != null) {
            if ($event.target.checked) {
                //entity.goodsDesc.specificationItems=[{"attributeName":"网络制式","attributeValue":[]},]
                object.attributeValue.push(value);
            } else {
                //移除被选中的样式
                object.attributeValue.splice(object.attributeValue.indexOf(value), 1);
                if (object.attributeValue.length == 0) {
                    //把属性值为[]空的从集合中移除掉,比如只选择了网络规格,又选择了内存.但是又取消了内存规格的选择
                    $scope.entity.goodsDesc.specificationItems.splice(
                        $scope.entity.goodsDesc.specificationItems.indexOf(object)
                    );
                }
            }

        } else {
            //entity.goodsDesc.specificationItems=[]
            $scope.entity.goodsDesc.specificationItems.push({"attributeName": name, "attributeValue": [value]})
        }
    }

    //创建SKU list集合列表
    $scope.createItemList = function () {
        //定义初始结构
        // 定义集合var list=[{ spec:{'屏幕尺寸':"4.0","网络制式":"移动4G"},price:0,num:99999,status:'0',isDefault:'0' }]
        $scope.entity.itemList = [{spec: {}, price: 0, num: 99999, status: '0', isDefault: '0'}];//列表初始化
        //spec内容应该与$scope.entity.goodsDesc.specificationItems一致
        var items = $scope.entity.goodsDesc.specificationItems;
        //alert(JSON.stringify($scope.entity.goodsDesc.specificationItems));
        for (var i = 0; i < items.length; i++) {
            $scope.entity.itemList = addColumn($scope.entity.itemList, items[i].attributeName, items[i].attributeValue);
        }
    }
//columnName是网络制式/储存大小,columnValue是勾选的移动4G,移动3G等等...
    addColumn = function (list, columnName, columnValues) {
        var newList = [];//新的集合
        for (var i = 0; i < list.length; i++) {
            var oldRow = list[i];
            for (var j = 0; j < columnValues.length; j++) {
                //取出list的i号位进行深度克隆
                var newRow = JSON.parse(JSON.stringify(oldRow));
                newRow.spec[columnName] = columnValues[j];
                newList.push(newRow);
            }
        }
        return newList;
    }

});
