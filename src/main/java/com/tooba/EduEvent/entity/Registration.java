package com.tooba.EduEvent.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "registrations")
// Prevents the same user from registering for the same event twice
@CompoundIndex(name = "uq_user_event", def = "{'userId': 1, 'eventId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Registration {

    @Id
    private String id;

    private String userId;

    private String eventId;

    private RegistrationStatus status; // REGISTERED or WAITLISTED

    @CreatedDate
    private LocalDateTime registeredAt;
}
