package org.example;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

public class DatabaseManager {
    private static final String JDBC_URL;

    static {
        Properties props = new Properties();
        String propFileName = "config.properties";
        try (InputStream inputStream = DatabaseManager.class.getClassLoader().getResourceAsStream(propFileName)) {

            if (inputStream == null) {
                // Если файл не найден, приложение не должно работать.
                throw new RuntimeException("Property file '" + propFileName + "' not found in the classpath");


            }
            // Загрузка свойства из файла
            props.load(Objects.requireNonNull(inputStream));
            // Получаем значение по ключу "db.url"
            JDBC_URL = props.getProperty("db.url");

            if (JDBC_URL.startsWith("jdbc:sqlite:")) {
                String path = JDBC_URL.substring("jdbc:sqlite:".length());


                File dbFile = new File(path);
                File parent = dbFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();


                }
            }

        } catch (IOException e) {
            // Ошибка чтения файла - это критическая ошибка для старта приложения
            throw new RuntimeException("Cannot load a properties file", e);
        }

        // Загрузка драйвера (опционально, но хорошая практика)
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC Driver not found.", e);


        }
    }


    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }
}