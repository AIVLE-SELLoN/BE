package com.aivle.sellon.domain.guideline.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPdfS3Meta is a Querydsl query type for PdfS3Meta
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QPdfS3Meta extends BeanPath<PdfS3Meta> {

    private static final long serialVersionUID = 1168393451L;

    public static final QPdfS3Meta pdfS3Meta = new QPdfS3Meta("pdfS3Meta");

    public final StringPath companyId = createString("companyId");

    public final StringPath companyName = createString("companyName");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> fileSizeBytes = createNumber("fileSizeBytes", Long.class);

    public final StringPath newFileName = createString("newFileName");

    public final DateTimePath<java.time.LocalDateTime> objectExpiresAt = createDateTime("objectExpiresAt", java.time.LocalDateTime.class);

    public final StringPath originalFileName = createString("originalFileName");

    public final DateTimePath<java.time.LocalDateTime> presignedExpiresAt = createDateTime("presignedExpiresAt", java.time.LocalDateTime.class);

    public final StringPath presignedUrl = createString("presignedUrl");

    public final StringPath s3BucketName = createString("s3BucketName");

    public final StringPath s3FilePath = createString("s3FilePath");

    public final StringPath s3FullKey = createString("s3FullKey");

    public QPdfS3Meta(String variable) {
        super(PdfS3Meta.class, forVariable(variable));
    }

    public QPdfS3Meta(Path<? extends PdfS3Meta> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPdfS3Meta(PathMetadata metadata) {
        super(PdfS3Meta.class, metadata);
    }

}

