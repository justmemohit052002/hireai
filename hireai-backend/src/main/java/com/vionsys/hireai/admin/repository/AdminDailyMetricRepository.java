package com.vionsys.hireai.admin.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vionsys.hireai.admin.entity.AdminDailyMetric;

@Repository
public interface AdminDailyMetricRepository extends JpaRepository<AdminDailyMetric, UUID> {

    Optional<AdminDailyMetric> findByMetricDate(LocalDate metricDate);

    List<AdminDailyMetric> findByMetricDateBetweenOrderByMetricDateAsc(LocalDate startDate, LocalDate endDate);

    @Modifying
    @Query("DELETE FROM AdminDailyMetric m WHERE m.metricDate < :cutoffDate")
    int deleteByMetricDateBefore(@Param("cutoffDate") LocalDate cutoffDate);
}
