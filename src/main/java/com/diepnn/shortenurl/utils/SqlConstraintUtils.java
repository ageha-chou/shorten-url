package com.diepnn.shortenurl.utils;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * Helper class to detect SQL constraint violations
 */
public class SqlConstraintUtils {
    /**
     * Detects if the given exception is caused by a primary key violation
     *
     * @param dive the exception to check
     * @param pkConstraintName the name of the primary key constraint, if known
     * @return true if the exception is caused by a primary key violation, false otherwise
     */
    public static boolean isPrimaryKeyViolation(DataIntegrityViolationException dive, String pkConstraintName) {
        Throwable t = dive.getCause();
        if (t instanceof ConstraintViolationException cve && cve.getConstraintName() != null) {
            String constraintName = cve.getConstraintName().toLowerCase();

            // 1) Prefer explicit constraint name comparison
            if (pkConstraintName != null && constraintName.contains(pkConstraintName.toLowerCase())) {
                return true;
            }

            // 2) Fallback: vendor-specific hints
            SQLException sql = cve.getSQLException();
            String state = sql != null ? sql.getSQLState() : null;
            int code = sql != null ? sql.getErrorCode() : 0;
            String message = (sql != null ? sql.getMessage() : cve.getMessage());
            String msg = message != null ? message.toLowerCase() : "";

            // PostgreSQL/H2: both use 23505 for unique; detect PK by message or default PK name pattern
            if ("23505".equals(state)) {
                // Heuristic: messages often mention "... violates primary key constraint ..."
                return msg.contains("primary key") || msg.contains("_pkey") || msg.contains("primary_key");
            }

            // MySQL/MariaDB
            if ("23000".equals(state) && code == 1062) {
                // Duplicate entry '...' for key 'PRIMARY'
                return msg.contains("for key 'primary'") || msg.contains("for key `primary`");
            }
        }

        return false;
    }

    /**
     * Detects if the given exception is caused by a unique constraint violation
     *
     * @param dive the exception to check
     * @param uniqueConstraintNames the names of the unique constraints, if known (optional)
     * @return true if the exception is caused by a unique constraint violation, false otherwise
     * */
    public static boolean isUniqueConstraintViolation(DataIntegrityViolationException dive, String ...uniqueConstraintNames) {
        Throwable t = dive.getCause();
        if (t instanceof ConstraintViolationException cve && cve.getConstraintName() != null) {
            String constraintName = cve.getConstraintName().toLowerCase();

            if (uniqueConstraintNames != null) {
                if (Arrays.stream(uniqueConstraintNames).map(String::toLowerCase).anyMatch(constraintName::equals)) {
                    return true;
                }
            }

            SQLException sql = cve.getSQLException();
            String state = sql != null ? sql.getSQLState() : null;
            int code = sql != null ? sql.getErrorCode() : 0;

            // PostgreSQL/H2 unique violation
            if ("23505".equals(state)) {
                return true;
            }

            // MySQL/MariaDB duplicate
            if ("23000".equals(state) && code == 1062) {
                return true;
            }
        }

        return false;
    }

}
