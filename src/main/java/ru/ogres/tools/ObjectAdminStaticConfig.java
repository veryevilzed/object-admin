package ru.ogres.tools;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Created by zed on 16.08.16.
 */

@Configuration
@EnableWebMvc
public class ObjectAdminStaticConfig extends WebMvcConfigurerAdapter {

    private static final String[] CLASSPATH_RESOURCE_LOCATIONS = { "classpath:/META-INF/static/" };

    @Value("${object-admin.static-path:/__oa__static}")
    String staticPath;

    @Value("${object-admin.schemas-path:/__oa__schemas}")
    String schemasPath;

    @Value("${object-admin.schemas-location:classpath:/schemas/}")
    String schemasLocation;


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(staticPath + "/**").addResourceLocations(CLASSPATH_RESOURCE_LOCATIONS);
        registry.addResourceHandler(schemasPath + "/**").addResourceLocations(schemasLocation);
    }
}
