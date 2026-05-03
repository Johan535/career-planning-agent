
# 开发规范指南

为保证代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下规范。

## 一、项目环境与技术栈

### 1.1 项目基础信息
- **项目名称**：career-planning-agent
- **开发作者**：小嘎
- **工作区路径**：`D:\code\career-planning-agent`
- **操作系统**：Windows 11

### 1.2 技术栈要求
- **主框架**：Spring Boot 3.4.4
- **语言版本**：Java 21.0.8
- **构建工具**：Maven
- **核心依赖**：
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa` (隐含依赖)
  - `spring-boot-starter-validation`
  - `spring-boot-starter-jdbc`
  - `spring-boot-starter-data-pgvector` (集成向量数据库)
  - `spring-ai-alibaba-starter-dashscope` (阿里云DashScope AI)
  - `spring-ai-mcp-client-spring-boot-starter` (MCP客户端)
  - `knife4j-spring-boot-starter` (接口文档)
  - `itext-core` (PDF生成)
  - `jsoup` (HTML解析)
- **开发辅助**：
  - `lombok` (代码简化)
  - `hutool-all` (工具包)
  - `kryo` (序列化)

## 二、目录结构规范

项目目录结构如下，请严格保持此结构：

```text
career-planning-agent
├── ai-agent-fronted/          # 前端项目目录 (Vue/React等)
│   ├── public/
│   └── src/
│       ├── api/
│       ├── assets/
│       ├── components/
│       ├── router/
│       └── views/
├── chat-memory/               # 聊天记忆模块
├── src/                       # Java源码根目录
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── johan/
│   │   │           └── careerplanningagent/
│   │   │               ├── agent/           # Agent相关模型
│   │   │               ├── app/             # 应用层
│   │   │               ├── chatmemory/      # 聊天记忆
│   │   │               ├── config/          # 配置类
│   │   │               ├── constant/        # 常量定义
│   │   │               ├── controller/      # 控制器层
│   │   │               ├── exception/       # 异常处理
│   │   │               ├── model/           # 数据模型
│   │   │               ├── rag/             # RAG相关模块
│   │   │               ├── service/         # 服务层
│   │   │               └── tool/            # 工具类
│   │   └── resources/
│   │       ├── document/      # 文档资源
│   │       ├── static/        # 静态资源
│   │       └── templates/     # 模板文件
│   └── test/
│       └── java/              # 单元测试
└── tmp/                       # 临时文件目录
    ├── download/              # 下载文件
    ├── file/                  # 普通文件
    └── pdf/                   # PDF文件
```

## 三、分层架构规范

| 层级        | 职责说明                         | 开发约束与注意事项                                               |
|-------------|----------------------------------|----------------------------------------------------------------|
| **Controller** | 处理 HTTP 请求与响应，定义 API 接口 | 不得直接访问数据库，必须通过 Service 层调用。返回 DTO 对象。        |
| **Service**    | 实现业务逻辑、事务管理与数据校验   | 必须通过 Repository 层访问数据库。处理 AI 调用、RAG 检索等逻辑。    |
| **Repository** | 数据库访问与持久化操作             | 继承 `JpaRepository` 或自定义方法。使用 `@EntityGraph` 避免 N+1 查询。 |
| **Entity**     | 映射数据库表结构                   | 不得直接返回给前端，需通过 DTO 转换。包名统一为 `entity`。         |
| **Agent**      | Agent 模型与逻辑                   | 定义 Agent 行为、工具调用及状态管理。                             |
| **RAG**        | 检索增强生成逻辑                   | 处理文档加载、向量化、检索及结果合并。                             |

### 接口与实现分离
- 所有业务逻辑接口（如 `UserService`）需放在接口所在包中。
- 具体实现类需放在接口所在包下的 `impl` 子包中。

## 四、安全与性能规范

### 输入校验
- 使用 `@Valid` 与 JSR-303 校验注解（如 `@NotBlank`, `@Size` 等）。
  - 注意：Spring Boot 3.x 中校验注解位于 `jakarta.validation.constraints.*`。
- 禁止手动拼接 SQL 字符串，防止 SQL 注入攻击。
- API Key 等敏感信息应从环境变量或配置中心读取（参考 `application.yml`）。

### 事务管理
- `@Transactional` 注解仅用于 **Service 层**方法。
- 避免在循环中频繁提交事务。

### AI 与 RAG 性能
- 调用 AI 接口时注意超时设置（配置中默认 `120s`）。
- RAG 模块启用 `cloud-enabled: false` 时使用纯内存模式，大数据量需注意内存缓存策略。

## 五、代码风格规范

### 命名规范
| 类型       | 命名方式             | 示例                  |
|------------|----------------------|-----------------------|
| 类名       | UpperCamelCase       | `UserServiceImpl`     |
| 方法/变量  | lowerCamelCase       | `saveUser()`          |
| 常量       | UPPER_SNAKE_CASE     | `MAX_LOGIN_ATTEMPTS`  |

### 注释规范
- 所有类、方法、字段需添加 **Javadoc** 注释。
- 代码注释使用中文（开发者的第一语言）。

### 类型命名规范（阿里巴巴风格）
| 后缀 | 用途说明                     | 示例         |
|------|------------------------------|--------------|
| DTO  | 数据传输对象                 | `UserDTO`    |
| DO   | 数据库实体对象               | `UserDO`     |
| BO   | 业务逻辑封装对象             | `UserBO`     |
| VO   | 视图展示对象                 | `UserVO`     |
| Query| 查询参数封装对象             | `UserQuery`  |
| Cmd  | 命令对象                     | `CreateUserCmd` |

### 实体类简化工具
- 使用 Lombok 注解替代手动编写 getter/setter/构造方法：
  - `@Data`
  - `@NoArgsConstructor`
  - `@AllArgsConstructor`
  - `@Builder` (推荐用于复杂实体构造)

## 六、扩展性与日志规范

### 接口优先原则
- 所有业务逻辑通过接口定义（如 `UserService`），具体实现放在 `impl` 包中。

### 日志记录
- 使用 `@Slf4j` 注解代替 `System.out.println`。
- 记录关键业务操作日志，包括 AI 调用参数、RAG 检索结果等。

### 工具类与第三方集成
- **PDF生成**：使用 `itext-core` 生成 PDF 文档，注意字体包引入（`font-asian`）。
- **HTML解析**：使用 `jsoup` 进行 HTML 文档解析。
- **Markdown处理**：使用 `spring-ai-markdown-document-reader` 处理 Markdown 格式文档。
- **序列化**：使用 `kryo` 进行高性能序列化。

## 七、编码原则总结

| 原则       | 说明                                       |
|------------|--------------------------------------------|
| **SOLID**  | 高内聚、低耦合，增强可维护性与可扩展性     |
| **DRY**    | 避免重复代码，提高复用性                   |
| **KISS**   | 保持代码简洁易懂                           |
| **YAGNI**  | 不实现当前不需要的功能                     |
| **OWASP**  | 防范常见安全漏洞，如 SQL 注入、XSS 等      |

## 八、配置与环境变量

项目依赖以下环境变量或配置项（参考 `application.yml`）：

| 配置项              | 说明                           | 默认值               |
|---------------------|--------------------------------|----------------------|
| `DASHSCOPE_API_KEY`  | 阿里云 DashScope AI 密钥       | `test-key`           |
| `DB_URL`            | PostgreSQL 数据库连接地址       | `jdbc:postgresql://localhost:5432/career-planning-agent` |
| `DB_USERNAME`       | 数据库用户名                   | `postgres`           |
| `DB_PASSWORD`       | 数据库密码                     | `postgres`           |
| `SEARCH_API_KEY`    | 搜索服务 API 密钥              | (空)                 |
| `APP_API_KEY`       | 应用服务 API 密钥              | (空)                 |
| `external-service.enabled` | 外部服务开关           | `false`              |
| `app.security.enabled` | 安全认证开关                | `false`              |
