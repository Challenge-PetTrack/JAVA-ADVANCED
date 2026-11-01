package com.fiap.pettrack.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

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
        config.addDataSourceProperty("oracle.net.CONNECT_TIMEOUT", "10000");
        config.addDataSourceProperty("oracle.jdbc.ReadTimeout", "20000");
        return config;
    }

    @Bean
    @Primary
    public DataSource dataSource(HikariConfig hikariConfig) {
        HikariDataSource dataSource = new HikariDataSource();
        hikariConfig.copyStateTo(dataSource);
        return dataSource;
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> azureDualPortTomcatCustomizer(
            @Value("${server.port:8080}") int primaryPort) {
        return factory -> {
            String azureSite = System.getenv("WEBSITE_SITE_NAME");
            String envPort = System.getenv("PORT");
            String websitesPort = System.getenv("WEBSITES_PORT");
            if (!StringUtils.hasText(azureSite) || primaryPort <= 0) {
                return;
            }

            Set<Integer> candidatePorts = new LinkedHashSet<>();
            candidatePorts.add(80);
            candidatePorts.add(8080);
            parsePort(envPort, candidatePorts);
            parsePort(websitesPort, candidatePorts);
            candidatePorts.remove(primaryPort);

            for (int extraPort : candidatePorts) {
                if (extraPort > 0 && canBindPort(extraPort)) {
                    Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
                    connector.setPort(extraPort);
                    factory.addAdditionalTomcatConnectors(connector);
                    log.info("Conector HTTP adicional do Tomcat configurado na porta {} (porta principal: {})", extraPort, primaryPort);
                }
            }
        };
    }

    private void parsePort(String rawPort, Set<Integer> target) {
        if (StringUtils.hasText(rawPort)) {
            try {
                target.add(Integer.parseInt(rawPort.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private boolean canBindPort(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(port));
            return true;
        } catch (Exception e) {
            log.warn("Porta adicional {} indisponivel para bind no Tomcat: {}", port, e.getMessage());
            return false;
        }
    }
}
