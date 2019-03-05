app.service("uploadService",function ($http) {
    this.uploadFile=function () {
        var formdata=new FormData();
        formdata.append('file',file.files[0]);
        if(formdata.length==0||formdata==null){
            alert("请选择图1111片!")
        }
        return $http(
            {
                method:'post',
                url:"../upload.do",
                data:formdata,
                headers: {'Content-Type':undefined},
                transformRequest: angular.identity//序列化
            }
        );
    }
})