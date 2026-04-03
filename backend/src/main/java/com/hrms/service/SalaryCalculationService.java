package com.hrms.service;

import com.hrms.entity.InsuredCity;
import com.hrms.entity.SalaryRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class SalaryCalculationService {

    public void applyCityRules(SalaryRecord record) {
        if (record == null) {
            return;
        }
        InsuredCity city = record.getInsuredCity();
        if (city == null) {
            clearContributions(record);
            return;
        }

        BigDecimal socialBase = n(record.getSocialSecurityBase());
        BigDecimal fundBase = n(record.getHousingFundBase());

        record.setPensionPersonal(calc(socialBase, city.getPensionPersonalRate()));
        record.setPensionCompany(calc(socialBase, city.getPensionCompanyRate()));
        record.setMedicalPersonal(calc(socialBase, city.getMedicalPersonalRate()));
        record.setMedicalCompany(calc(socialBase, city.getMedicalCompanyRate()));
        record.setUnemploymentPersonal(calc(socialBase, city.getUnemploymentPersonalRate()));
        record.setUnemploymentCompany(calc(socialBase, city.getUnemploymentCompanyRate()));
        record.setInjuryCompany(calc(socialBase, city.getInjuryCompanyRate()));
        record.setMaternityCompany(calc(socialBase, city.getMaternityCompanyRate()));
        record.setHousingFundPersonal(calc(fundBase, city.getHousingFundPersonalRate()));
        record.setHousingFundCompany(calc(fundBase, city.getHousingFundCompanyRate()));
    }

    public void normalizeCityRates(InsuredCity city) {
        if (city == null) {
            return;
        }
        city.setSocialAvgSalary(n(city.getSocialAvgSalary()));
        city.setPensionPersonalRate(rate(city.getPensionPersonalRate()));
        city.setPensionCompanyRate(rate(city.getPensionCompanyRate()));
        city.setMedicalPersonalRate(rate(city.getMedicalPersonalRate()));
        city.setMedicalCompanyRate(rate(city.getMedicalCompanyRate()));
        city.setUnemploymentPersonalRate(rate(city.getUnemploymentPersonalRate()));
        city.setUnemploymentCompanyRate(rate(city.getUnemploymentCompanyRate()));
        city.setInjuryCompanyRate(rate(city.getInjuryCompanyRate()));
        city.setMaternityCompanyRate(rate(city.getMaternityCompanyRate()));
        city.setHousingFundPersonalRate(rate(city.getHousingFundPersonalRate()));
        city.setHousingFundCompanyRate(rate(city.getHousingFundCompanyRate()));
    }

    private void clearContributions(SalaryRecord record) {
        record.setPensionPersonal(BigDecimal.ZERO);
        record.setPensionCompany(BigDecimal.ZERO);
        record.setMedicalPersonal(BigDecimal.ZERO);
        record.setMedicalCompany(BigDecimal.ZERO);
        record.setUnemploymentPersonal(BigDecimal.ZERO);
        record.setUnemploymentCompany(BigDecimal.ZERO);
        record.setInjuryCompany(BigDecimal.ZERO);
        record.setMaternityCompany(BigDecimal.ZERO);
        record.setHousingFundPersonal(BigDecimal.ZERO);
        record.setHousingFundCompany(BigDecimal.ZERO);
    }

    private BigDecimal calc(BigDecimal base, BigDecimal rate) {
        return n(base).multiply(rate(rate)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal n(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
