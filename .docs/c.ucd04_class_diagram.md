@startuml
title UCD-04 — Authenticate & Authorise
Class Diagram

skinparam classAttributeIconSize 0
skinparam linetype ortho
hide empty members

package "view.security_user.ucd04_authenticate_authorise" {

    class AuthenticateAuthoriseView <<boundary>> {
        +submitCredentials(username : String, password : char[]) : void
        +showAuthenticationSuccess() : void
        +showAuthenticationFailure(message : String) : void
        +showAccountUnavailable(message : String) : void
    }

    class LoginFormView <<boundary>> {
        -usernameInput : String
        -passwordInput : char[]

        +getUsername() : String
        +getPassword() : char[]
        +clearPassword() : void
        +showLoginError(message : String) : void
    }

    class AccessDeniedView <<boundary>> {
        -reason : String

        +showAccessDenied(reason : String) : void
    }
}


package "controller.security_user" {

    class AuthenticateAuthoriseController <<control>> {
        +authenticate(username : String, password : char[]) : boolean
        +authorise(userId : long, permissionCode : String) : boolean

        +requestPasswordReset(email : String) : boolean
        +resetPassword(token : String, newPassword : char[]) : boolean

        +logout() : void
    }
}


package "controller.common" {

    class SessionController <<control>> {
        -currentUserId : long
        -authenticated : boolean
        -expiresAt : LocalDateTime
        -permissionCodes : Set<String>

        +establishSession(account : UserAccount, roles : List<RolePermission>) : void
        +getCurrentUserId() : long
        +isAuthenticated() : boolean
        +hasPermission(permissionCode : String) : boolean
        +invalidateSession() : void
        +isExpired() : boolean
    }

    class NavigationController <<control>> {
        +navigateToAuthorisedHome() : void
        +navigateToLogin() : void
        +navigateToAccessDenied() : void
    }
}


package "model.security_user" {

    class UserAccount <<entity>> {
        -userId : long
        -username : String
        -email : String
        -status : AccountStatus
        -createdAt : LocalDateTime
        -updatedAt : LocalDateTime
        -lastLoginAt : LocalDateTime
        -version : long

        +isActive() : boolean
        +isDisabled() : boolean
        +isLocked() : boolean
        +recordSuccessfulLogin() : void
    }


    class Credential <<entity>> {
        -credentialId : long
        -userId : long

        -passwordHash : String
        -failedAttempts : int
        -lockedUntil : LocalDateTime

        -resetTokenHash : String
        -resetTokenExpiry : LocalDateTime
        -passwordUpdatedAt : LocalDateTime

        +verifyPassword(password : char[]) : boolean
        +recordFailedAttempt() : void
        +resetFailedAttempts() : void
        +isTemporarilyLocked() : boolean

        +setResetToken(tokenHash : String, expiry : LocalDateTime) : void
        +isResetTokenValid(token : String) : boolean
        +changePasswordHash(newHash : String) : void
        +clearResetToken() : void
    }


    class RolePermission <<entity>> {
        -roleId : long
        -roleName : String
        -description : String
        -permissionCodes : Set<String>
        -active : boolean

        +hasPermission(permissionCode : String) : boolean
        +isActive() : boolean
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
        +findById(userId : long) : UserAccount
        +findByUsername(username : String) : UserAccount
        +findByEmail(email : String) : UserAccount

        +save(account : UserAccount) : boolean
        +update(account : UserAccount, expectedVersion : long) : boolean
    }


    class CredentialStorage <<repository>> {
        +findByUserId(userId : long) : Credential
        +save(credential : Credential) : boolean
        +update(credential : Credential) : boolean
    }


    class RolePermissionStorage <<repository>> {
        +findRole(roleId : long) : RolePermission
        +findRolesByUserId(userId : long) : List<RolePermission>
        +findPermissionsByUserId(userId : long) : Set<String>
    }
}


' =========================
' VIEW STRUCTURE
' =========================

AuthenticateAuthoriseView *-- LoginFormView
AuthenticateAuthoriseView *-- AccessDeniedView

LoginFormView ..> AuthenticateAuthoriseController : submits credentials
AuthenticateAuthoriseView ..> AuthenticateAuthoriseController : invokes


' =========================
' CONTROLLER DEPENDENCIES
' =========================

AuthenticateAuthoriseController ..> UserAccountStorage
AuthenticateAuthoriseController ..> CredentialStorage
AuthenticateAuthoriseController ..> RolePermissionStorage

AuthenticateAuthoriseController ..> SessionController : establishes session
AuthenticateAuthoriseController ..> NavigationController : navigation


' =========================
' DOMAIN RELATIONSHIPS
' =========================

UserAccount "1" *-- "1" Credential : owns >

UserAccount "0..*" -- "0..*" RolePermission : assigned roles >

UserAccount --> AccountStatus


' =========================
' STORAGE PERSISTENCE
' =========================

UserAccountStorage ..> UserAccount : persists
CredentialStorage ..> Credential : persists
RolePermissionStorage ..> RolePermission : persists


note right of Credential
Raw passwords must never
be persisted.

passwordHash should contain
a salted adaptive password hash.
end note

@enduml