# MAUI开发环境部署

注:因开发`ios`和`macOs`需要mac系统，本文不做介绍

## 下载vs2022预览版

本文使用的是`Microsoft Visual Studio Enterprise 2022 (64 位) - Preview  版本 17.3.0 Preview` 

下载时需勾选MAUI开发环境

![image-20220514190442332](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20220514190442332.png)

### 创建`MAUI`项目

![image-20220514191147272](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20220514191147272.png)

### 创建完并等待项目初始化后,会弹出安装安卓SDK提示,点击接受

![image-20220514191812317](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20220514191812317.png)

### 目前谷歌的网址连不上,模拟器会下载失败，导致编译不过

![image-20220514192528510](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20220514192528510.png)

![image-20220514192750304](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20220514192750304.png)

将其他地方的模拟器文件夹考到安卓SDK文件下,并重启vs，再进行编译就能成功(有可能需要重启多次或编译多次才能成功)

![image-20220514193410061](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20220514193410061.png)

![image-20220514195530731](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20220514195530731.png)

### 使用windows平台启动程序,如果有以下报错,需打开操作系统开发人员模式

![image-20220514200748530](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20220514200748530.png)

![image-20220514200648462](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20220514200648462.png)

![image-20220514195946428](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20220514195946428.png)

#### 再次启动即可成功运行

![image-20220514200943707](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20220514200943707.png)

## 使用安卓模拟器调试程序

### 使用安卓的方式启动,会弹出模拟器镜像的新建界面，需创建及下载安卓模拟器镜像

![image-20220514201628863](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20220514201628863.png)

### 由于谷歌的链接被墙导致无法下载镜像

![image-20220514201818462](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20220514201818462.png)

### 安装安卓子系统

#### ***操作系统要求win11才能够安装

参考资料:[MAUI安卓子系统调试(附安装教程)_哔哩哔哩_bilibili](https://www.bilibili.com/video/BV1WF411Y7ge?p=1&share_medium=android&share_plat=android&share_session_id=1f1c6c69-1207-41bb-96c0-17aabc70c061&share_source=WEIXIN&share_tag=s_i&timestamp=1652493886&unique_k=KxdHQ5E)

相关网址:https://store.rg-adguard.net/

https://www.microsoft.com/store/producld/9p3395VX91NR

相关脚本:根据本机安装包文件所在位置而定

```powershell
add-apppackage "D:\Documents\Downloads\MicrosoftCorporationII.WindowsSubsystemForAndroid_2203.40000.3.0_neutral___8wekyb3d8bbwe.Msixbundle"
```

如果安装子系统出现以下问题,需要先安装`Microsoft.UI.Xaml.2.6_2.62108.18004.0_x64__8wekyb3d8bbwe.Appx`

![image-20220514005647274](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20220514005647274.png)

![image-20220514202726339](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20220514202726339.png)

安装完后再次执行命令即可

## 打开安卓子系统报错

![image-20220514203028316](E:\GitHub镜像库\hezhw-coder\learnNote\.net笔记\Note\images\image-20220514203028316.png)

参考以下地址可解决:

https://blog.csdn.net/FreeRxs/article/details/121483401

相关脚本:

```powershell
bcdedit /set hypervisorlaunchtype auto
```

使用`adb`命令链接安卓子系统相关脚本

```powershell
adb connect 127.0.0.1:58526
```

```
adb devices
```

发布安卓程序

参考资料:[发布用于Android的 .NET MAUI 应用 - .NET MAUI | Microsoft Docs](https://docs.microsoft.com/zh-cn/dotnet/maui/android/deployment/overview#publish)

```powershell
dotnet publish -f:net6.0-android -c:Release /p:AndroidSigningKeyPass=mypassword /p:AndroidSigningStorePass=mypassword
```

