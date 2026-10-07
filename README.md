# 县域儿童青少年近视筛查复诊配镜补助平台

面向县域教育、卫健部门与学校/筛查/医疗机构的一体化近视健康管理系统。以年度视力**筛查数据为起点**，把
**筛查 → 告知 → 家长确认 → 学校复核 → 转诊 → 复诊回写 → 处方 → 配镜 → 补助 → 周期复查 → 学校干预**
串成每个学生一条完整、可追溯的健康管理记录，解决“筛查完成后异常学生无人持续跟进”的问题。

- **前端**：Vue 3 + Vite + Element Plus + Pinia + ECharts
- **后端**：Spring Boot 3.3 + Spring Security(JWT) + Spring Data JPA
- **数据库/中间件**：PostgreSQL 15、Redis 7（短信网关队列 + 未读计数）
- **接口文档**：后端内置 OpenAPI 3 + Swagger UI（`/swagger-ui.html`）

## 一、一键启动（Docker Compose，推荐）

需要本机安装 Docker 与 Docker Compose。

```bash
cd /workspace/176-a
docker compose up --build
```

启动后访问：

| 入口 | 地址 |
| --- | --- |
| 前端页面 | http://localhost |
| 后端 API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |

首次启动自动建表（`schema.sql`，幂等）并写入演示种子数据（机构、10 个角色账号、学校班级学生、完整业务闭环及 8 类特殊场景）。

停止与清库：

```bash
docker compose down          # 停止
docker compose down -v       # 停止并删除数据库卷（下次启动重新播种）
```

## 二、本地开发启动

### 1. 基础设施

```bash
# PostgreSQL：创建库/账号（或使用现有实例，修改 application.yml 即可）
createdb -U postgres vision
psql -U postgres -c "CREATE USER vision WITH PASSWORD 'vision' SUPERUSER;"

# Redis
redis-server
```

### 2. 后端（端口 8080）

```bash
cd backend
mvn spring-boot:run
# 或：mvn package && java -jar target/vision-care.jar
```

可用环境变量覆盖默认连接：`DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`JWT_SECRET`。

### 3. 前端（端口 5173，/api 自动代理到 8080）

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173

生产构建：`npm run build`（产物在 `frontend/dist`，容器内由 Nginx 托管并反代后端）。

## 三、测试账号

所有账号密码统一为 **`123456`**：

| 用户名 | 角色 | 典型工作 |
| --- | --- | --- |
| `admin` | 平台管理员 | 全部数据、机构与账号管理 |
| `edu` | 县教体部门（李科长） | 趋势监管、补助审核发放、干预核验 |
| `health` | 县卫健部门（王主任） | 真实复诊率、干预执行核验 |
| `teacher.wang` | 学校健康老师 | 家长确认单复核转诊、暂缓跟进、问题处置 |
| `head.li` | 班主任（阳光小学） | 名单 CSV 导入、干预安排与执行 |
| `head.zhao` | 班主任（希望小学） | 复查提醒 |
| `screener.chen` | 筛查人员 | 批次管理、现场视力/屈光度/问卷录入 |
| `doctor.zhou` | 县医院眼科医生 | 复诊结果回写、散瞳处方、补助资格判定 |
| `optom.wu` | 合作眼科门诊视光师 | 门诊复诊回写 |
| `optician.sun` | 补助定点眼镜店店长 | 配镜登记、不适调试 |

## 四、功能与业务闭环

1. **名单与档案**：班主任 CSV 批量导入学生、年级班级（自动建班）、既往视力档案、家长联系方式；支持导入错误修正，错误行自动登记 `IMPORT_ERROR`。示例文件：`deploy/sample-students.csv`。
2. **年度筛查**：创建批次；现场记录裸眼视力（右/左）、球镜、散光、轴位、是否戴镜、用眼习惯问卷（户外/屏幕时长、读写距离、眼保健操、遗传史）、是否需复测、**设备校准编号**。系统按视力/屈光度自动判定正常/可疑/异常；请假登记 `ABSENT` 待补筛。
3. **异常双推送**：异常后自动生成家长确认单与学校复核单，短信通知经 **Redis Stream（`notify:sms-gateway`）** 模拟网关下发；家长电话无效自动登记 `PHONE_UNREACHABLE`。
4. **家长决策**：县医院复诊 / 合作眼科门诊复诊 / 暂缓并填写原因；健康老师可再次提醒。
5. **学校复核**：健康老师通过后自动开转诊单到对应机构，可退回补充。
6. **复诊回写**：是否真性近视、是否散瞳（是否已做）、近视程度、处方球镜/散光/轴位/瞳距、复查周期、是否符合配镜补助、医嘱；**与筛查结果差异较大（等效球镜差 ≥1.00D 或筛查正常但确诊近视）自动登记 `RESULT_DIVERGENCE`**；确诊近视自动按周期生成复查计划。
7. **配镜**：合作定点店按处方配镜，医生判定补助资格的自动发起补助申请（封顶 300 元/副）；家长**自行外配**登记 `SELF_GLASSES` 并交学校跟进。
8. **配镜后不适**：家长反馈 `GLASSES_DISCOMFORT` → 眼镜店调试 → 家长确认舒适，全程留痕。
9. **配镜补助**：申请 → 县教体审核（通过/驳回）→ 发放；资格有争议可发起 `SUBSIDY_DISPUTE` 复核。
10. **学校日常干预**：座位调整、户外活动、用眼习惯提醒、家长提醒、定期检查；支持批量安排、登记执行、部门核验，并与医疗复诊结果连接。
11. **周期复查**：按医嘱（默认半年）生成计划，到期短信提醒、到场登记；定时任务（每日 07:30）扫描逾期，自动标记并登记 `MISSED_REVIEW`（也可在“复诊回写/复查”页手动触发）。
12. **统一健康管理时间线**：学生详情页按时间呈现上述全部环节的事件、操作人、角色与数据，8 类特殊情况（请假、电话无法接通、批量导入错误、复诊差异大、自行外配、补助争议、配镜不适、复查未到场）全部挂到同一条记录持续跟进。
13. **县域监管看板**：闭环漏斗、真实复诊率、干预执行率、复查到场率、按月趋势、分年级/分学校/分片区（城东、城西）统计、学生状态分布。

## 五、验证方式（已实际执行）

以下验证均已在本工程上实际跑通：

1. **后端单元/集成测试（H2，2 个用例全绿）**
   ```bash
   cd backend && mvn test
   ```
   覆盖：建档 → 异常筛查自动告知 → 家长确认 → 复核转诊 → 复诊回写（差异预警/复查计划）→ 配镜 → 补助审核发放 → 干预执行核验 → 统计与时间线；以及 CSV 导入错误登记、请假补筛流程。
   实测结果：`Tests run: 2, Failures: 0, Errors: 0`。

2. **端到端接口验证（真实 PostgreSQL + Redis，39 项检查全部通过）**
   ```bash
   # 前置：后端已连真实 PG/Redis 启动
   bash deploy/e2e-verify.sh
   ```
   实测结果：**通过 39 / 失败 0**，包括：
   - CSV 导入成功/失败行计数与 `IMPORT_ERROR` 问题登记；
   - 异常录入后学生状态 `AWAIT_CONFIRM`、短信进入 Redis Stream；
   - 家长暂缓 → 改选县医院 → 复核生成转诊单 → 状态 `AWAIT_REVISIT`；
   - 复诊回写后 `REVISIT_DONE`、差异问题自动登记、复查计划生成；
   - 配镜自动发起补助 → 审核 → 发放后 `SUBSIDY_PAID`；
   - 配镜不适 → 调试 → `CONFIRMED_OK`；批量干预 → 执行 → 核验；
   - 自行外配、补助争议种子场景；逾期扫描自动登记 `MISSED_REVIEW`；
   - 时间线包含 SCREENING_ABNORMAL/PARENT_CHOICE/SCHOOL_REVIEW_APPROVE/REVISIT_RESULT/GLASSES_FITTED/SUBSIDY_PAID/INTERVENTION_*/FOLLOWUP_* 等全部关键事件；
   - 监管统计（复诊率、学校排名 2 所、片区 2 个、月度趋势）。

3. **前端构建与联调**
   ```bash
   cd frontend && npm install && npm run build   # 构建成功
   npm run dev                                   # 实测 http://localhost:5173 返回 200，/api 代理登录返回 200
   ```

4. **手工走查建议路径**
   - 以 `screener.chen` 登录 → “筛查组织/录入”选择批次做一次现场录入（视力填 4.6 可触发异常）；
   - 以 `teacher.wang` 登录 → “家长确认/学校复核”看到待办，完成复核转诊；
   - 以 `doctor.zhou` 登录 → “复诊回写”录入处方；
   - 以 `optician.sun` 登录 → “配镜与补助”登记配镜；
   - 以 `edu` 登录 → 审核发放补助、核验干预、查看“趋势与监管”；
   - 打开任一学生“健康记录”查看统一时间线；在“异常跟进清单”处理 8 类问题。

## 六、目录结构

```
176-a/
├── docker-compose.yml          # pg + redis + backend + frontend 一键编排
├── deploy/
│   ├── e2e-verify.sh           # 端到端接口验证脚本（39 项检查）
│   └── sample-students.csv     # 名单导入示例（含错误行）
├── backend/                    # Spring Boot 3 工程
│   ├── Dockerfile
│   ├── src/main/resources/{application.yml,schema.sql}
│   └── src/main/java/com/county/vision/{common,entity,repo,controller,service,config}
└── frontend/                   # Vue 3 工程
    ├── Dockerfile + nginx.conf
    └── src/views/              # 看板/档案/筛查/复核/复诊/配镜补助/干预/问题清单/统计/管理
```

## 七、说明与限制

- **关于 Compose 验证**：交付环境无 Docker 守护进程，`docker compose up` 未在本环境实际执行；Dockerfile/Compose 配置已按标准多阶段构建编写。本环境的验证均以本机 PostgreSQL 15 + Redis 7 直连方式完成（见第五节），与 Compose 使用同一套镜像构建流程。
- 短信/微信通知通过 Redis Stream 模拟真实网关队列，可用 `redis-cli XRANGE notify:sms-gateway - +` 查看下发内容。
- 补助标准（300 元/副封顶）、异常判定阈值（裸眼 <5.0 或球镜 ≤ -0.50D）、差异阈值（≥1.00D）在后端代码中集中定义，可按县域政策调整。
- JWT 密钥请在生产环境通过 `JWT_SECRET` 环境变量替换默认值。
