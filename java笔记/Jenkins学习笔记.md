# 虚拟机安装Centos注意事项

Centos7在vmvare中设置网络[(70条消息) VMware中CentOs7联网设置_centos7虚拟机怎么联网_白晓森丶的博客-CSDN博客](https://blog.csdn.net/KelanMai/article/details/127483444)

![image-20230409211907427](images\image-20230409211907427.png)

以下这一步和参考文档有区别,用该要勾选上,不然主机ping不通虚拟机

![image-20230409231101433](images\image-20230409231101433.png)

执行以下命令繁殖挂起或重启后连不上网络

```shell
service NetworkManager stop                     # 关闭NetworkManager服务
```

```shell
chkconfig NetworkManager off                   # 禁止开机自启动
```

# 部署GitLab

1. 安装相关依赖

   ```shell
   yum -y install policycoreutils openssh-server openssh-clients postfix
   ```

   

2. 启动ssh服务&设置为开机启动

   ```shell
   systemctl enable sshd && sudo systemctl start sshd
   ```

   查看ssh服务是否已经开启

   ```shell
   systemctl status sshd
   ```

   ![image-20230410124918801](images\image-20230410124918801.png)

3. 设置postfix开机自启，并启动，postfix支持gitlab发信功能(这步暂时报错，不会影响使用,暂时不理)

   ```shell
   systemctl enable postfix && systemctl start postfix
   ```

   开启报错处理

   ![image-20230410125307731](images\image-20230410125307731.png)

   打开以下文件

   ```
   vi /etc/postfix/main.cf
   ```

   将配置改成以下参数

   ```
   inet_protocols = ipv4
   inet_interfaces = all
   ```

   修改前

   ![image-20230410125635547](images\image-20230410125635547.png)

   修改后

   

4. 开放ssh以及http服务，然后重新加载防火墙列表

   ```shell
   firewall-cmd --add-service=ssh --permanent
   ```

   ```shell
   firewall-cmd --add-service=http --permanent
   ```

   ```shell
   firewall-cmd --reload
   ```

   如果关闭防火墙就不需要做以上配置

5. GitLab安装

   下载地址

   [Index of /gitlab-ce/yum/el7/ | 清华大学开源软件镜像站 | Tsinghua Open Source Mirror](https://mirrors.tuna.tsinghua.edu.cn/gitlab-ce/yum/el7/?C=M&O=D)

   本文下载的是12.4.2版本

   将安装包上传到服务器，并使用个命令安装

   ```shell
   rpm -ivh gitlab-ce-12.4.2-ce.0.el7.x86_64.rpm
   ```

   ![image-20230410131849321](images\image-20230410131849321.png)

   ![image-20230410132314589](images\image-20230410132314589.png)

6. 修改gitlab配置

   ```shell
   vi /etc/gitlab/gitlab.rb
   ```

   修改itlab访问地址和端口，默认为80，我们改为82

   external_url http://192.168.121.100:82
   nginx['listen_port] = 82

   ![image-20230410133924963](images\image-20230410133924963.png)

   ![image-20230410152916103](images\image-20230410152916103.png)

   

7. 重载配置及启动gitlab

   ```
   gitlab-ctl reconfigure
   ```

   ```
   gitlab-ctl restart
   ```

   

8. 把端口添加到防火墙

   ```
   firewall-cmd --zone=public --add-port=82/tcp --permanent
   ```

   ```
   firewall-cmd --reload
   ```

   

## GitLab添加组、创建用户、创建项目

### 创建组

### 创建项目

![image-20230410160112483](images\image-20230410160112483.png)

### 创建用户

![image-20230410160403896](images\image-20230410160403896.png)

创建成功后界面点击Edit进行设置密码

![image-20230410160800059](images\image-20230410160800059.png)

此处密码设置为`he_zhw@123`

![image-20230410160919090](images\image-20230410160919090.png)

#### 将用户添加到组

![image-20230410161246899](images\image-20230410161246899.png)

#### 5种权限的作用范围

![image-20230410161513845](images\image-20230410161513845.png)

### 上传代码到仓库

在idea创建一个web项目

![1681115590410](images\1681115590410.png)

将项目添加到本地仓库

![1681115762088](images\1681115762088.jpg)

![1681115859791](images\1681115859791.png)

添加失败的话需要先配置GIt

这里使用的是vs2022自带的git(D:\VisualStudio\Microsoft Visual Studio\2022\Enterprise\Common7\IDE\CommonExtensions\Microsoft\TeamFoundation\Team Explorer\Git\cmd)

![1681116239972](images\1681116239972.jpg)

添加代码到本地仓库

![1681116434188](images\1681116434188.jpg)

提交代码到本地仓库

![1681116557620](images\1681116557620.jpg)

添加远程仓库



![1681117043023](images\1681117043023.jpg)

![1681117838701](images\1681117838701.jpg)

![1681117863092](images\1681117863092.jpg)

**注，克隆地址里复制的ip会有确实,暂时不清楚什么原因,先手动补上**





推送至远程仓库

![1681119653330](images\1681119653330.png)

提示报错,原因是账户权限不够(要将提升人员权限)

![1681119578295](images\1681119578295.jpg)

提升完权限后就可以推送成功了

![1681119819114](images\1681119819114.jpg)



# 安装Jenkins

使用国内镜像下载

```url
https://mirrors.tuna.tsinghua.edu.cn/jenkins/redhat-stable/?C=M&O=A
```



执行安装

```
 rpm -ivh jenkins-2.332.3-1.1.noarch.rpm
```



![1681123150897](images\1681123150897.jpg)

修改jkenins配置

```
vi /etc/sysconfig/jenkins
```

![1681123531888](images\1681123531888.jpg)

![1681123531905](images\1681123531905.jpg)

启动Jenkins

```
systemctl start jenkins
```

防火墙添加8888端口

```
firewall-cmd --zone=public --add-port=8888/tcp --permanent
```

```
firewall-cmd --reload
```

解决改了上面配置,监听端口还是8080的情况

查看当前状态

```
sudo service jenkins status
```

![image-20230410205214193](images\image-20230410205214193.png)



切换目录

```
cd /usr/lib/systemd/system
```

编辑配置文件

```
vim jenkins.service
```

将端口改成8888

![image-20230410205654722](images\image-20230410205654722.png)

重新加载配置和重启服务

```
systemctl daemon-reload
```

```
sudo service jenkins restart
```

再次查看状态

```
sudo service jenkins status
```

![image-20230410210354617](images\image-20230410210354617.png)

从提示信息中获取管理员密码并填入

![image-20230410211208491](images\image-20230410211208491.png)

```
cat /var/lib/jenkins/secrets/initialAdminPassword
```

![image-20230410211343799](images\image-20230410211343799.png)



跳过插件安装,后续通过国内镜像安装

![image-20230410211628428](images\image-20230410211628428.png)

选择无并点击安装

![image-20230410211725950](images\image-20230410211725950.png)

创建一个用户

用户名:he_zhw 密码:he_zhw@123

![image-20230410211936157](images\image-20230410211936157.png)

![image-20230410212009900](images\image-20230410212009900.png)

### 配置国内插件镜像

按步骤点击菜单Jenkins->Manage Jenkins->Manage Plugins，点击Available

这样做是为了把Jenkins官方的插件列表下载到本地，接着修改地址文件，替换为国内插件地址



切换到配置目录

```
cd /var/lib/jenkins/updates
```

将官方镜像地址替换成国内镜像

```
sed -i 's/http:\/\/updates.jenkins-ci.org\/download/https:\/\/mirrors.tuna.tsinghua.edu.cn\/jenkins/g' default.json && sed -i 's/http:\/\/www.google.com/https:\/\/www.baidu.com/g' default.json
```

在前台管理界面把url改成国内镜像

```
https://mirrors.tuna.tsinghua.edu.cn/jenkins/updates/update-center.json
```

![image-20230410223205910](images\image-20230410223205910.png)

### 重启jenkins

http://192.168.121.101:8888/restart

![image-20230410223341973](images\image-20230410223341973.png)

下载中文汉化插件

![image-20230410223933633](images\image-20230410223933633.png)

### jenkins权限分配

#### 安装权限插件

安装`Role-based Authorization Strategy`插件

![1681182387803](images\1681182387803.jpg)

#### 开启权限全局安全配置

![1681182591909](images\1681182591909.jpg)

选择role-based策略,然后保存

![1681183502533](images\1681183502533.jpg)



#### 创建角色

![1681183581781](images\1681183581781.jpg)

![1681183667696](images\1681183667696.jpg)

分配全局角色与项目角色

![1681184195086](images\1681184195086.jpg)

#### 新建用户

![1681184285166](images\1681184285166.jpg)

![1681184308483](images\1681184308483.png)

新增的用户密码与用户名同名

![1681184419624](images\1681184419624.jpg)

#### 为用户分配角色

![1681184599272](images\1681184599272.jpg)

![1681184725039](images\1681184725039.jpg)

#### 新建项目

![1681185081399](images\1681185081399.jpg)



![1681185256501](images\1681185256501.jpg)





以不同的用户登录,不同的角色权限看到不同的项目



![1681185451488](images\1681185451488.jpg)

![1681185562352](images\1681185562352.jpg)



### 安装凭证插件

#### 安装Credentials Binding插件

安装成功后多了凭证菜单

![1681186373897](images\1681186373897.jpg)

#### Jenkins安装git插件

![1681195243427](images\1681195243427.jpg)





#### 服务器上安装Git工具

安装命令

```
yum install git -y
```

查看版本

```
git --version
```

#### 升级jenkins至最新版

由于安装的jenkins版本过低,插件不兼容,需要先升级jenkins

进入jenkins登录机器，找到jenkins.war

```
find / -name jenkins.war
```

![1681196234932](images\1681196234932.jpg)

进入该目录把war包移动到其他目录下

```
cd /usr/share/java/
mv jenkins.war /root/jenkins
```

关闭jenkins服务

```arduino
systemctl stop jenkins
```

查看服务状态

```
systemctl status jenkins
```

把最新的war包移动到/usr/share/java/目录下

```
mv jenkins.war /usr/share/java/
```

查询系统自带的OpenJdk

```
rpm -qa|grep java
```

![1681197622061](E:\GitHubRepositories\learnNote\java笔记\images\1681197622061.jpg)



将jdk1.8全部卸载掉

```
rpm -e --nodeps java-1.8.0-openjdk-headless-1.8.0.362.b08-1.el7_9.x86_64
```

```
rpm -e --nodeps java-1.8.0-openjdk-src-1.8.0.362.b08-1.el7_9.x86_64
```

```
rpm -e --nodeps java-1.8.0-openjdk-javadoc-1.8.0.362.b08-1.el7_9.noarch
```

```
rpm -e --nodeps java-1.8.0-openjdk-javadoc-zip-1.8.0.362.b08-1.el7_9.noarch
```

然后当前版本就变成11

![1681197914744](images\1681197914744.jpg)

启动Jenkins

```
 systemctl start jenkins
```

#### 添加凭证

##### 添加普通用户验证

![1681199201534](images\1681199201534.jpg)

![1681199381516](images\1681199381516.jpg)

###### 为项目进行配置仓库

![1681199700957](images\1681199700957.jpg)

###### 从仓库上拉取源码

点击Build Now进行拉取代码

![1681199956174](images\1681199956174.jpg)

查看详细日志

![1681200209129](images\1681200209129.jpg)



在服务器上可以看到指定的源代码

![1681200357458](images\1681200357458.jpg)

##### 添加SSH私钥凭证

使用root用户生成公钥和私钥

```
ssh-keygen -t rsa
```

秘钥文件默认保存在/root/.ssh目录下

![1681200740442](images\1681200740442.jpg)

###### 在gitlab服务器上设置公钥(必须是管理员账号登录gitlab)

![1681200991606](images\1681200991606.jpg)

打开公钥文件，将公钥复制到gitlab中

```
cat id_rsa.pub
```

![1681201261001](images\1681201261001.jpg)



###### 在jenkins服务器上设置私钥

```
cat id_rsa
```

![1681201598405](images\1681201598405.jpg)

添加了秘钥还是报错

![1681204067615](images\1681204067615.jpg)

![1681203959635](images\1681203959635.jpg)

## 安装和配置Maven

重新安装jdk1.8

```
yum install -y java-1.8.0-openjdk-devel.x86_64
```

下载Maven

https://maven.apache.org/download.cgi

解压文件

```
tar -xzf apache-maven-3.9.1-bin.tar.gz
```

创建Maven目录

```
mkdir -p /opt/maven
```

将解压好的文件移动到新创建的maven目录

```
mv apache-maven-3.9.1/* /opt/maven
```

配置环境变量

```
vi /etc/profile
```

```
export JAVA_HOME=/usr/lib/jvm/java-1.8.0
export MAVEN_HOME=/opt/maven
export PATH=$PATH:$JAVA_HOME/bin:$MAVEN_HOME/bin
```

![1681206861819](images\1681206861819.jpg)

让配置生效

```
source /etc/profile
```

### Jenkins配置Maven

![1681207834738](images\1681207834738.jpg)



![1681207793049](images\1681207793049.png)

配置全局环境变量

![1681208137162](images\1681208137162.jpg)

![1681208255623](images\1681208255623.jpg)

### 修改Maven的settings.xml

创建本地仓库目录

```
mkdir /root/repo
```

将本地仓库改为新创建的目录并添加阿里云私服镜像链接

```
vi /opt/maven/conf/settings.xml
```

本地仓库会造成无法构建,先不走这个步骤

```xml
<localRepository>/root/repo</1ocalRepository>
```

![1681208750066](images\1681208750066.jpg)

```xml
<mirror>
    <id>alimaven</id>
    <name>aliyun maven</name>
    <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
    <mirrorOf>central</mirrorOf>
</mirror>
```

![1681210844258](images\1681210844258.jpg)

## 项目构建

![1681209451322](images\1681209451322.jpg)



![1681209527233](images\1681209527233.png)

```
mvn clean package
```

构建时提示找不到mvn命令

![1681210588570](images\1681210588570.jpg)

需要执行以下命令

```
ln -s /opt/maven/bin/mvn /usr/sbin/mvn
```





# 安装Tomcat

jdk使用系统自带的版本

解压Tomcat安装包

```
tar -xzf apache-tomcat-8.5.87.tar.gz
```

创建自定义目录

```
mkdir -p /opt/tomcat
```

将解压好的文件放到创建好的目录

```
mv /root/apache-tomcat-8.5.87/* /opt/tomcat
```

防火墙开启8080端口

```
firewall-cmd --zone=public --add-port=8080/tcp --permanent
```

```
firewall-cmd --reload
```

启动Tomcat

```
/opt/tomcat/bin/startup.sh
```

成功访问界面

![image-20230411222738199](images\image-20230411222738199.png)



## 配置Tomcat用户

编辑Tomcat的tomcat-users.xml文件

```
vi /opt/tomcat/conf/tomcat-users.xml
```

在tomcat-users之间添加以下配置

```xml

	<role rolename="tomcat"/>
    <role rolename="ro1e1"/>
    <role rolename="manager-script"/>
    <role rolename="manager-gui"/>
    <role rolename="manager-status"/>
    <role rolename="admin-gui"/>
	<role rolename="admin-script"/>
    <user username="tomcat" password="tomcat" roles="manager-gui ,manager-script,tomcat ,admin-gui ,admin-script"/>

```

![image-20230411230723450](images\image-20230411230723450.png)



注意:为了能够刚才配置的用户登录到Tomcat，还需要修改以下配置

```
vi /opt/tomcat/webapps/manager/META-INF/context.xml
```

注释掉以下这段

![image-20230411224620533](images\image-20230411224620533.png)

重启TomCat服务

```
/opt/tomcat/bin/shutdown.sh
```

```
/opt/tomcat/bin/startup.sh
```

![image-20230411230834505](images\image-20230411230834505.png)







# 新版本Jenkins使用JDK与Maven配置

由于新版本的jenkins默认最低jdk11,要使用jdk1.8需要重新配置

先卸载掉所有的jdk1.8



![1681268421055](images\1681268421055.jpg)



解压jdk

```
tar -xzf jdk-8u291-linux-i586.tar.gz
```

创建本地文件夹

```
mkdir -p /opt/localjdk
```

将解压好的文件移动到创建好的文件夹

```
mv /root/jdk1.8.0_291 /opt/localjdk
```

配置javahome环境变量

```
vi /etc/profile
```

![1681269910071](images\1681269910071.jpg)

让配置生效

```
source /etc/profile
```

重启jenkins

```
http://192.168.121.101:8888/restart
```

jenkins重新配置jdk环境变量

![1681270634181](images\1681270634181.png)

![1681270703930](images\1681270703930.jpg)



配置成功后构建时提示以下错误

![1681271193703](images\1681271193703.jpg)

需要安装`glibc.i686`插件

```
sudo yum install glibc.i686
```

确认生成的war包里的字节码文件是否是jdk1.8

![1681271731490](images\1681271731490.jpg)
