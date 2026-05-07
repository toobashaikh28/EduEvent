package com.tooba.EduEvent.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leaderboard")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class Leaderboard { // The annotations MUST be right above this line

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne 
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne 
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(precision = 5, scale = 2)
    private java.math.BigDecimal score;
    
    private Integer rank;
}