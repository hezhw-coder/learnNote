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
