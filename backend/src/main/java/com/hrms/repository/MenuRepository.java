package com.hrms.repository;

import com.hrms.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findAllByOrderBySortOrderAsc();

    Optional<Menu> findByPath(String path);
}
