package com.aivle.sellon.domain.guideline.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGuideline is a Querydsl query type for Guideline
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGuideline extends EntityPathBase<Guideline> {

    private static final long serialVersionUID = -1280038396L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGuideline guideline = new QGuideline("guideline");

    public final com.aivle.sellon.global.QBaseEntity _super = new com.aivle.sellon.global.QBaseEntity(this);

    public final StringPath alertId = createString("alertId");

    public final com.aivle.sellon.domain.company.entity.QCompany company;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final StringPath guidelineId = createString("guidelineId");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath noticeMessage = createString("noticeMessage");

    public final QPdfS3Meta pdfS3Meta;

    public final StringPath sourcePayload = createString("sourcePayload");

    public final EnumPath<com.aivle.sellon.domain.report.enums.ReportStatus> status = createEnum("status", com.aivle.sellon.domain.report.enums.ReportStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath validationReport = createString("validationReport");

    public QGuideline(String variable) {
        this(Guideline.class, forVariable(variable), INITS);
    }

    public QGuideline(Path<? extends Guideline> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGuideline(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGuideline(PathMetadata metadata, PathInits inits) {
        this(Guideline.class, metadata, inits);
    }

    public QGuideline(Class<? extends Guideline> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.company = inits.isInitialized("company") ? new com.aivle.sellon.domain.company.entity.QCompany(forProperty("company")) : null;
        this.pdfS3Meta = inits.isInitialized("pdfS3Meta") ? new QPdfS3Meta(forProperty("pdfS3Meta")) : null;
    }

}

