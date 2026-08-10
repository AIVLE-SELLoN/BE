package com.aivle.sellon.domain.guideline.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGuidelineSummary is a Querydsl query type for GuidelineSummary
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGuidelineSummary extends EntityPathBase<GuidelineSummary> {

    private static final long serialVersionUID = -13858558L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGuidelineSummary guidelineSummary = new QGuidelineSummary("guidelineSummary");

    public final com.aivle.sellon.global.QBaseEntity _super = new com.aivle.sellon.global.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final DateTimePath<java.time.LocalDateTime> detectedAt = createDateTime("detectedAt", java.time.LocalDateTime.class);

    public final QGuideline guideline;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath linkedInquiries = createString("linkedInquiries");

    public final StringPath productGroupId = createString("productGroupId");

    public final StringPath productName = createString("productName");

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QGuidelineSummary(String variable) {
        this(GuidelineSummary.class, forVariable(variable), INITS);
    }

    public QGuidelineSummary(Path<? extends GuidelineSummary> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGuidelineSummary(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGuidelineSummary(PathMetadata metadata, PathInits inits) {
        this(GuidelineSummary.class, metadata, inits);
    }

    public QGuidelineSummary(Class<? extends GuidelineSummary> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.guideline = inits.isInitialized("guideline") ? new QGuideline(forProperty("guideline"), inits.get("guideline")) : null;
    }

}

