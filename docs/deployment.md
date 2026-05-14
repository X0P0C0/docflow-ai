# 部署说明

## 环境基线

- 后端: JDK 17
- Maven: 3.9.x
- 前端: Node 20 LTS
- 数据库: MySQL 8.x
- 可选: Redis

## 本地开发与服务器的区别

- 本地开发机推荐使用 `fnm` 管理多版本 Node，因为你可能同时维护 Node 16/18/20 的不同项目。
- 线上服务器通常不需要 `fnm`，更常见做法是直接安装固定版本的 `Node 20`。
- 也就是说，`fnm` 是开发便利工具，不是生产环境必需品。

## 本地开发脚本

- 本地一键启动脚本: [scripts/start-all.bat](D:\java\project\docflow-ai\scripts\start-all.bat)
- 该脚本会优先使用 `D:\develop\fnm\fnm.exe`，找不到时再尝试从 PATH 中定位 `fnm`
- 该脚本会切到 `Node 20`，必要时自动重装前端依赖

## 服务器建议做法

### 1. 直接安装固定版本

推荐服务器直接安装并固定以下版本:
- `JDK 17`
- `Maven 3.9.x`
- `Node 20 LTS`
- `MySQL 8.x`

### 2. 不要依赖本地路径

服务器脚本不要写死这种本地开发路径:
- `D:\develop\fnm`
- `D:\develop\java\jdk-17`

服务器应改为使用服务器自己的安装目录，或者直接依赖系统 PATH。

### 3. 前后端部署思路

开发阶段:
- 前端使用 `npm run dev`
- 后端使用 `mvn spring-boot:run`

正式部署时更推荐:
- 前端先 `npm run build`
- 将 `dist` 部署给 Nginx
- 后端打成 jar 运行
- Nginx 反向代理后端接口

## 面试时可以怎么讲

你可以这样表达:
- 本地开发环境使用 `fnm` 管理 Node 多版本，避免不同项目之间的版本冲突。
- 项目开发基线固定为 `Node 20 + JDK 17`。
- 生产环境不依赖 `fnm`，而是直接安装固定版本运行时，确保部署稳定性。
- 本地脚本和部署脚本分离，这是为了把开发便利性和生产稳定性解耦。
