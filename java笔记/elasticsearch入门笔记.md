# elasticsearch入门笔记

## Docker安装elasticsearch

略

## Docker启动elasticsearch

### 创建网络

```shell
docker network create es-net
```



### window下启动命令:

```shell
docker run -d --name es     -e "ES_JAVA_OPTS=-Xms512m -Xmx512m"     -e "discovery.type=single-node"     -v es-data:/usr/share/elasticsearch/data     -v es-plugins:/usr/share/elasticsearch/plugins    --privileged     --network es-net     -p 9200:9200     -p 9300:9300 elasticsearch:7.12.1
```



### linux下启动命令:

```shell
docker run -d \
	--name es \
    -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
    -e "discovery.type=single-node" \
    -v es-data:/usr/share/elasticsearch/data \
    -v es-plugins:/usr/share/elasticsearch/plugins \
    --privileged \
    --network es-net \
    -p 9200:9200 \
    -p 9300:9300 \
elasticsearch:7.12.1
```

### 命令解释：

- `-e "cluster.name=es-docker-cluster"`：设置集群名称
- `-e "http.host=0.0.0.0"`：监听的地址，可以外网访问
- `-e "ES_JAVA_OPTS=-Xms512m -Xmx512m"`：内存大小
- `-e "discovery.type=single-node"`：非集群模式
- `-v es-data:/usr/share/elasticsearch/data`：挂载逻辑卷，绑定es的数据目录
- `-v es-logs:/usr/share/elasticsearch/logs`：挂载逻辑卷，绑定es的日志目录
- `-v es-plugins:/usr/share/elasticsearch/plugins`：挂载逻辑卷，绑定es的插件目录
- `--privileged`：授予逻辑卷访问权
- `--network es-net` ：加入一个名为es-net的网络中
- `-p 9200:9200`：端口映射配置

### 成功启动界面

![image-20230701172811764](images\image-20230701172811764.png)

## 安装kibana

kibana可以给我们提供一个elasticsearch的可视化界面，便于我们学习。

### docker导入镜像包

略

### docker启动kibana

#### window下启动命令:

```shell
docker run -d --name kibana -e ELASTICSEARCH_HOSTS=http://es:9200 --network=es-net -p 5601:5601  kibana:7.12.1
```



#### linux下启动命令:

```sh
docker run -d \
--name kibana \
-e ELASTICSEARCH_HOSTS=http://es:9200 \
--network=es-net \
-p 5601:5601  \
kibana:7.12.1
```

### 命令解释

- `--network es-net` ：加入一个名为es-net的网络中，与elasticsearch在同一个网络中
- `-e ELASTICSEARCH_HOSTS=http://es:9200"`：设置elasticsearch的地址，因为kibana已经与elasticsearch在一个网络，因此可以用容器名直接访问elasticsearch
- `-p 5601:5601`：端口映射配置

kibana启动一般比较慢，需要多等待一会，可以通过命令：

```sh
docker logs -f kibana
```

查看运行日志，当查看到下面的日志，说明成功：

![image-20230701173643493](images\image-20230701173643493.png)

此时，在浏览器输入地址访问：http://localhost:5601，即可看到结果

## 安装IK分词器

```shell
# 进入容器内部
docker exec -it es /bin/bash

# 在线下载并安装
./bin/elasticsearch-plugin  install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.12.1/elasticsearch-analysis-ik-7.12.1.zip

#退出
exit
#重启容器
docker restart es
```

