-- ==============================================================================
-- HireAI Database Maintenance: Safe Cleanup of Test Fixture Users
-- File: docs/cleanup-test-users.sql
--
-- IMPORTANT SAFETY NOTICE:
-- 1. Always take a complete database backup before executing data deletion scripts:
--    pg_dump -U postgres -d hireai -F c -b -v -f hireai_backup_$(date +%Y%m%d_%H%M%S).dump
-- 2. This script runs inside a single TRANSACTION and ends with ROLLBACK by default.
-- 3. Review the SELECT outputs first. Only replace ROLLBACK with COMMIT when you have
--    verified the row counts and account lists.
-- 4. Passwords of test accounts: All test-created accounts used dummy non-bcrypt strings
--    ('$2a$10$hashedPasswordHere'). They are unusable for password logins, but for
--    defense-in-depth, deactivate them first if not deleting immediately.
-- ==============================================================================

BEGIN;

-- ------------------------------------------------------------------------------
-- 1. INSPECT: Select all suspected test fixture accounts
-- ------------------------------------------------------------------------------
SELECT 
    u.id,
    u.email,
    u.first_name,
    u.last_name,
    r.name AS role_name,
    u.enabled,
    u.account_non_locked,
    u.created_at,
    u.last_login_at
FROM users u
LEFT JOIN roles r ON u.role_id = r.id
WHERE u.email LIKE 'test-%@vionsys.com'
   OR u.email LIKE 'test_%@vionsys.com'
   OR (u.first_name IN ('Active', 'Locked', 'Demoted') AND u.last_name = 'Admin')
ORDER BY u.created_at DESC;

-- ------------------------------------------------------------------------------
-- 2. INSPECT DEPENDENCIES: Check row counts in dependent tables
-- ------------------------------------------------------------------------------
WITH test_user_ids AS (
    SELECT id FROM users
    WHERE email LIKE 'test-%@vionsys.com'
       OR email LIKE 'test_%@vionsys.com'
       OR (first_name IN ('Active', 'Locked', 'Demoted') AND last_name = 'Admin')
)
SELECT 'refresh_tokens' AS table_name, count(*) AS dependent_rows FROM refresh_tokens WHERE user_id IN (SELECT id FROM test_user_ids)
UNION ALL
SELECT 'password_reset_tokens', count(*) FROM password_reset_tokens WHERE user_id IN (SELECT id FROM test_user_ids)
UNION ALL
SELECT 'email_verification_otps', count(*) FROM email_verification_otps WHERE user_id IN (SELECT id FROM test_user_ids)
UNION ALL
SELECT 'notifications', count(*) FROM notifications WHERE user_id IN (SELECT id FROM test_user_ids)
UNION ALL
SELECT 'direct_chat_participants', count(*) FROM direct_chat_participants WHERE user_id IN (SELECT id FROM test_user_ids)
UNION ALL
SELECT 'recruiter_profiles', count(*) FROM recruiter_profiles WHERE user_id IN (SELECT id FROM test_user_ids)
UNION ALL
SELECT 'candidate_profiles', count(*) FROM candidate_profiles WHERE user_id IN (SELECT id FROM test_user_ids)
UNION ALL
SELECT 'admin_audit_log (actor/target)', count(*) FROM admin_audit_log WHERE actor_user_id IN (SELECT id FROM test_user_ids) OR target_user_id IN (SELECT id FROM test_user_ids);

-- ------------------------------------------------------------------------------
-- 3. IMMEDIATE MITIGATION: Deactivate all suspect accounts prior to deletion
-- ------------------------------------------------------------------------------
UPDATE users
SET enabled = false,
    account_non_locked = false,
    updated_at = NOW()
WHERE email LIKE 'test-%@vionsys.com'
   OR email LIKE 'test_%@vionsys.com'
   OR (first_name IN ('Active', 'Locked', 'Demoted') AND last_name = 'Admin');

-- ------------------------------------------------------------------------------
-- 4. DELETE DEPENDENT ROWS: Cascading deletion order
-- ------------------------------------------------------------------------------
WITH test_user_ids AS (
    SELECT id FROM users
    WHERE email LIKE 'test-%@vionsys.com'
       OR email LIKE 'test_%@vionsys.com'
       OR (first_name IN ('Active', 'Locked', 'Demoted') AND last_name = 'Admin')
)
DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM test_user_ids);

WITH test_user_ids AS (
    SELECT id FROM users
    WHERE email LIKE 'test-%@vionsys.com'
       OR email LIKE 'test_%@vionsys.com'
       OR (first_name IN ('Active', 'Locked', 'Demoted') AND last_name = 'Admin')
)
DELETE FROM password_reset_tokens WHERE user_id IN (SELECT id FROM test_user_ids);

WITH test_user_ids AS (
    SELECT id FROM users
    WHERE email LIKE 'test-%@vionsys.com'
       OR email LIKE 'test_%@vionsys.com'
       OR (first_name IN ('Active', 'Locked', 'Demoted') AND last_name = 'Admin')
)
DELETE FROM email_verification_otps WHERE user_id IN (SELECT id FROM test_user_ids);

WITH test_user_ids AS (
    SELECT id FROM users
    WHERE email LIKE 'test-%@vionsys.com'
       OR email LIKE 'test_%@vionsys.com'
       OR (first_name IN ('Active', 'Locked', 'Demoted') AND last_name = 'Admin')
)
DELETE FROM notifications WHERE user_id IN (SELECT id FROM test_user_ids);

WITH test_user_ids AS (
    SELECT id FROM users
    WHERE email LIKE 'test-%@vionsys.com'
       OR email LIKE 'test_%@vionsys.com'
       OR (first_name IN ('Active', 'Locked', 'Demoted') AND last_name = 'Admin')
)
DELETE FROM direct_chat_participants WHERE user_id IN (SELECT id FROM test_user_ids);

WITH test_user_ids AS (
    SELECT id FROM users
    WHERE email LIKE 'test-%@vionsys.com'
       OR email LIKE 'test_%@vionsys.com'
       OR (first_name IN ('Active', 'Locked', 'Demoted') AND last_name = 'Admin')
)
DELETE FROM recruiter_profiles WHERE user_id IN (SELECT id FROM test_user_ids);

WITH test_user_ids AS (
    SELECT id FROM users
    WHERE email LIKE 'test-%@vionsys.com'
       OR email LIKE 'test_%@vionsys.com'
       OR (first_name IN ('Active', 'Locked', 'Demoted') AND last_name = 'Admin')
)
DELETE FROM candidate_profiles WHERE user_id IN (SELECT id FROM test_user_ids);

-- ------------------------------------------------------------------------------
-- 5. DELETE TEST USER ACCOUNTS
-- ------------------------------------------------------------------------------
DELETE FROM users
WHERE email LIKE 'test-%@vionsys.com'
   OR email LIKE 'test_%@vionsys.com'
   OR (first_name IN ('Active', 'Locked', 'Demoted') AND last_name = 'Admin');

-- ------------------------------------------------------------------------------
-- 6. VERIFY REMAINING COUNT
-- ------------------------------------------------------------------------------
SELECT count(*) AS remaining_real_users FROM users;

-- DEFAULT IS ROLLBACK FOR SAFETY.
-- Change ROLLBACK to COMMIT only after you have reviewed and verified all results.
ROLLBACK;
-- COMMIT;
