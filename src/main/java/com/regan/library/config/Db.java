package com.regan.library.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Central JDBC connection factory.
 * Reads db.properties from the working directory first, then the classpath,
 * so credentials never need to be committed to the repository.
 */
public final class Db {

    private static final String FILE = "db.properties";
    private static final Properties PROPS = load();

    private Db() {}

    private static Properties load() {
        Properties p = new Properties();
        try {
            Path local = Path.of(FILE);
            if (Files.exists(local)) {
                try (InputStream in = Files.newInputStream(local)) {
                    p.load(in);
                    return p;
                }
            }
            try (InputStream in = Db.class.getClassLoader().getResourceAsStream(FILE)) {
                if (in == null) {
                    throw new IllegalStateException(
                        "db.properties not found. Copy src/main/resources/db.properties.example " +
                        "to db.properties and review the local connection settings.");
                }
                p.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + FILE, e);
        }
        return p;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                PROPS.getProperty("db.url"),
                PROPS.getProperty("db.user"),
                PROPS.getProperty("db.password"));
    }
}
