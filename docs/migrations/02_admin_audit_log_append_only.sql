-- ==============================================================================
-- HireAI Migration: Append-Only Protection for Administrative Audit Trail
-- File: docs/migrations/02_admin_audit_log_append_only.sql
-- Description: Enforces append-only immutability on admin_audit_logs at the database level.
--              Blocks any UPDATE or DELETE operations via PostgreSQL trigger.
-- ==============================================================================

CREATE OR REPLACE FUNCTION prevent_audit_log_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit log records are immutable and cannot be updated or deleted (operation: %, record id: %)',
        TG_OP, OLD.id;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_admin_audit_log_mutation ON admin_audit_log;

CREATE TRIGGER trg_prevent_admin_audit_log_mutation
BEFORE UPDATE OR DELETE ON admin_audit_log
FOR EACH ROW
EXECUTE FUNCTION prevent_audit_log_mutation();
