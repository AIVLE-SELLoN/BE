package com.aivle.sellon.rawdb.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Bean
    @ConfigurationProperties("raw-db.datasource")
    public DataSourceProperties rawDbDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource rawDbDataSource() {
        return rawDbDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean rawDbEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        // raw DB는 읽기 전용이라 ddl-auto를 validate로 고정해 BE가 스키마를 못 건드리게 한다.
        return builder
                .dataSource(rawDbDataSource())
                .packages("com.aivle.sellon.rawdb.entity")
                .persistenceUnit("rawDb")
                .properties(Map.of("hibernate.hbm2ddl.auto", "validate"))
                .build();
    }

    @Bean
    public PlatformTransactionManager rawDbTransactionManager(
            @Qualifier("rawDbEntityManagerFactory") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
