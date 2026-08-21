sequenceDiagram
    autonumber

    actor Doctor

    participant MPV as ManagePrescriptionView
    participant PFV as PrescriptionFormView
    participant PIV as PrescriptionItemView

    participant MPC as ManagePrescriptionController

    participant P as Prescription
    participant PI as PrescriptionItem

    participant PS as PrescriptionStorage

    Note over Doctor,PS: Precondition: Doctor is authenticated and authorised

    Doctor->>MPV: Open Manage Prescription
    MPV->>MPC: requestPrescriptionManagement()

    alt Create New Prescription

        Doctor->>PFV: Select Create Prescription
        PFV->>MPC: createPrescription(patientId)

        MPC->>P: create(patientId)
        P-->>MPC: new Prescription

        MPC-->>PFV: displayPrescriptionForm()

        Doctor->>PIV: Enter medicine, dosage,<br/>quantity, frequency, instructions
        PIV->>MPC: submitPrescriptionItem(itemData)

        MPC->>PI: create(itemData)
        PI->>PI: validateMedicationData()

        alt Invalid medicine / dosage / quantity
            PI-->>MPC: validationFailed
            MPC-->>PIV: showValidationError()
            PIV-->>Doctor: Display invalid fields

        else Valid medication data
            PI-->>MPC: valid PrescriptionItem

            MPC->>P: addPrescriptionItem(PI)
            P-->>MPC: itemAdded

            Doctor->>PFV: Submit Prescription
            PFV->>MPC: savePrescription()

            MPC->>P: validatePrescription()

            alt Prescription invalid
                P-->>MPC: validationFailed
                MPC-->>PFV: showValidationError()
                PFV-->>Doctor: Correct prescription information

            else Prescription valid
                P-->>MPC: valid

                MPC->>PS: save(P)
                PS-->>MPC: prescriptionSaved

                MPC-->>MPV: prescriptionCreated()
                MPV-->>Doctor: Display created prescription
            end
        end

    else View Prescription

        Doctor->>MPV: Select Prescription
        MPV->>MPC: viewPrescription(prescriptionId)

        MPC->>PS: findById(prescriptionId)

        alt Prescription not found
            PS-->>MPC: notFound
            MPC-->>MPV: showNotFound()
            MPV-->>Doctor: Prescription not found

        else Prescription found
            PS-->>MPC: Prescription
            MPC-->>MPV: displayPrescription(P)
            MPV-->>Doctor: Show prescription details
        end

    else Edit Prescription

        Doctor->>MPV: Select Edit Prescription
        MPV->>MPC: editPrescription(prescriptionId)

        MPC->>PS: findById(prescriptionId)
        PS-->>MPC: Prescription

        MPC-->>PFV: displayEditablePrescription(P)

        Doctor->>PFV: Modify prescription details
        PFV->>MPC: submitChanges(changes)

        MPC->>P: updateClinicalDetails(changes)
        P->>P: validatePrescription()

        alt Invalid modification
            P-->>MPC: validationFailed
            MPC-->>PFV: showValidationError()
            PFV-->>Doctor: Display validation errors

        else Modification valid
            P-->>MPC: updatedPrescription

            MPC->>PS: update(P)

            alt Persistence failure
                PS-->>MPC: updateFailed
                MPC-->>PFV: showSaveError()
                PFV-->>Doctor: Update failed

            else Update successful
                PS-->>MPC: updateSuccessful
                MPC-->>MPV: prescriptionUpdated()
                MPV-->>Doctor: Display updated prescription
            end
        end

    else Cancel Prescription

        Doctor->>MPV: Select Cancel Prescription
        MPV->>MPC: cancelPrescription(prescriptionId)

        MPC->>PS: findById(prescriptionId)
        PS-->>MPC: Prescription

        MPC->>P: checkCancellationAllowed()

        alt Cancellation not allowed
            P-->>MPC: rejected
            MPC-->>MPV: cancellationRejected()
            MPV-->>Doctor: Cannot cancel prescription

        else Cancellation allowed
            P-->>MPC: allowed
            MPC->>P: cancel()
            P-->>MPC: cancelledPrescription

            MPC->>PS: update(P)
            PS-->>MPC: updateSuccessful

            MPC-->>MPV: prescriptionCancelled()
            MPV-->>Doctor: Display cancellation confirmation
        end
    end