package com.aivle.sellon.domain.inquiries.entity;

import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "faq")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faq_key")
    private Long faqKey;

    @Column(name = "faq_title")
    private String faqTitle;

    @Column(name = "faq_question")
    private String faqQuestion;

    @Column(name = "faq_answer")
    private String faqAnswer;
}