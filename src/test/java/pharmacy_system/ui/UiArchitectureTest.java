package pharmacy_system.ui;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;

class UiArchitectureTest {
    private static final Path JSP_ROOT = Path.of("src/main/webapp/WEB-INF/jsp");

    @Test
    void jspSourcesContainNoScriptletsInlineStylesOrInlineScripts() throws Exception {
        Pattern scriptlet = Pattern.compile("<%(?!@)");
        Pattern inlineScript = Pattern.compile("<script(?![^>]*\\bsrc=)", Pattern.CASE_INSENSITIVE);
        try (var paths = Files.walk(JSP_ROOT)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String source = Files.readString(path);
                assertFalse(scriptlet.matcher(source).find(), () -> "Scriptlet in " + path);
                assertFalse(source.contains("style=\""), () -> "Inline style in " + path);
                assertFalse(inlineScript.matcher(source).find(), () -> "Inline script in " + path);
            }
        }
    }

    @Test
    void fixedControllerAndLayerBoundariesHold() {
        JavaClasses imported = new ClassFileImporter().importPackages("pharmacy_system");
        classes().that().areAnnotatedWith(Controller.class).should().resideInAnyPackage(
                "pharmacy_system.controller.common..",
                "pharmacy_system.controller.security_user..",
                "pharmacy_system.controller.clinical_prescription..",
                "pharmacy_system.controller.patient_information..",
                "pharmacy_system.controller.pharmacy_operations..",
                "pharmacy_system.controller.management_dss..").check(imported);
        noClasses().should().resideInAnyPackage("..service..", "..dto..", "..repository..",
                "..mapper..", "..config..", "..facade..", "..usecase..").check(imported);
    }

    @Test
    void everyProgressiveEnhancementModuleGuardsForMissingTargets() throws Exception {
        Path modules = Path.of("src/main/resources/static/js");
        try (var paths = Files.list(modules)) {
            var files = paths.filter(path -> path.toString().endsWith(".js")).toList();
            assertTrue(files.size() == 6);
            for (Path file : files) {
                String source = Files.readString(file);
                assertTrue(source.contains("return"), () -> "No no-op guard in " + file);
            }
        }
    }
}
