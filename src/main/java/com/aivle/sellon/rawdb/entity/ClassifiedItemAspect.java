package com.aivle.sellon.rawdb.entity;

import com.aivle.sellon.domain.channels.enums.InquiryType;
import com.aivle.sellon.rawdb.entity.converter.InquiryTypeConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "classified_item_aspect")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassifiedItemAspect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Convert(converter = InquiryTypeConverter.class)
    @Column(name = "aspect", nullable = false)
    private InquiryType aspect;

    @Column(name = "sentiment", nullable = false)
    private Short sentiment;

    @Column(name = "mixed_signal")
    private Boolean mixedSignal;
}
