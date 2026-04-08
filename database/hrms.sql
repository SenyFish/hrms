/*
 Navicat Premium Dump SQL

 Source Server         : ;ocal
 Source Server Type    : MySQL
 Source Server Version : 80045 (8.0.45)
 Source Host           : localhost:3306
 Source Schema         : hrms

 Target Server Type    : MySQL
 Target Server Version : 80045 (8.0.45)
 File Encoding         : 65001

 Date: 08/04/2026 10:06:20
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for asset_approval
-- ----------------------------
DROP TABLE IF EXISTS `asset_approval`;
CREATE TABLE `asset_approval`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `applicant_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `apply_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `apply_time` datetime(6) NULL DEFAULT NULL,
  `approve_time` datetime(6) NULL DEFAULT NULL,
  `approver_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `asset_id` bigint NOT NULL,
  `applicant_user_id` bigint NULL DEFAULT NULL,
  `requested_quantity` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKd9l729ugf8oa60yl05x213ve`(`asset_id` ASC) USING BTREE,
  CONSTRAINT `FKd9l729ugf8oa60yl05x213ve` FOREIGN KEY (`asset_id`) REFERENCES `asset_info` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of asset_approval
-- ----------------------------
INSERT INTO `asset_approval` VALUES (5, '邵珠峰', '办公使用', '2026-04-02 01:46:39.887893', '2026-04-02 01:47:19.911346', '系统管理员', '', '已通过', 1, 6, 1);
INSERT INTO `asset_approval` VALUES (6, '周新晟', '办公使用', '2026-04-03 00:47:03.910314', '2026-04-03 01:39:06.859973', '系统管理员', '', '已通过', 2, 5, 1);

-- ----------------------------
-- Table structure for asset_info
-- ----------------------------
DROP TABLE IF EXISTS `asset_info`;
CREATE TABLE `asset_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `asset_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `department_id` bigint NULL DEFAULT NULL,
  `purchase_date` date NULL DEFAULT NULL,
  `quantity` int NULL DEFAULT NULL,
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `unit_price` decimal(12, 2) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_g91iiq4dsxmqc6rtesi0smur7`(`asset_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of asset_info
-- ----------------------------
INSERT INTO `asset_info` VALUES (1, 'AST202604010001', '办公电脑惠普', '办公用品', '2026-04-01 09:14:16.778792', 4, '2025-04-01', 98, '', '在库', 5000.00);
INSERT INTO `asset_info` VALUES (2, 'AST202604020002', '办公椅', '办公用品', '2026-04-02 08:47:25.391495', 4, '2026-04-02', 98, '', '在库', 90.00);

-- ----------------------------
-- Table structure for attendance_record
-- ----------------------------
DROP TABLE IF EXISTS `attendance_record`;
CREATE TABLE `attendance_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `att_date` date NOT NULL,
  `clock_in` datetime(6) NULL DEFAULT NULL,
  `clock_out` datetime(6) NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK6g080yufjqnuyoby0moacp79y`(`user_id` ASC, `att_date` ASC) USING BTREE,
  CONSTRAINT `FKs6gsykw1pcfeiif9o449y0p10` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of attendance_record
-- ----------------------------
INSERT INTO `attendance_record` VALUES (1, '2026-04-01', '2026-04-01 03:55:21.712508', NULL, '2026-04-01 03:55:21.713484', NULL, '迟到待下班', 3);
INSERT INTO `attendance_record` VALUES (2, '2026-04-01', '2026-04-01 08:20:14.634555', '2026-04-01 08:24:08.643995', '2026-04-01 08:20:14.634555', NULL, '迟到早退', 5);
INSERT INTO `attendance_record` VALUES (3, '2026-04-01', '2026-04-01 08:27:25.873226', NULL, '2026-04-01 08:27:25.873226', NULL, '迟到待下班', 6);
INSERT INTO `attendance_record` VALUES (4, '2026-04-02', '2026-04-02 01:16:23.564237', '2026-04-02 01:16:25.850326', '2026-04-02 00:56:16.222836', NULL, '迟到早退', 5);
INSERT INTO `attendance_record` VALUES (5, '2026-04-02', '2026-04-02 01:27:32.571092', NULL, '2026-04-02 01:18:23.245009', NULL, '上班正常', 6);
INSERT INTO `attendance_record` VALUES (6, '2026-04-03', '2026-04-03 00:30:11.848509', NULL, '2026-04-03 00:30:11.849508', NULL, '上班正常', 6);
INSERT INTO `attendance_record` VALUES (8, '2026-04-04', '2026-04-04 10:20:43.489544', NULL, '2026-04-04 10:20:43.490544', NULL, '迟到待下班', 6);
INSERT INTO `attendance_record` VALUES (9, '2026-04-06', '2026-04-06 07:39:28.689500', '2026-04-06 07:39:43.972660', '2026-04-06 07:39:28.690520', NULL, '迟到早退', 8);
INSERT INTO `attendance_record` VALUES (10, '2026-04-06', '2026-04-06 08:16:48.890532', '2026-04-06 08:16:48.701983', '2026-04-06 08:16:33.363538', NULL, '迟到早退', 9);
INSERT INTO `attendance_record` VALUES (11, '2026-04-07', '2026-04-07 00:59:10.299439', '2026-04-07 00:59:12.982225', '2026-04-07 00:59:10.300445', NULL, '早退', 9);

-- ----------------------------
-- Table structure for business_trip_request
-- ----------------------------
DROP TABLE IF EXISTS `business_trip_request`;
CREATE TABLE `business_trip_request`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `approve_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `approve_time` datetime(6) NULL DEFAULT NULL,
  `approver_id` bigint NULL DEFAULT NULL,
  `approver_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `attachment_file_id` bigint NULL DEFAULT NULL,
  `attachment_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `attachment_remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `department_id` bigint NULL DEFAULT NULL,
  `department_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `destination` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `employee_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `employee_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `estimated_days` decimal(10, 1) NOT NULL,
  `estimated_expense` decimal(12, 2) NULL DEFAULT NULL,
  `position_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `serial_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `trip_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_7u13xcl919rkg4jmbnh7i7u98`(`serial_no` ASC) USING BTREE,
  INDEX `FK7vdys39sval344ugcyouadplp`(`user_id` ASC) USING BTREE,
  CONSTRAINT `FK7vdys39sval344ugcyouadplp` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of business_trip_request
-- ----------------------------
INSERT INTO `business_trip_request` VALUES (1, '不同意出差申请', '2026-04-06 08:15:38.823014', 2, 'hr', 4, '新建 文本文档.txt', '123', '2026-04-06 08:15:20.222384', 5, '', '999', '吉吉国王', '123123', 4.0, 1500.00, NULL, 'BT202604060001', '已驳回', '外出培训', 9);

-- ----------------------------
-- Table structure for department
-- ----------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fine_per_late` decimal(38, 2) NULL DEFAULT NULL,
  `late_grace_minutes` int NULL DEFAULT NULL,
  `leave_settings_note` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `overtime_rate` decimal(38, 2) NULL DEFAULT NULL,
  `parent_id` bigint NULL DEFAULT NULL,
  `sort_order` int NULL DEFAULT NULL,
  `work_end_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `work_start_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of department
-- ----------------------------
INSERT INTO `department` VALUES (1, 50.00, 10, '年假需提前 3 天申请', '研发中心', 1.50, 0, 1, '18:00', '09:00');
INSERT INTO `department` VALUES (2, 50.00, 10, '年假需提前 3 天申请', '销售部', 1.50, 0, 1, '18:00', '09:00');
INSERT INTO `department` VALUES (3, 50.00, 10, '', '信息技术部', 1.50, 0, 1, '10:00', '09:30');
INSERT INTO `department` VALUES (4, 50.00, 10, '', '财务部', 1.50, 0, 1, '18:00', '09:00');

-- ----------------------------
-- Table structure for employee_care_plan
-- ----------------------------
DROP TABLE IF EXISTS `employee_care_plan`;
CREATE TABLE `employee_care_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `budget_amount` decimal(12, 2) NULL DEFAULT NULL,
  `care_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `content` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `plan_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `planned_time` datetime(6) NULL DEFAULT NULL,
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `user_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_ath5ypopgis02p97migx1kdvf`(`plan_code` ASC) USING BTREE,
  INDEX `FKjbr3ocrbvmc95rva2vk2w2f33`(`user_id` ASC) USING BTREE,
  CONSTRAINT `FKjbr3ocrbvmc95rva2vk2w2f33` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of employee_care_plan
-- ----------------------------
INSERT INTO `employee_care_plan` VALUES (3, 5000.00, '入职周年', '为王人事安排入职周年关怀，关怀日期：2026-04-19。', '2026-04-02 07:16:21.838472', 'CAREP202604020002', '2026-04-18 16:00:00.000000', '首页提醒一键生成', '已完成', '2026-04-02 07:21:36.247664', 2);
INSERT INTO `employee_care_plan` VALUES (4, 1000.00, '生日慰问', '为赵员工安排生日关怀，关怀日期：2026-04-12。', '2026-04-02 07:29:00.477211', 'CAREP202604020004', '2026-04-11 16:00:00.000000', '首页提醒一键生成', '待执行', '2026-04-02 07:32:31.633034', 3);

-- ----------------------------
-- Table structure for employee_care_record
-- ----------------------------
DROP TABLE IF EXISTS `employee_care_record`;
CREATE TABLE `employee_care_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `care_time` datetime(6) NULL DEFAULT NULL,
  `care_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `content` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `cost_amount` decimal(12, 2) NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `handler_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `record_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `plan_id` bigint NULL DEFAULT NULL,
  `user_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_hsg6ir9cngr0e3d4m8kd9djnm`(`record_code` ASC) USING BTREE,
  INDEX `FK3wbmellfmxi4ls1i8h0uwexkg`(`plan_id` ASC) USING BTREE,
  INDEX `FKqyikwsyq3k3nvb3ye4mvqhi3c`(`user_id` ASC) USING BTREE,
  CONSTRAINT `FK3wbmellfmxi4ls1i8h0uwexkg` FOREIGN KEY (`plan_id`) REFERENCES `employee_care_plan` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKqyikwsyq3k3nvb3ye4mvqhi3c` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of employee_care_record
-- ----------------------------
INSERT INTO `employee_care_record` VALUES (1, '2026-04-18 16:00:00.000000', '入职周年', '为王人事安排入职周年关怀，关怀日期：2026-04-19。', 0.00, '2026-04-02 07:21:07.380347', '系统自动补录', 'CARER202604020001', '关怀计划完成后自动生成', '已执行', '2026-04-02 07:21:07.380347', 3, 2);
INSERT INTO `employee_care_record` VALUES (2, '2026-04-11 16:00:00.000000', '生日慰问', '为赵员工安排生日关怀，关怀日期：2026-04-12。', 1000.00, '2026-04-02 07:32:22.999088', '系统自动补录', 'CARER202604020002', '关怀计划完成后自动生成', '已执行', '2026-04-02 07:32:22.999088', 4, 3);

-- ----------------------------
-- Table structure for employee_contract
-- ----------------------------
DROP TABLE IF EXISTS `employee_contract`;
CREATE TABLE `employee_contract`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contract_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `employee_id` bigint NOT NULL,
  `employee_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `end_date` date NOT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `serial_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_date` date NOT NULL,
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `contract_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `reminder_date` date NULL DEFAULT NULL,
  `reminder_days` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_rkmvpkrs7oy8nodmgk505jiff`(`serial_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of employee_contract
-- ----------------------------
INSERT INTO `employee_contract` VALUES (2, '劳动合同', '2026-04-04 10:43:12.583821', 3, '赵员工', '2027-08-04', '', 'CT202604040002', '2022-04-17', '生效中', NULL, NULL, NULL);

-- ----------------------------
-- Table structure for employee_dispute
-- ----------------------------
DROP TABLE IF EXISTS `employee_dispute`;
CREATE TABLE `employee_dispute`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `dispute_date` date NOT NULL,
  `dispute_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `employee_id` bigint NOT NULL,
  `employee_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `resolution` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `serial_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_4aof8swma5jpejyebtm5crm46`(`serial_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of employee_dispute
-- ----------------------------
INSERT INTO `employee_dispute` VALUES (1, '2026-04-06 07:58:21.168593', '你他妈欠我钱', '2026-04-06', '工资', 8, '方业鑫', '完事了', 'DP202604060001', '已结案');

-- ----------------------------
-- Table structure for finance_expense
-- ----------------------------
DROP TABLE IF EXISTS `finance_expense`;
CREATE TABLE `finance_expense`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(12, 2) NOT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `department_id` bigint NULL DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `expense_time` datetime(6) NOT NULL,
  `serial_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_d4eellpatamdsf28t83lmle15`(`serial_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of finance_expense
-- ----------------------------
INSERT INTO `finance_expense` VALUES (1, 1000000.00, '2026-04-01 09:03:44.427819', 2, '公司合作', '2026-04-01 09:03:09.000000', 'EXP202604010001');
INSERT INTO `finance_expense` VALUES (12, 100000.00, '2026-04-02 08:43:25.074746', 4, '采购办公用品', '2026-04-02 08:42:50.000000', 'EXP202604020002');
INSERT INTO `finance_expense` VALUES (13, 10000.00, '2026-04-02 08:44:24.177036', 4, '公司团建支出', '2026-04-02 08:44:03.000000', 'EXP202604020013');
INSERT INTO `finance_expense` VALUES (14, 100000.00, '2026-04-02 08:45:13.330193', 4, '投放广告', '2026-04-02 08:44:38.000000', 'EXP202604020014');

-- ----------------------------
-- Table structure for insured_city
-- ----------------------------
DROP TABLE IF EXISTS `insured_city`;
CREATE TABLE `insured_city`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `region_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `social_avg_salary` decimal(38, 2) NULL DEFAULT NULL,
  `housing_fund_company_rate` decimal(8, 4) NULL DEFAULT NULL,
  `housing_fund_personal_rate` decimal(8, 4) NULL DEFAULT NULL,
  `injury_company_rate` decimal(8, 4) NULL DEFAULT NULL,
  `maternity_company_rate` decimal(8, 4) NULL DEFAULT NULL,
  `medical_company_rate` decimal(8, 4) NULL DEFAULT NULL,
  `medical_personal_rate` decimal(8, 4) NULL DEFAULT NULL,
  `pension_company_rate` decimal(8, 4) NULL DEFAULT NULL,
  `pension_personal_rate` decimal(8, 4) NULL DEFAULT NULL,
  `unemployment_company_rate` decimal(8, 4) NULL DEFAULT NULL,
  `unemployment_personal_rate` decimal(8, 4) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of insured_city
-- ----------------------------
INSERT INTO `insured_city` VALUES (1, '上海市', '310100', '', 10000.00, 0.0800, 0.0800, 0.0020, 0.0300, 0.0950, 0.0300, 0.1800, 0.0800, 0.0070, 0.0050);
INSERT INTO `insured_city` VALUES (2, '青岛市', '266000', '', 10000.00, 0.0700, 0.0700, 0.0020, 0.0100, 0.0950, 0.0200, 0.1600, 0.0800, 0.0050, 0.0050);

-- ----------------------------
-- Table structure for kaichu
-- ----------------------------
DROP TABLE IF EXISTS `kaichu`;
CREATE TABLE `kaichu`  (
  `kaichu` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kaichu
-- ----------------------------

-- ----------------------------
-- Table structure for leave_request
-- ----------------------------
DROP TABLE IF EXISTS `leave_request`;
CREATE TABLE `leave_request`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `approve_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `approve_time` datetime(6) NULL DEFAULT NULL,
  `approver_id` bigint NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `end_time` datetime(6) NULL DEFAULT NULL,
  `leave_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reason` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `start_time` datetime(6) NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKo1dvkcugt9k5fixhh2tglb1n2`(`user_id` ASC) USING BTREE,
  CONSTRAINT `FKo1dvkcugt9k5fixhh2tglb1n2` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of leave_request
-- ----------------------------
INSERT INTO `leave_request` VALUES (1, '不同意', '2026-04-06 07:52:23.439822', 2, '2026-04-01 03:55:21.716485', '2026-04-01 11:55:21.716485', '事假', '示例请假待审批', '2026-04-01 03:55:21.716485', '已驳回', 3);
INSERT INTO `leave_request` VALUES (2, '同意', '2026-04-01 08:23:41.987414', 1, '2026-04-01 08:21:59.983997', '2026-04-01 08:21:32.816000', '事假', '', '2026-04-01 08:21:32.816000', '已通过', 5);
INSERT INTO `leave_request` VALUES (3, '同意', '2026-04-06 07:52:12.582357', 2, '2026-04-02 01:19:03.524003', '2026-04-03 01:18:49.000000', '事假', '', '2026-04-02 01:18:49.973000', '已通过', 6);

-- ----------------------------
-- Table structure for performance_review
-- ----------------------------
DROP TABLE IF EXISTS `performance_review`;
CREATE TABLE `performance_review`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assessment_period` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `employee_id` bigint NOT NULL,
  `employee_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `evaluator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `grade` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `score` decimal(6, 2) NOT NULL,
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of performance_review
-- ----------------------------
INSERT INTO `performance_review` VALUES (3, '2026-Q1', '2026-04-04 10:34:16.841579', 3, '赵员工', '', 'C', '', 70.00, '待确认');
INSERT INTO `performance_review` VALUES (4, '2026-01', '2026-04-06 08:31:44.473845', 9, '吉吉国王', '王人事', 'A', '', 100.00, '已确认');

-- ----------------------------
-- Table structure for promotion_plan
-- ----------------------------
DROP TABLE IF EXISTS `promotion_plan`;
CREATE TABLE `promotion_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `current_position` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `employee_id` bigint NOT NULL,
  `employee_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `planned_date` date NOT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_position` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of promotion_plan
-- ----------------------------

-- ----------------------------
-- Table structure for recruitment_candidate
-- ----------------------------
DROP TABLE IF EXISTS `recruitment_candidate`;
CREATE TABLE `recruitment_candidate`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `education` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `email` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `expected_salary` decimal(12, 2) NULL DEFAULT NULL,
  `interview_time` datetime(6) NULL DEFAULT NULL,
  `interviewer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `result` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `resume_remark` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `source_channel` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `position_id` bigint NULL DEFAULT NULL,
  `referral_time` datetime(6) NULL DEFAULT NULL,
  `referrer_employee_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `referrer_id` bigint NULL DEFAULT NULL,
  `referrer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `resume_file_id` bigint NULL DEFAULT NULL,
  `resume_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK4jw0fvonujm3inad54uv97quu`(`position_id` ASC) USING BTREE,
  CONSTRAINT `FK4jw0fvonujm3inad54uv97quu` FOREIGN KEY (`position_id`) REFERENCES `recruitment_position` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of recruitment_candidate
-- ----------------------------
INSERT INTO `recruitment_candidate` VALUES (1, '2026-04-03 00:31:52.166437', '研究生', '1888888888@qq.com', 17000.00, NULL, NULL, '李华', '18888888888', NULL, '待处理', NULL, '员工内推', '已发Offer', '2026-04-03 07:07:11.822417', 12, '2026-04-03 00:31:52.161754', 'E005', 6, '邵珠峰', NULL, NULL);
INSERT INTO `recruitment_candidate` VALUES (2, '2026-04-06 08:19:18.501911', '大专', '1@1.com', 1111171000.00, '2026-04-06 16:20:00.000000', '杨志平', '吉吉国王', '12312312312', '招我就完了', '待处理', '不招我你们会后悔的', '员工内推', '已入职', '2026-04-06 08:28:41.121125', 12, '2026-04-06 08:19:18.501911', '123123', 9, '吉吉国王', NULL, NULL);

-- ----------------------------
-- Table structure for recruitment_position
-- ----------------------------
DROP TABLE IF EXISTS `recruitment_position`;
CREATE TABLE `recruitment_position`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `close_time` datetime(6) NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `department_id` bigint NULL DEFAULT NULL,
  `filled_count` int NULL DEFAULT NULL,
  `job_description` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `job_requirements` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `planned_count` int NULL DEFAULT NULL,
  `position_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `position_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `publish_time` datetime(6) NULL DEFAULT NULL,
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `requirement_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_petihogs23a4y75v8a4n6v6cn`(`position_code` ASC) USING BTREE,
  INDEX `FK4469ui77l5btli557s7cx0wns`(`requirement_id` ASC) USING BTREE,
  CONSTRAINT `FK4469ui77l5btli557s7cx0wns` FOREIGN KEY (`requirement_id`) REFERENCES `recruitment_requirement` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of recruitment_position
-- ----------------------------
INSERT INTO `recruitment_position` VALUES (12, NULL, '2026-04-02 09:22:44.099263', 1, 1, '', '', 3, 'POS202604020001', '软件开发师', '2026-04-02 09:22:44.099263', NULL, '招聘中', '2026-04-06 08:28:41.125400', 9);
INSERT INTO `recruitment_position` VALUES (13, NULL, '2026-04-03 07:06:49.551107', 1, 1, '', '', 3, 'POS202604030013', '软件开发师', '2026-04-03 07:06:49.550107', NULL, '招聘中', '2026-04-03 07:06:49.551107', 9);

-- ----------------------------
-- Table structure for recruitment_requirement
-- ----------------------------
DROP TABLE IF EXISTS `recruitment_requirement`;
CREATE TABLE `recruitment_requirement`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `applicant_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `approve_time` datetime(6) NULL DEFAULT NULL,
  `approver_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `budget_salary_max` decimal(12, 2) NULL DEFAULT NULL,
  `budget_salary_min` decimal(12, 2) NULL DEFAULT NULL,
  `completed_count` int NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `department_id` bigint NULL DEFAULT NULL,
  `expected_onboard_date` date NULL DEFAULT NULL,
  `headcount` int NULL DEFAULT NULL,
  `job_description` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `job_title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `requirement_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `applicant_user_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_2uc5kmptv0ajkl6562peeicd1`(`requirement_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of recruitment_requirement
-- ----------------------------
INSERT INTO `recruitment_requirement` VALUES (9, '王人事', '2026-04-02 09:22:43.255590', '王人事', 20000.00, 15000.00, 2, '2026-04-02 09:22:41.875805', 1, '2026-04-04', 3, '', '软件开发师', '', '', 'REQ202604020001', '招聘中', '2026-04-06 08:28:41.129581', 2);

-- ----------------------------
-- Table structure for salary_record
-- ----------------------------
DROP TABLE IF EXISTS `salary_record`;
CREATE TABLE `salary_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `base_salary` decimal(38, 2) NULL DEFAULT NULL,
  `housing_fund_base` decimal(38, 2) NULL DEFAULT NULL,
  `housing_fund_company` decimal(38, 2) NULL DEFAULT NULL,
  `housing_fund_personal` decimal(38, 2) NULL DEFAULT NULL,
  `injury_company` decimal(38, 2) NULL DEFAULT NULL,
  `maternity_company` decimal(38, 2) NULL DEFAULT NULL,
  `medical_company` decimal(38, 2) NULL DEFAULT NULL,
  `medical_personal` decimal(38, 2) NULL DEFAULT NULL,
  `pension_company` decimal(38, 2) NULL DEFAULT NULL,
  `pension_personal` decimal(38, 2) NULL DEFAULT NULL,
  `remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `salary_month` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `social_security_base` decimal(38, 2) NULL DEFAULT NULL,
  `unemployment_company` decimal(38, 2) NULL DEFAULT NULL,
  `unemployment_personal` decimal(38, 2) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `insured_city_id` bigint NULL DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKqoq3mkv25qguepu6pcv2855w3`(`insured_city_id` ASC) USING BTREE,
  INDEX `FKmyexigixdo4p19564i0jisppn`(`user_id` ASC) USING BTREE,
  CONSTRAINT `FKmyexigixdo4p19564i0jisppn` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKqoq3mkv25qguepu6pcv2855w3` FOREIGN KEY (`insured_city_id`) REFERENCES `insured_city` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of salary_record
-- ----------------------------
INSERT INTO `salary_record` VALUES (1, 15000.00, 12000.00, 960.00, 960.00, 24.00, 360.00, 1140.00, 360.00, 2160.00, 960.00, '', '2026-04', 12000.00, 84.00, 60.00, '2026-04-02 02:33:48.117993', 1, 3);
INSERT INTO `salary_record` VALUES (2, 10000.00, 9000.00, 720.00, 720.00, 18.00, 270.00, 855.00, 270.00, 1620.00, 720.00, '', '2026-04', 9000.00, 63.00, 45.00, '2026-04-04 08:56:41.085632', 1, 5);
INSERT INTO `salary_record` VALUES (3, 15000.00, 12000.00, 840.00, 840.00, 24.00, 120.00, 1140.00, 240.00, 1920.00, 960.00, '', '2026-04', 12000.00, 60.00, 60.00, '2026-04-02 02:27:51.242588', 2, 6);
INSERT INTO `salary_record` VALUES (5, 100.00, 100.00, 7.00, 7.00, 0.20, 1.00, 9.50, 2.00, 16.00, 8.00, '', '2026-04', 100.00, 0.50, 0.50, '2026-04-06 08:29:57.001558', 2, 8);

-- ----------------------------
-- Table structure for sys_file
-- ----------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `relative_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `size_bytes` bigint NULL DEFAULT NULL,
  `stored_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `uploader_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_file
-- ----------------------------
INSERT INTO `sys_file` VALUES (1, '2026-04-01 06:09:05.545514', 'image.png', '629cacaa-e607-41d4-9c13-bc01a7f94d22.png', 1321644, '629cacaa-e607-41d4-9c13-bc01a7f94d22.png', 1);
INSERT INTO `sys_file` VALUES (2, '2026-04-02 02:35:18.868196', '企业文化.ppt', '0359dc6f-e8be-40e7-969e-a48c1d354e70.ppt', 20992, '0359dc6f-e8be-40e7-969e-a48c1d354e70.ppt', 1);
INSERT INTO `sys_file` VALUES (3, '2026-04-06 07:50:36.346076', '新建 文本文档.txt', 'b5604c83-3674-408c-aa5b-0bc35c7cb9fc.txt', 80, 'b5604c83-3674-408c-aa5b-0bc35c7cb9fc.txt', 2);
INSERT INTO `sys_file` VALUES (4, '2026-04-06 08:15:14.525079', '新建 文本文档.txt', '5716a4d7-16ee-47dd-bc7c-cfe5fbac1f22.txt', 80, '5716a4d7-16ee-47dd-bc7c-cfe5fbac1f22.txt', 9);

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `parent_id` bigint NULL DEFAULT NULL,
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sort_order` int NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, 'HomeFilled', 0, '/home', 1, '首页');
INSERT INTO `sys_menu` VALUES (2, 'Setting', 0, '/system', 10, '系统管理');
INSERT INTO `sys_menu` VALUES (3, 'Folder', 2, '/system/files', 11, '文件管理');
INSERT INTO `sys_menu` VALUES (4, 'User', 2, '/system/employees', 12, '员工管理');
INSERT INTO `sys_menu` VALUES (5, 'OfficeBuilding', 2, '/system/departments', 13, '部门管理');
INSERT INTO `sys_menu` VALUES (6, 'Lock', 0, '/permission', 20, '权限管理');
INSERT INTO `sys_menu` VALUES (7, 'Avatar', 6, '/permission/roles', 21, '角色管理');
INSERT INTO `sys_menu` VALUES (8, 'Menu', 6, '/permission/menus', 22, '菜单管理');
INSERT INTO `sys_menu` VALUES (9, 'Money', 0, '/salary', 30, '薪资管理');
INSERT INTO `sys_menu` VALUES (10, 'Document', 9, '/salary/records', 31, '五险一金与工资');
INSERT INTO `sys_menu` VALUES (11, 'Location', 9, '/salary/cities', 32, '参保城市');
INSERT INTO `sys_menu` VALUES (12, 'Clock', 0, '/attendance', 40, '考勤管理');
INSERT INTO `sys_menu` VALUES (13, 'Calendar', 12, '/attendance/leaves', 41, '请假审批');
INSERT INTO `sys_menu` VALUES (14, 'List', 12, '/attendance/records', 42, '考勤记录');
INSERT INTO `sys_menu` VALUES (19, 'User', 0, '/recruitment', 60, '招聘管理');
INSERT INTO `sys_menu` VALUES (20, 'Document', 19, '/recruitment/requirements', 61, '招聘需求');
INSERT INTO `sys_menu` VALUES (21, 'OfficeBuilding', 19, '/recruitment/positions', 62, '招聘职位');
INSERT INTO `sys_menu` VALUES (22, 'Avatar', 19, '/recruitment/candidates', 63, '候选人管理');
INSERT INTO `sys_menu` VALUES (23, 'User', 19, '/recruitment/referrals', 64, '内推记录');
INSERT INTO `sys_menu` VALUES (24, 'Calendar', 0, '/care', 70, '员工关怀');
INSERT INTO `sys_menu` VALUES (25, 'Document', 24, '/care/plans', 71, '关怀计划');
INSERT INTO `sys_menu` VALUES (27, 'DataAnalysis', 24, '/care/stats', 73, '关怀统计');
INSERT INTO `sys_menu` VALUES (28, 'OfficeBuilding', 0, '/training', 80, '培训发展');
INSERT INTO `sys_menu` VALUES (29, 'Calendar', 28, '/training/sessions', 81, '组织培训');
INSERT INTO `sys_menu` VALUES (30, 'DataAnalysis', 28, '/training/promotions', 82, '规划员工晋升');
INSERT INTO `sys_menu` VALUES (31, 'DataAnalysis', 9, '/salary/performance', 33, '绩效考核');
INSERT INTO `sys_menu` VALUES (32, 'User', 0, '/relations', 70, '员工关系');
INSERT INTO `sys_menu` VALUES (33, 'Document', 32, '/relations/contracts', 71, '签合同');
INSERT INTO `sys_menu` VALUES (34, 'Avatar', 32, '/relations/disputes', 72, '处理纠纷');
INSERT INTO `sys_menu` VALUES (35, 'Location', 12, '/attendance/trips', 43, '出差申请');

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `priority` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_notice
-- ----------------------------
INSERT INTO `sys_notice` VALUES (1, '请使用 Navicat 连接 MySQL 数据库 hrms 进行数据维护。', '2026-04-01 03:55:21.704484', 'high', '欢迎使用人力资源管理系统');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 'ADMIN', '全部功能', '系统管理员');
INSERT INTO `sys_role` VALUES (2, 'HR', '人事与考勤薪资', '人事');
INSERT INTO `sys_role` VALUES (3, 'EMP', '个人考勤与申请', '员工');

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `role_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`, `menu_id`) USING BTREE,
  INDEX `FKf3mud4qoc7ayew8nml4plkevo`(`menu_id` ASC) USING BTREE,
  CONSTRAINT `FKf3mud4qoc7ayew8nml4plkevo` FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKkeitxsgxwayackgqllio4ohn5` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (1, 1);
INSERT INTO `sys_role_menu` VALUES (2, 1);
INSERT INTO `sys_role_menu` VALUES (3, 1);
INSERT INTO `sys_role_menu` VALUES (1, 2);
INSERT INTO `sys_role_menu` VALUES (2, 2);
INSERT INTO `sys_role_menu` VALUES (1, 3);
INSERT INTO `sys_role_menu` VALUES (2, 3);
INSERT INTO `sys_role_menu` VALUES (1, 4);
INSERT INTO `sys_role_menu` VALUES (2, 4);
INSERT INTO `sys_role_menu` VALUES (1, 5);
INSERT INTO `sys_role_menu` VALUES (2, 5);
INSERT INTO `sys_role_menu` VALUES (1, 6);
INSERT INTO `sys_role_menu` VALUES (1, 7);
INSERT INTO `sys_role_menu` VALUES (1, 8);
INSERT INTO `sys_role_menu` VALUES (1, 9);
INSERT INTO `sys_role_menu` VALUES (2, 9);
INSERT INTO `sys_role_menu` VALUES (3, 9);
INSERT INTO `sys_role_menu` VALUES (1, 10);
INSERT INTO `sys_role_menu` VALUES (2, 10);
INSERT INTO `sys_role_menu` VALUES (3, 10);
INSERT INTO `sys_role_menu` VALUES (1, 11);
INSERT INTO `sys_role_menu` VALUES (2, 11);
INSERT INTO `sys_role_menu` VALUES (1, 12);
INSERT INTO `sys_role_menu` VALUES (2, 12);
INSERT INTO `sys_role_menu` VALUES (3, 12);
INSERT INTO `sys_role_menu` VALUES (1, 13);
INSERT INTO `sys_role_menu` VALUES (2, 13);
INSERT INTO `sys_role_menu` VALUES (3, 13);
INSERT INTO `sys_role_menu` VALUES (1, 14);
INSERT INTO `sys_role_menu` VALUES (2, 14);
INSERT INTO `sys_role_menu` VALUES (3, 14);
INSERT INTO `sys_role_menu` VALUES (1, 19);
INSERT INTO `sys_role_menu` VALUES (2, 19);
INSERT INTO `sys_role_menu` VALUES (3, 19);
INSERT INTO `sys_role_menu` VALUES (1, 20);
INSERT INTO `sys_role_menu` VALUES (2, 20);
INSERT INTO `sys_role_menu` VALUES (1, 21);
INSERT INTO `sys_role_menu` VALUES (2, 21);
INSERT INTO `sys_role_menu` VALUES (3, 21);
INSERT INTO `sys_role_menu` VALUES (1, 22);
INSERT INTO `sys_role_menu` VALUES (2, 22);
INSERT INTO `sys_role_menu` VALUES (3, 23);
INSERT INTO `sys_role_menu` VALUES (1, 24);
INSERT INTO `sys_role_menu` VALUES (2, 24);
INSERT INTO `sys_role_menu` VALUES (1, 25);
INSERT INTO `sys_role_menu` VALUES (2, 25);
INSERT INTO `sys_role_menu` VALUES (1, 27);
INSERT INTO `sys_role_menu` VALUES (2, 27);
INSERT INTO `sys_role_menu` VALUES (1, 28);
INSERT INTO `sys_role_menu` VALUES (2, 28);
INSERT INTO `sys_role_menu` VALUES (3, 28);
INSERT INTO `sys_role_menu` VALUES (1, 29);
INSERT INTO `sys_role_menu` VALUES (2, 29);
INSERT INTO `sys_role_menu` VALUES (3, 29);
INSERT INTO `sys_role_menu` VALUES (1, 30);
INSERT INTO `sys_role_menu` VALUES (2, 30);
INSERT INTO `sys_role_menu` VALUES (1, 31);
INSERT INTO `sys_role_menu` VALUES (2, 31);
INSERT INTO `sys_role_menu` VALUES (1, 32);
INSERT INTO `sys_role_menu` VALUES (2, 32);
INSERT INTO `sys_role_menu` VALUES (3, 32);
INSERT INTO `sys_role_menu` VALUES (1, 33);
INSERT INTO `sys_role_menu` VALUES (2, 33);
INSERT INTO `sys_role_menu` VALUES (3, 33);
INSERT INTO `sys_role_menu` VALUES (1, 34);
INSERT INTO `sys_role_menu` VALUES (2, 34);
INSERT INTO `sys_role_menu` VALUES (3, 34);
INSERT INTO `sys_role_menu` VALUES (1, 35);
INSERT INTO `sys_role_menu` VALUES (2, 35);
INSERT INTO `sys_role_menu` VALUES (3, 35);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `department_id` bigint NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `employee_no` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `real_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` int NULL DEFAULT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `role_id` bigint NULL DEFAULT NULL,
  `birthday` date NULL DEFAULT NULL,
  `hire_date` date NULL DEFAULT NULL,
  `position_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_51bvuyvihefoh4kp5syh2jpi4`(`username` ASC) USING BTREE,
  INDEX `FK4dm5kxn3potpfgdigehw7pdyu`(`role_id` ASC) USING BTREE,
  CONSTRAINT `FK4dm5kxn3potpfgdigehw7pdyu` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, '2026-04-01 03:55:21.695127', 1, 'admin@company.com', 'E001', '$2a$10$ra6rdgv0TgDDaiVtMBo1JuFZMuI1uanygq4UIaitcLNf8Q9YU.fne', '13800000000', '系统管理员', 1, 'admin', 1, '2026-04-10', '2020-04-03', NULL);
INSERT INTO `sys_user` VALUES (2, '2026-04-01 03:55:21.700465', 1, 'hr@company.com', 'E002', '$2a$10$R.kbN3yTfKLtCRjiQEMXoeJUKQtdB869oG5mklXuNK7/5MCHBvq7y', '13800000000', '王人事', 1, 'hr', 2, '1999-04-11', '2024-04-19', NULL);
INSERT INTO `sys_user` VALUES (3, '2026-04-01 03:55:21.703484', 1, 'emp@company.com', 'E003', '$2a$10$Y9nPq73Z5gX7eQdp6Lqc4O0pfIDXANtNGh39IlGSjY/ewzwY2NFr.', '13800000000', '赵员工', 1, 'emp', 3, '2000-04-12', '2025-04-04', NULL);
INSERT INTO `sys_user` VALUES (5, '2026-04-01 06:08:44.450736', 1, '', 'E004', '$2a$10$QZcxYxGNvSN4Zo3.ocSkbepZdiwT80cCN8l8jo9.17J5bBTd7P4ii', '123456', '周新晟', 1, 'zzz', 3, '2004-04-08', NULL, NULL);
INSERT INTO `sys_user` VALUES (6, '2026-04-01 08:26:52.725214', 3, '', 'E005', '$2a$10$JBgUbRttGmSMJZFFnSSQw.OQONLcJZ7g260Yv6MPXOK2CXAo.p53y', '1887788888', '邵珠峰', 1, 'shao', 3, '2005-05-16', NULL, NULL);
INSERT INTO `sys_user` VALUES (8, '2026-04-06 07:34:26.443224', 1, 'aeowu86@163.com', '112233', '$2a$10$ikpvo0/O1EGNb8bLZoBH/upXtzYy3iF9tcAoEid96blP8XUokrnp.', '1212111223', '方业鑫', 1, 'fang', 3, NULL, NULL, NULL);
INSERT INTO `sys_user` VALUES (9, '2026-04-06 07:55:46.125632', 1, '1@1.com', '123123', '$2a$10$/vrq3ssdso9BuZ1oZ65Rou.ekeG1.ZlsJRbG./gl9qXoocK.C8zEm', '12312312312', '吉吉国王', 1, 'jiji', 3, '2024-11-06', '2026-04-06', NULL);
INSERT INTO `sys_user` VALUES (10, '2026-04-06 08:35:36.753140', 1, '', 'qwee112', '$2a$10$lWNy1.96hJBxyUtMM67CP.7lyFP5OGObPllb4TzIvL8xB9OsAq.gq', '1122334455', '李出事', 1, 'hr2', 2, '2026-04-06', '2026-04-06', NULL);

-- ----------------------------
-- Table structure for training_session
-- ----------------------------
DROP TABLE IF EXISTS `training_session`;
CREATE TABLE `training_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `department_id` bigint NULL DEFAULT NULL,
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `trainer_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `training_time` datetime(6) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of training_session
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
