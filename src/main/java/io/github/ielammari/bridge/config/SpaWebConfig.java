package io.github.ielammari.bridge.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the bundled React SPA from the jar.
 * <p>
 * Client side routes such as /offres or /profil are not real files, so any
 * request that does not match a static asset falls back to index.html and lets
 * React Router resolve it. API paths are excluded, so an unknown endpoint still
 * returns a proper 404 instead of the HTML shell.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

	private static final String STATIC_LOCATION = "classpath:/static/";
	private static final String INDEX = "/static/index.html";

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
				.addResourceLocations(STATIC_LOCATION)
				.resourceChain(true)
				.addResolver(new PathResourceResolver() {
					@Override
					protected Resource getResource(String resourcePath, Resource location) throws IOException {
						Resource requested = location.createRelative(resourcePath);
						if (requested.exists() && requested.isReadable()) {
							return requested;
						}
						if (resourcePath.startsWith("api/")) {
							return null;
						}
						return new ClassPathResource(INDEX);
					}
				});
	}

}
