-- REFERENCE snippet (not auto-applied). Every table whose entity extends
-- com.telcocrm.common.persistence.entity.BaseEntity MUST include these columns, otherwise
-- Hibernate ddl-auto=validate will fail at startup. Add them to each create-table migration.

--   id         UUID         PRIMARY KEY,
--   created_at TIMESTAMPTZ  NOT NULL,
--   updated_at TIMESTAMPTZ  NOT NULL,
--   created_by VARCHAR(255),
--   updated_by VARCHAR(255),
--   version    BIGINT       NOT NULL
