-- =====================================================================================
-- Sunrise Dental Clinic - tables, indexes and sequence
--
-- Ordinary statements only, separated by ';'. The stored functions and the trigger live in
-- routines.sql because their bodies contain semicolons that would break the script splitter.
-- Everything here is idempotent so the application can restart without losing data.
-- =====================================================================================

CREATE TABLE IF NOT EXISTS staff (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(60)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('RECEPTIONIST', 'ADMIN')),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS dentist (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    speciality VARCHAR(100),
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS treatment_type (
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(40)   NOT NULL UNIQUE,
    name             VARCHAR(100)  NOT NULL,
    base_cost        NUMERIC(10,2) NOT NULL CHECK (base_cost >= 0),
    duration_minutes INTEGER       NOT NULL DEFAULT 30 CHECK (duration_minutes > 0)
);

CREATE TABLE IF NOT EXISTS patient (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    address        VARCHAR(255) NOT NULL,
    contact_number VARCHAR(20)  NOT NULL,
    email          VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_patient_contact ON patient (contact_number);

CREATE TABLE IF NOT EXISTS appointment (
    id                 BIGSERIAL PRIMARY KEY,
    appointment_number VARCHAR(20) NOT NULL UNIQUE,
    patient_id         BIGINT      NOT NULL REFERENCES patient (id),
    dentist_id         BIGINT      NOT NULL REFERENCES dentist (id),
    treatment_type_id  BIGINT      NOT NULL REFERENCES treatment_type (id),
    appointment_date   DATE        NOT NULL,
    appointment_time   TIME        NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'BOOKED'
                       CHECK (status IN ('BOOKED', 'ATTENDED', 'CANCELLED', 'NO_SHOW')),
    session_count      INTEGER     NOT NULL DEFAULT 1 CHECK (session_count > 0),
    notes              VARCHAR(500),
    created_at         TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(60)
);

CREATE INDEX IF NOT EXISTS idx_appointment_dentist_date ON appointment (dentist_id, appointment_date);

-- Database-level guard against two receptionists booking the SAME slot simultaneously.
--
-- AppointmentSchedulingRules already rejects clashes, but it reads the diary and then
-- inserts, and those two steps are not atomic: under a genuine race both requests can
-- pass validation before either has inserted. A partial unique index makes the database
-- the final arbiter, which is the only place that can be.
--
-- SCOPE, stated precisely: this closes the *identical start time* race - the likely one,
-- where two staff are offered the same free slot and both take it. It does NOT close
-- partial overlaps (a 10:15 booking racing a 10:00-10:45 one), because a unique index
-- cannot express interval overlap. Closing that would need an exclusion constraint over
-- a tsrange, which is noted as future work rather than implemented here.
--
-- Partial (WHERE ...) so that cancelled and no-show rows do not occupy the slot: a
-- cancelled appointment must leave the time free to be booked again.
CREATE UNIQUE INDEX IF NOT EXISTS uq_appointment_dentist_slot
    ON appointment (dentist_id, appointment_date, appointment_time)
    WHERE status IN ('BOOKED', 'ATTENDED');
CREATE INDEX IF NOT EXISTS idx_appointment_date ON appointment (appointment_date);

CREATE TABLE IF NOT EXISTS bill (
    id                BIGSERIAL PRIMARY KEY,
    bill_number       VARCHAR(20)   NOT NULL UNIQUE,
    appointment_id    BIGINT        NOT NULL UNIQUE REFERENCES appointment (id),
    consultation_fee  NUMERIC(10,2) NOT NULL CHECK (consultation_fee >= 0),
    treatment_cost    NUMERIC(10,2) NOT NULL CHECK (treatment_cost >= 0),
    adjustment        NUMERIC(10,2) NOT NULL DEFAULT 0,
    adjustment_reason VARCHAR(200),
    total             NUMERIC(10,2) NOT NULL CHECK (total >= 0),
    issued_at         TIMESTAMP     NOT NULL DEFAULT NOW(),
    issued_by         VARCHAR(60)
);

-- Audit trail written by the trigger declared in routines.sql.
CREATE TABLE IF NOT EXISTS appointment_audit (
    id                 BIGSERIAL PRIMARY KEY,
    appointment_number VARCHAR(20) NOT NULL,
    old_status         VARCHAR(20),
    new_status         VARCHAR(20) NOT NULL,
    changed_at         TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Appointment numbers come from a database sequence rather than "SELECT MAX(id) + 1" in Java.
-- Two receptionists registering a patient at the same moment would both read the same maximum
-- and generate the same number; a sequence is atomic and cannot collide.
CREATE SEQUENCE IF NOT EXISTS appointment_number_seq START WITH 1 INCREMENT BY 1;
