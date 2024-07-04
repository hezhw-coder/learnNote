# TypeScript学习笔记

## 一、安装Node.js

[Node.js — 在任何地方运行 JavaScript (nodejs.org)](https://nodejs.org/zh-cn)

目的是使用npm包管理工具

## 二、安装TS编译工具包

#### 安装TS编译工具包命令

```powershell
npm i -g typescript
```

#### 查看编译包版本

```powershell
tsc -v
```

#### 编译ts文件命令

```powershell
tsc C:\Users\10256\Desktop\hello.ts
```

执行后后将ts文件编译成js文件

#### 执行js文件命令

```powershell
node C:\Users\10256\Desktop\hello.js
```

#### 简化执行ts文件的操作

安装ts-node包

```powershell
npm i -g ts-node
```

执行ts文件命令

```powershell
ts-node C:\Users\10256\Desktop\hello.ts
```

注:ts-node命令内部也是先将ts代码转成js代码再执行,但是不会单独生成js文件

## 三、webpack自动化构建

### 新建项目

在项目根目录执行以下命令初始化包管理配置文件

```powershell
npm init -y
```

![image-20240703201723347](images\image-20240703201723347.png)

### 新建src文件夹

![image-20240703201850875](images\image-20240703201850875.png)

#### 增加index.html与index.js文件

![image-20240703202415198](images\image-20240703202415198.png)

### 安装JQuery

```powershell
npm install jquery -S
```

![image-20240703205641249](images\image-20240703205641249.png)

## 安装webpack

```powershell
npm install webpack webpack-cli -D
```

### 在项目中配置webpack

#### 在项目根目录中创建webpack.config.js

```json
module.exports={
   mode:'development'//mode 用来指定构建模式。可选值有 development 和 production(production会自动压缩js文件)
}
```

![image-20240703214726599](images\image-20240703214726599.png)

#### 在 package.json 的 scripts 节点下，新增 dev 脚本如下

```js
  "scripts": {
    "dev": "webpack"
  }
```

![image-20240703212104497](images\image-20240703212104497.png)

### 执行打包命令

```powershell
npm run dev
```

执行后会自动生成main.js文件

![image-20240703212934135](images\image-20240703212934135.png)

#### index.html改成引用main.js

![image-20240703214928918](images\image-20240703214928918.png)

#### 访问html文件即可实现奇偶隔行变色

![image-20240703215040970](images\image-20240703215040970.png)

### 修改打包默认配置约定

![image-20240703220140475](images\image-20240703220140475.png)

![image-20240703221020707](images\image-20240703221020707.png)

![image-20240703221615829](images\image-20240703221615829.png)

```json
const path = require('path')// 导入 node.js 中专门操作路径的模块
module.exports={
   mode:'development',//mode 用来指定构建模式。可选值有 development 和 production
   entry:path.join(__dirname,'./src/index.js'),// 打包入口文件的路径
   output:{
      path:path.join(__dirname,'./dist'),//输出文件的存放路径
      filename:'bundle.js'//输出文件的名称
   }
}
```

### 安装webpack-dev-server插件

目的是当修改源码保存时时自动打包

```powershell
npm install webpack-dev-server -D
```

![image-20240703224146315](images\image-20240703224146315.png)

#### 配置webpack-dev-server

![1720017832842](images\1720017832842.jpg)

```json
  "scripts": {
    "dev": "webpack serve"
  },
```

![image-20240703224523087](images\image-20240703224523087.png)

#### 修改html中引用的js文件

![image-20240703230923112](images\image-20240703230923112.png)

## 四、使用webpack配置TypeScript打包环境

### 1.初始化项目

```powershell
npm init -y
```

![image-20240704203942857](images\image-20240704203942857.png)

### 2.安装webpack、TypeScript以及对应的ts-loader

```powershell
npm i -D webpack webpack-cli typescript ts-loader
```

![image-20240704204524159](images\image-20240704204524159.png)

### 3.配置webpack配置文件

```js
const path = require('path')// 导入 node.js 中专门操作路径的模块
module.exports = {
    mode: 'development',//mode 用来指定构建模式。可选值有 development 和 production
    entry: path.join(__dirname, './src/index.ts'),// 打包入口文件的路径
    output: {
        path: path.join(__dirname, './dist'),//输出文件的存放路径
        filename: 'bundle.js'//输出文件的名称
    },
    devServer: {
        static: "./",
    },
    //指定webpack打包时要使用块
    module: {
        //指定要加载的规则
        rules: [
            {
                // test指定的是规则生效的文件
                test: /\.ts$/,
                // 要使用的loader
                use: 'ts-loader',
                //打包要排除的文件
                exclude: /node-modules/
            }
        ]
    }

}

```

![image-20240704205430672](images\image-20240704205430672.png)

### 4.添加tsconfig.json配置文件

```json
{
    "compilerOptions": {
    "module": "ES6",
    "target": "ES6",
    "sourceMap": false
    }
}
```

![image-20240704210406606](images\image-20240704210406606.png)

### 5.配置package.json

```json
"build": "webpack"
```

![image-20240704210904159](images\image-20240704210904159.png)

### 6.编写index.ts文件并执行打包命令

![image-20240704211238510](images\image-20240704211238510.png)

## 错误处理集合

### vs code无法执行tsc命令

![image-20240702201014003](images\image-20240702201014003.png)

解决方式:

终端执行以下命令

```powershell
set-ExecutionPolicy RemoteSigned 
```

### 较新版webpack-dev-server打开链接显示Cannot GET问题

![image-20240703225839089](images\image-20240703225839089.png)

解决方式:在webpack.config.js配置中添加以下节点后再重新执行启动命令

```json
devServer: {
      static: "./",
    }
```

![image-20240703230202409](images\image-20240703230202409.png)