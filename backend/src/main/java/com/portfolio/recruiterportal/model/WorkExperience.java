package com.portfolio.recruiterportal.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "work_experience")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String jobTitle;

    private String location;
    private String startDate;
    private String endDate;
    private Boolean isCurrent;

    @Column(length = 3000)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "work_highlights", joinColumns = @JoinColumn(name = "work_id"))
    @Column(name = "highlight")
    private List<String> highlights;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "work_technologies", joinColumns = @JoinColumn(name = "work_id"))
    @Column(name = "technology")
    private List<String> technologies;

    private String companyLogoUrl;
    private Integer sortOrder;
}
