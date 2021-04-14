

# Spring主要功能源码

# 启动

直接运行com.zhiyuan.Test.main方法即可；


# 实现功能
 1. 通过配置类扫描注入beanDefinition；
 2. 通过beanDefinition实例化bean；
 3. 单例bean和prototype原型bean处理；
 4. @Autowired的属性注入；
 5. @InitializingBean 初始化；
 6. 初始化前 后的后置处理器


# 待实现 
 
 1. bean的循环依赖；
 2. AOP的后置处理器；
 3. 事务的后置处理器；