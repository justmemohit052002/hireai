-- ==============================================================================
-- HireAI Migration: Daily Traffic Metrics Persistence
-- File: docs/migrations/01_admin_daily_metrics.sql
-- Description: Creates table admin_daily_metrics to persist aggregated HTTP traffic
--              and error counts across application restarts with 90-day retention.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS admin_daily_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_date DATE NOT NULL UNIQUE,
    requests BIGINT NOT NULL DEFAULT 0,
    client_errors_4xx BIGINT NOT NULL DEFAULT 0,
    server_errors_5xx BIGINT NOT NULL DEFAULT 0,
    avg_latency_ms DOUBLE PRECISION,
    p95_latency_ms DOUBLE PRECISION,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_admin_daily_metric_date ON admin_daily_metrics (metric_date);
