package com.sunrise.dental.config;

import com.sunrise.dental.web.AuthInterceptor;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

/**
 * Spring MVC configuration: view resolution, static resources, JSON conversion, validation and
 * the authentication interceptor.
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
        "com.sunrise.dental.web",
        "com.sunrise.dental.api",
        "com.sunrise.dental.service",
        "com.sunrise.dental.dao",
        // ClinicProperties lives here and is a @Component, so this package must be scanned too.
        "com.sunrise.dental.config"
})
@PropertySource("classpath:application.properties")
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /**
     * Resolves the {@code ${...}} placeholders that {@code @Value} annotations refer to.
     *
     * <p>Required explicitly here, and easy to miss: plain Spring does not register this
     * automatically the way Spring Boot does, so without it every {@code @Value} would be
     * injected as the literal string "${clinic.consultation-fee}" and fail to parse. The method
     * must be {@code static} because a {@code BeanFactoryPostProcessor} has to be created
     * before the configuration class itself is instantiated.</p>
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    /**
     * Maps a view name returned by a controller onto a JSP file, so a controller returning
     * "appointments/list" renders /WEB-INF/jsp/appointments/list.jsp.
     *
     * <p>The JSPs live under WEB-INF deliberately: the servlet container refuses to serve
     * anything under that directory directly, so a page can only be reached by going through a
     * controller. A visitor cannot bypass the authentication interceptor by typing the .jsp
     * path into the address bar.</p>
     */
    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/jsp/");
        resolver.setSuffix(".jsp");
        resolver.setViewClass(JstlView.class);
        return resolver;
    }

    /** Serves the stylesheet from /resources/css/** without going through a controller. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/")
                .setCachePeriod(3600);
    }

    /** Applies the session check to every route except the ones the interceptor whitelists. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/resources/**");
    }

    /**
     * Registers Jackson so LocalDate and LocalTime serialise as ISO strings ("2026-09-10",
     * "10:00:00") rather than as numeric arrays. Without this the console client could not
     * parse the dates it receives.
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Built by the shared factory so the controller tests serialise identically.
        converters.add(new MappingJackson2HttpMessageConverter(JsonMapperFactory.create()));
    }

    /** Enables the {@code @Valid} annotations declared on the request DTOs. */
    @Bean
    @Override
    public Validator getValidator() {
        return new LocalValidatorFactoryBean();
    }
}
