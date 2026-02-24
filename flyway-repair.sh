#!/bin/bash
# ============================================================
# Flyway Repair Script for Ecommerce Management System
# ============================================================
# Run this ONCE to fix Flyway migration state.
# It drops the flyway_schema_history + all tables, then the app
# will re-run all migrations (V1–V15) fresh on next startup.
#
# Usage: ./flyway-repair.sh
# ============================================================

set -e

# Load .env
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$SCRIPT_DIR/.env" ]; then
    export $(grep -v '^#' "$SCRIPT_DIR/.env" | xargs)
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-ecommerce_db}"
DB_USER="${DB_USER:-peter}"
DB_PASSWORD="${DB_PASSWORD:-peter}"

echo "============================================"
echo " Flyway Full Reset for: $DB_NAME"
echo "============================================"
echo ""
echo "WARNING: This will DROP and RECREATE the database!"
echo "         All data will be lost."
echo ""
read -p "Are you sure? (y/N): " confirm
if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
    echo "Aborted."
    exit 0
fi

echo ""
echo ">>> Dropping database $DB_NAME..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" -e "DROP DATABASE IF EXISTS $DB_NAME;" 2>/dev/null

echo ">>> Creating database $DB_NAME..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" -e "CREATE DATABASE $DB_NAME DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null

echo ""
echo "============================================"
echo " Database reset complete!"
echo " Now start the app and Flyway will run all"
echo " migrations (V1 through V15) automatically."
echo "============================================"
echo ""
echo " Run:  ./mvnw spring-boot:run"
echo ""

