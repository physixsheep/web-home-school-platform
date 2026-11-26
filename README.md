# 基于 Web 的高中家校协同智慧互通平台

面向高中学校管理员、教师、学生和家长的前后端分离 Web 平台。

当前已完成第一个可运行纵切面：

- Vue 3 + TypeScript + Element Plus 前端基础框架
- Spring Boot 3 + Spring Security + JWT 后端基础框架
- 登录、退出、当前用户查询
- BCrypt 密码校验、禁用账号拦截、Token 版本失效
- 四角色独立首页路径和前端路由守卫
- MySQL 身份、组织、人员与关系基础表
- 四类角色测试账号及关联测试数据

第二阶段管理员组织与人员管理也已完成：

- 用户、教师、学生、家长账号及档案管理
- 年级、班级管理
- 家长学生绑定
- 教师班主任和学科任课关联
- 学生分班和班级履历

校园通知管理已完成：

- 管理员发布全校、年级和班级通知
- 教师向本人有效任教班级发布通知
- 通知草稿、发布和撤回
- 学生、家长和教师按范围阅读通知
- 幂等已读记录和发布人阅读名单
- 私有附件上传、鉴权下载和临时附件清理

作业管理已完成：

- 教师向本人任教班级和学科布置作业
- 作业草稿、发布和关闭
- 学生文字与附件提交
- 截止控制、重复提交和退回后重交
- 教师查看未交、已交、已批改和已退回清单
- 教师批改、评语和退回
- 家长按绑定孩子查看作业完成及批改情况
- 作业和学生提交附件的资源级鉴权下载

学业成绩管理已完成：

- 管理员维护考试和学科
- 教师按任教班级、学科录入和修改成绩
- 缺考、成绩备注和满分范围校验
- 班级平均分、最高分、最低分统计
- 学生个人成绩查询
- 家长按绑定孩子查询
- ECharts 历次考试总分趋势和最近一次学科柱状图

请假审批管理已完成：

- 学生病假、事假和其他请假申请
- 请假时间、原因和证明附件
- 重叠请假校验
- 待审批请假撤销
- 当前班主任批准或驳回
- 审批意见、审批教师和审批时间
- 家长按绑定孩子查看请假记录
- 管理员查看全校请假记录
- 请假证明附件的角色和业务范围鉴权

学生成长档案已完成：

- 学习表现、德育表现、校园活动、荣誉奖励和综合评价
- 班主任创建、编辑和删除本班成长记录
- 记录发生日期、班级快照和记录教师
- 家长可见/仅校内可见控制
- 图片及附件上传和鉴权下载
- 学生查看本人成长时间线
- 家长按绑定孩子查看成长档案
- 管理员查看全校成长档案

家校互动管理已完成：

- 学生或家长选择本班教师发起留言
- 家长孩子绑定和班级教师关系双重校验
- 教师收件箱、未读、已读和已回复状态
- 教师回复及家长/学生继续回复
- 教师关闭会话
- 历史沟通记录和最后回复预览
- 管理员全校留言审计

数据统计工作台已完成：

- 管理员全校学生、教师、家长和班级数量
- 已发布通知和待审批请假数量
- 教师管理班级、学生和已发布作业数量
- 教师作业提交率、待批改和待审批请假数量
- 管理员学生年级分布和通知范围图表
- 教师班级学生/提交量和待处理事项图表
- 所有指标从 MySQL 实时聚合，不使用固定假数据

最终集成阶段已完成：

- 已登录用户操作日志和查询参数脱敏
- 操作日志管理员查询
- 统一数据库初始化脚本
- 单机部署与 Nginx 示例
- 全模块 MySQL 烟雾回归

## 环境要求

| 组件 | 版本 |
|---|---|
| JDK | 17 LTS |
| Maven | 3.9+ |
| Node.js | 20.19+、22.12+ 或 24 |
| pnpm | 11+ |
| MySQL | 8.0+ |

当前电脑默认 Java 是 8，但 JDK 17 已安装在：

```text
C:\Users\liu\.jdks\ms-17.0.17
```

项目启动脚本会显式使用该 JDK，避免 Maven 调用 Java 8。

## 初始化数据库

在项目根目录依次执行：

```powershell
mysql --default-character-set=utf8mb4 -u root -p -e "source database/init_all.sql"
```

也可以逐文件执行：

```powershell
mysql --default-character-set=utf8mb4 -u root -p -e "source database/00_create_database.sql"
mysql --default-character-set=utf8mb4 -u root -p home_school_platform -e "source database/01_foundation_schema.sql"
mysql --default-character-set=utf8mb4 -u root -p home_school_platform -e "source database/02_foundation_data.sql"
mysql --default-character-set=utf8mb4 -u root -p home_school_platform -e "source database/03_notice_schema.sql"
mysql --default-character-set=utf8mb4 -u root -p home_school_platform -e "source database/04_notice_data.sql"
mysql --default-character-set=utf8mb4 -u root -p home_school_platform -e "source database/05_assignment_schema.sql"
mysql --default-character-set=utf8mb4 -u root -p home_school_platform -e "source database/06_assignment_data.sql"
mysql --default-character-set=utf8mb4 -u root -p home_school_platform -e "source database/07_score_schema.sql"
mysql --default-character-set=utf8mb4 -u root -p home_school_platform -e "source database/08_score_data.sql"
mysql --default-character-set=utf8mb4 -u root -p home_school_platform -e "source database/09_leave_schema.sql"
mysql --default-character-set=utf8mb4 -u root -p home_school_platform -e "source database/10_leave_data.sql"
mysql --default-character-set=utf8mb4 -u root -p home_school_platform -e "source database/11_growth_schema.sql"
mysql --default-character-set=utf8mb4 -u root -p home_school_platform -e "source database/12_growth_data.sql"
mysql --default-character-set=utf8mb4 -u root -p home_school_platform -e "source database/13_interaction_schema.sql"
mysql --default-character-set=utf8mb4 -u root -p home_school_platform -e "source database/14_interaction_data.sql"
mysql --default-character-set=utf8mb4 -u root -p home_school_platform -e "source database/15_operation_log_schema.sql"
```

如 MySQL 账号不是 `root`，请同时设置：

```powershell
$env:DB_USERNAME='你的数据库账号'
$env:DB_PASSWORD='你的数据库密码'
```

不要把真实数据库密码提交到仓库。

## 启动后端

```powershell
.\scripts\dev-backend.ps1
```

后端地址：

```text
http://localhost:8080
```

Swagger UI：

```text
http://localhost:8080/swagger-ui.html
```

可用环境变量：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/home_school_platform...` | 数据库连接 |
| `DB_USERNAME` | `root` | 数据库账号 |
| `DB_PASSWORD` | 空 | 数据库密码 |
| `JWT_EXPIRATION_MINUTES` | `120` | Token 有效期 |
| `JWT_SECRET` | 仅开发默认值 | 生产环境必须替换的 Base64 密钥 |

## 启动前端

首次安装依赖：

```powershell
pnpm --dir frontend install
```

启动开发服务器：

```powershell
.\scripts\dev-frontend.ps1
```

前端地址：

```text
http://127.0.0.1:5173
```

Vite 会将 `/api` 请求代理到 `http://localhost:8080`。

## 测试账号

| 角色 | 账号 | 初始密码 |
|---|---|---|
| 学校管理员 | `admin` | `Admin@123` |
| 教师 | `teacher01` | `Teacher@123` |
| 学生 | `student01` | `Student@123` |
| 家长 | `parent01` | `Parent@123` |

账号只用于本地开发和验收，不能直接用于生产环境。

## 构建

后端：

```powershell
$env:JAVA_HOME='C:\Users\liu\.jdks\ms-17.0.17'
mvn -f backend/pom.xml clean verify
```

前端：

```powershell
pnpm --dir frontend build
```

或执行：

```powershell
.\scripts\build-all.ps1
```

认证模块真实数据库烟雾测试：

```powershell
.\scripts\smoke-auth.ps1
```

该脚本会使用系统临时目录启动独立 MySQL 实例，验证四角色登录、当前用户查询、退出后 Token 失效和错误密码响应，结束后自动停止并清理。它不会修改本机已有 `home_school_platform` 数据库。

## 当前接口

| 方法 | 路径 | 权限 |
|---|---|---|
| POST | `/api/auth/login` | 公开 |
| POST | `/api/auth/logout` | 已登录 |
| GET | `/api/auth/me` | 已登录 |
| GET/POST | `/api/admin/users` | 管理员 |
| GET/POST | `/api/admin/grades` | 管理员 |
| GET/POST | `/api/admin/classes` | 管理员 |
| GET/POST | `/api/admin/parent-bindings` | 管理员 |
| GET/POST | `/api/admin/teacher-assignments` | 管理员 |
| GET/POST | `/api/admin/student-classes` | 管理员 |
| GET | `/api/notices` | 已登录 |
| GET/POST | `/api/notices/manage`、`/api/notices` | 管理员或教师 |
| POST | `/api/notices/{id}/read` | 有通知访问权限 |
| POST | `/api/files` | 管理员、教师或学生 |
| GET | `/api/files/{id}/download` | 有业务资源访问权限 |
| GET/POST | `/api/assignments` | 学生、家长按孩子或教师任教范围 |
| GET | `/api/assignments/manage` | 教师 |
| POST | `/api/assignments/{id}/submission` | 学生 |
| GET | `/api/assignments/{id}/submissions` | 发布教师 |
| POST | `/api/submissions/{id}/grade` | 发布教师 |
| POST | `/api/submissions/{id}/return` | 发布教师 |
| GET/POST | `/api/admin/exams` | 管理员 |
| GET/POST | `/api/admin/subjects` | 管理员 |
| GET | `/api/scores/workspace-options` | 教师、管理员 |
| GET/PUT | `/api/scores/entry`、`/api/scores/batch` | 任教教师 |
| GET | `/api/scores/statistics` | 教师、管理员 |
| GET | `/api/scores/report` | 学生、家长或管理员 |
| GET/POST | `/api/leaves` | 学生、家长、管理员 |
| GET | `/api/leaves/approval` | 班主任 |
| POST | `/api/leaves/{id}/cancel` | 学生本人 |
| POST | `/api/leaves/{id}/decision` | 当前班主任 |
| GET | `/api/growth-records` | 学生本人、家长绑定孩子、管理员全校 |
| GET | `/api/growth-records/manage` | 班主任 |
| POST | `/api/growth-records` | 班主任 |
| PUT/DELETE | `/api/growth-records/{id}` | 记录教师或管理员 |
| GET | `/api/messages/mine` | 学生、家长 |
| GET | `/api/messages/received` | 教师、管理员 |
| POST | `/api/messages` | 学生、家长 |
| GET | `/api/messages/{id}` | 会话参与方或管理员 |
| POST | `/api/messages/{id}/replies` | 会话参与方 |
| POST | `/api/messages/{id}/read` | 接收教师 |
| POST | `/api/messages/{id}/close` | 接收教师 |
| GET | `/api/dashboard/admin` | 管理员 |
| GET | `/api/dashboard/teacher` | 教师 |
| GET | `/api/admin/operation-logs` | 管理员 |

完整接口可在 Swagger UI 中查看。

## 目录

```text
backend/      Spring Boot 后端
frontend/     Vue 3 前端
database/     MySQL 脚本
docs/         需求、设计与测试文档
storage/      本地私有附件目录
scripts/      本地开发脚本
```

详细实现和测试结果见：

- [认证模块开发与测试记录](docs/06-认证模块开发与测试记录.md)
- [组织与人员管理开发与测试记录](docs/07-组织与人员管理开发与测试记录.md)
- [校园通知管理开发与测试记录](docs/08-校园通知管理开发与测试记录.md)
- [作业管理开发与测试记录](docs/09-作业管理开发与测试记录.md)
- [学业成绩管理开发与测试记录](docs/10-学业成绩管理开发与测试记录.md)
- [请假审批管理开发与测试记录](docs/11-请假审批管理开发与测试记录.md)
- [学生成长档案开发与测试记录](docs/12-学生成长档案开发与测试记录.md)
- [家校互动管理开发与测试记录](docs/13-家校互动管理开发与测试记录.md)
- [数据统计工作台开发与测试记录](docs/14-数据统计工作台开发与测试记录.md)
- [操作日志与审计](docs/15-操作日志与审计.md)
- [部署说明](docs/16-部署说明.md)
- [最终验收报告](docs/17-最终验收报告.md)

