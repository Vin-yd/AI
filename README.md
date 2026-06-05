# RobotChat AI 机器人 (RobotChat AI )

基于 Spring Boot + Vue 3 的全栈 AI 对话平台，集成阿里云百炼大模型、RAG 知识库检索、SearXNG 联网搜索，支持通用对话与智能客服两大场景。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| **后端框架** | Spring Boot 3.4.5 + JDK 21 |
| **ORM** | MyBatis-Plus 3.5.12 |
| **数据库** | PostgreSQL 17 + pgvector（向量存储） |
| **AI 框架** | Spring AI 1.1.1 |
| **大模型** | 阿里云百炼 DashScope（qwen-omni-turbo / text-embedding-v4） |
| **搜索引擎** | SearXNG |
| **前端框架** | Vue 3 + Vite |
| **UI 组件库** | Ant Design Vue 4 |
| **状态管理** | Pinia |
| **样式** | Tailwind CSS |
| **Markdown** | markdown-it + highlight.js |
| **HTTP 通信** | Axios + SSE（流式对话） |

---

## 项目结构

```
xiaoha-ai-robot/
├── xiaoha-ai-robot-springboot/   ← 后端 Spring Boot
│   ├── src/main/java/com/vin/ai/robot/
│   │   ├── controller/           ← REST API 接口
│   │   ├── service/              ← 业务逻辑层
│   │   ├── config/               ← Spring 配置
│   │   ├── model/                ← 数据模型 / DTO
│   │   ├── domain/               ← 领域对象
│   │   ├── enums/                ← 枚举常量
│   │   ├── exception/            ← 异常处理
│   │   ├── aspect/               ← AOP 切面
│   │   ├── event/                ← Spring 事件
│   │   ├── utils/                ← 工具类
│   │   └── reader/               ← 文档读取解析
│   ├── src/main/resources/
│   │   ├── application.yml       ← 基础配置
│   │   ├── application-dev.yml   ← 开发环境（不入库，含密钥）
│   │   └── application-prod.yml  ← 生产环境
│   ├── Dockerfile                ← 多阶段构建
│   ├── docker-compose.yml        ← 服务编排
│   └── pom.xml
│
└── xiaoha-ai-robot-vue3/         ← 前端 Vue 3
    ├── src/
    │   ├── views/                ← 页面组件
    │   │   ├── Index.vue         ← 首页
    │   │   ├── ChatPage.vue      ← 通用对话页
    │   │   └── CustomerServiceChatPage.vue  ← 智能客服页
    │   ├── router/               ← 路由配置
    │   ├── stores/               ← Pinia 状态管理
    │   └── components/           ← 公共组件
    └── package.json
```

---

## 功能模块

- **通用 AI 对话** — 接入阿里云百炼大模型，支持多轮对话、流式输出（SSE）
- **智能客服（RAG）** — 基于 pgvector 向量检索 + 知识库文档，实现上下文增强问答
- **联网搜索** — 集成 SearXNG 搜索引擎，支持实时信息检索
- **Markdown 渲染** — 回复内容支持代码高亮、表格等富文本展示

---

## 本地开发

### 环境要求

- JDK 21+
- Maven 3.9+
- Node.js 20.19+ / 22.12+
- PostgreSQL 17 + pgvector 扩展
- SearXNG

### 1. 启动后端

```bash
cd xiaoha-ai-robot-springboot

# 复制开发配置模板，填入你的密钥
cp src/main/resources/application-dev.example.yml src/main/resources/application-dev.yml

# 启动（默认 dev 环境，端口 8080）
mvn spring-boot:run
```

### 2. 启动前端

```bash
cd xiaoha-ai-robot-vue3

npm install
npm run dev
```

前端开发服务器默认运行在 `http://localhost:5173`，自动代理后端 API 到 `8080` 端口。

### 3. 启动依赖服务

PostgreSQL 需提前安装 pgvector 扩展：

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

SearXNG 可通过 Docker 快速启动：

```bash
docker run -d -p 8888:8080 searxng/searxng:latest
```

---

## Docker 部署

进入后端目录，复制环境变量模板后一键启动：

```bash
cd xiaoha-ai-robot-springboot

# 创建 .env 文件，填入真实密钥
cp .env.example .env
vim .env

# 构建并启动所有服务（PostgreSQL + SearXNG + Spring Boot）
docker-compose up -d

# 查看日志
docker-compose logs -f app
```

服务端口映射：

| 服务 | 宿主机端口 | 容器端口 |
|------|-----------|---------|
| Spring Boot 应用 | 8080 | 8080 |
| PostgreSQL | 5432 | 5432 |
| SearXNG | 8888 | 8080 |

---

## 环境变量

部署时需要设置以下环境变量（写在 `.env` 文件中）：

| 变量名 | 说明 | 必填 |
|--------|------|------|
| `DB_USERNAME` | 数据库用户名 | 否（默认 postgres） |
| `DB_PASSWORD` | 数据库密码 | 是 |
| `DASHSCOPE_API_KEY` | 阿里云百炼 API Key | 是 |
| `SEARXNG_SECRET_KEY` | SearXNG 密钥 | 否（自动生成） |

---

## 配置说明

| 配置文件 | 用途 | Git 追踪 |
|----------|------|----------|
| `application.yml` | 基础配置，端口、日志 | ✅ |
| `application-dev.yml` | 开发环境，本地数据库 | ❌（含密钥） |
| `application-dev.example.yml` | 开发配置脱敏模板 | ✅ |
| `application-prod.yml` | 生产环境，环境变量注入 | ✅ |
| `.env.example` | 环境变量模板 | ✅ |
| `.env` | 实际环境变量 | ❌（含密钥） |
