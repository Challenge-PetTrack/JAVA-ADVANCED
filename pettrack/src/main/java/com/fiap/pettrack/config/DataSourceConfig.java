package com.fiap.pettrack.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariConfig hikariConfig(DataSourceProperties properties) {
        HikariConfig config = new HikariConfig();
        if (StringUtils.hasText(properties.determineDriverClassName())) {
            config.setDriverClassName(properties.determineDriverClassName());
        }
        if (StringUtils.hasText(properties.determineUrl())) {
            config.setJdbcUrl(properties.determineUrl());
        }
        if (StringUtils.hasText(properties.determineUsername())) {
            config.setUsername(properties.determineUsername());
        }
        if (properties.determinePassword() != null) {
            config.setPassword(properties.determinePassword());
        }
        if (StringUtils.hasText(properties.getName())) {
            config.setPoolName(properties.getName());
        }
        return config;
    }

    @Bean
    @Primary
    public DataSource dataSource(HikariConfig hikariConfig) {
        HikariDataSource dataSource = new HikariDataSource();
        hikariConfig.copyStateTo(dataSource);
        return dataSource;
    }
}
