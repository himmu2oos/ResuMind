package com.portfolio.recruiterportal.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    private String title;

    @Column(length = 2000)
    private String summary;

    private String email;
    private String phone;
    private String location;
    private String photoUrl;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "skill")
    private List<String> skills;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_languages", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "language")
    private List<String> languages;
}
