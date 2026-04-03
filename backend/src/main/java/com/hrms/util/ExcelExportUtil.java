package com.hrms.util;

import com.hrms.entity.AttendanceRecord;
import com.hrms.entity.EmployeeCarePlan;
import com.hrms.entity.SalaryRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ExcelExportUtil {

    private ExcelExportUtil() {
    }

    public static byte[] salaryMonthReport(String month, List<SalaryRecord> rows) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("工资-" + month);
            Row h = sh.createRow(0);
            String[] cols = {
                    "员工", "工号", "月份", "基本工资",
                    "养老个人", "养老企业", "医疗个人", "医疗企业",
                    "失业个人", "失业企业", "工伤企业", "生育企业",
                    "公积金个人", "公积金企业", "社保基数", "公积金基数",
                    "参保城市", "备注"
            };
            for (int i = 0; i < cols.length; i++) {
                h.createCell(i).setCellValue(cols[i]);
            }

            int r = 1;
            for (SalaryRecord s : rows) {
                Row row = sh.createRow(r++);
                int c = 0;
                row.createCell(c++).setCellValue(s.getUser() != null ? text(s.getUser().getRealName()) : "");
                row.createCell(c++).setCellValue(s.getUser() != null ? text(s.getUser().getEmployeeNo()) : "");
                row.createCell(c++).setCellValue(text(s.getSalaryMonth()));
                row.createCell(c++).setCellValue(number(s.getBaseSalary()));
                row.createCell(c++).setCellValue(number(s.getPensionPersonal()));
                row.createCell(c++).setCellValue(number(s.getPensionCompany()));
                row.createCell(c++).setCellValue(number(s.getMedicalPersonal()));
                row.createCell(c++).setCellValue(number(s.getMedicalCompany()));
                row.createCell(c++).setCellValue(number(s.getUnemploymentPersonal()));
                row.createCell(c++).setCellValue(number(s.getUnemploymentCompany()));
                row.createCell(c++).setCellValue(number(s.getInjuryCompany()));
                row.createCell(c++).setCellValue(number(s.getMaternityCompany()));
                row.createCell(c++).setCellValue(number(s.getHousingFundPersonal()));
                row.createCell(c++).setCellValue(number(s.getHousingFundCompany()));
                row.createCell(c++).setCellValue(number(s.getSocialSecurityBase()));
                row.createCell(c++).setCellValue(number(s.getHousingFundBase()));
                row.createCell(c++).setCellValue(s.getInsuredCity() != null ? text(s.getInsuredCity().getName()) : "");
                row.createCell(c++).setCellValue(text(s.getRemark()));
            }

            for (int i = 0; i < cols.length; i++) {
                sh.autoSizeColumn(i);
            }

            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("导出工资月报失败", e);
        }
    }

    public static byte[] attendanceMonthReport(String title, List<AttendanceRecord> rows) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet(title);
            Row h = sh.createRow(0);
            String[] cols = {"员工", "工号", "日期", "上班打卡", "下班打卡", "状态", "备注"};
            for (int i = 0; i < cols.length; i++) {
                h.createCell(i).setCellValue(cols[i]);
            }

            int r = 1;
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
            for (AttendanceRecord a : rows) {
                Row row = sh.createRow(r++);
                int c = 0;
                row.createCell(c++).setCellValue(a.getUser() != null ? text(a.getUser().getRealName()) : "");
                row.createCell(c++).setCellValue(a.getUser() != null ? text(a.getUser().getEmployeeNo()) : "");
                row.createCell(c++).setCellValue(a.getAttDate() != null ? a.getAttDate().toString() : "");
                row.createCell(c++).setCellValue(a.getClockIn() != null ? df.format(a.getClockIn()) : "");
                row.createCell(c++).setCellValue(a.getClockOut() != null ? df.format(a.getClockOut()) : "");
                row.createCell(c++).setCellValue(text(a.getStatus()));
                row.createCell(c++).setCellValue(text(a.getRemark()));
            }

            for (int i = 0; i < cols.length; i++) {
                sh.autoSizeColumn(i);
            }

            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("导出考勤报表失败", e);
        }
    }

    public static byte[] carePlanReport(String title, List<EmployeeCarePlan> rows) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet(title);
            Row h = sh.createRow(0);
            String[] cols = {"计划编号", "关怀员工", "工号", "关怀类型", "计划时间", "预算金额", "状态", "关怀内容", "备注"};
            for (int i = 0; i < cols.length; i++) {
                h.createCell(i).setCellValue(cols[i]);
            }

            int r = 1;
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
            for (EmployeeCarePlan plan : rows) {
                Row row = sh.createRow(r++);
                int c = 0;
                row.createCell(c++).setCellValue(text(plan.getPlanCode()));
                row.createCell(c++).setCellValue(plan.getUser() != null ? text(plan.getUser().getRealName()) : "");
                row.createCell(c++).setCellValue(plan.getUser() != null ? text(plan.getUser().getEmployeeNo()) : "");
                row.createCell(c++).setCellValue(text(plan.getCareType()));
                row.createCell(c++).setCellValue(plan.getPlannedTime() != null ? df.format(plan.getPlannedTime()) : "");
                row.createCell(c++).setCellValue(number(plan.getBudgetAmount()));
                row.createCell(c++).setCellValue(text(plan.getStatus()));
                row.createCell(c++).setCellValue(text(plan.getContent()));
                row.createCell(c++).setCellValue(text(plan.getRemark()));
            }

            for (int i = 0; i < cols.length; i++) {
                sh.autoSizeColumn(i);
            }

            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("导出关怀计划失败", e);
        }
    }

    private static double number(java.math.BigDecimal value) {
        return value != null ? value.doubleValue() : 0D;
    }

    private static String text(String value) {
        return value != null ? value : "";
    }
}
