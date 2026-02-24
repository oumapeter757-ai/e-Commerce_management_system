package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * V15: Reconcile ALL entity columns with database schema.
 *
 * Java-based Flyway migration because MySQL 8.0 doesn't support
 * "ALTER TABLE ... ADD COLUMN IF NOT EXISTS" (only MariaDB does),
 * and Flyway's JDBC runner doesn't support DELIMITER for stored procedures.
 *
 * This migration is fully idempotent — safe to run multiple times.
 * It checks INFORMATION_SCHEMA before adding each column/index/FK.
 */
public class V15__Reconcile_Schema_With_Entities extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();

        // ==================== cart_items ====================
        addColumnIfNotExists(conn, "cart_items", "selected",            "BOOLEAN NOT NULL DEFAULT TRUE");
        addColumnIfNotExists(conn, "cart_items", "saved_for_later",     "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumnIfNotExists(conn, "cart_items", "price_at_addition",   "DECIMAL(10,2) NULL");
        addColumnIfNotExists(conn, "cart_items", "notes",               "VARCHAR(500) NULL");
        addColumnIfNotExists(conn, "cart_items", "max_buy_quantity",    "INT NULL");
        addColumnIfNotExists(conn, "cart_items", "product_variant_id",  "BIGINT NULL");
        addColumnIfNotExists(conn, "cart_items", "version",             "BIGINT DEFAULT 0");

        // ==================== carts ====================
        addColumnIfNotExists(conn, "carts", "expires_at",              "DATETIME NULL");
        addColumnIfNotExists(conn, "carts", "estimated_shipping_cost", "DECIMAL(10,2) NOT NULL DEFAULT 0.00");
        addColumnIfNotExists(conn, "carts", "selected_items_total",    "DECIMAL(10,2) NOT NULL DEFAULT 0.00");
        addColumnIfNotExists(conn, "carts", "total_savings",           "DECIMAL(10,2) NOT NULL DEFAULT 0.00");
        addColumnIfNotExists(conn, "carts", "version",                 "BIGINT DEFAULT 0");

        // ==================== inventory ====================
        addColumnIfNotExists(conn, "inventory", "version",             "BIGINT DEFAULT 0");

        // ==================== payments ====================
        addColumnIfNotExists(conn, "payments", "idempotency_key",      "VARCHAR(64) NULL");

        // ==================== order_items ====================
        addColumnIfNotExists(conn, "order_items", "product_variant_id", "BIGINT NULL");

        // ==================== products ====================
        addColumnIfNotExists(conn, "products", "max_buy_quantity",      "INT NULL DEFAULT NULL");

        // ==================== New tables ====================
        createTableIfNotExists(conn, "coupons",
                """
                CREATE TABLE IF NOT EXISTS coupons (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    description TEXT,
                    discount_type VARCHAR(30) NOT NULL,
                    discount_value DECIMAL(10,2) NOT NULL,
                    min_order_amount DECIMAL(10,2) DEFAULT 0.00,
                    max_discount_amount DECIMAL(10,2),
                    usage_limit INT,
                    usage_count INT DEFAULT 0,
                    per_user_limit INT DEFAULT 1,
                    starts_at DATETIME NOT NULL,
                    expires_at DATETIME NOT NULL,
                    is_active BOOLEAN DEFAULT TRUE,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_coupon_code (code),
                    INDEX idx_coupon_active (is_active),
                    INDEX idx_coupon_expires (expires_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        createTableIfNotExists(conn, "coupon_usages",
                """
                CREATE TABLE IF NOT EXISTS coupon_usages (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    coupon_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    order_id BIGINT,
                    used_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (coupon_id) REFERENCES coupons(id),
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY (order_id) REFERENCES orders(id),
                    INDEX idx_coupon_user (coupon_id, user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        createTableIfNotExists(conn, "product_variants",
                """
                CREATE TABLE IF NOT EXISTS product_variants (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    product_id BIGINT NOT NULL,
                    sku VARCHAR(100) NOT NULL UNIQUE,
                    variant_name VARCHAR(100),
                    color VARCHAR(50),
                    size VARCHAR(50),
                    material VARCHAR(100),
                    price_adjustment DECIMAL(10,2) DEFAULT 0.00,
                    stock_quantity INT NOT NULL DEFAULT 0,
                    image_url VARCHAR(500),
                    is_active BOOLEAN DEFAULT TRUE,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
                    INDEX idx_variant_product (product_id),
                    INDEX idx_variant_sku (sku),
                    INDEX idx_variant_active (is_active)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        createTableIfNotExists(conn, "flash_deals",
                """
                CREATE TABLE IF NOT EXISTS flash_deals (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    product_id BIGINT NOT NULL,
                    deal_name VARCHAR(255),
                    deal_price DECIMAL(10,2) NOT NULL,
                    original_price DECIMAL(10,2) NOT NULL,
                    discount_percentage DECIMAL(5,2),
                    starts_at DATETIME NOT NULL,
                    ends_at DATETIME NOT NULL,
                    stock_limit INT,
                    sold_count INT DEFAULT 0,
                    is_active BOOLEAN DEFAULT TRUE,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
                    INDEX idx_flash_product (product_id),
                    INDEX idx_flash_active (is_active),
                    INDEX idx_flash_dates (starts_at, ends_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        // ==================== Foreign keys ====================
        addForeignKeyIfNotExists(conn, "cart_items", "fk_cart_item_variant",
                "ALTER TABLE cart_items ADD CONSTRAINT fk_cart_item_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)");

        addForeignKeyIfNotExists(conn, "order_items", "fk_order_item_variant",
                "ALTER TABLE order_items ADD CONSTRAINT fk_order_item_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)");

        // ==================== Unique indexes ====================
        addIndexIfNotExists(conn, "payments", "uk_payments_idempotency_key",
                "ALTER TABLE payments ADD CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key)");

        // ==================== Backfill price_at_addition ====================
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "UPDATE cart_items ci JOIN products p ON ci.product_id = p.id " +
                    "SET ci.price_at_addition = ci.unit_price WHERE ci.price_at_addition IS NULL");
        }
    }

    // ===================== HELPER METHODS =====================

    private boolean columnExists(Connection conn, String table, String column) throws Exception {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void addColumnIfNotExists(Connection conn, String table, String column, String definition) throws Exception {
        if (!columnExists(conn, table, column)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
            }
        }
    }

    private boolean constraintExists(Connection conn, String table, String constraintName) throws Exception {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                     "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, constraintName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean indexExists(Connection conn, String table, String indexName) throws Exception {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                     "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, indexName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void addForeignKeyIfNotExists(Connection conn, String table, String fkName, String ddl) throws Exception {
        if (!constraintExists(conn, table, fkName)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(ddl);
            }
        }
    }

    private void addIndexIfNotExists(Connection conn, String table, String indexName, String ddl) throws Exception {
        if (!indexExists(conn, table, indexName)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(ddl);
            }
        }
    }

    private void createTableIfNotExists(Connection conn, String tableName, String ddl) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(ddl);
        }
    }
}

