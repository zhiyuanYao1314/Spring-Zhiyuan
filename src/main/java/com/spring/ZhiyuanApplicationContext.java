package com.spring;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IOC容器，该ApplicationContext实现了
 * 1. 通过配置类扫描注入beanDefinition；
 * 2. 通过beanDefinition实例化bean；
 * 4. 单例bean和prototype原型bean处理；
 * 5. @Autowired的属性注入；
 * 6. @InitializingBean 初始化；
 * 7. 初始化前 后的后置处理器
 *
 * 待实现 todo:
 * 1. bean的循环依赖；
 * 2. AOP的后置处理器；
 * 3. 事务的后置处理器；
 *
 */
public class ZhiyuanApplicationContext {

    private Class configClass;

    private ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap<>(); // 单例池
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public ZhiyuanApplicationContext(Class configClass) {
        this.configClass = configClass;

        // 解析配置类
        // ComponentScan注解--->扫描路径--->扫描--->Beandefinition--->BeanDefinitionMap
        scan(configClass);

        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();
            if (beanDefinition.getScope().equals("singleton")) {
                Object bean = createBean(beanName, beanDefinition); // 单例Bean
                singletonObjects.put(beanName, bean);
            }
        }


    }

    public Object createBean(String beanName, BeanDefinition beanDefinition) {

        Class clazz = beanDefinition.getClazz();
        try {

            // 1 在这里进行bean实例化之前后置处理器操作的操作， 然后直接return，
            // 相当于拦截了，就不会进行后面的(实例化，属性注入，Aware接口， 初始化，以及初始化前后的后置处理了)操作了

            // 2 实例化 通过反射
            Object instance = clazz.getDeclaredConstructor().newInstance();


            // 3 依赖注入
            for (Field declaredField : clazz.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(Autowired.class)) {
                    Object bean = getBean(declaredField.getName());
                    declaredField.setAccessible(true);
                    declaredField.set(instance, bean);
                }
            }

            // 4 Aware回调
            if (instance instanceof BeanNameAware) {
                ((BeanNameAware) instance).setBeanName(beanName);
            }

            // 5.1 初始化前的后置处理器
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(instance, beanName);
            }

            // 5.2 初始化
            if (instance instanceof InitializingBean) {
                try {
                    ((InitializingBean) instance).afterPropertiesSet();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // 5.3 初始化后的后置处理器
            // AOP的实现 在这里是通过一个AOP的后置处理器 AnnotationAwareBeanPostProcessor，
            //这个后置处理器会进行代理，在代理中，进行一个AOP的调用，执行所有的AOP方法before, after等方法。

            // 一个AOP的postprocessor会链式调用所有的AOP方法，
            // 另一方面，如果有多个PostProcessor，每个PostProcessor都进行了代理，
            // 从这个循环中，可以看出来，会进行bean的嵌套代理。
            // ApostProcessor代理
            //   BpostProcessor代理
            //     CpostProcessor代理
            //        AOPpostProcessor代理
            //           before1
            //              before2
            //                 原方法...
            //              after2
            //           after2
            // ..........
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(instance, beanName);
            }

            // BeanPostProcessor

            return instance;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void scan(Class configClass) {
        ComponentScan componentScanAnnotation = (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);
        String path = componentScanAnnotation.value(); // 扫描路径 com.zhiyuan.service
        path = path.replace(".", "/");
        ClassLoader classLoader = ZhiyuanApplicationContext.class.getClassLoader();
        URL resource = classLoader.getResource(path);
        File file = new File(resource.getFile());
        if (file.isDirectory()) {

            File[] files = file.listFiles();
            for (File f : files) {
                String fileName = f.getAbsolutePath();
                if (fileName.endsWith(".class")) {
                    String className = fileName.substring(fileName.indexOf("com"), fileName.indexOf(".class"));
                    className = className.replace("/", ".");

                    try {

                        // Spring源码 判断类上是否有component注解，并非通过classLoader， 而是查看字节码，
                        // 这里只是模拟 扫描的时候，判断 类上是否有Component注解
                        Class<?> clazz = classLoader.loadClass(className);
                        if (clazz.isAnnotationPresent(Component.class)) {
                            // 表示当前这个类是一个Bean
                            // 解析类--->BeanDefinition


                            // 判断clazz是否实现了BeanPostProcessor这个类
                            if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                                BeanPostProcessor instance = (BeanPostProcessor) clazz.getDeclaredConstructor().newInstance();
                                // 扫描的时候，将所有的bean后置处理器放入集合中，
                                //在Spring源码中，这些bean后置处理器 也是先于普通类进行初始化的；
                                // 并且会单独抽出一个方法，在beanDefinition完成后，再进行后置处理器的初始化。
                                beanPostProcessorList.add(instance);
                            }

                            Component componentAnnotation = clazz.getDeclaredAnnotation(Component.class);
                            String beanName = componentAnnotation.value();

                            BeanDefinition beanDefinition = new BeanDefinition();
                            beanDefinition.setClazz(clazz);
                            if (clazz.isAnnotationPresent(Scope.class)) {
                                Scope scopeAnnotation = clazz.getDeclaredAnnotation(Scope.class);
                                beanDefinition.setScope(scopeAnnotation.value());
                            } else {
                                beanDefinition.setScope("singleton");
                            }

                            beanDefinitionMap.put(beanName, beanDefinition);

                        }

                    } catch (ClassNotFoundException | NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    public Object getBean(String beanName) {
        if (beanDefinitionMap.containsKey(beanName)) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if (beanDefinition.getScope().equals("singleton")) {
                Object o = singletonObjects.get(beanName);
                return o;
            } else {
                Object bean = createBean(beanName, beanDefinition);
                return bean;
            }

        } else {
            // 不存在对应的Bean
            throw new NullPointerException();
        }
    }
}
