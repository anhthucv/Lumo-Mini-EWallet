package com.chethu.paymentledgerservice.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;

import com.chethu.paymentledgerservice.PaymentLedgerServiceApplication;

/**
 * Verifies the physical MySQL schema generated for Task 38.
 *
 * The conditions intentionally allow only the dedicated local test database,
 * before Spring creates a context or opens a datasource connection.
 */
@SpringBootTest(classes = PaymentLedgerServiceApplication.class)
@ActiveProfiles("rollback-integration")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL",
        matches = "jdbc:mysql://127\\.0\\.0\\.1:3306/payment_ledger_test")
@EnabledIfEnvironmentVariable(named = "TEST_DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "TEST_DB_PASSWORD", matches = ".+")
class DatabaseConstraintsIntegrationTest {
    private static final String SCHEMA = "payment_ledger_test";

    private final JdbcTemplate jdbcTemplate;

    DatabaseConstraintsIntegrationTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @DynamicPropertySource
    static void localDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> required("TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> required("TEST_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> required("TEST_DB_PASSWORD"));
    }

    @Test
    void task38ConstraintsAndIndexesExistInMySqlSchema() {
        assertCheckConstraint("accounts", "ck_accounts_balance_nonnegative", "balance>=0");
        assertCheckConstraint("transactions", "ck_transactions_amount_positive", "amount>0");
        assertCheckConstraint("ledger_entries", "ck_ledger_entries_amount_positive", "amount>0");
        assertTransactionHistoryIndex();
        assertIdempotencyUniqueBoundary();
    }

    private void assertCheckConstraint(String table, String constraint, String expectedClause) {
        List<String> clauses = jdbcTemplate.queryForList("""
                SELECT cc.CHECK_CLAUSE
                FROM information_schema.TABLE_CONSTRAINTS tc
                JOIN information_schema.CHECK_CONSTRAINTS cc
                  ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
                 AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
                WHERE tc.CONSTRAINT_SCHEMA = ?
                  AND tc.TABLE_NAME = ?
                  AND tc.CONSTRAINT_NAME = ?
                """, String.class, SCHEMA, table, constraint);

        assertEquals(1, clauses.size(), "Missing MySQL CHECK constraint " + constraint);
        assertEquals(expectedClause, normalizeClause(clauses.get(0)),
                "Unexpected condition for MySQL CHECK constraint " + constraint);
    }

    private void assertTransactionHistoryIndex() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
                SELECT SEQ_IN_INDEX, COLUMN_NAME
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = ?
                  AND TABLE_NAME = 'transactions'
                  AND INDEX_NAME = 'idx_transactions_account_created_at'
                ORDER BY SEQ_IN_INDEX
                """, SCHEMA);

        assertEquals(2, columns.size(), "Transaction history index must contain two columns");
        assertEquals(1, ((Number) columns.get(0).get("SEQ_IN_INDEX")).intValue());
        assertEquals("account_id", columns.get(0).get("COLUMN_NAME"));
        assertEquals(2, ((Number) columns.get(1).get("SEQ_IN_INDEX")).intValue());
        assertEquals("created_at", columns.get(1).get("COLUMN_NAME"));
    }

    private void assertIdempotencyUniqueBoundary() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
                SELECT kcu.ORDINAL_POSITION, kcu.COLUMN_NAME
                FROM information_schema.TABLE_CONSTRAINTS tc
                JOIN information_schema.KEY_COLUMN_USAGE kcu
                  ON kcu.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
                 AND kcu.TABLE_NAME = tc.TABLE_NAME
                 AND kcu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
                WHERE tc.CONSTRAINT_SCHEMA = ?
                  AND tc.TABLE_NAME = 'idempotency_records'
                  AND tc.CONSTRAINT_NAME = 'uk_idempotency_account_key'
                  AND tc.CONSTRAINT_TYPE = 'UNIQUE'
                ORDER BY kcu.ORDINAL_POSITION
                """, SCHEMA);

        assertEquals(2, columns.size(), "Idempotency uniqueness must contain exactly two columns");
        assertEquals("account_id", columns.get(0).get("COLUMN_NAME"));
        assertEquals("idempotency_key", columns.get(1).get("COLUMN_NAME"));
        assertTrue(columns.stream().noneMatch(column -> "operation_type".equals(column.get("COLUMN_NAME"))),
                "operation_type must not be part of idempotency uniqueness");
    }

    private String normalizeClause(String clause) {
        return clause.toLowerCase(Locale.ROOT).replaceAll("[`\\s()]", "");
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for schema integration tests");
        }
        return value;
    }
}
