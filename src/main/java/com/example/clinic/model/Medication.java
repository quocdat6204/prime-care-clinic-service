package com.example.clinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "medications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Medication extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    String name;

    @Column(name = "strength", nullable = false, length = 100)
    String strength;

    @Column(name = "dosage_form", nullable = false, length = 100)
    String dosageForm;

    @Column(name = "unit", nullable = false, length = 50)
    String unit;

    @Column(name = "description", length = 2000)
    String description;

    @Builder.Default
    @Column(name = "active", nullable = false)
    boolean active = true;
}
