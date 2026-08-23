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
                ensureColumn(connection, "credentials", "reset_token_hash", "TEXT");
                ensureColumn(connection, "credentials", "reset_token_expiry", "TEXT");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS roles (role_id INTEGER PRIMARY KEY AUTOINCREMENT, role_name TEXT NOT NULL UNIQUE, permissions TEXT NOT NULL, active INTEGER NOT NULL, version INTEGER NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS user_roles (user_id INTEGER NOT NULL, role_id INTEGER NOT NULL, PRIMARY KEY(user_id, role_id), FOREIGN KEY(user_id) REFERENCES user_accounts(user_id) ON DELETE CASCADE, FOREIGN KEY(role_id) REFERENCES roles(role_id) ON DELETE CASCADE)");
                // Domain tables mirror the model aggregates.  The active application adapters
                // for these domains are still in-memory, but retaining the schema here keeps
                // development data durable and provides a safe migration target for SQLite adapters.
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS user_profiles (profile_id INTEGER PRIMARY KEY, user_id INTEGER UNIQUE, profile_type TEXT NOT NULL, full_name TEXT NOT NULL, phone_number TEXT, contact_email TEXT, address TEXT, preferences_json TEXT NOT NULL DEFAULT '{}', patient_identifier TEXT UNIQUE, date_of_birth TEXT, emergency_contact TEXT, professional_registration TEXT, specialization TEXT, department TEXT, branch TEXT, created_at TEXT NOT NULL, updated_at TEXT NOT NULL, version INTEGER NOT NULL, FOREIGN KEY(user_id) REFERENCES user_accounts(user_id) ON DELETE SET NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS medicines (medicine_id INTEGER PRIMARY KEY, medicine_code TEXT NOT NULL UNIQUE, medicine_name TEXT NOT NULL, generic_name TEXT, dosage_form TEXT, strength TEXT, unit TEXT, description TEXT, active INTEGER NOT NULL, created_at TEXT NOT NULL, updated_at TEXT NOT NULL, version INTEGER NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS inventory_items (inventory_id INTEGER PRIMARY KEY, medicine_id INTEGER NOT NULL, batch_number TEXT NOT NULL, expiry_date TEXT NOT NULL, quantity_on_hand INTEGER NOT NULL CHECK(quantity_on_hand >= 0), reorder_level INTEGER NOT NULL CHECK(reorder_level >= 0), version INTEGER NOT NULL, UNIQUE(medicine_id, batch_number), FOREIGN KEY(medicine_id) REFERENCES medicines(medicine_id))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS stock_movements (movement_id INTEGER PRIMARY KEY, inventory_id INTEGER NOT NULL, medicine_id INTEGER NOT NULL, movement_type TEXT NOT NULL, quantity_delta INTEGER NOT NULL, balance_after INTEGER NOT NULL CHECK(balance_after >= 0), reason TEXT, performed_by INTEGER NOT NULL, created_at TEXT NOT NULL, FOREIGN KEY(inventory_id) REFERENCES inventory_items(inventory_id), FOREIGN KEY(medicine_id) REFERENCES medicines(medicine_id), FOREIGN KEY(performed_by) REFERENCES user_accounts(user_id))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS prescriptions (prescription_id INTEGER PRIMARY KEY, patient_id INTEGER NOT NULL, doctor_id INTEGER NOT NULL, clinical_notes TEXT, status TEXT NOT NULL, issued_at TEXT, status_changed_at TEXT, status_changed_by INTEGER, status_change_reason TEXT, created_at TEXT NOT NULL, updated_at TEXT NOT NULL, cancelled_at TEXT, cancellation_reason TEXT, version INTEGER NOT NULL, FOREIGN KEY(patient_id) REFERENCES user_profiles(profile_id), FOREIGN KEY(doctor_id) REFERENCES user_accounts(user_id), FOREIGN KEY(status_changed_by) REFERENCES user_accounts(user_id))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS prescription_items (prescription_item_id INTEGER PRIMARY KEY, prescription_id INTEGER NOT NULL, medicine_id INTEGER NOT NULL, medicine_name TEXT NOT NULL, dosage TEXT NOT NULL, dosage_unit TEXT NOT NULL, frequency TEXT NOT NULL, route TEXT NOT NULL, duration_days INTEGER NOT NULL CHECK(duration_days > 0), instructions TEXT, quantity INTEGER NOT NULL CHECK(quantity > 0), FOREIGN KEY(prescription_id) REFERENCES prescriptions(prescription_id) ON DELETE CASCADE, FOREIGN KEY(medicine_id) REFERENCES medicines(medicine_id))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS dispense_records (dispense_id INTEGER PRIMARY KEY, prescription_id INTEGER NOT NULL UNIQUE, patient_id INTEGER NOT NULL, pharmacist_id INTEGER, required_quantities_json TEXT NOT NULL, dispensed_quantities_json TEXT NOT NULL, status TEXT NOT NULL, verified_at TEXT, dispensed_at TEXT, created_at TEXT NOT NULL, updated_at TEXT NOT NULL, failure_reason TEXT, version INTEGER NOT NULL, FOREIGN KEY(prescription_id) REFERENCES prescriptions(prescription_id), FOREIGN KEY(patient_id) REFERENCES user_profiles(profile_id), FOREIGN KEY(pharmacist_id) REFERENCES user_accounts(user_id))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS notifications (notification_id INTEGER PRIMARY KEY, patient_id INTEGER NOT NULL, prescription_id INTEGER NOT NULL, event_type TEXT NOT NULL, notification_type TEXT NOT NULL, title TEXT NOT NULL, message TEXT NOT NULL, recipient TEXT, delivery_status TEXT NOT NULL, deduplication_key TEXT NOT NULL UNIQUE, created_at TEXT NOT NULL, delivered_at TEXT, failed_at TEXT, read_at TEXT, failure_reason TEXT, FOREIGN KEY(patient_id) REFERENCES user_profiles(profile_id), FOREIGN KEY(prescription_id) REFERENCES prescriptions(prescription_id))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS reports (report_id INTEGER PRIMARY KEY, report_type TEXT NOT NULL, title TEXT NOT NULL, criteria_json TEXT NOT NULL, generated_by INTEGER NOT NULL, generated_at TEXT NOT NULL, row_count INTEGER NOT NULL, data_json TEXT NOT NULL, version INTEGER NOT NULL, FOREIGN KEY(generated_by) REFERENCES user_accounts(user_id))");
            }
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Could not initialise the SQLite data store", exception);
        }
    }

    public Connection connection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private static void ensureColumn(Connection connection, String table, String column, String type)
            throws SQLException {
        try (var columns = connection.createStatement().executeQuery("PRAGMA table_info(" + table + ")")) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("name"))) return;
            }
        }
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        }
    }
}
