# centeos7安装openGauss

## 将安装包放到tmp文件夹

![image-20241115210659168](images\image-20241115210659168.png)

## 安装依赖包文件

```shell
yum install libaio-devel flex bison ncurses-devel glibc-devel patch redhat-lsb-core readline-devel libnsl
```

## 关闭selinux和防火墙

```shell
vim /etc/selinux/config
```

修改config文件，将SELINUX的值变为"disabled"

![image-20241115211122676](images\image-20241115211122676.png)

检测防火墙是否关闭

```shell
systemctl status firewalld
systemctl stop firewalld
systemctl disable firewalld

```

## 设置字符集参数

vim /etc/profile在行尾添加

```shell
export LANG=en_US.UTF-8
```

![image-20241115211930457](images\image-20241115211930457.png)

## 关闭swap

```shell
swapoff -a
```

## 创建工作用户和目录

```
[root@localhost opt]# useradd opengauss  
[root@localhost opt]# mkdir -p /opt/opengauss
```

```
[root@localhost tmp]# tar -jxf openGauss-5.0.3-CentOS-64bit.tar.bz2 -C /opt/opengauss
[root@localhost tmp]# cd /opt/opengauss/  
[root@localhost opengauss]# chmod 755 -R /opt/opengauss  
[root@localhost opengauss]# chown -R opengauss /opt/opengauss  
[root@localhost opengauss]# sudo -iu opengauss  
[opengauss@localhost ~]$ cd /opt/opengauss/simpleInstall  
[opengauss@localhost simpleInstall]$ sh install.sh  -w "A7b#9wXc" &&source ~/.bashrc
```

如果出以下提示需在root用户执行以下脚本

![image-20241115213258882](images\image-20241115213258882.png)

```
sysctl -w kernel.sem="250 85000 250 330"
```

出现以下提示需更改配置

![image-20241115214038690](images\image-20241115214038690.png)

![image-20241115214225332](images\image-20241115214225332.png)

```
sudo vim /etc/security/limits.conf
```

```
opengauss soft nofile 1000000
opengauss hard nofile 1000000
opengauss soft nproc 1000000
opengauss hard nproc 1000000
```

![image-20241115214758497](images\image-20241115214758497.png)

```
sudo vim /etc/security/limits.d/20-nproc.conf
```

```
opengauss soft nproc 1000000
```

![image-20241115214640028](images\image-20241115214640028.png)

## 配置gs_ctl环境变量

- root用户切换到根目录

```
cd /
```

![image-20241115223309581](images\image-20241115223309581.png)

- 查看openGauss的数据目录

  ```
  ps -ef | grep gaussdb
  ```

  ![image-20241115224001744](images\image-20241115224001744.png)

- 编辑环境变量配置文件

  ```
  vim ~/.bashrc
  ```

  在文件末尾添加以下环境变量配置

  ```
  # OpenGauss Environment Variables
  export GAUSSHOME=/opt/opengauss
  export PATH=$GAUSSHOME/bin:$PATH
  export LD_LIBRARY_PATH=$GAUSSHOME/lib:$LD_LIBRARY_PATH
  export PGDATA=/opt/opengauss/data/single_node
  ```

  ![image-20241115225346267](images\image-20241115225346267.png)

- 使环境变量生效

  ```
  source ~/.bashrc
  ```

  

- 查看工具版本号

  ```
  gs_ctl --version
  ```

  ![image-20241115225431881](images\image-20241115225431881.png)

- 查看数据库状态

  ```
  gs_ctl status
  ```

- 连接当前服务器上的数据库

  ```
  gsql -d postgres -p 5432 -r
  ```

  

- 查看数据库名称

  ```
  SELECT current_database();
  ```

  

- jdbc依赖jar包

  ```xml
  <dependency>
      <groupId>org.opengauss</groupId>
      <artifactId>opengauss-jdbc</artifactId>
      <version>5.0.0</version>
  </dependency>
  ```

  

- 

![image-20241116020247628](images\image-20241116020247628.png)

![image-20241116020311208](images\image-20241116020311208.png)

```
host    all             all             0.0.0.0/0            md5
```

设置完之后重启数据库

```
gc_tcl restart
```

## 允许初始用户连接配置

![image-20241116020744412](images\image-20241116020744412.png)

![image-20241116020545029](images\image-20241116020545029.png)

![image-20241116021340120](images\image-20241116021340120.png)

![image-20241116021427545](images\image-20241116021427545.png)

```
gs_ctl reload
```

第二种方法，创建新用户

```
CREATE USER he_zhw WITH PASSWORD 'A7b#9wXc';
GRANT ALL PRIVILEGES ON DATABASE postgres TO he_zhw;
```

![image-20241116022406395](images\image-20241116022406395.png)

```
GRANT CONNECT ON DATABASE postgres TO he_zhw;
```

![image-20241116023426108](images\image-20241116023426108.png)



![image-20241116025617635](images\image-20241116025617635.png)