添加用户测试：如果不想用 ApiFox / Postman，可以直接先在浏览器访问查询接口：http://localhost:8080/api/day03/users（此时返回 data: []）。

用终端发一个 POST 请求新增用户：

Bash
curl -X POST http://localhost:8080/api/day03/user \
-H "Content-Type: application/json" \
-d '{"name":"张三", "age":25, "email":"zhangsan@gmail.com"}'
再次刷新浏览器查询接口 http://localhost:8080/api/day03/users 就能看到刚刚插入的数据了！