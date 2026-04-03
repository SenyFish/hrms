package com.hrms.config;

import com.hrms.entity.InsuredCity;
import com.hrms.entity.SalaryRecord;
import com.hrms.repository.InsuredCityRepository;
import com.hrms.repository.SalaryRecordRepository;
import com.hrms.service.SalaryCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SalaryCityRuleMigrationRunner implements CommandLineRunner {

    private final InsuredCityRepository insuredCityRepository;
    private final SalaryRecordRepository salaryRecordRepository;
    private final SalaryCalculationService salaryCalculationService;

    @Override
    public void run(String... args) {
        List<InsuredCity> cities = insuredCityRepository.findAll();
        boolean cityChanged = false;
        for (InsuredCity city : cities) {
            cityChanged |= fillDefaultRates(city);
            salaryCalculationService.normalizeCityRates(city);
        }
        if (cityChanged) {
            insuredCityRepository.saveAll(cities);
        }

        List<SalaryRecord> records = salaryRecordRepository.findAll();
        boolean recordChanged = false;
        for (SalaryRecord record : records) {
            if (record.getInsuredCity() == null) {
                continue;
            }
            salaryCalculationService.applyCityRules(record);
            recordChanged = true;
        }
        if (recordChanged) {
            salaryRecordRepository.saveAll(records);
        }
    }

    private boolean fillDefaultRates(InsuredCity city) {
        boolean changed = false;
        changed |= setIfNull(city::getPensionPersonalRate, city::setPensionPersonalRate, "0.08");
        changed |= setIfNull(city::getPensionCompanyRate, city::setPensionCompanyRate, "0.16");
        changed |= setIfNull(city::getMedicalPersonalRate, city::setMedicalPersonalRate, "0.02");
        changed |= setIfNull(city::getMedicalCompanyRate, city::setMedicalCompanyRate, "0.095");
        changed |= setIfNull(city::getUnemploymentPersonalRate, city::setUnemploymentPersonalRate, "0.005");
        changed |= setIfNull(city::getUnemploymentCompanyRate, city::setUnemploymentCompanyRate, "0.005");
        changed |= setIfNull(city::getInjuryCompanyRate, city::setInjuryCompanyRate, "0.002");
        changed |= setIfNull(city::getMaternityCompanyRate, city::setMaternityCompanyRate, "0.01");
        changed |= setIfNull(city::getHousingFundPersonalRate, city::setHousingFundPersonalRate, "0.07");
        changed |= setIfNull(city::getHousingFundCompanyRate, city::setHousingFundCompanyRate, "0.07");
        if (city.getSocialAvgSalary() == null) {
            city.setSocialAvgSalary(BigDecimal.ZERO);
            changed = true;
        }
        return changed;
    }

    private boolean setIfNull(ValueGetter getter, ValueSetter setter, String defaultValue) {
        if (getter.get() != null) {
            return false;
        }
        setter.set(new BigDecimal(defaultValue));
        return true;
    }

    @FunctionalInterface
    private interface ValueGetter {
        BigDecimal get();
    }

    @FunctionalInterface
    private interface ValueSetter {
        void set(BigDecimal value);
    }
}
