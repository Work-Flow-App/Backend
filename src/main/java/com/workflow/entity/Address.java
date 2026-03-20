package com.workflow.entity;

import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    // Optional flexible data (good for "additional info")
    @Column(columnDefinition = "TEXT")
    private String additionalInfo;

    // Optional: geo-location (future-proof)
    private Double latitude;
    private Double longitude;
}