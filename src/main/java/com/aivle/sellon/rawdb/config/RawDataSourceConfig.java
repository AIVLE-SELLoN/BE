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
        return builder
                .dataSource(rawDbDataSource())
                .packages("com.aivle.sellon.rawdb.entity")
                .persistenceUnit("rawDb")
                .build();
    }

    @Bean
    public PlatformTransactionManager rawDbTransactionManager(
            @Qualifier("rawDbEntityManagerFactory") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
