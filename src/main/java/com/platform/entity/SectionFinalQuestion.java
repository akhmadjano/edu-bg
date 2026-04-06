package com.platform.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "section_final_questions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SectionFinalQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String options;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String correctAnswers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;
}