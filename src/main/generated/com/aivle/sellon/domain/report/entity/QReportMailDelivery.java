package com.aivle.sellon.domain.report.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReportMailDelivery is a Querydsl query type for ReportMailDelivery
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReportMailDelivery extends EntityPathBase<ReportMailDelivery> {

    private static final long serialVersionUID = -1673059983L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReportMailDelivery reportMailDelivery = new QReportMailDelivery("reportMailDelivery");

    public final com.aivle.sellon.global.QBaseEntity _super = new com.aivle.sellon.global.QBaseEntity(this);

    public final NumberPath<Integer> attemptCount = createNumber("attemptCount", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath lastError = createString("lastError");

    public final QReport report;

    public final DateTimePath<java.time.LocalDateTime> scheduledAt = createDateTime("scheduledAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> sentAt = createDateTime("sentAt", java.time.LocalDateTime.class);

    public final EnumPath<com.aivle.sellon.domain.report.enums.ReportMailDeliveryStatus> status = createEnum("status", com.aivle.sellon.domain.report.enums.ReportMailDeliveryStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QReportMailDelivery(String variable) {
        this(ReportMailDelivery.class, forVariable(variable), INITS);
    }

    public QReportMailDelivery(Path<? extends ReportMailDelivery> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReportMailDelivery(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReportMailDelivery(PathMetadata metadata, PathInits inits) {
        this(ReportMailDelivery.class, metadata, inits);
    }

    public QReportMailDelivery(Class<? extends ReportMailDelivery> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.report = inits.isInitialized("report") ? new QReport(forProperty("report"), inits.get("report")) : null;
    }

}

