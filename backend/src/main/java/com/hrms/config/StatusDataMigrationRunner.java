package com.hrms.config;

import com.hrms.entity.AssetInfo;
import com.hrms.entity.AttendanceRecord;
import com.hrms.entity.Department;
import com.hrms.entity.LeaveRequest;
import com.hrms.entity.User;
import com.hrms.repository.AssetInfoRepository;
import com.hrms.repository.AttendanceRecordRepository;
import com.hrms.repository.DepartmentRepository;
import com.hrms.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StatusDataMigrationRunner implements CommandLineRunner {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final DepartmentRepository departmentRepository;
    private final AssetInfoRepository assetInfoRepository;

    @Override
    public void run(String... args) {
        migrateAttendanceStatuses();
        migrateLeaveStatuses();
        migrateAssetStatuses();
    }

    private void migrateAttendanceStatuses() {
        List<AttendanceRecord> records = attendanceRecordRepository.findAll();
        boolean changed = false;
        for (AttendanceRecord record : records) {
            String target = calculateAttendanceStatus(record, resolveDepartment(record.getUser()));
            if (!target.equals(record.getStatus())) {
                record.setStatus(target);
                changed = true;
            }
        }
        if (changed) {
            attendanceRecordRepository.saveAll(records);
        }
    }

    private void migrateLeaveStatuses() {
        List<LeaveRequest> requests = leaveRequestRepository.findAll();
        boolean changed = false;
        for (LeaveRequest request : requests) {
            String mapped = mapLeaveStatus(request.getStatus());
            if (!mapped.equals(request.getStatus())) {
                request.setStatus(mapped);
                changed = true;
            }
        }
        if (changed) {
            leaveRequestRepository.saveAll(requests);
        }
    }

    private void migrateAssetStatuses() {
        List<AssetInfo> assets = assetInfoRepository.findAll();
        boolean changed = false;
        for (AssetInfo asset : assets) {
            String mapped = mapAssetStatus(asset.getStatus(), asset.getQuantity());
            if (!mapped.equals(asset.getStatus())) {
                asset.setStatus(mapped);
                changed = true;
            }
        }
        if (changed) {
            assetInfoRepository.saveAll(assets);
        }
    }

    private Department resolveDepartment(User user) {
        if (user == null || user.getDepartmentId() == null) {
            return null;
        }
        return departmentRepository.findById(user.getDepartmentId()).orElse(null);
    }

    private String calculateAttendanceStatus(AttendanceRecord rec, Department department) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalTime startTime = parseWorkTime(department != null ? department.getWorkStartTime() : null, LocalTime.of(9, 0));
        LocalTime endTime = parseWorkTime(department != null ? department.getWorkEndTime() : null, LocalTime.of(18, 0));
        LocalDate day = rec.getAttDate() != null ? rec.getAttDate() : LocalDate.now(zoneId);

        LocalDateTime workStart = LocalDateTime.of(day, startTime);
        LocalDateTime workEnd = LocalDateTime.of(day, endTime);
        LocalDateTime clockIn = toLocalDateTime(rec.getClockIn(), zoneId);
        LocalDateTime clockOut = toLocalDateTime(rec.getClockOut(), zoneId);

        boolean late = clockIn != null && clockIn.isAfter(workStart);
        boolean earlyLeave = clockOut != null && clockOut.isBefore(workEnd);

        if (clockIn == null && clockOut == null) {
            return "未打卡";
        }
        if (clockIn != null && clockOut == null) {
            return late ? "迟到待下班" : "上班正常";
        }
        if (clockIn == null) {
            return earlyLeave ? "缺上班卡且早退" : "缺上班卡";
        }
        if (late && earlyLeave) {
            return "迟到早退";
        }
        if (late) {
            return "迟到";
        }
        if (earlyLeave) {
            return "早退";
        }
        return "正常";
    }

    private String mapLeaveStatus(String status) {
        if (status == null || status.isBlank()) {
            return "待审批";
        }
        return switch (status) {
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已驳回";
            case "normal" -> "正常";
            case "late" -> "迟到";
            case "absent" -> "缺勤";
            case "leave" -> "请假";
            default -> status;
        };
    }

    private String mapAssetStatus(String status, Integer quantity) {
        int remain = quantity != null ? quantity : 0;
        if (remain <= 0) {
            return "已领完";
        }
        if (status == null || status.isBlank() || "在用".equals(status)) {
            return "在库";
        }
        return status;
    }

    private LocalTime parseWorkTime(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalTime.parse(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(instant, zoneId).toLocalDateTime();
    }
}
