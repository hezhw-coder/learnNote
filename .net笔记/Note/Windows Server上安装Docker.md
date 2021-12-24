# Windows Server上安装Docker

**虚拟机中安装后无法下载镜像，因为虚拟机无法开启虚拟化**

参考文档[准备 Windows 操作系统容器 | Microsoft Docs](https://docs.microsoft.com/zh-cn/virtualization/windowscontainers/quick-start/set-up-environment?tabs=Windows-Server#install-docker)

![image-20211222152723540](images\image-20211222152723540.png)

```powershell
Install-Module -Name DockerMsftProvider -Repository PSGallery -Force
```

如果没安装NuGet会先提示安装

![image-20211222153447692](images\image-20211222153447692.png)

安装NuGet报错处理

![image-20211222154014832](images\image-20211222154014832.png)

解决方式[(27条消息) Windows Server2016 安装docker 所踩的坑_xuefuruanjian的博客-CSDN博客](https://blog.csdn.net/xuefuruanjian/article/details/115169381)

```powershell
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
```

再执行安装Nuget

```powershell
Install-PackageProvider -Name NuGet -MinimumVersion 2.8.5.201 -Force
```

![image-20211222155152788](images\image-20211222155152788.png)

```powershell
Install-Module -Name DockerMsftProvider -Force
```

```powershell
Install-Package -Name docker -ProviderName DockerMsftProvider
```



部署Docker服务

创建容器

```powershell
docker build -t mywebapi .
```

![image-20211224130605723](images\image-20211224130605723.png)

运行容器

前面的端口可自定义,后面的端口对应的是应用程序运行时监听的端口号

```powershell
docker run -d -p 35678:5000 --name mywebapi mywebapi
```

容器迁移

```powershell
docker save -o mywebapi.t
```

包镜像包放到Docker.exe同级目录

```powershell
docker load --input mywebapi.tar
```

## 卸载Docker

```powershell
Uninstall-Package -Name docker -ProviderName DockerMsftProvider
```

