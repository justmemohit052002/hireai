package com.vionsys.hireai.admin.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "admin_daily_metrics",
    indexes = {
        @Index(name = "idx_admin_daily_metric_date", columnList = "metric_date", unique = true)
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDailyMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "metric_date", nullable = false, unique = true)
    private LocalDate metricDate;

    @Column(name = "requests", nullable = false)
    private long requests;

    @Column(name = "client_errors_4xx", nullable = false)
    private long clientErrors4xx;

    @Column(name = "server_errors_5xx", nullable = false)
    private long serverErrors5xx;

    @Column(name = "avg_latency_ms")
    private Double avgLatencyMs;

    @Column(name = "p95_latency_ms")
    private Double p95LatencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
