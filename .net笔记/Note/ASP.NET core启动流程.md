# 启动流程



## 一.调用`Microsoft.Extensions.Hosting.dll`下的静态方法`Microsoft.Extensions.Hosting.Host.CreateDefaultBuilder()`注册通用主机

![image-20211017160656421](images\image-20211017160656421.png)

![image-20211017160805255](images\image-20211017160805255.png)

## 二、调用`Microsoft.AspNetCore.Hosting.dll`的静态方法Microsoft.Extensions.Hosting.GenericHostWebHostBuilderExtensions.*ConfigureWebHost*()注册web主机

![image-20211017161412003](images\image-20211017161412003.png)

最后调用`Microsoft.AspNetCore.dll`下的静态方法`Microsoft.AspNetCore.WebHost.ConfigureWebDefaults(IWebHostBuilder **builder**)`

![image-20211017161958320](images\image-20211017161958320.png)

## 三、注册主机的各种委托会放到`Microsoft.Extensions.Hosting.dll`下的主机`Microsoft.Extensions.Hosting.HostBuilder`的本地集合中

![image-20211017210621238](images\image-20211017210621238.png)

![image-20211017211145284](images\image-20211017211145284.png)

## 四、紧接着调用主机的Build()方法将注册的委托都执行一遍,初始化Startup类型，并调用Startup的`ConfigureServices`方法

![image-20211017212116036](images\image-20211017212116036.png)

![image-20211017212228674](images\image-20211017212228674.png)

![image-20211017212544312](images\image-20211017212544312.png)

## 五、执行`Microsoft.Extensions.Hosting.Abstractions.dll`中`Microsoft.Extensions.Hosting.HostingAbstractionsHostExtensions`的*Run*方法运行应用并阻止调用线程，直到关闭主机

![image-20211017214447829](images\image-20211017214447829.png)

![image-20211017215610685](images\image-20211017215610685.png)

![image-20211017220437397](images\image-20211017220437397.png)

![image-20211017221144428](images\image-20211017221144428.png)

![image-20211018010559573](images\image-20211018010559573.png)

![image-20211018010645381](images\image-20211018010645381.png)