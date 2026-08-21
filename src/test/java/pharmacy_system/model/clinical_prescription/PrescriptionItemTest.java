package pharmacy_system.model.clinical_prescription;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PrescriptionItem} model.
 * 
 * Validates: Requirements 4.1, 4.6
 */
class PrescriptionItemTest {

    @Test
    void constructor_withValidFields_createsItem() {
        PrescriptionItem item = new PrescriptionItem(
            123L,
            456L,
            "Paracetamol 500mg",
            "500mg",
            "Twice daily",
            "Take after meals",
            20
        );
        
        assertEquals(123L, item.getPrescriptionItemId());
        assertEquals(456L, item.getMedicineId());
        assertEquals("Paracetamol 500mg", item.getMedicineName());
        assertEquals("500mg", item.getDosage());
        assertEquals("Twice daily", item.getFrequency());
        assertEquals("Take after meals", item.getInstructions());
        assertEquals(20, item.getQuantity());
    }

    @Test
    void constructor_withoutPrescriptionItemId_createsItemWithZeroId() {
        PrescriptionItem item = new PrescriptionItem(
            456L,
            "Paracetamol 500mg",
            "500mg",
            "Twice daily",
            "Take after meals",
            20
        );
        
        assertEquals(0L, item.getPrescriptionItemId());
        assertEquals(456L, item.getMedicineId());
        assertEquals("Paracetamol 500mg", item.getMedicineName());
        assertEquals("500mg", item.getDosage());
        assertEquals("Twice daily", item.getFrequency());
        assertEquals("Take after meals", item.getInstructions());
        assertEquals(20, item.getQuantity());
    }

    @Test
    void validateRequiredFields_withAllValidFields_returnsEmptyList() {
        PrescriptionItem item = createValidItem();
        
        List<String> errors = item.validateRequiredFields();
        
        assertTrue(errors.isEmpty());
        assertTrue(item.hasRequiredFields());
    }

    @Test
    void validateRequiredFields_withZeroMedicineId_returnsError() {
        PrescriptionItem item = new PrescriptionItem(
            0L,
            "Paracetamol 500mg",
            "500mg",
            "Twice daily",
            "Take after meals",
            20
        );
        
        List<String> errors = item.validateRequiredFields();
        
        assertEquals(List.of("medicineId"), errors);
        assertFalse(item.hasRequiredFields());
    }

    @Test
    void validateRequiredFields_withNegativeMedicineId_returnsError() {
        PrescriptionItem item = new PrescriptionItem(
            -1L,
            "Paracetamol 500mg",
            "500mg",
            "Twice daily",
            "Take after meals",
            20
        );
        
        List<String> errors = item.validateRequiredFields();
        
        assertEquals(List.of("medicineId"), errors);
        assertFalse(item.hasRequiredFields());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t", "\n"})
    void validateRequiredFields_withBlankMedicineName_returnsError(String medicineName) {
        PrescriptionItem item = new PrescriptionItem(
            456L,
            medicineName,
            "500mg",
            "Twice daily",
            "Take after meals",
            20
        );
        
        List<String> errors = item.validateRequiredFields();
        
        assertEquals(List.of("medicineName"), errors);
        assertFalse(item.hasRequiredFields());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t", "\n"})
    void validateRequiredFields_withBlankDosage_returnsError(String dosage) {
        PrescriptionItem item = new PrescriptionItem(
            456L,
            "Paracetamol 500mg",
            dosage,
            "Twice daily",
            "Take after meals",
            20
        );
        
        List<String> errors = item.validateRequiredFields();
        
        assertEquals(List.of("dosage"), errors);
        assertFalse(item.hasRequiredFields());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t", "\n"})
    void validateRequiredFields_withBlankFrequency_returnsError(String frequency) {
        PrescriptionItem item = new PrescriptionItem(
            456L,
            "Paracetamol 500mg",
            "500mg",
            frequency,
            "Take after meals",
            20
        );
        
        List<String> errors = item.validateRequiredFields();
        
        assertEquals(List.of("frequency"), errors);
        assertFalse(item.hasRequiredFields());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t", "\n"})
    void validateRequiredFields_withBlankInstructions_returnsError(String instructions) {
        PrescriptionItem item = new PrescriptionItem(
            456L,
            "Paracetamol 500mg",
            "500mg",
            "Twice daily",
            instructions,
            20
        );
        
        List<String> errors = item.validateRequiredFields();
        
        assertEquals(List.of("instructions"), errors);
        assertFalse(item.hasRequiredFields());
    }

    @Test
    void validateRequiredFields_withZeroQuantity_returnsError() {
        PrescriptionItem item = new PrescriptionItem(
            456L,
            "Paracetamol 500mg",
            "500mg",
            "Twice daily",
            "Take after meals",
            0
        );
        
        List<String> errors = item.validateRequiredFields();
        
        assertEquals(List.of("quantity"), errors);
        assertFalse(item.hasRequiredFields());
    }

    @Test
    void validateRequiredFields_withNegativeQuantity_returnsError() {
        PrescriptionItem item = new PrescriptionItem(
            456L,
            "Paracetamol 500mg",
            "500mg",
            "Twice daily",
            "Take after meals",
            -5
        );
        
        List<String> errors = item.validateRequiredFields();
        
        assertEquals(List.of("quantity"), errors);
        assertFalse(item.hasRequiredFields());
    }

    @Test
    void validateRequiredFields_withPositiveQuantity_passesValidation() {
        PrescriptionItem item = new PrescriptionItem(
            456L,
            "Paracetamol 500mg",
            "500mg",
            "Twice daily",
            "Take after meals",
            1
        );
        
        List<String> errors = item.validateRequiredFields();
        
        assertTrue(errors.isEmpty());
        assertTrue(item.hasRequiredFields());
    }

    @Test
    void validateRequiredFields_withMultipleErrors_returnsAllErrors() {
        PrescriptionItem item = new PrescriptionItem(
            -1L,
            null,
            "",
            "   ",
            "\t",
            0
        );
        
        List<String> errors = item.validateRequiredFields();
        
        assertEquals(6, errors.size());
        assertTrue(errors.contains("medicineId"));
        assertTrue(errors.contains("medicineName"));
        assertTrue(errors.contains("dosage"));
        assertTrue(errors.contains("frequency"));
        assertTrue(errors.contains("instructions"));
        assertTrue(errors.contains("quantity"));
        assertFalse(item.hasRequiredFields());
    }

    @Test
    void validateRequiredFields_returnsImmutableList() {
        PrescriptionItem item = createValidItem();
        
        List<String> errors = item.validateRequiredFields();
        
        // Should be immutable
        assertThrows(UnsupportedOperationException.class, () -> errors.add("extraError"));
    }

    @Test
    void setters_updateFieldsCorrectly() {
        PrescriptionItem item = createValidItem();
        
        item.setPrescriptionItemId(999L);
        item.setMedicineId(888L);
        item.setMedicineName("Ibuprofen 400mg");
        item.setDosage("400mg");
        item.setFrequency("Three times daily");
        item.setInstructions("Take with food");
        item.setQuantity(30);
        
        assertEquals(999L, item.getPrescriptionItemId());
        assertEquals(888L, item.getMedicineId());
        assertEquals("Ibuprofen 400mg", item.getMedicineName());
        assertEquals("400mg", item.getDosage());
        assertEquals("Three times daily", item.getFrequency());
        assertEquals("Take with food", item.getInstructions());
        assertEquals(30, item.getQuantity());
    }

    @Test
    void setters_preserveValidation() {
        PrescriptionItem item = createValidItem();
        assertTrue(item.hasRequiredFields());
        
        item.setMedicineId(0L);
        assertFalse(item.hasRequiredFields());
        
        item.setMedicineId(456L);
        assertTrue(item.hasRequiredFields());
        
        item.setQuantity(-1);
        assertFalse(item.hasRequiredFields());
        
        item.setQuantity(20);
        assertTrue(item.hasRequiredFields());
    }

    private PrescriptionItem createValidItem() {
        return new PrescriptionItem(
            123L,
            456L,
            "Paracetamol 500mg",
            "500mg",
            "Twice daily",
            "Take after meals",
            20
        );
    }
}