export const menuMeta: Record<
  string,
  {
    section: string;
    sectionEn: string;
    description: string;
    accent: string;
  }
> = {
  "/home": {
    section: "驾驶舱",
    sectionEn: "Overview",
    description: "查看组织概览、考勤趋势和近期提醒。",
    accent: "overview",
  },
  "/system/files": {
    section: "系统管理",
    sectionEn: "System",
    description: "管理公告与文件资料的上传、分发和存档。",
    accent: "system",
  },
  "/system/employees": {
    section: "系统管理",
    sectionEn: "System",
    description: "维护员工档案、部门归属和角色分配。",
    accent: "system",
  },
  "/system/departments": {
    section: "系统管理",
    sectionEn: "System",
    description: "配置部门排班、请假说明与迟到规则。",
    accent: "system",
  },
  "/permission/roles": {
    section: "权限管理",
    sectionEn: "Security",
    description: "管理角色定义并分配菜单权限。",
    accent: "permission",
  },
  "/permission/menus": {
    section: "权限管理",
    sectionEn: "Security",
    description: "维护系统菜单、层级结构和图标。",
    accent: "permission",
  },
  "/salary/records": {
    section: "薪资管理",
    sectionEn: "Payroll",
    description: "查看月度薪资、社保与公积金记录。",
    accent: "salary",
  },
  "/salary/cities": {
    section: "薪资管理",
    sectionEn: "Payroll",
    description: "维护参保城市与社保规则参数。",
    accent: "salary",
  },
  "/attendance/leaves": {
    section: "考勤管理",
    sectionEn: "Attendance",
    description: "发起请假、审批请假并追踪状态。",
    accent: "attendance",
  },
  "/attendance/records": {
    section: "考勤管理",
    sectionEn: "Attendance",
    description: "查询打卡记录、导入导出和统计。",
    accent: "attendance",
  },
  "/finance/expenses": {
    section: "财务管理",
    sectionEn: "Finance",
    description: "追踪费用报销和支出状态。",
    accent: "finance",
  },
  "/finance/assets": {
    section: "财务管理",
    sectionEn: "Finance",
    description: "维护固定资产与使用状态。",
    accent: "finance",
  },
  "/finance/approvals": {
    section: "财务管理",
    sectionEn: "Finance",
    description: "集中处理财务审批待办。",
    accent: "finance",
  },
  "/recruitment/requirements": {
    section: "招聘管理",
    sectionEn: "Hiring",
    description: "管理招聘需求、预算与时间节奏。",
    accent: "recruitment",
  },
  "/recruitment/positions": {
    section: "招聘管理",
    sectionEn: "Hiring",
    description: "维护岗位开放状态与招聘渠道。",
    accent: "recruitment",
  },
  "/recruitment/candidates": {
    section: "招聘管理",
    sectionEn: "Hiring",
    description: "查看候选人进度、来源和转化表现。",
    accent: "recruitment",
  },
  "/care/plans": {
    section: "员工关怀",
    sectionEn: "Care",
    description: "安排关怀计划并跟踪执行情况。",
    accent: "care",
  },
  "/care/stats": {
    section: "员工关怀",
    sectionEn: "Care",
    description: "查看生日、周年与关怀覆盖数据。",
    accent: "care",
  },
};

export const accentClassMap: Record<string, string> = {
  overview: "accent-overview",
  system: "accent-system",
  permission: "accent-permission",
  salary: "accent-salary",
  attendance: "accent-attendance",
  finance: "accent-finance",
  recruitment: "accent-recruitment",
  care: "accent-care",
};
