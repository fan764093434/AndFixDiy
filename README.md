# 热修复学习笔记

首先感谢老师Darren的无私分享，贴上老师的个人地址

***Darren学习群:546270670***

[Darren的BLOG](http://blog.csdn.net/z240336124/)

[Darren的简书](http://www.jianshu.com/p/c0ec2a7fc26a)

[阿里热修复](https://github.com/alibaba/AndFix)

###### 学习热修复时，首先得清楚在Android项目中，类是如何加载的；如下图：

![类的加载流程图](https://github.com/fan764093434/AndFixDiy/blob/master/images/class_loading.png)

***源码摘抄，结合上图，让我们更清楚类是如何加载的***
```
activity = mInstrumentation.newActivity(cl,
                                        component.getClassName(),
                                        r.intent);
```
```
public Activity newActivity(ClassLoader cl, String className,Intent intent) throws InstantiationException,
                IllegalAccessException,
                ClassNotFoundException {
    // 通过classLoader找到activity的calss,利用反射实例化对象 TestActivity
    return (Activity)cl.loadClass(className).newInstance();
}
```
***PathClassLoader  -> BaseDexClassLoader -> ClassLoader***
```
protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
      Class<?> clazz = findLoadedClass(className);
      if (clazz == null) {
          if (clazz == null) {
               try {
                   clazz = findClass(className);
               } catch (ClassNotFoundException e) {
                    e.addSuppressed(suppressed);
                    throw e;
               }
          }
      }
      return clazz;
}
```
***BaseDexClassLoader --> PathDexList --> findClass方法***
```
/**
     * Finds the named class in one of the dex files pointed at by
     * this instance. This will find the one in the earliest listed
     * path element. If the class is found but has not yet been
     * defined, then this method will define it in the defining
     * context that this instance was constructed with.
     *
     * @param name       of class to find
     * @param suppressed suppressed exceptions encountered whilst finding the class
     * @return the named class or {@code null} if the class is not
     * found in any of the dex files
     */
    public Class findClass(String name, List<Throwable> suppressed) {
        for (Element element : dexElements) {
            DexFile dex = element.dexFile;
            if (dex != null) {
                Class clazz = dex.loadClassBinaryName(name, definingContext, suppressed);
                if (clazz != null) {
                    return clazz;
                }
            }
        }
        if (dexElementsSuppressedExceptions != null) {
            suppressed.addAll(Arrays.asList(dexElementsSuppressedExceptions));
        }
        return null;
    }
```
#### 项目原理

***读取修改过的dex文件中的类插入到用户安装的有错误的dex文件所有类之前，使得ClassLoader在加载类时先拿到的是没有bug的类去执行操作。***

#### dex文件的生成步骤
1. 修改后将项目签名打包成apk;
2. 将打包好的apk文件后缀名改为zip
3. 解压文件
4. 找到classes.dex文件，修改为喜欢的xxx.dex;

#### 阿里热修复和自定义比较
自定义热修复是将所有的类生成的dex文件放入我们自己的服务器，每次用户打开项目时去做检测更新，这样会导致
dex文件过大，用户更新耗时，再执行修复操作时效率较低。
而阿里的人修复是通过没有错误的apk和有错误的apk进行拆分，只找到有错误的地方，生成一个拆分包，这样拆分包的体积小，
用户更新
效率更高，而且在执行时它只用找到有更新标志的Annotation(@MethodReplace)的地方，进行替换工作。
#### 项目libary的使用方法
```
dependencies {
    compile 'com.github.fan764093434:AndFixDiy:1.0.1'
}
```
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
#### 项目使用
* 1.初始化
```
try {
    //初始化自定义热修复
    manager = new AndFixManager(this);
    // 加载所有修复的Dex包
    manager.loadFixDex();
} catch (NoSuchFieldException e) {
    e.printStackTrace();
} catch (IllegalAccessException e) {
    e.printStackTrace();
}
```
* 2.在你觉得合适的地方下载dex文件，保存在您觉得合适的地方，这里不再做过多的记录了，
   因为我自己知道。
* 3.在合适的地方加载dex文件，代码示例如下
```
/**
 * 自己的修复方式
 */
private void fixDexBug() {
    File fixFile = new File(Environment.getExternalStorageDirectory(), "fix.dex");
    if (fixFile.exists()) {
        AndFixManager fixDexManager = new AndFixManager(this);
        try {
            fixDexManager.fixDex(fixFile.getAbsolutePath());
            Toast.makeText(this, "修复成功", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "修复失败", Toast.LENGTH_LONG).show();
        }
    }
}
```

