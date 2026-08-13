package com.aivle.sellon.domain.inquiries.entity;

import com.aivle.sellon.domain.inquiries.enums.InquireType;
import com.aivle.sellon.domain.inquiries.enums.InquiryStatus;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cs_table")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CsInquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquire_key")
    private Long inquireKey;

    @Column(name = "inquire_title", nullable = false)
    private String inquireTitle;

    @Column(name = "inquire_content", nullable = false)
    private String inquireContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "inquire_type", nullable = false)
    private InquireType inquireType;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Column(name = "inquire_answer")
    private String inquireAnswer;

    @Enumerated(EnumType.STRING)
    @Column(name = "inquiry_status")
    private InquiryStatus inquiryStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static CsInquiry of(User user, String inquireTitle,
                               String inquireContent, InquireType inquireType, String attachmentUrl) {
        CsInquiry entity = new CsInquiry();
        entity.user = user;
        entity.inquireTitle = inquireTitle;
        entity.inquireContent = inquireContent;
        entity.inquireType = inquireType;
        entity.attachmentUrl = attachmentUrl;
        entity.inquiryStatus = InquiryStatus.WAITING;
        return entity;
    }

    public void answer(String inquireAnswer) {
        this.inquireAnswer = inquireAnswer;
        this.inquiryStatus = InquiryStatus.CLEARED;
    }

    public void update(String inquireTitle, String inquireContent, InquireType inquireType, String attachmentUrl) {
        this.inquireTitle = inquireTitle;
        this.inquireContent = inquireContent;
        this.inquireType = inquireType;
        this.attachmentUrl = attachmentUrl;
    }

    public void remove() {
        delete();
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public boolean isAnswered() {
        return this.inquiryStatus != InquiryStatus.WAITING;
    }
}