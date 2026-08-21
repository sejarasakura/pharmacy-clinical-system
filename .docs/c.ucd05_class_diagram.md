 @startuml
title UCD-05 — Manage Profile
Class Diagram

skinparam classAttributeIconSize 0
skinparam linetype ortho
hide empty members


package "view.security_user.ucd05_manage_profile" {

    class ManageProfileView <<boundary>> {
        +openProfile() : void
        +showProfile(profile : UserProfile) : void
        +showProfileUnavailable() : void
        +showUpdateSuccess() : void
    }


    class ProfileDetailsView <<boundary>> {
        -displayedProfile : UserProfile

        +display(profile : UserProfile) : void
        +refresh(profile : UserProfile) : void
    }


    class ProfileFormView <<boundary>> {
        -pendingChanges : Map<String, Object>

        +collectChanges() : Map<String, Object>
        +submitChanges() : void

        +showValidationErrors(errors : List<String>) : void
        +showRestrictedFieldError() : void
        +showSaveError() : void
    }
}


package "controller.security_user" {

    class ManageProfileController <<control>> {
        +loadProfile(userId : long) : UserProfile

        +updateProfile(
            userId : long,
            changes : Map<String, Object>
        ) : boolean

        -validateOwnership(
            requestedUserId : long,
            authenticatedUserId : long
        ) : boolean

        -validateEditableFields(
            profile : UserProfile,
            changes : Map<String, Object>
        ) : List<String>
    }
}


package "controller.common" {

    class SessionController <<control>> {
        +getCurrentUserId() : long
        +isAuthenticated() : boolean
        +hasPermission(permissionCode : String) : boolean
    }
}


package "model.security_user.profile" {

    abstract class UserProfile <<entity>> {
        #profileId : long
        #userId : long

        #fullName : String
        #phoneNumber : String
        #contactEmail : String
        #address : String

        #preferences : Map<String, String>

        #createdAt : LocalDateTime
        #updatedAt : LocalDateTime
        #version : long

        +validateProfileData() : List<String>

        +applyChanges(
            changes : Map<String, Object>
        ) : void

        +updateContact(
            phone : String,
            email : String
        ) : void

        +updateAddress(address : String) : void

        +updatePreference(
            key : String,
            value : String
        ) : void
    }


    class DoctorProfile <<entity>> {
        -medicalRegistrationNo : String
        -speciality : String

        +validateProfessionalDetails() : boolean
    }


    class PatientProfile <<entity>> {
        -dateOfBirth : LocalDate
        -emergencyContact : String

        +calculateAge() : int
    }


    class PharmacyProfile <<entity>> {
        -pharmacistRegistrationNo : String
        -pharmacyUnit : String

        +validateProfessionalDetails() : boolean
    }


    class AdminProfile <<entity>> {
        -staffId : String
        -department : String
    }
}


package "storage.security_user" {

    class ProfileStorage <<repository>> {
        +findByProfileId(profileId : long) : UserProfile
        +findByUserId(userId : long) : UserProfile

        +save(profile : UserProfile) : boolean

        +update(
            profile : UserProfile,
            expectedVersion : long
        ) : boolean
    }
}


' =========================
' VIEW STRUCTURE
' =========================

ManageProfileView *-- ProfileDetailsView
ManageProfileView *-- ProfileFormView

ManageProfileView ..> ManageProfileController
ProfileFormView ..> ManageProfileController


' =========================
' CONTROLLER
' =========================

ManageProfileController ..> SessionController : validates current user
ManageProfileController ..> ProfileStorage : reads / updates
ManageProfileController ..> UserProfile : validates changes


' =========================
' INHERITANCE
' =========================

UserProfile <|-- DoctorProfile
UserProfile <|-- PatientProfile
UserProfile <|-- PharmacyProfile
UserProfile <|-- AdminProfile


' =========================
' STORAGE
' =========================

ProfileStorage ..> UserProfile : persists


note right of ManageProfileController
UCD-05 is self-service.

requestedUserId must match
the authenticated user's identity
unless an explicitly authorised
administrative operation exists.
end note


note bottom of UserProfile
Role assignment and account state
do NOT belong to UserProfile.

Those remain UserAccount /
RolePermission responsibilities.
end note

@enduml