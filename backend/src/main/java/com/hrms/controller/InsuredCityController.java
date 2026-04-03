package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.InsuredCity;
import com.hrms.entity.SalaryRecord;
import com.hrms.repository.InsuredCityRepository;
import com.hrms.repository.SalaryRecordRepository;
import com.hrms.security.LoginUser;
import com.hrms.service.SalaryCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/insured-cities")
@RequiredArgsConstructor
public class InsuredCityController {

    private final InsuredCityRepository insuredCityRepository;
    private final SalaryRecordRepository salaryRecordRepository;
    private final SalaryCalculationService salaryCalculationService;

    @GetMapping
    public ApiResponse<List<InsuredCity>> list(@AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        List<InsuredCity> list = insuredCityRepository.findAll().stream()
                .sorted(Comparator.comparing(InsuredCity::getId, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList();
        return ApiResponse.ok(list);
    }

    @PostMapping
    @Transactional
    public ApiResponse<InsuredCity> create(@RequestBody InsuredCity body,
                                           @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        salaryCalculationService.normalizeCityRates(body);
        return ApiResponse.ok(insuredCityRepository.save(body));
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<InsuredCity> update(@PathVariable Long id,
                                           @RequestBody InsuredCity body,
                                           @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        InsuredCity city = insuredCityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("参保城市不存在"));
        copy(body, city);
        salaryCalculationService.normalizeCityRates(city);
        InsuredCity saved = insuredCityRepository.save(city);

        List<SalaryRecord> records = salaryRecordRepository.findByInsuredCity_Id(saved.getId());
        for (SalaryRecord record : records) {
            record.setInsuredCity(saved);
            salaryCalculationService.applyCityRules(record);
        }
        salaryRecordRepository.saveAll(records);
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        insuredCityRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private void copy(InsuredCity source, InsuredCity target) {
        target.setName(source.getName());
        target.setRegionCode(source.getRegionCode());
        target.setSocialAvgSalary(source.getSocialAvgSalary());
        target.setPensionPersonalRate(source.getPensionPersonalRate());
        target.setPensionCompanyRate(source.getPensionCompanyRate());
        target.setMedicalPersonalRate(source.getMedicalPersonalRate());
        target.setMedicalCompanyRate(source.getMedicalCompanyRate());
        target.setUnemploymentPersonalRate(source.getUnemploymentPersonalRate());
        target.setUnemploymentCompanyRate(source.getUnemploymentCompanyRate());
        target.setInjuryCompanyRate(source.getInjuryCompanyRate());
        target.setMaternityCompanyRate(source.getMaternityCompanyRate());
        target.setHousingFundPersonalRate(source.getHousingFundPersonalRate());
        target.setHousingFundCompanyRate(source.getHousingFundCompanyRate());
        target.setRemark(source.getRemark());
    }

    private static boolean isAdmin(LoginUser u) {
        return u != null && "ADMIN".equals(u.getRoleCode());
    }

    private static boolean isHr(LoginUser u) {
        return u != null && "HR".equals(u.getRoleCode());
    }

    private static void assertAdmin(LoginUser u) {
        if (!isAdmin(u)) {
            throw new AccessDeniedException("需要管理员权限");
        }
    }

    private static void assertAdminOrHr(LoginUser u) {
        if (!isAdmin(u) && !isHr(u)) {
            throw new AccessDeniedException("需要管理员或人事权限");
        }
    }
}
