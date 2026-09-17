# 交接文档：Java 后端 → Agent 开发学习路线

| 项 | 值 |
|---|---|
| 文档日期 | 2026-02（本会话） |
| 交接来源 | 一次"求学习路线"的咨询会话 |
| 交接目标 | 让下一个 agent / 同事能直接接手执行学习计划，无需重新问背景 |
| 已产出代码 | **无**。本会话只产出了路线图与环境勘察结论 |
| 相关的既有项目 | `/Users/qizhenhai/Desktop/agent`（Spring Boot 空骨架，从未构建） |

---

## 1. 背景与目标

### 1.1 用户画像

- **本职**：Java 后端开发
- **诉求**：想学 Agent 开发，要**学习步骤 + 大纲 + 资料推荐**
- **隐含状态**：已建了一个 Spring Boot 项目 `Desktop/agent`，但里面**一行 LLM 相关代码都没有**，说明处于"想开始但还没真正动手"的阶段

### 1.2 本会话的目标与结论

**目标**：给出一份可执行的学习路线。

**核心论点（本会话最重要的判断）**：
> Agent 开发中约 80% 是**工程问题**（状态管理、并发、超时重试、持久化、可观测性、权限），而非算法问题。用户已有的 Java 后端能力是**最大存量优势**，因此路径应是"用 Java 生态切入，把 LLM 当作一个**不稳定的远程依赖**来治理"，**不应**去补 Python / PyTorch 数据科学路线。

**Agent 的最小定义**（贯穿全程的心智模型）：
```
Agent = LLM（决策） + Tools（行动能力） + Loop（循环执行） + Memory（状态）
```

### 1.3 已破除的三个误区（已向用户明确）

| 误区 | 结论 |
|---|---|
| 要先学 Python 和 PyTorch | 不需要，只调 API，不训练模型 |
| Agent 是新范式很玄 | 本质是 `while` 循环 + 工具调用 + 状态机 |
| 要先把 LLM 原理搞透 | 懂 token / 上下文窗口 / 幻觉成因即可起步 |

---

## 2. 已完成的工作与关键决策

### 2.1 已完成：一份 7 阶段学习大纲（已交付给用户）

| 阶段 | 主题 | 周期 | 验收产出 |
|---|---|---|---|
| 0 | 认知对齐（Chatbot / Workflow / Agent 边界） | 2-3 天 | 能对业务需求判断该用 Workflow 还是 Agent |
| 1 | LLM 应用基础（调 API、结构化输出） | 1 周 | CLI 聊天 + 文本抽 JSON 的接口 |
| 2 | **Function Calling / Tool Use（手写循环）** | 1 周 | 不依赖框架的裸 tool loop + 查天气/查库 Agent |
| 3 | Agent 循环范式与记忆（ReAct / Plan-Execute / Reflection） | 2 周 | 可暂停等人工确认的研究助手 |
| 4 | RAG | 1-2 周 | 企业内部文档问答，准确率可量化 |
| 5 | 框架深入 + 工程化（含 MCP） | 2 周 | 有 trace、有评估、有降级的可上线服务 |
| 6 | 进阶（多 Agent、沙箱、长任务） | 持续 | — |

资料推荐已按优先级给出：Anthropic《Building Effective Agents》与《12-Factor Agents》为**一天内必读**；Java 侧主攻 Spring AI / LangChain4j；Python 生态（LangGraph、LlamaIndex）**只读不写**用于理解概念上限；论文只挑 ReAct、Reflexion、Toolformer、CoT。

### 2.2 已完成：环境勘察（以下均为**实测事实**，非推测）

**工具链**

| 项 | 实测结果 |
|---|---|
| 默认 JDK | `19.0.2`（Oracle OpenJDK, arm64） |
| 已装 JDK | 19.0.2 / Corretto **17.0.6** / Zulu 17.0.6 / Corretto 8 / Zulu 8 |
| `JAVA_HOME` | **未设置** |
| Maven | 3.9.2 @ `/Users/qizhenhai/devTools/apache-maven-3.9.2` |
| Gradle CLI | **不在 PATH 上** → 必须使用 `./gradlew` |
| Gradle wrapper | **9.7.1** |
| Docker | **未安装**（`docker: command not found`） |
| Python | 3.9.6 |
| MySQL | **正在运行**，`127.0.0.1:3306` 端口开放 |
| Maven Central 可达 | ✅ HTTP 200 |
| LLM API Key | 环境变量中**未发现任何**（仅有本 harness 自身的 `PI_MODEL`/`PI_PROVIDER`，与本项目无关） |

**既有项目 `/Users/qizhenhai/Desktop/agent`**

- Spring Boot **4.1.1**，Gradle 插件 `io.spring.dependency-management 1.1.7`
- Java toolchain 声明为 **17**
- 依赖仅：`spring-boot-starter`、Lombok、devtools、**`mysql-connector-j`(runtimeOnly)**、spring-boot-starter-test
- 源码仅 3 个文件：`AgentApplication.java`、`AgentApplicationTests.java`、`application.properties`（内容仅 `spring.application.name=agent`）
- **不是 git 仓库**（`fatal: not a git repository`）
- **从未构建过**（无 `build/` 目录）
- **无任何 LLM / AI 相关依赖** → 阶段 1 在代码层面**尚未开始**

### 2.3 已核实的关键版本事实（决定框架选型，务必保留）

通过 Maven Central `maven-metadata.xml` 实测：

| 构件 | 实测结论 |
|---|---|
| `org.springframework.ai:spring-ai-bom` | `<release>2.0.1`（**已 GA**） |
| `spring-ai-starter-model-openai` | `2.0.1` 存在（HTTP 200） |
| `spring-ai-starter-mcp-client` | `2.0.1` 存在（HTTP 200）→ **MCP 是一等公民** |
| `dev.langchain4j:langchain4j` | `<release>1.20.0` |
| `langchain4j-open-ai` | `1.20.0` 存在（HTTP 200） |
| `langchain4j-spring-boot-starter` | 仅 `<release>1.20.0-beta30` → **仍是 beta** |
| `langchain4j-mcp` | 仅 `1.20.0-beta30` → **仍是 beta** |
| `spring-boot-starter` | 最新 `<release>4.2.0-M1`（故项目的 4.1.1 是正式版） |

### 2.4 关键决策

> ⚠️ **重要区分**：用户在会话中**只提了两个请求**（要路线图、要交接文档），**从未对任何技术选型表态**。因此下列决策全部是**提案状态（PROPOSED）**，而非已确认决策。下一个 agent **不得**把它们当作既定事实。

| # | 决策 | 原因 | 状态 |
|---|---|---|---|
| D1 | 主线走 Java 生态，**不转 Python** | 后端工程能力是最大存量优势；企业里要落地 Agent 的地方（订单、审批、风控、数据平台）全是 Java 存量系统 | PROPOSED |
| D2 | **优先 Workflow，而非 Agent** | Agent 成本高、不可靠；能用固定流程解决的绝不用 Agent。这是新手最大坑 | PROPOSED |
| D3 | **先手写 tool calling 循环，再上框架** | 这是整条路线最关键的认知跃迁；不理解循环就用框架等于背 API | PROPOSED |
| D4 | **框架首选 Spring AI 2.0.1**，LangChain4j 作备选 | ① 既有项目已是 Boot 4.1.1，与 Spring AI 2.0.x 同代；② LangChain4j 的 Spring Boot starter 与 MCP 模块**仍是 beta30**，生产风险高 | PROPOSED（有实证支撑） |
| D5 | **MCP 作为重点投资** | 是把 Agent 接入企业系统的标准通道，掌握者仍少，且与后端能力高度互补 | PROPOSED |
| D6 | 向量库倾向 **pgvector** | 对 Java 友好、运维简单 | PROPOSED ⚠️ **但本机跑的是 MySQL，此提案需修正** |

### 2.5 已交付的行动计划（前 15 天）

已给用户一份逐日的两周计划（第 1-2 天读书 + 拿 API key；第 3-5 天跑通对话与结构化输出；第 6-10 天**手写** tool loop；第 11-14 天 ReAct + 记忆 + RAG；第 15 天接 Langfuse 复盘）。

### 2.6 已交付的战略建议

1. 别丢掉 Java——把 Agent 塞进生产环境才是护城河
2. 优先做"能落地的窄场景"，别做通用助手
3. 把 MCP 当重点投资

---

## 3. 未完成项与下一步

### 3.1 未完成项

- ❌ **零代码产出**。阶段 0-6 **全部未开始**
- ❌ `Desktop/agent` 中未添加任何 LLM 依赖
- ❌ 未申请 / 配置任何 LLM API key
- ❌ 项目未初始化 git，从未构建
- ❌ 用户的实际情况（能否访问外部 API / 有无内网模型 / 目标是公司落地还是个人提升）**问了但用户未回答**——用户直接转向了交接文档请求
- ❌ 未生成"可打卡周计划清单"（曾作为选项提供，用户未选）
- ❌ 未写"手写 tool calling 循环的 Java 最小可运行代码"（曾作为选项提供，用户未选）

### 3.2 下一步（按优先级）

**P0 — 先确认，别急着写代码**
1. 向用户确认 2.4 节的 D1-D6，尤其是 **D4 框架选型** 与 **D6 向量库（MySQL vs 引入 PostgreSQL）**
2. 确认用户是否能访问外部 LLM API。若不能，需改为内网/国产模型，D4 的选型要重做

**P1 — 让既有骨架可运行**（这是当前唯一的实体资产，先把它救活）
3. `git init`（当前无版本控制，风险高）
4. 用 `./gradlew` 做第一次构建，验证 Boot 4.1.1 + JDK 17 toolchain
5. 确认 `mysql-connector-j` 这个孤立的 runtimeOnly 依赖是否会触发 DataSource 自动配置失败

**P2 — 落地阶段 1**
6. 加入 `spring-ai-bom:2.0.1` + `spring-ai-starter-model-openai:2.0.1`
7. 配置 API key（**走环境变量或本地未提交配置，切勿硬编码**）
8. 写第一个 `ChatClient` 接口 + 一个"文本抽 JSON"的 Controller

**P3 — 落地阶段 2（关键跃迁）**
9. **不依赖框架**手写 tool calling 循环，配两个工具（一个 HTTP API、一个查 MySQL 表）
10. 加上最大步数、超时、错误回传、日志 trace、工具权限白名单

**P4** — 按 2.1 节大纲推进阶段 3-6

---

## 4. 已知风险与待确认问题

### 4.1 环境风险

| 风险 | 详情 | 影响 | 缓解 |
|---|---|---|---|
| **JDK 版本错配** | `build.gradle` 声明 toolchain **17**，但系统默认 java 是 **19**，且 `JAVA_HOME` 未设置 | 首次构建可能因 toolchain 解析失败 | Corretto **17.0.6 已安装**，可显式指定 `org.gradle.java.installations.paths` 或设置 `JAVA_HOME` |
| **无版本控制** | 项目不是 git 仓库 | 无回滚、无 diff、实验易丢 | 第一步就 `git init` + `.gitignore`（已有 `.gitignore`，可复用） |
| **无 Docker** | `docker` 命令不存在 | 阻塞**阶段 6 代码执行沙箱**；也无法用容器起 Milvus 等向量库；后续 RAG 部署受限 | 短期用 pgvector/MySQL 替代，或安装 Docker Desktop / Colima |
| **MySQL 而非 PostgreSQL** | 本机 3306 有 MySQL 在跑 | 与 D6 的 pgvector 提案冲突 | 二选一：改造 D6 用 MySQL 向量能力（如 MySQL 9 的 VECTOR 类型 / 或专用向量库），或引入 PostgreSQL |
| **无 API Key** | 环境变量中无任何 LLM key | 阶段 1 无法启动 | 先确认网络与账号可用性 |
| **Gradle CLI 不在 PATH** | — | 误用 `gradle` 会失败 | 一律使用 `./gradlew`；首次会下载 Gradle 9.7.1（网络+耗时） |

### 4.2 技术待确认

| # | 待确认问题 | 为何重要 |
|---|---|---|
| Q1 | **Spring AI 2.0.1 与 Spring Boot 4.1.1 是否真正兼容？** | 实测 `spring-ai-bom:2.0.1` 的 POM **未声明** `spring-boot-dependencies` 版本；两者同代但**未经编译验证**。这是 D4 成立与否的关键，必须先做一次最小构建 |
| Q2 | `mysql-connector-j` 作为孤立 runtimeOnly 依赖，是否导致 `contextLoads` 测试失败？ | 从未构建过，属**未验证**状态 |
| Q3 | 用户能否访问外部 LLM API？ | 不能则整个 D1/D4 路线需重做 |
| Q4 | 用户目标是公司落地还是个人提升？ | 决定是深入工程化（阶段5）还是快速 Demo |
| Q5 | 向量库最终选型？ | 见 4.1 MySQL/PG 冲突 |
| Q6 | LangChain4j 的 beta starter 是否可接受？ | 若用户坚持 LangChain4j，需接受 beta 依赖或改为手工装配（不用 starter） |

### 4.3 长期风险（后续阶段需提前设计）

- **Prompt Injection / 工具越权**：工具权限必须白名单 + 人工确认，尤其写操作（如 `DELETE`）
- **模型输出不稳定**：对工程的影响是幂等、重试、参数校验、返回错误让模型自我修正
- **成本失控**：需步数 / token / 时间**三重预算**
- **多 Agent 陷阱**：大多数场景**不需要**多 Agent，勿被带偏

---

## 5. 如何验证当前状态

### 5.1 已实测通过的验证（可直接复现，结果同 2.2 节）

```bash
# 工具链
java -version 2>&1 | head -3          # 期望 19.0.2
/usr/libexec/java_home -V             # 期望列出 Corretto 17.0.6
/usr/libexec/java_home -v 17          # 期望返回 corretto-17.0.6 路径
echo "${JAVA_HOME:-(unset)}"          # 期望 (unset)
mvn -version | head -2                # 期望 Maven 3.9.2
which gradle; gradle --version        # 期望 NOT FOUND（故必须用 ./gradlew）
docker --version                      # 期望 command not found
nc -z 127.0.0.1 3306 && echo OPEN     # 期望 OPEN（MySQL 在跑）

# LLM key 是否存在
env | grep -iE 'OPENAI|DEEPSEEK|DASHSCOPE|ANTHROPIC|ZHIPU|MOONSHOT' | sed -E 's/=.*/=<set>/'
# 期望：无输出

# 既有项目状态
cd /Users/qizhenhai/Desktop/agent
git rev-parse --is-inside-work-tree   # 期望 fatal: not a git repository
ls -d build                           # 期望 No such file（从未构建）
find src -type f                      # 期望仅 3 个文件
grep -iE "spring-ai|langchain" build.gradle   # 期望无匹配（确认零 LLM 依赖）
```

### 5.2 版本事实复核命令（验证 2.3 节）

```bash
curl -s https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-bom/maven-metadata.xml \
  | grep -oE "<release>[^<]+"                       # 期望 2.0.1
curl -s -o /dev/null -w "%{http_code}\n" \
  https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-starter-mcp-client/2.0.1/spring-ai-starter-mcp-client-2.0.1.pom  # 期望 200
curl -s https://repo1.maven.org/maven2/dev/langchain4j/langchain4j-spring-boot-starter/maven-metadata.xml \
  | grep -oE "<release>[^<]+"                       # 期望 1.20.0-beta30（证实仍是 beta）
```

### 5.3 ⚠️ 尚未执行的验证（**下一个 agent 必须自己跑**）

```bash
cd /Users/qizhenhai/Desktop/agent

# 第一次构建：会下载 Gradle 9.7.1 + Spring Boot 4.1.1，耗时较长
./gradlew build
# 期望：BUILD SUCCESSFUL
# 若报 toolchain 找不到 JDK 17 → 设置 JAVA_HOME 到 corretto-17.0.6
# 若 contextLoads 失败 → 检查 mysql-connector-j 是否触发 DataSource 自动配置

./gradlew dependencies --configuration runtimeClasspath | grep -i mysql
# 确认 mysql 驱动的实际影响范围
```

### 5.4 交接完成的判定标准（Definition of Done）

下一个接手者应能在**不重读原会话**的前提下：

1. 复述 1.2 节的核心论点与 Agent 最小定义
2. 说清 D1-D6 是**提案**而非已确认决策，并指出哪两条最容易翻车（D4、D6）
3. 跑通 5.1 与 5.2 的全部命令并得到一致结果
4. 明确知道下一步是 **P0 先确认选型**，而不是直接开始写代码

---

## 附：文件清单与位置

| 路径 | 说明 |
|---|---|
| `/Users/qizhenhai/Desktop/agent/` | 既有 Spring Boot 4.1.1 空骨架（**唯一实体资产，零 LLM 代码**） |
| `/Users/qizhenhai/Desktop/agent/HANDOFF.md` | 本交接文档 |
| `/Users/qizhenhai/Desktop/agent/build.gradle` | toolchain 17；含孤立 `mysql-connector-j`；**无 LLM 依赖** |
| `/Users/qizhenhai/Desktop/agent/src/main/java/com/example/agent/AgentApplication.java` | 默认启动类，无业务代码 |
| `/Users/qizhenhai/Desktop/agent/gradle/wrapper/gradle-wrapper.properties` | Gradle **9.7.1** |
| `/Users/qizhenhai/devTools/apache-maven-3.9.2` | Maven 安装位置 |
