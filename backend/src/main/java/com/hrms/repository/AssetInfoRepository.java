package com.hrms.repository;

import com.hrms.entity.AssetInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AssetInfoRepository extends JpaRepository<AssetInfo, Long>, JpaSpecificationExecutor<AssetInfo> {
}
