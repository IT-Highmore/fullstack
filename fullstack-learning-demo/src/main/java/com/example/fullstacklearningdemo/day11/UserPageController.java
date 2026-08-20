package com.example.fullstacklearningdemo.day11;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fullstacklearningdemo.day02.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/day11")
public class UserPageController {

  @Autowired
  private UserPageMapper userPageMapper;

  @GetMapping("/users/page")
  public Result<IPage<UserVO>> getUserPage(
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "5") Integer size,
      @RequestParam(required = false) String name) {

    // 构建 MyBatis-Plus Page 对象
    Page<UserVO> pageParam = new Page<>(page, size);

    // 执行分页查询
    IPage<UserVO> UserVOPage = userPageMapper.selectUserPageVo(pageParam, name);

    return Result.success(UserVOPage);
  }
}