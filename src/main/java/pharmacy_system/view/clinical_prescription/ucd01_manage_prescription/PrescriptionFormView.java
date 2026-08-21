package pharmacy_system.view.clinical_prescription.ucd01_manage_prescription;

import pharmacy_system.controller.clinical_prescription.ManagePrescriptionController;
import pharmacy_system.model.clinical_prescription.Prescription;
import pharmacy_system.model.clinical_prescription.PrescriptionItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Form view for prescription data collection and display (UCD-01: Manage Prescription).
 * Handles input fields for prescription-level data and delegates item management to PrescriptionItemView.
 * Surfaces field validation errors.
 *
 * Requirement traceability: 4.1, 4.2
 */
public class PrescriptionFormView {

    private final ManagePrescriptionController controller;
    private long patientId;
    private String clinicalNotes = "";
    private List<PrescriptionItemView> itemViews = new ArrayList<>();
    private String lastErrorMessage = "";
    private String lastSaveErrorMessage = "";

    public PrescriptionFormView(ManagePrescriptionController controller) {
        this.controller = controller;
    }

    /**
     * Collects prescription data from the form.
     * Returns a map with keys: "patientId" (long), "clinicalNotes" (String), "items" (List<PrescriptionItem>).
     * Requirements: 4.1 (prescription creation with items)
     */
    public Map<String, Object> collectPrescriptionData() {
        Map<String, Object> data = new HashMap<>();
        data.put("patientId", this.patientId);
        data.put("clinicalNotes", this.clinicalNotes);

        List<PrescriptionItem> items = new ArrayList<>();
        for (PrescriptionItemView itemView : this.itemViews) {
            Map<String, Object> itemData = itemView.collectItemData();
            if (itemData != null && !itemData.isEmpty()) {
                // Convert map to PrescriptionItem
                PrescriptionItem item = mapToPrescriptionItem(itemData);
                if (item != null) {
                    items.add(item);
                }
            }
        }
        data.put("items", items);

        return data;
    }

    /**
     * Populates the form with an existing prescription's data.
     * Requirements: 4.1 (view prescription), 4.2 (edit prescription)
     */
    public void populateForm(Prescription prescription) {
        if (prescription == null) {
            this.lastErrorMessage = "Prescription is null";
            return;
        }

        this.patientId = prescription.getPatientId();
        this.clinicalNotes = prescription.getClinicalNotes();
        this.itemViews.clear();

        for (PrescriptionItem item : prescription.getItems()) {
            PrescriptionItemView itemView = new PrescriptionItemView(this.controller);
            itemView.populateItem(item);
            this.itemViews.add(itemView);
        }
    }

    /**
     * Adds a new prescription item view to the form.
     * Requirements: 4.1 (prescription with 1..50 items)
     */
    public void addItemView() {
        if (this.itemViews.size() < 50) {
            this.itemViews.add(new PrescriptionItemView(this.controller));
        }
    }

    /**
     * Removes a prescription item view from the form.
     * Requirements: 4.1 (manage items)
     */
    public void removeItemView(int index) {
        if (index >= 0 && index < this.itemViews.size()) {
            this.itemViews.remove(index);
        }
    }

    /**
     * Gets the number of item views currently in the form.
     * For testing/UI feedback.
     */
    public int getItemCount() {
        return this.itemViews.size();
    }

    /**
     * Gets a specific item view by index.
     * For testing purposes.
     */
    public PrescriptionItemView getItemView(int index) {
        if (index >= 0 && index < this.itemViews.size()) {
            return this.itemViews.get(index);
        }
        return null;
    }

    /**
     * Submits the prescription form and calls the controller to create/update.
     * Requirements: 4.1, 4.2
     */
    public void submitPrescription() {
        this.lastErrorMessage = "";
        this.lastSaveErrorMessage = "";
        // In a real UI, this would trigger the controller action
        // Here it just marks the form as submitted
    }

    /**
     * Shows field validation errors.
     * Requirements: 4.6 (validation error indication)
     */
    public void showValidationErrors(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            this.lastErrorMessage = "Validation error";
        } else {
            this.lastErrorMessage = "Validation errors: " + String.join("; ", errors);
        }
    }

    /**
     * Shows a save error (e.g., not found, permission denied).
     * Requirements: 4.6, 4.7
     */
    public void showSaveError() {
        this.lastSaveErrorMessage = "Failed to save prescription";
    }

    /**
     * Shows a specific save error message.
     * Requirements: 4.6, 4.7
     */
    public void showSaveError(String message) {
        this.lastSaveErrorMessage = message;
    }

    /**
     * Gets the patient ID from the form.
     * For testing purposes.
     */
    public long getPatientId() {
        return this.patientId;
    }

    /**
     * Sets the patient ID in the form.
     * For testing purposes.
     */
    public void setPatientId(long patientId) {
        this.patientId = patientId;
    }

    /**
     * Gets the clinical notes from the form.
     * For testing purposes.
     */
    public String getClinicalNotes() {
        return this.clinicalNotes;
    }

    /**
     * Sets the clinical notes in the form.
     * For testing purposes.
     */
    public void setClinicalNotes(String clinicalNotes) {
        this.clinicalNotes = clinicalNotes;
    }

    /**
     * Gets the last validation error message.
     * For testing purposes.
     */
    public String getLastErrorMessage() {
        return this.lastErrorMessage;
    }

    /**
     * Gets the last save error message.
     * For testing purposes.
     */
    public String getLastSaveErrorMessage() {
        return this.lastSaveErrorMessage;
    }

    /**
     * Converts a map of item data to a PrescriptionItem.
     * Helper method for collectPrescriptionData.
     */
    private PrescriptionItem mapToPrescriptionItem(Map<String, Object> itemData) {
        try {
            long medicineId = ((Number) itemData.get("medicineId")).longValue();
            String medicineName = (String) itemData.get("medicineName");
            String dosage = (String) itemData.get("dosage");
            int quantity = ((Number) itemData.get("quantity")).intValue();
            String frequency = (String) itemData.get("frequency");
            String instructions = (String) itemData.get("instructions");

            return new PrescriptionItem(medicineId, medicineName, dosage, quantity, frequency, instructions);
        } catch (Exception e) {
            return null;
        }
    }
}
