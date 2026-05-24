package com.sba;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs on every startup — fixes NULL boolean columns in existing DB rows.
 * Prevents Hibernate "Null value assigned to primitive type" errors.
 */
@Component
public class DataMigrationRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        try {
            jdbc.execute("UPDATE cloud_files SET is_compressed = 0 WHERE is_compressed IS NULL");
            jdbc.execute("UPDATE cloud_files SET is_duplicate = 0 WHERE is_duplicate IS NULL");
            jdbc.execute("UPDATE cloud_files SET compression_ratio = 1.0 WHERE compression_ratio IS NULL");
            jdbc.execute("UPDATE cloud_files SET upload_time_ms = 0 WHERE upload_time_ms IS NULL");
            jdbc.execute("UPDATE cloud_files SET compressed_size = file_size WHERE compressed_size IS NULL OR compressed_size = 0");

            jdbc.execute("UPDATE secondary_backups SET is_compressed = 0 WHERE is_compressed IS NULL");
            jdbc.execute("UPDATE secondary_backups SET compressed_size = 0 WHERE compressed_size IS NULL");

            // tertiary_backups table may not exist yet — safe ignore
            try {
                jdbc.execute("UPDATE tertiary_backups SET is_compressed = 0 WHERE is_compressed IS NULL");
            } catch (Exception ignored) {}

            System.out.println("[SBA] Data migration complete — NULL boolean columns fixed.");
        } catch (Exception e) {
            System.out.println("[SBA] Data migration warning: " + e.getMessage());
        }
    }
}
