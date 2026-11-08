package com.example.clinic.model;

import com.example.clinic.util.ServiceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalService extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 30)
    ServiceType serviceType;

    @Column(name = "price", nullable = false, precision = 18, scale = 0)
    BigDecimal price;

    @Column(name = "room_number", nullable = false, length = 100)
    String roomNumber;

    @Column(name = "description", length = 2000)
    String description;

    @Builder.Default
    @Column(name = "active", nullable = false)
    boolean active = true;
}
