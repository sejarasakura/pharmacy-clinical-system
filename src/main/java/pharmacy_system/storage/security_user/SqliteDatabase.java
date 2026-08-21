package pharmacy_system.storage.security_user;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Owns the local SQLite schema used for persistent security and registration data. */
public final class SqliteDatabase {
    private final String url;

    public SqliteDatabase(Path databaseFile) {
        try {
            Files.createDirectories(databaseFile.toAbsolutePath().getParent());
            this.url = "jdbc:sqlite:" + databaseFile.toAbsolutePath();
            try (Connection connection = connection(); var statement = connection.createStatement()) {
                statement.executeUpdate("PRAGMA foreign_keys = ON");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS user_accounts (user_id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, email TEXT NOT NULL UNIQUE, status TEXT NOT NULL, approved INTEGER NOT NULL, created_at TEXT NOT NULL, updated_at TEXT NOT NULL, last_login_at TEXT, disabled_at TEXT, disabled_reason TEXT, version INTEGER NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS credentials (credential_id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL UNIQUE, password_hash TEXT NOT NULL, failed_attempts INTEGER NOT NULL, locked_until TEXT, password_updated_at TEXT NOT NULL, version INTEGER NOT NULL, FOREIGN KEY(user_id) REFERENCES user_accounts(user_id) ON DELETE CASCADE)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS roles (role_id INTEGER PRIMARY KEY AUTOINCREMENT, role_name TEXT NOT NULL UNIQUE, permissions TEXT NOT NULL, active INTEGER NOT NULL, version INTEGER NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS user_roles (user_id INTEGER NOT NULL, role_id INTEGER NOT NULL, PRIMARY KEY(user_id, role_id), FOREIGN KEY(user_id) REFERENCES user_accounts(user_id) ON DELETE CASCADE, FOREIGN KEY(role_id) REFERENCES roles(role_id) ON DELETE CASCADE)");
            }
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Could not initialise the SQLite data store", exception);
        }
    }

    public Connection connection() throws SQLException { return DriverManager.getConnection(url); }
}
