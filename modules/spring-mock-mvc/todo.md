* Automatically register MockMvcConfigurer:
  
  ```java
  BeanDefinitionRegistry bdr = new SimpleBeanDefinitionRegistry();
  ClassPathBeanDefinitionScanner s = new ClassPathBeanDefinitionScanner(bdr);
  
  TypeFilter tf = new AssignableTypeFilter(CLASS_YOU_WANT.class);
  s.addIncludeFilter(tf);
  s.scan("package.you.want1", "package.you.want2");       
  String[] beans = bdr.getBeanDefinitionNames();
  ```

