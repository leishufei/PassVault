package com.example.passvault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.passvault.data.model.Category
import com.example.passvault.data.model.PasswordEntry

@Database(
    entities = [PasswordEntry::class, Category::class],
    version = 8,
    exportSchema = false
)
abstract class PasswordDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // PasswordEntry: 新增 uuid 列
                db.execSQL("ALTER TABLE password_entries ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                // 为已有记录生成伪 UUID
                db.execSQL("""
                    UPDATE password_entries
                    SET uuid = lower(hex(randomblob(4))) || '-' ||
                               lower(hex(randomblob(2))) || '-4' ||
                               substr(lower(hex(randomblob(2))),2) || '-' ||
                               substr('89ab', abs(random()) % 4 + 1, 1) ||
                               substr(lower(hex(randomblob(2))),2) || '-' ||
                               lower(hex(randomblob(6)))
                    WHERE uuid = ''
                """.trimIndent())

                // Category: 新增 uuid / updatedAt / isDeleted 列
                db.execSQL("ALTER TABLE categories ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE categories ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    UPDATE categories
                    SET uuid = lower(hex(randomblob(4))) || '-' ||
                               lower(hex(randomblob(2))) || '-4' ||
                               substr(lower(hex(randomblob(2))),2) || '-' ||
                               substr('89ab', abs(random()) % 4 + 1, 1) ||
                               substr(lower(hex(randomblob(2))),2) || '-' ||
                               lower(hex(randomblob(6))),
                        updatedAt = createdAt
                    WHERE uuid = ''
                """.trimIndent())
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    DELETE FROM password_entries
                    WHERE id NOT IN (
                        SELECT MAX(id) FROM password_entries GROUP BY uuid
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_password_entries_uuid ON password_entries(uuid)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE password_entries ADD COLUMN appPackageName TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    UPDATE password_entries SET isDeleted = 1, updatedAt = ${System.currentTimeMillis()}
                    WHERE id NOT IN (
                        SELECT MIN(id) FROM password_entries
                        WHERE isDeleted = 0
                        GROUP BY title, username
                    )
                    AND isDeleted = 0
                    AND (title != '' OR username != '')
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE password_entries_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uuid TEXT NOT NULL DEFAULT '',
                        title TEXT NOT NULL,
                        username TEXT NOT NULL,
                        encryptedPassword BLOB NOT NULL,
                        passwordIv BLOB NOT NULL,
                        website TEXT NOT NULL DEFAULT '',
                        appPackageName TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL DEFAULT '',
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        iconColor INTEGER NOT NULL DEFAULT 0,
                        strength INTEGER NOT NULL DEFAULT 0,
                        categoryId INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO password_entries_new (
                        id, uuid, title, username, encryptedPassword, passwordIv,
                        website, appPackageName, notes, isFavorite, iconColor,
                        strength, categoryId, createdAt, updatedAt, isDeleted
                    )
                    SELECT id, uuid, title, username, encryptedPassword, passwordIv,
                           website, appPackageName, notes, isFavorite, iconColor,
                           strength, categoryId, createdAt, updatedAt, isDeleted
                    FROM password_entries
                """.trimIndent())

                db.execSQL("DROP TABLE password_entries")
                db.execSQL("ALTER TABLE password_entries_new RENAME TO password_entries")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_password_entries_uuid ON password_entries(uuid)")
            }
        }
    }
}
