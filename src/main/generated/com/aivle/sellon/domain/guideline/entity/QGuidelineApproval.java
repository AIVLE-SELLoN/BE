package com.aivle.sellon.domain.guideline.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGuidelineApproval is a Querydsl query type for GuidelineApproval
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGuidelineApproval extends EntityPathBase<GuidelineApproval> {

    private static final long serialVersionUID = -1787056025L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGuidelineApproval guidelineApproval = new QGuidelineApproval("guidelineApproval");

    public final com.aivle.sellon.global.QBaseEntity _super = new com.aivle.sellon.global.QBaseEntity(this);

    public final StringPath comment = createString("comment");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final QGuideline guideline;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QGuidelineApproval(String variable) {
        this(GuidelineApproval.class, forVariable(variable), INITS);
    }

    public QGuidelineApproval(Path<? extends GuidelineApproval> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGuidelineApproval(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGuidelineApproval(PathMetadata metadata, PathInits inits) {
        this(GuidelineApproval.class, metadata, inits);
    }

    public QGuidelineApproval(Class<? extends GuidelineApproval> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.guideline = inits.isInitialized("guideline") ? new QGuideline(forProperty("guideline"), inits.get("guideline")) : null;
    }

}

