-- 县域儿童青少年近视筛查复诊配镜补助平台 数据库结构
-- 幂等建表脚本（H2 PostgreSQL 兼容模式亦可执行）

CREATE TABLE IF NOT EXISTS organizations (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(120) NOT NULL,
    org_type     VARCHAR(32)  NOT NULL,           -- SCHOOL/SCREENING_AGENCY/HOSPITAL/OPTOMETRY_CLINIC/OPTICAL_SHOP/EDU_BUREAU/HEALTH_COMMISSION
    area         VARCHAR(60),                     -- 所属片区
    contact_name VARCHAR(40),
    contact_phone VARCHAR(20),
    address      VARCHAR(200),
    created_at   TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS app_users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(60)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    real_name     VARCHAR(40)  NOT NULL,
    role          VARCHAR(40)  NOT NULL,          -- PLATFORM_ADMIN/EDU_BUREAU/HEALTH_COMMISSION/SCHOOL_HEALTH_TEACHER/HEAD_TEACHER/SCREENER/DOCTOR/OPTOMETRIST/OPTICIAN/PARENT
    phone         VARCHAR(20),
    organization_id BIGINT REFERENCES organizations(id),
    active        BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS school_classes (
    id               BIGSERIAL PRIMARY KEY,
    school_id        BIGINT NOT NULL REFERENCES organizations(id),
    grade            VARCHAR(20) NOT NULL,        -- 年级
    class_name       VARCHAR(20) NOT NULL,        -- 班级
    head_teacher_id  BIGINT REFERENCES app_users(id),
    head_teacher_name VARCHAR(40),
    head_teacher_phone VARCHAR(20),
    created_at       TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS students (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(40) NOT NULL,
    gender           VARCHAR(8),
    birth_date       DATE,
    school_id        BIGINT NOT NULL REFERENCES organizations(id),
    class_id         BIGINT REFERENCES school_classes(id),
    student_no       VARCHAR(40),                 -- 学籍号
    grade            VARCHAR(20),
    parent_name      VARCHAR(40),
    parent_phone     VARCHAR(20),
    past_vision_note VARCHAR(400),                -- 既往视力档案
    status           VARCHAR(32) NOT NULL DEFAULT 'PENDING_SCREENING',
    latest_screening_id BIGINT,
    latest_diagnosis_id BIGINT,
    created_at       TIMESTAMP DEFAULT now(),
    updated_at       TIMESTAMP DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_students_school ON students(school_id);
CREATE INDEX IF NOT EXISTS idx_students_class ON students(class_id);
CREATE INDEX IF NOT EXISTS idx_students_status ON students(status);

CREATE TABLE IF NOT EXISTS screening_batches (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    school_id       BIGINT NOT NULL REFERENCES organizations(id),
    grade           VARCHAR(20),
    screener_org_id BIGINT REFERENCES organizations(id),
    screening_date  DATE NOT NULL,
    notes           VARCHAR(400),
    created_by      BIGINT REFERENCES app_users(id),
    total_count     INT DEFAULT 0,
    abnormal_count  INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS screenings (
    id                 BIGSERIAL PRIMARY KEY,
    batch_id           BIGINT NOT NULL REFERENCES screening_batches(id),
    student_id         BIGINT NOT NULL REFERENCES students(id),
    absent             BOOLEAN DEFAULT FALSE,
    absent_reason      VARCHAR(200),
    naked_od           NUMERIC(3,1),              -- 右眼裸眼视力
    naked_os           NUMERIC(3,1),              -- 左眼裸眼视力
    sph_od             NUMERIC(4,2),              -- 右眼球镜(屈光度)
    sph_os             NUMERIC(4,2),
    cyl_od             NUMERIC(4,2),              -- 右眼散光
    cyl_os             NUMERIC(4,2),
    axis_od            INT,                       -- 右眼轴位
    axis_os            INT,
    wearing_glasses    BOOLEAN DEFAULT FALSE,     -- 是否戴镜
    hours_outdoor      NUMERIC(3,1),              -- 日均户外活动(小时)
    screen_hours       NUMERIC(3,1),              -- 日均电子屏幕(小时)
    reading_distance_ok BOOLEAN,                  -- 读写距离是否合格
    eye_exercise_daily BOOLEAN,                   -- 每日眼保健操
    parental_myopia    BOOLEAN,                   -- 父母近视
    questionnaire_note VARCHAR(300),
    need_retest        BOOLEAN DEFAULT FALSE,     -- 是否需要复测
    device_calib_no    VARCHAR(40),               -- 设备校准编号
    result             VARCHAR(16) NOT NULL,      -- NORMAL/ABNORMAL/SUSPECTED
    screener_id        BIGINT REFERENCES app_users(id),
    screening_time     TIMESTAMP,
    created_at         TIMESTAMP DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_screenings_student ON screenings(student_id);
CREATE INDEX IF NOT EXISTS idx_screenings_batch ON screenings(batch_id);

CREATE TABLE IF NOT EXISTS parent_confirmations (
    id                 BIGSERIAL PRIMARY KEY,
    screening_id       BIGINT NOT NULL REFERENCES screenings(id),
    student_id         BIGINT NOT NULL REFERENCES students(id),
    status             VARCHAR(16) NOT NULL DEFAULT 'PENDING', -- PENDING/CONFIRMED/ON_HOLD
    choice             VARCHAR(20),               -- COUNTY_HOSPITAL/PARTNER_CLINIC/POSTPONE
    reason             VARCHAR(400),              -- 暂缓原因等
    notified_at        TIMESTAMP,
    confirmed_at       TIMESTAMP,
    decided_by_name    VARCHAR(40),
    school_review_status VARCHAR(16) NOT NULL DEFAULT 'PENDING', -- PENDING/APPROVED/REJECTED
    school_review_by   BIGINT REFERENCES app_users(id),
    school_review_at   TIMESTAMP,
    school_review_note VARCHAR(400),
    created_at         TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS issues (
    id              BIGSERIAL PRIMARY KEY,
    student_id      BIGINT REFERENCES students(id),
    screening_id    BIGINT REFERENCES screenings(id),
    issue_type      VARCHAR(24) NOT NULL,         -- ABSENT/PHONE_UNREACHABLE/IMPORT_ERROR/RESULT_DIVERGENCE/SELF_GLASSES/SUBSIDY_DISPUTE/GLASSES_DISCOMFORT/MISSED_REVIEW
    title           VARCHAR(120) NOT NULL,
    description     VARCHAR(600),
    status          VARCHAR(12) NOT NULL DEFAULT 'OPEN', -- OPEN/PROCESSING/RESOLVED
    resolution      VARCHAR(600),
    created_by      BIGINT REFERENCES app_users(id),
    created_by_name VARCHAR(40),
    created_by_role VARCHAR(40),
    resolved_by     BIGINT REFERENCES app_users(id),
    created_at      TIMESTAMP DEFAULT now(),
    resolved_at     TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_issues_student ON issues(student_id);
CREATE INDEX IF NOT EXISTS idx_issues_status ON issues(status);

CREATE TABLE IF NOT EXISTS referrals (
    id             BIGSERIAL PRIMARY KEY,
    student_id     BIGINT NOT NULL REFERENCES students(id),
    screening_id   BIGINT REFERENCES screenings(id),
    confirmation_id BIGINT REFERENCES parent_confirmations(id),
    target_org_id  BIGINT REFERENCES organizations(id),
    target_type    VARCHAR(20),                   -- COUNTY_HOSPITAL/PARTNER_CLINIC
    status         VARCHAR(12) NOT NULL DEFAULT 'PENDING', -- PENDING/COMPLETED/CANCELLED
    issued_by      BIGINT REFERENCES app_users(id),
    issued_at      TIMESTAMP DEFAULT now(),
    completed_at   TIMESTAMP
);

CREATE TABLE IF NOT EXISTS revisit_diagnoses (
    id                  BIGSERIAL PRIMARY KEY,
    student_id          BIGINT NOT NULL REFERENCES students(id),
    referral_id         BIGINT REFERENCES referrals(id),
    org_id              BIGINT REFERENCES organizations(id),
    doctor_id           BIGINT REFERENCES app_users(id),
    doctor_name         VARCHAR(40),
    visit_date          DATE NOT NULL,
    true_myopia         BOOLEAN,                  -- 是否真性近视
    need_mydriasis      BOOLEAN,                  -- 是否需要散瞳
    mydriasis_done      BOOLEAN DEFAULT FALSE,    -- 已散瞳验光
    degree              VARCHAR(12),              -- NONE/LOW/MEDIUM/HIGH
    rx_sph_od           NUMERIC(4,2),             -- 处方：右眼球镜
    rx_sph_os           NUMERIC(4,2),
    rx_cyl_od           NUMERIC(4,2),
    rx_cyl_os           NUMERIC(4,2),
    rx_axis_od          INT,
    rx_axis_os          INT,
    pd                  NUMERIC(4,1),             -- 瞳距
    revisit_cycle_months INT DEFAULT 6,           -- 复查周期
    subsidy_eligible    BOOLEAN DEFAULT FALSE,    -- 是否符合配镜补助
    subsidy_note        VARCHAR(400),
    divergence_note     VARCHAR(400),             -- 与筛查结果差异说明
    advice              VARCHAR(400),
    created_at          TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS glasses (
    id               BIGSERIAL PRIMARY KEY,
    student_id       BIGINT NOT NULL REFERENCES students(id),
    diagnosis_id     BIGINT REFERENCES revisit_diagnoses(id),
    org_id           BIGINT REFERENCES organizations(id),
    org_name         VARCHAR(120),
    source           VARCHAR(16) NOT NULL,        -- SUBSIDY_SHOP/SELF_EXTERNAL
    status           VARCHAR(24) NOT NULL DEFAULT 'FITTED', -- ORDERED/FITTED/DISCOMFORT_REPORTED/READJUSTED/CONFIRMED_OK
    price            NUMERIC(10,2),
    subsidy_amount   NUMERIC(10,2) DEFAULT 0,
    fitted_date      DATE,
    discomfort_date  DATE,
    discomfort_note  VARCHAR(400),
    readjust_note    VARCHAR(400),
    confirmed_date   DATE,
    created_at       TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS subsidies (
    id           BIGSERIAL PRIMARY KEY,
    student_id   BIGINT NOT NULL REFERENCES students(id),
    glasses_id   BIGINT REFERENCES glasses(id),
    diagnosis_id BIGINT REFERENCES revisit_diagnoses(id),
    amount       NUMERIC(10,2) NOT NULL,
    status       VARCHAR(12) NOT NULL DEFAULT 'PENDING', -- PENDING/APPROVED/REJECTED/DISPUTED/PAID
    apply_note   VARCHAR(400),
    decision_note VARCHAR(400),
    decided_by   BIGINT REFERENCES app_users(id),
    decided_by_name VARCHAR(40),
    decided_at   TIMESTAMP,
    paid_at      TIMESTAMP,
    created_at   TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS interventions (
    id           BIGSERIAL PRIMARY KEY,
    student_id   BIGINT NOT NULL REFERENCES students(id),
    class_id     BIGINT REFERENCES school_classes(id),
    school_id    BIGINT REFERENCES organizations(id),
    type         VARCHAR(24) NOT NULL,            -- SEAT_ADJUSTMENT/OUTDOOR_ACTIVITY/EYE_HABIT_REMINDER/PARENT_NOTICE/PERIODIC_CHECK
    title        VARCHAR(120) NOT NULL,
    detail       VARCHAR(600),
    planned_date DATE,
    status       VARCHAR(12) NOT NULL DEFAULT 'PLANNED', -- PLANNED/DONE/VERIFIED
    operator_id  BIGINT REFERENCES app_users(id),
    executed_at  TIMESTAMP,
    effect_note  VARCHAR(400),
    created_at   TIMESTAMP DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_interventions_student ON interventions(student_id);
CREATE INDEX IF NOT EXISTS idx_interventions_status ON interventions(status);

CREATE TABLE IF NOT EXISTS follow_ups (
    id           BIGSERIAL PRIMARY KEY,
    student_id   BIGINT NOT NULL REFERENCES students(id),
    diagnosis_id BIGINT REFERENCES revisit_diagnoses(id),
    due_date     DATE NOT NULL,                   -- 应复查日期
    status       VARCHAR(12) NOT NULL DEFAULT 'PENDING', -- PENDING/NOTIFIED/DONE/MISSED
    reminded_at  TIMESTAMP,
    done_date    DATE,
    result_note  VARCHAR(400),
    created_at   TIMESTAMP DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_followups_due ON follow_ups(due_date, status);

CREATE TABLE IF NOT EXISTS timeline_events (
    id          BIGSERIAL PRIMARY KEY,
    student_id  BIGINT NOT NULL REFERENCES students(id),
    event_type  VARCHAR(40) NOT NULL,
    title       VARCHAR(120) NOT NULL,
    detail      VARCHAR(800),
    actor_name  VARCHAR(40),
    actor_role  VARCHAR(40),
    ref_type    VARCHAR(40),
    ref_id      BIGINT,
    event_time  TIMESTAMP DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_timeline_student ON timeline_events(student_id, event_time);

CREATE TABLE IF NOT EXISTS notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES app_users(id),
    phone       VARCHAR(20),
    channel     VARCHAR(12) DEFAULT 'SMS',
    title       VARCHAR(120) NOT NULL,
    content     VARCHAR(600) NOT NULL,
    student_id  BIGINT REFERENCES students(id),
    biz_type    VARCHAR(32),
    status      VARCHAR(12) DEFAULT 'PENDING',    -- PENDING/SENT/READ
    created_at  TIMESTAMP DEFAULT now(),
    sent_at     TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, status);
