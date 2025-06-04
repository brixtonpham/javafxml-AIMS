#!/bin/bash
# setup_test_db.sh
# Script để khởi tạo và chuẩn bị database test

echo "🔧 Chuẩn bị AIMS Test Database..."

# Đường dẫn tới các file
PROJECT_ROOT="/home/namu10x/Desktop/AIMS_Project"
TEST_DB="$PROJECT_ROOT/src/test/resources/aims_test.db"
MAIN_DB="$PROJECT_ROOT/target/classes/aims_database.db"
SCHEMA_SCRIPT="$PROJECT_ROOT/src/main/java/com/aims/core/infrastructure/database/scripts/V1__create_tables.sql"
SEED_SCRIPT="$PROJECT_ROOT/src/test/resources/test_data/seed_data.sql"

# Kiểm tra các file cần thiết
if [ ! -f "$MAIN_DB" ]; then
    echo "❌ Không tìm thấy database chính: $MAIN_DB"
    echo "   Vui lòng build project trước: mvn compile"
    exit 1
fi

if [ ! -f "$SCHEMA_SCRIPT" ]; then
    echo "❌ Không tìm thấy schema script: $SCHEMA_SCRIPT"
    exit 1
fi

# Tạo thư mục test resources nếu chưa có
mkdir -p "$(dirname "$TEST_DB")"
mkdir -p "$PROJECT_ROOT/src/test/resources/test_data"

# Sao chép database chính thành database test
echo "📋 Sao chép database chính thành aims_test.db..."
cp "$MAIN_DB" "$TEST_DB"

# Xóa tất cả dữ liệu hiện có và tạo lại schema
echo "🗑️  Xóa dữ liệu cũ và tạo lại schema..."
sqlite3 "$TEST_DB" "
    PRAGMA foreign_keys = OFF;
    
    -- Lấy danh sách tất cả các bảng
    SELECT 'DROP TABLE IF EXISTS ' || name || ';' 
    FROM sqlite_master 
    WHERE type='table' AND name != 'sqlite_sequence';
" | sqlite3 "$TEST_DB"

# Tạo lại schema từ script
echo "🏗️  Tạo lại cấu trúc bảng..."
sqlite3 "$TEST_DB" < "$SCHEMA_SCRIPT"

# Seed dữ liệu mẫu nếu có
if [ -f "$SEED_SCRIPT" ]; then
    echo "🌱 Seed dữ liệu mẫu..."
    sqlite3 "$TEST_DB" < "$SEED_SCRIPT"
else
    echo "⚠️  Không tìm thấy seed script, bỏ qua seed data"
fi

# Seed comprehensive test data từ V2__seed_test_data.sql
COMPREHENSIVE_SEED="$PROJECT_ROOT/src/test/resources/test_data/V2__seed_test_data.sql"
if [ -f "$COMPREHENSIVE_SEED" ]; then
    echo "🌱 Seed dữ liệu test comprehensive..."
    sqlite3 "$TEST_DB" < "$COMPREHENSIVE_SEED"
else
    echo "⚠️  Không tìm thấy comprehensive seed script"
fi

# Kiểm tra kết quả
echo "✅ Kiểm tra database test..."
TABLE_COUNT=$(sqlite3 "$TEST_DB" "SELECT COUNT(*) FROM sqlite_master WHERE type='table';")
echo "   Số bảng đã tạo: $TABLE_COUNT"

if [ "$TABLE_COUNT" -gt 0 ]; then
    echo "🎉 Database test đã được chuẩn bị thành công!"
    echo "   Database path: $TEST_DB"
    echo ""
    echo "📊 Danh sách bảng:"
    sqlite3 "$TEST_DB" "SELECT '  - ' || name FROM sqlite_master WHERE type='table' ORDER BY name;"
    echo ""
    echo "🔍 Kiểm tra dữ liệu mẫu:"
    USER_COUNT=$(sqlite3 "$TEST_DB" "SELECT COUNT(*) FROM USER_ACCOUNT;" 2>/dev/null || echo "0")
    PRODUCT_COUNT=$(sqlite3 "$TEST_DB" "SELECT COUNT(*) FROM PRODUCT;" 2>/dev/null || echo "0")
    echo "   - Users: $USER_COUNT"
    echo "   - Products: $PRODUCT_COUNT"
else
    echo "❌ Có lỗi khi tạo database test"
    exit 1
fi
