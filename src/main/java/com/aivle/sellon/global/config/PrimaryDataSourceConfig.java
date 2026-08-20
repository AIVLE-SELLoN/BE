package com.aivle.sellon.global.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class PrimaryDataSourceConfig {

    // 로컬 postgres 컨테이너는 SSL 미설정이라 기본값은 prefer(PgJDBC 기본과 동일).
    // RDS는 prod.yaml에서 require로 강제한다.
    @Value("${spring.datasource.sslmode:prefer}")
    private String sslMode;

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {
        HikariDataSource dataSource = dataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        dataSource.addDataSourceProperty("sslmode", sslMode);
        return dataSource;
    }

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            DataSource dataSource
    ) {
        return builder
                .dataSource(dataSource)
                .packages("com.aivle.sellon.domain")
                .persistenceUnit("sellon")
                .build();
    }

    @Primary
    @Bean
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
