package com.sunrise.dental.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Data-tier configuration: connection pool, transaction manager, schema bootstrap and the
 * password encoder.
 */
@Configuration
@EnableTransactionManagement
public class PersistenceConfig {

    private final Environment env;

    public PersistenceConfig(Environment env) {
        this.env = env;
    }

    /**
     * A pooled DataSource. Opening a fresh TCP connection per request would dominate the cost
     * of a query; the pool keeps a small number open and hands them out.
     *
     * <p>Credentials are read from the environment first and fall back to the values in
     * application.properties, so a deployment can supply real credentials without them ever
     * being committed to the repository.</p>
     */
    @Bean(destroyMethod = "close")
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(setting("DB_URL", "db.url"));
        config.setUsername(setting("DB_USERNAME", "db.username"));
        config.setPassword(setting("DB_PASSWORD", "db.password"));
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setPoolName("sunrise-dental-pool");
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Enables {@code @Transactional}. Registering an appointment writes to both the patient and
     * appointment tables; without a transaction a failure between the two would leave a patient
     * row with no appointment.
     */
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Creates the schema and seeds reference data on startup. Every script is idempotent, so
     * this is safe on every restart and a fresh checkout needs no manual SQL step.
     *
     * <p>Three scripts rather than one, because of a genuine limitation: Spring's script
     * splitter does not understand PostgreSQL dollar-quoting, so a {@code $$ ... $$} function
     * body full of semicolons would be chopped into fragments and fail. The stored functions
     * and the trigger therefore live in their own script whose statements are separated by
     * {@code ;;}, which cannot occur inside a function body.</p>
     */
    @Bean
    public SchemaBootstrap schemaBootstrap(DataSource dataSource) {
        // Tables, indexes and the sequence: ordinary statements, default ';' separator.
        ResourceDatabasePopulator tables = new ResourceDatabasePopulator();
        tables.addScript(new ClassPathResource("db/schema.sql"));
        DatabasePopulatorUtils.execute(tables, dataSource);

        // Stored functions and the trigger: separated by ';;'.
        ResourceDatabasePopulator routines = new ResourceDatabasePopulator();
        routines.addScript(new ClassPathResource("db/routines.sql"));
        routines.setSeparator(";;");
        DatabasePopulatorUtils.execute(routines, dataSource);

        // Reference data.
        ResourceDatabasePopulator seed = new ResourceDatabasePopulator();
        seed.addScript(new ClassPathResource("db/data.sql"));
        DatabasePopulatorUtils.execute(seed, dataSource);

        return new SchemaBootstrap();
    }

    /** Marker type so the populator runs exactly once, as a bean, before the DAOs are used. */
    public static class SchemaBootstrap {
    }

    private String setting(String envVar, String propertyKey) {
        String fromEnv = System.getenv(envVar);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return env.getProperty(propertyKey, "");
    }
}
