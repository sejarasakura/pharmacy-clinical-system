package pharmacy_system.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PharmaCareStylesheetTest {

    @Test
    void servedStylesheetIsTheAuthoritativeDesignSystemAsset() throws Exception {
        Path source = Path.of(".docs/pharmacare.css");
        Path served = Path.of("src/main/resources/static/css/pharmacare.css");
        assertArrayEquals(Files.readAllBytes(source), Files.readAllBytes(served));

        try (var paths = Files.walk(Path.of("src/main/resources/static"))) {
            List<Path> stylesheets = paths.filter(path -> path.toString().endsWith(".css")).toList();
            assertEquals(List.of(served), stylesheets);
        }
    }
}
