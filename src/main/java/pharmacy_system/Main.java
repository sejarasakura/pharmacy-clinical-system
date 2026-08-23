package pharmacy_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import pharmacy_system.controller.common.NavigationController;
import pharmacy_system.controller.common.SessionController;
import pharmacy_system.model.security_user.Credential;
import pharmacy_system.model.security_user.PasswordHasher;
import pharmacy_system.model.security_user.RolePermission;
import pharmacy_system.model.security_user.Sha256PasswordHasher;
import pharmacy_system.model.security_user.UserAccount;
import pharmacy_system.storage.clinical_prescription.InMemoryPrescriptionStorage;
import pharmacy_system.storage.clinical_prescription.PrescriptionStorage;
import pharmacy_system.storage.management_dss.InMemoryReportStorage;
import pharmacy_system.storage.management_dss.ReportStorage;
import pharmacy_system.storage.patient_information.InMemoryNotificationStorage;
import pharmacy_system.storage.patient_information.NotificationStorage;
import pharmacy_system.storage.pharmacy_operations.DispenseStorage;
import pharmacy_system.storage.pharmacy_operations.InMemoryDispenseRecordStorage;
import pharmacy_system.storage.pharmacy_operations.SqliteInventoryStorage;
import pharmacy_system.storage.pharmacy_operations.SqliteMedicineStorage;
import pharmacy_system.storage.pharmacy_operations.InventoryStorage;
import pharmacy_system.storage.pharmacy_operations.MedicineStorage;
import pharmacy_system.storage.security_user.CredentialStorage;
import pharmacy_system.storage.security_user.InMemoryUserProfileStorage;
import pharmacy_system.storage.security_user.RolePermissionStorage;
import pharmacy_system.storage.security_user.UserAccountStorage;
import pharmacy_system.storage.security_user.UserProfileStorage;
import pharmacy_system.storage.security_user.SqliteDatabase;
import pharmacy_system.storage.security_user.SqliteCredentialStorage;
import pharmacy_system.storage.security_user.SqliteRolePermissionStorage;
import pharmacy_system.storage.security_user.SqliteUserAccountStorage;

import java.util.Set;
import java.nio.file.Path;

/** Spring Boot entry point and composition root for the fixed application structure. */
@SpringBootApplication(proxyBeanMethods = false)
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    SessionController sessionController() {
        return new SessionController(60);
    }

    @Bean
    NavigationController navigationController(SessionController sessionController) {
        return new NavigationController(sessionController);
    }

    @Bean
    PasswordHasher passwordHasher() {
        return new Sha256PasswordHasher();
    }

    @Bean SqliteDatabase sqliteDatabase() {
        return new SqliteDatabase(Path.of(environmentOrDefault("PHARMACARE_DB_PATH", "data/pharmacare.db")));
    }
    @Bean UserAccountStorage userAccountStorage(SqliteDatabase database) { return new SqliteUserAccountStorage(database); }
    @Bean CredentialStorage credentialStorage(SqliteDatabase database, PasswordHasher hasher) { return new SqliteCredentialStorage(database, hasher); }
    @Bean RolePermissionStorage rolePermissionStorage(SqliteDatabase database) { return new SqliteRolePermissionStorage(database); }
    @Bean UserProfileStorage userProfileStorage() { return new InMemoryUserProfileStorage(); }
    @Bean PrescriptionStorage prescriptionStorage() { return new InMemoryPrescriptionStorage(); }
    @Bean NotificationStorage notificationStorage() { return new InMemoryNotificationStorage(); }
    @Bean MedicineStorage medicineStorage(SqliteDatabase database) { return new SqliteMedicineStorage(database); }
    @Bean InventoryStorage inventoryStorage(SqliteDatabase database) { return new SqliteInventoryStorage(database); }
    @Bean DispenseStorage dispenseStorage() { return new InMemoryDispenseRecordStorage(); }
    @Bean ReportStorage reportStorage() { return new InMemoryReportStorage(); }

    /** Seeds the minimum development identities required to enter the protected UI. */
    @Bean
    CommandLineRunner bootstrapAdministrator(UserAccountStorage accounts,
                                             CredentialStorage credentials,
                                             RolePermissionStorage roles,
                                             PasswordHasher hasher,
                                             SqliteDatabase database) {
        return args -> {
            RolePermission patient = seedRole(roles, "Patient", Set.of(
                    "MANAGE_PROFILE", "VIEW_PRESCRIPTION_STATUS", "VIEW_NOTIFICATIONS"));
            RolePermission doctor = seedRole(roles, "Doctor", Set.of(
                    "MANAGE_PROFILE", "MANAGE_PRESCRIPTION", "PRESCRIPTION_STATUS_UPDATE"));
            RolePermission pharmacist = seedRole(roles, "Pharmacist", Set.of(
                    "MANAGE_PROFILE", "DISPENSE_MEDICATION", "MANAGE_INVENTORY"));
            RolePermission administrator = seedRole(roles, "Administrator", Set.of(
                    "MANAGE_PROFILE", "MANAGE_USER_ACCOUNT", "GENERATE_REPORTS"));

            // Retain all four role seeds even though only Administrator is assigned here.
            if (patient == null || doctor == null || pharmacist == null) {
                throw new IllegalStateException("Operational role bootstrap failed");
            }

            String username = environmentOrDefault("PHARMACARE_ADMIN_USERNAME", "admin");
            String email = environmentOrDefault("PHARMACARE_ADMIN_EMAIL", "admin@pharmacare.local");
            String password = environmentOrDefault("PHARMACARE_ADMIN_PASSWORD", "Admin@123");

            if (accounts.findByUsername(username).isEmpty()) {
                UserAccount pending = new UserAccount(0L, username, email);
                pending.approveRegistration();
                UserAccount admin = accounts.create(pending);
                credentials.create(new Credential(0L, admin.getUserId(), password.toCharArray(), hasher));
                if (!roles.assignRoleToUser(admin.getUserId(), administrator.getRoleId())) {
                    throw new IllegalStateException("Administrator role assignment failed");
                }
            }

            seedDemoAccount(accounts, credentials, roles, hasher,
                    "pharmacist.demo", "pharmacist.demo@pharmacare.local", "Demo@12345", pharmacist);
            UserAccount ainaDemo = seedDemoAccount(accounts, credentials, roles, hasher,
                    "aina.patient", "aina.rahman@pharmacare.local", "Demo@12345", patient);
            UserAccount danielDemo = seedDemoAccount(accounts, credentials, roles, hasher,
                    "daniel.patient", "daniel.wong@pharmacare.local", "Demo@12345", patient);
            UserAccount kavithaDemo = seedDemoAccount(accounts, credentials, roles, hasher,
                    "kavitha.patient", "kavitha.devi@pharmacare.local", "Demo@12345", patient);
            UserAccount faridahDemo = seedDemoAccount(accounts, credentials, roles, hasher,
                    "faridah.patient", "faridah.osman@pharmacare.local", "Demo@12345", patient);
            UserAccount marcusDemo = seedDemoAccount(accounts, credentials, roles, hasher,
                    "marcus.patient", "marcus.lee@pharmacare.local", "Demo@12345", patient);
            UserAccount nurulDemo = seedDemoAccount(accounts, credentials, roles, hasher,
                    "nurul.patient", "nurul.huda@pharmacare.local", "Demo@12345", patient);
            linkProfile(database, 101L, ainaDemo.getUserId());
            linkProfile(database, 102L, danielDemo.getUserId());
            linkProfile(database, 103L, kavithaDemo.getUserId());
            linkProfile(database, 104L, faridahDemo.getUserId());
            linkProfile(database, 105L, marcusDemo.getUserId());
            linkProfile(database, 106L, nurulDemo.getUserId());
        };
    }

    /** Creates an active, role-assigned development identity only when it is absent. */
    private static UserAccount seedDemoAccount(UserAccountStorage accounts,
                                               CredentialStorage credentials,
                                               RolePermissionStorage roles,
                                               PasswordHasher hasher,
                                               String username, String email, String password,
                                               RolePermission role) {
        UserAccount account = accounts.findByUsername(username).orElseGet(() -> {
            UserAccount created = new UserAccount(0L, username, email);
            created.approveRegistration();
            UserAccount persisted = accounts.create(created);
            credentials.create(new Credential(0L, persisted.getUserId(), password.toCharArray(), hasher));
            return persisted;
        });
        roles.assignRoleToUser(account.getUserId(), role.getRoleId());
        return account;
    }

    private static void linkProfile(SqliteDatabase database, long profileId, long userId) {
        try (var connection = database.connection();
             var statement = connection.prepareStatement(
                     "UPDATE user_profiles SET user_id = ?, updated_at = CURRENT_TIMESTAMP WHERE profile_id = ?")) {
            statement.setLong(1, userId);
            statement.setLong(2, profileId);
            statement.executeUpdate();
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException("Could not link the sample patient profile", exception);
        }
    }

    private static RolePermission seedRole(RolePermissionStorage roles,
                                           String name,
                                           Set<String> permissions) {
        return roles.findByRoleName(name)
                .orElseGet(() -> roles.create(new RolePermission(name, permissions, true)));
    }

    private static String environmentOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
