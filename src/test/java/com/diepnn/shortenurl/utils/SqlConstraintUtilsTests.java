package com.diepnn.shortenurl.utils;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class SqlConstraintUtilsTests {
    private DataIntegrityViolationException buildDive(String sqlState,
                                                      int errorCode,
                                                      String sqlMessage,
                                                      String constraintName) {
        SQLException sqlEx = new SQLException(sqlMessage, sqlState, errorCode);
        // Hibernate ConstraintViolationException that carries the constraint name
        ConstraintViolationException cve = new ConstraintViolationException("constraint violation", sqlEx, "SELECT 1", constraintName);
        return new DataIntegrityViolationException("wrapped DIVE", cve);
    }

    @Nested
    @DisplayName("isPrimaryKeyViolation")
    class PrimaryKey {
        @Test
        @DisplayName("returns true when explicit constraint name matches (case-insensitive, contains)")
        void pk_byExplicitNameMatch() {
            DataIntegrityViolationException dive =
                    buildDive("23505", 0, "duplicate key violates unique constraint", "url_info_pkey");

            assertTrue(SqlConstraintUtils.isPrimaryKeyViolation(dive, "PKEY"));
            assertTrue(SqlConstraintUtils.isPrimaryKeyViolation(dive, "url_info")); // contains
        }

        @Test
        @DisplayName("PostgreSQL/H2: SQLState 23505 + message mentions primary key")
        void pk_postgresByStateAndMessage() {
            DataIntegrityViolationException dive =
                    buildDive("23505", 0, "duplicate key value violates primary key constraint \"url_info_pkey\"", "url_info_pkey");

            assertTrue(SqlConstraintUtils.isPrimaryKeyViolation(dive, null));
        }

        @Test
        @DisplayName("PostgreSQL/H2: SQLState 23505 + default _pkey pattern")
        void pk_postgresByPkeyPattern() {
            DataIntegrityViolationException dive =
                    buildDive("23505", 0, "duplicate key value violates unique constraint \"url_info_pkey\"", "url_info_pkey");

            assertTrue(SqlConstraintUtils.isPrimaryKeyViolation(dive, null));
        }

        @Test
        @DisplayName("MySQL/MariaDB: 23000 + 1062 + message mentions for key 'PRIMARY'")
        void pk_mysqlByCodeAndMessage() {
            DataIntegrityViolationException dive =
                    buildDive("23000", 1062, "Duplicate entry 'abc' for key 'PRIMARY'", "PRIMARY");

            assertTrue(SqlConstraintUtils.isPrimaryKeyViolation(dive, null));
        }

        @Test
        @DisplayName("returns false when unique violation but not PK (PostgreSQL 23505 no PK hints)")
        void pk_negative_uniqueButNotPk() {
            DataIntegrityViolationException dive =
                    buildDive("23505", 0, "duplicate key value violates unique constraint \"uq_url_alias\"", "uq_url_alias");

            assertFalse(SqlConstraintUtils.isPrimaryKeyViolation(dive, null));
        }

        @Test
        @DisplayName("returns false when not a ConstraintViolationException or no constraint name")
        void pk_negative_notConstraintViolationOrNoName() {
            // No constraint name: SqlConstraintUtils short-circuits and returns false
            SQLException sqlEx = new SQLException("some error", "99999", 0);
            ConstraintViolationException cve =
                    new ConstraintViolationException("no constraint name", sqlEx, "SELECT 1", null);
            DataIntegrityViolationException dive = new DataIntegrityViolationException("wrapped", cve);

            assertFalse(SqlConstraintUtils.isPrimaryKeyViolation(dive, null));
        }
    }

    @Nested
    @DisplayName("isUniqueConstraintViolation")
    class UniqueConstraint {
        @Test
        @DisplayName("returns true when explicit unique constraint name matches exactly (case-insensitive)")
        void unique_byExplicitName() {
            DataIntegrityViolationException dive =
                    buildDive("23505", 0, "duplicate key value violates unique constraint \"uq_url_alias\"", "UQ_URL_ALIAS");

            assertTrue(SqlConstraintUtils.isUniqueConstraintViolation(dive, "uq_url_alias", "another_constraint"));
        }

        @Test
        @DisplayName("PostgreSQL/H2: SQLState 23505")
        void unique_postgresByState() {
            DataIntegrityViolationException dive =
                    buildDive("23505", 0, "duplicate key value violates unique constraint \"uq_url_alias\"", "uq_url_alias");

            assertTrue(SqlConstraintUtils.isUniqueConstraintViolation(dive));
        }

        @Test
        @DisplayName("MySQL/MariaDB: 23000 + 1062")
        void unique_mysqlByCode() {
            DataIntegrityViolationException dive =
                    buildDive("23000", 1062, "Duplicate entry 'abc' for key 'uq_url_alias'", "uq_url_alias");

            assertTrue(SqlConstraintUtils.isUniqueConstraintViolation(dive));
        }

        @Test
        @DisplayName("returns false when constraint names provided do not match")
        void unique_negative_namesDoNotMatch() {
            DataIntegrityViolationException dive =
                    buildDive("00000", 0, "some other error", "some_other_constraint");

            assertFalse(SqlConstraintUtils.isUniqueConstraintViolation(dive, "uq_url_alias"));
        }

        @Test
        @DisplayName("returns false when not a ConstraintViolationException or no constraint name")
        void unique_negative_notConstraintViolationOrNoName() {
            SQLException sqlEx = new SQLException("some error", "23505", 0);
            ConstraintViolationException cve =
                    new ConstraintViolationException("no constraint name", sqlEx, "SELECT 1", null);
            DataIntegrityViolationException dive = new DataIntegrityViolationException("wrapped", cve);

            assertFalse(SqlConstraintUtils.isUniqueConstraintViolation(dive));
        }
    }
}
