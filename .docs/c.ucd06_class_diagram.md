@startuml
title UCD-06 — Manage User Account
Class Diagram

skinparam classAttributeIconSize 0
skinparam linetype ortho
hide empty members


package "view.security_user.ucd06_manage_user_account" {

    class ManageUserAccountView <<boundary>> {
        +openUserManagement() : void

        +showAccounts(accounts : List<UserAccount>) : void

        +showOperationSuccess(message : String) : void
        +showOperationFailure(message : String) : void
    }


    class UserAccountListView <<boundary>> {
        -selectedUserId : long

        +displayAccounts(accounts : List<UserAccount>) : void
        +selectAccount(userId : long) : void
        +getSelectedUserId() : long
    }


    class UserAccountFormView <<boundary>> {
        -username : String
        -email : String
        -selectedRoleId : long

        +collectAccountData() : Map<String, Object>

        +showValidationErrors(errors : List<String>) : void
        +showDuplicateAccountError() : void
        +showInvalidRoleError() : void
        +showAccountNotFound() : void
        +showConcurrentUpdateError() : void
    }
}


package "controller.security_user" {

    class ManageUserAccountController <<control>> {

        +loadUserAccounts() : List<UserAccount>

        +createUserAccount(
            username : String,
            email : String
        ) : UserAccount

        +approveRegistration(userId : long) : boolean

        +assignRole(
            userId : long,
            roleId : long
        ) : boolean

        +removeRole(
            userId : long,
            roleId : long
        ) : boolean

        +enableAccount(userId : long) : boolean

        +disableAccount(
            userId : long,
            reason : String
        ) : boolean

        +unlockAccount(userId : long) : boolean

        -validateAccountData(
            account : UserAccount
        ) : List<String>

        -validateRoleAssignment(
            actorId : long,
            userId : long,
            role : RolePermission
        ) : boolean
    }
}


package "controller.common" {

    class SessionController <<control>> {
        +getCurrentUserId() : long
        +hasPermission(permissionCode : String) : boolean
        +requirePermission(permissionCode : String) : void
    }
}


package "model.security_user" {

    class UserAccount <<entity>> {

        -userId : long
        -username : String
        -email : String

        -status : AccountStatus
        -registrationApproved : boolean

        -createdAt : LocalDateTime
        -updatedAt : LocalDateTime
        -disabledAt : LocalDateTime
        -disabledReason : String

        -version : long

        +validateRequiredFields() : List<String>

        +approveRegistration() : void

        +enable() : void

        +disable(reason : String) : void

        +lock() : void

        +unlock() : void

        +isActive() : boolean

        +canAuthenticate() : boolean
    }


    class RolePermission <<entity>> {

        -roleId : long
        -roleName : String
        -description : String

        -permissionCodes : Set<String>
        -active : boolean

        +hasPermission(permissionCode : String) : boolean

        +isActive() : boolean

        +validateAssignment() : boolean
    }


    enum AccountStatus {
        PENDING
        ACTIVE
        DISABLED
        LOCKED
    }
}


package "storage.security_user" {

    class UserAccountStorage <<repository>> {

        +findAll() : List<UserAccount>

        +findById(userId : long) : UserAccount

        +findByUsername(username : String) : UserAccount

        +findByEmail(email : String) : UserAccount

        +existsByUsername(username : String) : boolean

        +existsByEmail(email : String) : boolean

        +save(account : UserAccount) : boolean

        +update(
            account : UserAccount,
            expectedVersion : long
        ) : boolean
    }


    class RolePermissionStorage <<repository>> {

        +findRole(roleId : long) : RolePermission

        +findAllRoles() : List<RolePermission>

        +findRolesByUserId(
            userId : long
        ) : List<RolePermission>

        +assignRole(
            userId : long,
            roleId : long
        ) : boolean

        +removeRole(
            userId : long,
            roleId : long
        ) : boolean
    }
}


' =========================
' VIEW STRUCTURE
' =========================

ManageUserAccountView *-- UserAccountListView
ManageUserAccountView *-- UserAccountFormView

ManageUserAccountView ..> ManageUserAccountController
UserAccountListView ..> ManageUserAccountController
UserAccountFormView ..> ManageUserAccountController


' =========================
' CONTROLLER
' =========================

ManageUserAccountController ..> SessionController : checks admin permission

ManageUserAccountController ..> UserAccountStorage
ManageUserAccountController ..> RolePermissionStorage

ManageUserAccountController ..> UserAccount
ManageUserAccountController ..> RolePermission


' =========================
' DOMAIN RELATIONSHIPS
' =========================

UserAccount "0..*" -- "0..*" RolePermission : assigned >

UserAccount --> AccountStatus


' =========================
' STORAGE
' =========================

UserAccountStorage ..> UserAccount : persists
RolePermissionStorage ..> RolePermission : persists


note right of ManageUserAccountController
Administrative commands must
re-check authorisation here.

Hiding buttons in the View
is NOT sufficient access control.
end note


note bottom of RolePermissionStorage
The user-role relationship can
be implemented using a join table:

user_role
- user_id
- role_id

This avoids embedding permissions
directly inside UserAccount.
end note


note bottom of UserAccount
version supports optimistic locking
for concurrent administrative updates.
end note

@enduml