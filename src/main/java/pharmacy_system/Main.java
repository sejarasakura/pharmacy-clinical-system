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
import pharmacy_system.storage.pharmacy_operations.InMemoryInventoryItemStorage;
import pharmacy_system.storage.pharmacy_operations.InMemoryMedicineStorage;
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

    @Bean SqliteDatabase sqliteDatabase() { return new SqliteDatabase(Path.of("data", "pharmacare.db")); }
    @Bean UserAccountStorage userAccountStorage(SqliteDatabase database) { return new SqliteUserAccountStorage(database); }
    @Bean CredentialStorage credentialStorage(SqliteDatabase database, PasswordHasher hasher) { return new SqliteCredentialStorage(database, hasher); }
    @Bean RolePermissionStorage rolePermissionStorage(SqliteDatabase database) { return new SqliteRolePermissionStorage(database); }
    @Bean UserProfileStorage userProfileStorage() { return new InMemoryUserProfileStorage(); }
    @Bean PrescriptionStorage prescriptionStorage() { return new InMemoryPrescriptionStorage(); }
    @Bean NotificationStorage notificationStorage() { return new InMemoryNotificationStorage(); }
    @Bean MedicineStorage medicineStorage() { return new InMemoryMedicineStorage(); }
    @Bean InventoryStorage inventoryStorage() { return new InMemoryInventoryItemStorage(); }
    @Bean DispenseStorage dispenseStorage() { return new InMemoryDispenseRecordStorage(); }
    @Bean ReportStorage reportStorage() { return new InMemoryReportStorage(); }

    /** Seeds the minimum development identities required to enter the protected UI. */
    @Bean
    CommandLineRunner bootstrapAdministrator(UserAccountStorage accounts,
                                             CredentialStorage credentials,
                                             RolePermissionStorage roles,
                                             PasswordHasher hasher) {
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

            if (accounts.findByUsername(username).isPresent()) {
                return;
            }

            UserAccount pending = new UserAccount(0L, username, email);
            pending.approveRegistration();
            UserAccount admin = accounts.create(pending);
            credentials.create(new Credential(0L, admin.getUserId(), password.toCharArray(), hasher));
            if (!roles.assignRoleToUser(admin.getUserId(), administrator.getRoleId())) {
                throw new IllegalStateException("Administrator role assignment failed");
            }
        };
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
