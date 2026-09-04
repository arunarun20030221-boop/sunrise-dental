package com.sunrise.dental.config;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import java.nio.charset.StandardCharsets;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Bootstraps the application in place of a {@code web.xml}.
 *
 * <p>Tomcat discovers this class through the Servlet specification's {@code ServletContainer-
 * Initializer} mechanism and calls {@link #onStartup}, which is where the
 * {@link DispatcherServlet} - Spring MVC's FRONT CONTROLLER - is registered and mapped to "/".
 * Every request therefore enters through one servlet, which then dispatches to whichever
 * {@code @Controller} method matches the URL.</p>
 *
 * <p>Doing this in Java rather than XML keeps the wiring type-checked by the compiler, while
 * still leaving the whole request pipeline visible in our own source rather than inferred by a
 * framework.</p>
 */
public class WebAppInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(WebConfig.class, PersistenceConfig.class);

        ServletRegistration.Dynamic dispatcher =
                servletContext.addServlet("dispatcher", new DispatcherServlet(context));
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/");

        // Force UTF-8 on every request and response so that patient names containing Sinhala
        // or Tamil characters survive a round trip through a form.
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding(StandardCharsets.UTF_8.name());
        encodingFilter.setForceEncoding(true);
        servletContext.addFilter("characterEncodingFilter", encodingFilter)
                .addMappingForUrlPatterns(null, false, "/*");
    }
}
