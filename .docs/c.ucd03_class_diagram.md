@startuml
title UCD-03 — Send Alerts & Notifications
Class Diagram

skinparam classAttributeIconSize 0
skinparam linetype ortho
hide empty members


package "view.patient_information.ucd03_send_alerts_notifications" {

    class SendAlertsNotificationsView <<boundary>> {

        -lastDeliverySuccessful : boolean

        +showNotificationProcessed(
            notification : Notification
        ) : void

        +showDeliveryFailure(
            notification : Notification
        ) : void

        +showProcessingError(
            message : String
        ) : void
    }


    class NotificationView <<boundary>> {

        -displayedNotification : Notification

        +displayNotification(
            notification : Notification
        ) : void

        +clearNotification() : void

        +showUnreadIndicator() : void
    }
}


package "controller.patient_information" {

    class SendAlertsNotificationsController <<control>> {

        +publishDomainEvent(
            eventType : String,
            prescriptionId : long,
            patientId : long,
            recipient : String,
            patientActive : boolean
        ) : void

        +processNotification(
            eventType : String,
            prescriptionId : long,
            patientId : long,
            recipient : String,
            patientActive : boolean
        ) : Notification

        -isEligibleEvent(
            eventType : String
        ) : boolean

        -createNotification(
            eventType : String,
            prescriptionId : long,
            patientId : long,
            recipient : String
        ) : Notification

        -isDuplicate(
            notification : Notification
        ) : boolean

        -sendNotification(
            notification : Notification
        ) : boolean

        +markNotificationRead(
            notificationId : long
        ) : boolean
    }
}


package "model.patient_information" {

    class Notification <<entity>> {

        -notificationId : long

        -patientId : long
        -prescriptionId : long

        -eventType : String
        -notificationType : String

        -title : String
        -message : String

        -recipient : String

        -deliveryStatus : String

        -deduplicationKey : String

        -createdAt : LocalDateTime
        -deliveredAt : LocalDateTime
        -failedAt : LocalDateTime
        -readAt : LocalDateTime

        -failureReason : String

        +evaluateNotificationEvent() : boolean

        +resolveNotificationType() : String

        +createCancellationNotification() : void

        +createPreparingNotification() : void

        +createReadyNotification() : void

        +createDispensedNotification() : void

        +validateRecipient(
            patientActive : boolean
        ) : boolean

        +generateDeduplicationKey() : String

        +markDelivered() : void

        +markDeliveryFailed(
            reason : String
        ) : void

        +markNotDeliverable(
            reason : String
        ) : void

        +markRead() : void

        +isDelivered() : boolean

        +isRead() : boolean
    }
}


package "storage.patient_information" {

    class NotificationStorage <<repository>> {

        +findById(
            notificationId : long
        ) : Notification

        +findByPatientId(
            patientId : long
        ) : List<Notification>

        +existsByDeduplicationKey(
            key : String
        ) : boolean

        +save(
            notification : Notification
        ) : boolean

        +update(
            notification : Notification
        ) : boolean
    }
}


' =====================================================
' VIEW
' =====================================================

SendAlertsNotificationsView *-- NotificationView

SendAlertsNotificationsView ..> SendAlertsNotificationsController

NotificationView ..> SendAlertsNotificationsController : mark read


' =====================================================
' CONTROLLER
' =====================================================

SendAlertsNotificationsController ..> Notification : creates / manages

SendAlertsNotificationsController ..> NotificationStorage : persistence


' =====================================================
' STORAGE
' =====================================================

NotificationStorage ..> Notification : persists


note right of Notification
No NotificationType or
DeliveryStatus classes are added.

notificationType and deliveryStatus
remain String attributes because the
current project structure provides
only Notification.java.
end note


note bottom of NotificationStorage
deduplicationKey should identify
the same business notification.

Example logical key:

patientId
+ prescriptionId
+ eventType

This supports duplicate suppression
required by the sequence.
end note


note bottom of SendAlertsNotificationsController
No notification-provider class exists
in the supplied architecture.

Therefore sendNotification() remains
encapsulated inside this controller.

If email/SMS/push infrastructure is
introduced later, the architecture
would need another class, but that is
outside the current constraint.
end note

@enduml