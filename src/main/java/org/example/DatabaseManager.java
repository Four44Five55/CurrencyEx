package org.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {
    private static final HikariDataSource dataSource;

    static {
        try {
            // Загрузка конфигурации из файла
            Properties props = new Properties();
            String propFileName = "config.properties";
            try (InputStream inputStream = DatabaseManager.class.getClassLoader().getResourceAsStream(propFileName)) {
                if (inputStream == null) {
                    throw new RuntimeException("Property file '" + propFileName + "' not found in the classpath");
                }
                props.load(inputStream);
            }

            // Создание папки для БД SQLite, если ее нет
            String jdbcUrl = props.getProperty("db.url");
            if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:sqlite:")) {
                String path = jdbcUrl.substring("jdbc:sqlite:".length());
                File dbFile = new File(path);
                File parentDir = dbFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        throw new IOException("Failed to create database directory: " + parentDir.getAbsolutePath());
                    }
                }
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);

            // Для SQLite не нужны username/password
            // config.setUsername(props.getProperty("db.user"));
            // config.setPassword(props.getProperty("db.password"));

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000); // 30 секунд
            config.setIdleTimeout(600000);      // 10 минут
            config.setMaxLifetime(1800000);   // 30 минут

            // SQLite не имеет высокопроизводительного запроса для проверки соединения,
            // но можно оставить стандартный (или не указывать, полагаясь на драйвер)
            // config.setConnectionTestQuery("SELECT 1");

            // --- 4. Инициализация DataSource ---
            // DataSource - это и есть наш пул соединений
            dataSource = new HikariDataSource(config);

        } catch (IOException | RuntimeException e) {
            // Если что-то пошло не так при инициализации, приложение не может работать
            // Логируем ошибку и завершаем работу или бросаем непроверяемое исключение
            e.printStackTrace(); // Замените на логгер в реальном приложении
            throw new RuntimeException("Failed to initialize DatabaseManager", e);
        }
    }

    /**
     * Возвращает соединение из пула.
     *
     * @return Объект Connection.
     * @throws SQLException если не удалось получить соединение из пула.
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}

