package com.aivle.sellon.rawdb.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.aivle.sellon.rawdb.repository",
        entityManagerFactoryRef = "rawDbEntityManagerFactory",
        transactionManagerRef = "rawDbTransactionManager"
)
public class RawDataSourceConfig {

    // 기본값은 validate로 고정 - raw DB는 읽기 전용이라 BE가 스키마를 못 건드리게 한다.
    // CI는 매 실행마다 빈 raw-db 컨테이너로 시작해 검증할 기존 스키마가 없으므로,
    // application-test.yaml에서만 create-drop으로 넘겨쓴다.
    @Value("${raw-db.ddl-auto:validate}")
    private String ddlAuto;

    // 로컬 postgres 컨테이너는 SSL 미설정이라 기본값은 prefer(PgJDBC 기본과 동일).
    // RDS는 prod.yaml에서 require로 강제한다.
    @Value("${raw-db.sslmode:prefer}")
    private String sslMode;

    @Bean
    @ConfigurationProperties("raw-db.datasource")
    public DataSourceProperties rawDbDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource rawDbDataSource() {
        HikariDataSource dataSource = rawDbDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        dataSource.addDataSourceProperty("sslmode", sslMode);
        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean rawDbEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(rawDbDataSource())
                .packages("com.aivle.sellon.rawdb.entity")
                .persistenceUnit("rawDb")
                .properties(Map.of(
                        "hibernate.hbm2ddl.auto", ddlAuto,
                        // @Immutable 엔티티(RawCs/RawReview) 대상 bulk UPDATE(@Modifying @Query)는
                        // 상품 매핑 소급 반영에 의도적으로 사용하는 것이므로 Hibernate 7 기본 예외를 경고로 낮춘다.
                        "hibernate.query.immutable_entity_update_query_handling_mode", "warning"
                ))
                .build();
    }

    @Bean
    public PlatformTransactionManager rawDbTransactionManager(
            @Qualifier("rawDbEntityManagerFactory") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
