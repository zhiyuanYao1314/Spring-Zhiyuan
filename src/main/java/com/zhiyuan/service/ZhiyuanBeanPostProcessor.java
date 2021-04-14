package com.zhiyuan.service;

import com.spring.BeanPostProcessor;
import com.spring.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Component
public class ZhiyuanBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
//        if (beanName.equals("userService")) {
//            System.out.println("初始化前");
//            ((UserServiceImpl) bean).setName("周瑜好帅");
//        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("初始化后");
        // 匹配
        if (beanName.equals("userService")) {

            Object proxyInstance = Proxy.newProxyInstance(ZhiyuanBeanPostProcessor.class.getClassLoader(), bean.getClass().getInterfaces(), new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    System.out.println("代理逻辑");  // 找切点
                    return method.invoke(bean, args);
                }
            });

            return proxyInstance;

        }

        return bean;
    }
}
