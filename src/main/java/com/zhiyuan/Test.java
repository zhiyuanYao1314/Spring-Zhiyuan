package com.zhiyuan;

import com.spring.ZhiyuanApplicationContext;
import com.zhiyuan.service.UserService;

/**
 * 测试类，运行方法
 * @author yaozhiyuan
 */
public class Test {

    public static void main(String[] args) {
        ZhiyuanApplicationContext applicationContext = new ZhiyuanApplicationContext(AppConfig.class);

        UserService userService = (UserService) applicationContext.getBean("userService");
        userService.test();  // 1. 代理对象   2. 业务test
    }
}
