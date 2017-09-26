# TeslaX
基于 JMeter 的性能自动化测试探索。

**只是一个简单的 Demo ，给大家提个思路，希望能帮助大家，谢谢**

## 平台技术栈简介

用的技术如下：

* Spring Boot 1.5.7
* Spring WebMVC 4.3.11
* Spring Security 4.2.3
* Logback 1.1.11
* Thymeleaf 3.0.6
* RabbitMQ

## Startup

### 安装 RabbitMQ 消息队列

* 下载地址 http://www.rabbitmq.com/download.html
* 安装完成后启动 `sbin/rabbitmq-server`
* 安装 web 管理插件，地址 http://www.rabbitmq.com/management.html
* `rabbitmq-plugins enable rabbitmq_management` 安装后重启 rabbitmq
* 安装完成后，直接访问 `localhost:15672` 用户名、密码都是 `guest`
* 登录后选择 `Admin - Add user` 添加用户密码都是 `test` ，设置为 Admin
* 切换 test 用户登录
* 右侧选择 `Virtual Hosts` ，创建一个 `qa_mq` 的虚拟主机，点击创建好的虚拟主机名。在 `Set permission` 中添加用户 test
* 点击上方的 `Queues` ，添加一个 queue，Virtual Host 选择 `qa_mq` , Name 写 `QA_Platform`
* 点击上方的 `Exchanges`，添加一个 exchange，名字为 `QA_Platform_Exchange` , Type 为 `direct`
* 点击创建好的 exchange 的名字，点击 Bindings ，`To queue` 填写之前创建的 Queue 的名字 `QA_Platform` ，Routing key 填写 `QA_Platform_Route` ，点击 Bind 按钮

### 配置文件

* 在 application.yml 文件中配置自己本机的 JMeter 路径和本机的 RabbitMQ 配置

## 运行

* IDE 中跑起来之后，主页点击**压测尝试**
* 输入一个 GET 请求的地址即可
* 后端执行压测有两种方式，一种直接压测，会阻塞当前页面；另一种是放入 RabbitMQ 中，排队压测
