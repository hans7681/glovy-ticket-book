[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/hans7681/glovy-ticket-book)


# 🎬 Glovy 光维娱乐 - 全栈电影票预订平台

一个采用前后端分离架构的全功能电影票务预订系统，支持多角色权限管理和完整的票务业务流程。

<br>

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green)
![Vue.js](https://img.shields.io/badge/Vue.js-3.5-green)
![TypeScript](https://img.shields.io/badge/TypeScript-5.8-blue)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Element Plus](https://img.shields.io/badge/Element_Plus-2.9-brightgreen)

---

## 🚀 在线 Demo 与测试账号

您可以直接访问下面的链接来体验完整的项目功能。

**➡️ [点击访问在线Demo](https://lkxyrmrjjfue.sealosbja.site)**

---

为了方便您测试不同角色的功能，提供了以下测试账号：

| 角色 | 用户名 | 密码 |
| :--- | :--- | :--- |
| **系统管理员** | `test` | `123456` |
| **影院管理员** | `test2` | `123456` |
| **普通用户** | `test7` | `123456` |

---

## 🌟 项目特色 (Features)

* **前后端分离架构**：采用现代化的前后端分离开发模式，接口清晰，便于维护和扩展
* **多角色权限管理**：实现了基于 Spring Security + JWT 的精细化权限控制，支持系统管理员、影院管理员、普通用户三种角色 
* **完整票务业务流程**：包含电影管理、场次管理、座位选择、订单处理等完整业务链条 
* **实时座位锁定**：防止重复预订的座位锁定机制 
* **响应式管理界面**：基于 Element Plus 的现代化管理后台 

---

## ✨ 核心功能模块

### 👥 用户端功能
- 用户注册登录与个人信息管理
- 电影信息浏览与搜索
- 场次查询与座位选择 
- 在线订票与支付
- 订单历史查询与管理

### 🎭 影院管理功能
- 电影信息管理
- 场次排期管理
- 订单统计与分析

### ⚙️ 系统管理功能
- 用户权限管理
- 系统配置管理
- 数据统计与报表

---

## 🛠️ 技术栈 (Tech Stack)

#### 后端 (Backend)
* **核心框架:** Spring Boot 3.x
* **安全框架:** Spring Security + JWT
* **数据持久层:** MyBatis-Plus
* **数据库:** MySQL 8.0
* **开发工具:** Lombok, Maven

#### 前端 (Frontend)
* **核心框架:** Vue 3.5.13
* **UI 框架:** Element Plus 2.9.7
* **开发语言:** TypeScript 5.8.0
* **构建工具:** Vite 6.2.1
* **状态管理:** Pinia
* **路由管理:** Vue Router
* **HTTP 客户端:** Axios

---

## 🏗️ 项目架构

项目采用前后端分离的架构设计：

```
glovy-ticket-book/
├── backend/                 # 后端 Spring Boot 项目
│   ├── src/main/java/
│   │   └── com/backend/backend/
│   │       ├── config/     # 配置类(Security, MyBatis等)
│   │       ├── controller/ # 控制器层
│   │       ├── service/    # 业务逻辑层
│   │       ├── mapper/     # 数据访问层
│   │       ├── entity/     # 实体类
│   │       └── util/       # 工具类
│   └── pom.xml
├── frontend/               # 前端 Vue 项目
│   ├── src/
│   │   ├── views/         # 页面组件
│   │   ├── components/    # 可复用组件
│   │   ├── router/        # 路由配置
│   │   ├── stores/        # 状态管理
│   │   └── utils/         # 工具函数
│   └── package.json
└── README.md
```

---

## 🚀 本地运行 (Getting Started)

#### 1. 环境准备
* JDK 17
* Maven 3.8+
* Node.js 18+
* MySQL 8.0

#### 2. 后端启动
```bash
# 克隆项目到本地
git clone https://github.com/hans7681/glovy-ticket-book.git

# 进入后端项目目录
cd glovy-ticket-book/backend

# 修改 application.yml 中的数据库配置

# 运行项目
mvn spring-boot:run
```

#### 3. 前端启动
```bash
# 进入前端项目目录
cd glovy-ticket-book/frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

#### 4. 数据库初始化
将项目中的 `init.sql` 文件导入到你的本地 MySQL 数据库中。

---

## 🔐 权限设计

系统实现了基于角色的权限控制 ：

- **普通用户 (USER)**: 浏览电影、预订票务、管理个人订单
- **影院管理员 (CINEMA_ADMIN)**: 管理电影信息、场次安排、查看订单统计
- **系统管理员 (SYSTEM_ADMIN)**: 全系统权限，用户管理、系统配置

认证方式采用 JWT Token ，支持无状态的分布式部署。

---

## 📊 核心业务流程

### 订票流程
1. 用户浏览电影列表
2. 选择场次和座位
3. 系统锁定座位 
4. 确认订单并支付
5. 生成电子票据

### 管理流程
1. 影院管理员添加电影信息 
2. 创建并审核场次 
3. 监控订单状态 
4. 生成业务报表

---

## 🔄 更新日志

- **v1.0.0**: 初始版本发布，实现核心票务功能
- **功能完善**: 添加管理后台和权限控制
- **部署优化**: 容器化部署到云平台

---
