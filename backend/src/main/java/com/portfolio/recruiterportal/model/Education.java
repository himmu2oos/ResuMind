package com.portfolio.recruiterportal.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "education")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String institution;

    @Column(nullable = false)
    private String degree;

    private String fieldOfStudy;
    private String startDate;
    private String endDate;
    private Double gpa;

    @Column(length = 1500)
    private String description;

    private String logoUrl;
    private Integer sortOrder;
}
