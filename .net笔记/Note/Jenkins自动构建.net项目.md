# 安装JDK

新版的jenkins需要jdk的最低版本为11

![image-20230412143250423](images\image-20230412143250423.png)



下载地址

[Java Archive Downloads - Java SE 11 (oracle.com)](https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html)

![image-20230412143639031](images\image-20230412143639031.png)



# 安装Jenkins

下载地址[Jenkins download and deployment](https://www.jenkins.io/download/)

![image-20230412141452181](images\image-20230412141452181.png)

输入服务器的登录账号及密码点击测试凭证



![image-20230412142104543](images\image-20230412142104543.png)

弹出以下提示

![image-20230412142303095](images\image-20230412142303095.png)

在本地安全策略将登录用户添加到作为服务登录

![image-20230412142556461](images\image-20230412142556461.png)

再次测试即可成功

![image-20230412142634756](images\image-20230412142634756.png)

修改监听端口为8888,默认是8080端口

![image-20230412142808797](images\image-20230412142808797.png)



### 选择Jdk主目录

![image-20230412144119499](images\image-20230412144119499.png)

### 安装成功后输入以下地址即可访问

![image-20230412144544775](images\image-20230412144544775.png)

### 根据提示去服务器对应路径下打开对应文件查询密码

![image-20230412144925676](images\image-20230412144925676.png)

![image-20230412145038787](images\image-20230412145038787.png)

### 创建一个用户

he_zhw/he_zhw@123

![image-20230412145340265](images\image-20230412145340265.png)

### 配置国内插件镜像

将default中的updates.jenkins.io/download全部替换为mirrors.tuna.tsinghua.edu.cn/jenkins

![image-20230412150244729](D:\GitHubRepositories\learnNote\.net笔记\Note\images\image-20230412150244729.png)

把www.google.com换成www.baidu.com

![image-20230412150515152](images\image-20230412150515152.png)

在菜单将改成国内镜像链接

![image-20230412150712215](images\image-20230412150712215.png)

![image-20230412150801707](images\image-20230412150801707.png)



重启Jenkins

http://192.168.214.128:8888/restart

### 下载svn插件

注:Jenkins服务器上首先要先安装svn客户端

![image-20230412161328938](images\image-20230412161328938.png)

添加凭证

![image-20230412161828337](images\image-20230412161828337.png)

配置项目

![image-20230412161922461](images\image-20230412161922461.png)

配置自定义的工作空间

![image-20230412162410527](images\image-20230412162410527.png)

## 自动构建

### 在Jenkins服务器安装msbuild

下载地址[下载 Visual Studio Tools - 免费安装 Windows、Mac、Linux (microsoft.com)](https://visualstudio.microsoft.com/zh-hans/downloads/)

![image-20230412181744822](images\image-20230412181744822.png)

![image-20230412182154527](images\image-20230412182154527.png)

![image-20230412182219608](images\image-20230412182219608.png)

### 配置msbuild环境变量

![image-20230412185434741](images\image-20230412185434741.png)

![image-20230412185808193](images\image-20230412185808193.png)

验证环境变量

![image-20230412190453360](images\image-20230412190453360.png)

### 安装msbuild插件

![image-20230413171514231](images\image-20230413171514231.png)