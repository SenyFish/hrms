package com.hrms.service;

import com.hrms.dto.ProfileUpdateRequest;
import com.hrms.dto.UserSaveRequest;
import com.hrms.entity.Role;
import com.hrms.entity.User;
import com.hrms.repository.AttendanceRecordRepository;
import com.hrms.repository.BusinessTripRequestRepository;
import com.hrms.repository.EmployeeCarePlanRepository;
import com.hrms.repository.EmployeeCareRecordRepository;
import com.hrms.repository.EmployeeContractRepository;
import com.hrms.repository.EmployeeDisputeRepository;
import com.hrms.repository.LeaveRequestRepository;
import com.hrms.repository.PerformanceReviewRepository;
import com.hrms.repository.PromotionPlanRepository;
import com.hrms.repository.RecruitmentCandidateRepository;
import com.hrms.repository.RecruitmentRequirementRepository;
import com.hrms.repository.RoleRepository;
import com.hrms.repository.SalaryRecordRepository;
import com.hrms.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final BusinessTripRequestRepository businessTripRequestRepository;
    private final SalaryRecordRepository salaryRecordRepository;
    private final EmployeeCareRecordRepository employeeCareRecordRepository;
    private final EmployeeCarePlanRepository employeeCarePlanRepository;
    private final PerformanceReviewRepository performanceReviewRepository;
    private final PromotionPlanRepository promotionPlanRepository;
    private final EmployeeContractRepository employeeContractRepository;
    private final EmployeeDisputeRepository employeeDisputeRepository;
    private final RecruitmentCandidateRepository recruitmentCandidateRepository;
    private final RecruitmentRequirementRepository recruitmentRequirementRepository;

    public List<User> listAll() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public Page<User> listPage(String keyword, int page, int size) {
        return userRepository.findAll(
                keywordSpec(keyword),
                PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id"))
        );
    }

    @Transactional
    public User updateOwnProfile(Long userId, ProfileUpdateRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        String username = req.getUsername().trim();
        userRepository.findByUsername(username).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new IllegalArgumentException("账号已存在");
            }
        });
        user.setUsername(username);
        user.setRealName(req.getRealName().trim());
        user.setPhone(trimToNull(req.getPhone()));
        user.setEmail(trimToNull(req.getEmail()));
        user.setBirthday(parseDate(req.getBirthday()));
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.getPassword().trim()));
        }
        return userRepository.save(user);
    }

    public User get(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    @Transactional
    public User save(UserSaveRequest req) {
        User user;
        if (req.getId() == null) {
            user = new User();
            if (req.getPassword() == null || req.getPassword().isBlank()) {
                throw new IllegalArgumentException("新增用户必须设置密码");
            }
            user.setPassword(passwordEncoder.encode(req.getPassword()));
            userRepository.findByUsername(req.getUsername()).ifPresent(u -> {
                throw new IllegalArgumentException("用户名已存在");
            });
            user.setUsername(req.getUsername());
        } else {
            user = userRepository.findById(req.getId()).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
            if (req.getPassword() != null && !req.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(req.getPassword()));
            }
        }
        user.setRealName(req.getRealName());
        user.setEmployeeNo(req.getEmployeeNo());
        user.setPositionName(trimToNull(req.getPositionName()));
        user.setDepartmentId(req.getDepartmentId());
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setBirthday(parseDate(req.getBirthday()));
        user.setHireDate(parseDate(req.getHireDate()));
        if (req.getStatus() != null) {
            user.setStatus(req.getStatus());
        }
        if (req.getRoleId() != null) {
            Role role = roleRepository.findById(req.getRoleId()).orElseThrow(() -> new IllegalArgumentException("角色不存在"));
            user.setRole(role);
        }
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        employeeCareRecordRepository.deleteByUser_Id(id);
        employeeCarePlanRepository.deleteByUser_Id(id);
        attendanceRecordRepository.deleteByUser_Id(id);
        leaveRequestRepository.deleteByUser_Id(id);
        businessTripRequestRepository.deleteByUser_Id(id);
        salaryRecordRepository.deleteByUser_Id(id);
        performanceReviewRepository.deleteByEmployeeId(id);
        promotionPlanRepository.deleteByEmployeeId(id);
        employeeContractRepository.deleteByEmployeeId(id);
        employeeDisputeRepository.deleteByEmployeeId(id);
        recruitmentCandidateRepository.deleteByReferrerId(id);
        recruitmentRequirementRepository.deleteByApplicantUserId(id);
        userRepository.deleteById(id);
    }

    private Specification<User> keywordSpec(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String value = "%" + keyword.trim() + "%";
            Predicate[] predicates = new Predicate[]{
                    cb.like(root.get("username"), value),
                    cb.like(root.get("realName"), value),
                    cb.like(root.get("employeeNo"), value),
                    cb.like(root.get("positionName"), value),
                    cb.like(root.get("phone"), value),
                    cb.like(root.get("email"), value)
            };
            return cb.or(predicates);
        };
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
