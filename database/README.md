# 数据库脚本

当前阶段已完成基础组织与身份表，后续业务模块的脚本按模块追加。

| 文件 | 用途 |
|---|---|
| `00_create_database.sql` | 创建数据库 |
| `01_foundation_schema.sql` | 身份、组织、人员与关系表 |
| `02_foundation_data.sql` | 四种角色测试账号及基础关联数据 |
| `03_notice_schema.sql` | 通知、通知阅读和附件表 |
| `04_notice_data.sql` | 全校通知和班级通知测试数据 |
| `05_assignment_schema.sql` | 作业、作业提交和批改表 |
| `06_assignment_data.sql` | 已发布作业测试数据 |
| `07_score_schema.sql` | 考试与成绩表 |
| `08_score_data.sql` | 期中考试和三科成绩测试数据 |
| `09_leave_schema.sql` | 请假申请表 |
| `10_leave_data.sql` | 待审批请假测试数据 |
| `11_growth_schema.sql` | 学生成长档案表 |
| `12_growth_data.sql` | 学习表现和德育表现测试数据 |
| `13_interaction_schema.sql` | 家校留言会话和回复表 |
| `14_interaction_data.sql` | 家长留言和教师回复测试数据 |
| `15_operation_log_schema.sql` | 操作日志表 |
| `init_all.sql` | 一次初始化全部表与测试数据 |

本地初始化顺序：

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

测试账号：

| 角色 | 账号 | 初始密码 |
|---|---|---|
| 学校管理员 | `admin` | `Admin@123` |
| 教师 | `teacher01` | `Teacher@123` |
| 学生 | `student01` | `Student@123` |
| 家长 | `parent01` | `Parent@123` |

这些账号只用于本地开发和验收，不能直接用于生产环境。

