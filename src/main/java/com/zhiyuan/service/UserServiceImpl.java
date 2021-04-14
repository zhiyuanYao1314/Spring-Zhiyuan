package com.zhiyuan.service;

import com.spring.*;

@Component("userService")
public class UserServiceImpl implements UserService {

    @Autowired
    private OrderService orderService;

    private String name;

    public void setName(String name) {
        this.name = name;
    }


    public void test() {
        System.out.println(orderService);
        System.out.println(name);
    }


}
