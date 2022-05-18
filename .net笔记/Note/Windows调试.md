# .net调试

参考文档[.NET应用程序调试—原理、工具、方法 - 王清培 - 博客园 (cnblogs.com)](https://www.cnblogs.com/wangiqngpei557/p/4027413.html)

[下载 Windows 调试工具 - WinDbg - Windows drivers | Microsoft Docs](https://docs.microsoft.com/zh-cn/windows-hardware/drivers/debugger/debugger-download-tools)

[WinDbg 入门（用户模式） - Windows drivers | Microsoft Docs](https://docs.microsoft.com/zh-cn/windows-hardware/drivers/debugger/getting-started-with-windbg)

## 下载windows调试工具箱

[Windows SDK 存档 - Windows 应用开发 (microsoft.com)](https://developer.microsoft.com/zh-cn/windows/downloads/sdk-archive/)

这里下载的是10.0.22000.194版本

![image-20211221104813640](images\image-20211221104813640.png)

只安装windbg,安装目录`D:\Windows Kits`

![image-20211221093607392](images\image-20211221093607392.png)

## 调试Dump文件

一些命令一直显示busy可能是因为去网络服务商下载pdb文件，可开启以下命令进行监测

```
!sym noisy
```

-  加载SOS调试扩展dll

  ```
  .loadby sos clr
  ```

  

- 置并重新加载调试符号文件的命令，将.Net 一些重要的pdb文件下载到指定的路径中，加载到Windbg调试环境中

  ```
  .symfix+ C:\symbols
  ```

- 切换值x86环境(如果是32位应用程序在64位系统的任务管理器导出dump文件则需执行以下命令)

  ```powershell
  .load C:\Users\he_zhw\Downloads\soswow64\soswow64.dll
  .load wow64exts
  !sw
  ```

  

- 

