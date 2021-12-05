@echo 创建服务
@echo off
sc create MyWindowsService DisplayName="MyWindowsService" binPath="C:\Users\Administrator\Desktop\test\MyWindowsService.exe"
sc config MyWindowsService start=AUTO
pause