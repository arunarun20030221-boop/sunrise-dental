-- =====================================================================================
-- Sunrise Dental Clinic - stored functions and trigger
--
-- IMPORTANT: statements in this file are separated by ';;', not ';'. PL/pgSQL function
-- bodies are dollar-quoted and contain their own semicolons, which the default script
-- splitter would treat as statement boundaries and cut the function in half. See
-- PersistenceConfig#schemaBootstrap.
--
-- All definitions use CREATE OR REPLACE so the file is idempotent.
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- Next appointment number, formatted APT-<year>-<6 digits>, e.g. APT-2026-000042.
-- Draws from the atomic sequence so concurrent registrations cannot collide.
-- -------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION next_appointment_number()
RETURNS VARCHAR(20)
LANGUAGE plpgsql
AS $$
DECLARE
    next_value BIGINT;
BEGIN
    SELECT nextval('appointment_number_seq') INTO next_value;
    RETURN 'APT-' || TO_CHAR(NOW(), 'YYYY') || '-' || LPAD(next_value::TEXT, 6, '0');
END;
$$;;

-- -------------------------------------------------------------------------------------
-- Revenue by treatment type over a period.
--
-- A decision-support report: which treatments actually earn the clinic money. Aggregated in
-- the database so the application transfers a handful of summary rows rather than every bill
-- in the period. LEFT JOINs keep treatments with no bookings visible as zero rows, which is
-- itself useful information for the clinic.
-- -------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION revenue_by_treatment(from_date DATE, to_date DATE)
RETURNS TABLE (
    treatment_name    VARCHAR(100),
    appointment_count BIGINT,
    total_revenue     NUMERIC(12,2),
    average_bill      NUMERIC(12,2)
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT t.name,
           COUNT(b.id),
           COALESCE(SUM(b.total), 0)::NUMERIC(12,2),
           COALESCE(ROUND(AVG(b.total), 2), 0)::NUMERIC(12,2)
    FROM treatment_type t
             LEFT JOIN appointment a ON a.treatment_type_id = t.id
                 AND a.appointment_date BETWEEN from_date AND to_date
             LEFT JOIN bill b ON b.appointment_id = a.id
    GROUP BY t.name
    ORDER BY 3 DESC;
END;
$$;;

-- -------------------------------------------------------------------------------------
-- Dentist workload over a period: who is overbooked and who has capacity.
-- Uses FILTER to produce one row per dentist with a breakdown by status in a single pass.
-- -------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION dentist_workload(from_date DATE, to_date DATE)
RETURNS TABLE (
    dentist_name  VARCHAR(100),
    booked        BIGINT,
    attended      BIGINT,
    cancelled     BIGINT,
    no_show       BIGINT,
    total_minutes BIGINT
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT d.name,
           COUNT(*) FILTER (WHERE a.status = 'BOOKED'),
           COUNT(*) FILTER (WHERE a.status = 'ATTENDED'),
           COUNT(*) FILTER (WHERE a.status = 'CANCELLED'),
           COUNT(*) FILTER (WHERE a.status = 'NO_SHOW'),
           COALESCE(SUM(t.duration_minutes * a.session_count)
                    FILTER (WHERE a.status IN ('BOOKED', 'ATTENDED')), 0)::BIGINT
    FROM dentist d
             LEFT JOIN appointment a ON a.dentist_id = d.id
                 AND a.appointment_date BETWEEN from_date AND to_date
             LEFT JOIN treatment_type t ON t.id = a.treatment_type_id
    GROUP BY d.name
    ORDER BY 6 DESC;
END;
$$;;

-- -------------------------------------------------------------------------------------
-- Trigger function: record every appointment status change.
--
-- Implements the business rule "every status change must be traceable" in the database
-- itself, so it holds even when a row is changed by a script or from psql rather than
-- through the application. A rule enforced only in Java is a rule that can be bypassed.
-- -------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION log_appointment_status_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        INSERT INTO appointment_audit (appointment_number, old_status, new_status)
        VALUES (NEW.appointment_number, NULL, NEW.status);
    ELSIF (NEW.status IS DISTINCT FROM OLD.status) THEN
        INSERT INTO appointment_audit (appointment_number, old_status, new_status)
        VALUES (NEW.appointment_number, OLD.status, NEW.status);
    END IF;
    RETURN NEW;
END;
$$;;

DROP TRIGGER IF EXISTS trg_appointment_status_audit ON appointment;;

CREATE TRIGGER trg_appointment_status_audit
    AFTER INSERT OR UPDATE ON appointment
    FOR EACH ROW
EXECUTE FUNCTION log_appointment_status_change();;
