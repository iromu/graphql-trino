/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.iromu.trino.graphql.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring WebFlux security configuration class.
 *
 * <p>
 * This configuration sets up security for a Spring WebFlux application. It defines two
 * main beans: one for securing the application when security is enabled and one for
 * disabling security when it is explicitly disabled via configuration.
 * </p>
 *
 * <p>
 * The security is conditional based on the value of the `security.enabled` property in
 * the application configuration. If security is enabled, a filter chain will be
 * configured to secure endpoints. If security is disabled, all endpoints will be publicly
 * accessible.
 * </p>
 *
 * <p>
 * Security rules are applied as follows:
 * </p>
 * <ul>
 * <li>Some paths (like actuator endpoints, static assets, and OpenAPI docs) are publicly
 * accessible.</li>
 * <li>Other paths require authentication via OAuth2 JWT authentication when security is
 * enabled.</li>
 * <li>When security is disabled, all paths are publicly accessible.</li>
 * </ul>
 *
 * @author Ivan Rodriguez
 */
@Configuration
@EnableWebFluxSecurity
public class WebFluxSecurityConfig {

	/**
	 * The list of endpoints that are publicly accessible regardless of security settings.
	 *
	 * <p>
	 * This includes endpoints like actuator health check, static assets, icons, and
	 * OpenAPI documentation.
	 * </p>
	 */
	public static final String[] PERMIT_ALL = { "/actuator/**", "/assets/**", "/icons/**", "/v3/api-docs" };

	/**
	 * Configures a security filter chain for the application when security is enabled.
	 *
	 * <p>
	 * This bean is activated when the `security.enabled` property is set to `true`. It
	 * configures the following security settings for WebFlux:
	 * </p>
	 * <ul>
	 * <li>Disables CSRF (Cross-Site Request Forgery) protection, as it is not commonly
	 * used in stateless REST APIs.</li>
	 * <li>Disables form login (as it is not necessary for most REST APIs).</li>
	 * <li>Allows public access to certain paths (defined by {@link #PERMIT_ALL}), while
	 * requiring authentication for all other requests.</li>
	 * <li>Configures OAuth2 JWT-based resource server authentication.</li>
	 * </ul>
	 * @param http the {@link ServerHttpSecurity} instance used to configure the WebFlux
	 * security filter chain
	 * @return a configured {@link SecurityWebFilterChain} with the specified security
	 * settings
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "security", name = "enabled", havingValue = "true", matchIfMissing = false)
	public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
		http.csrf(ServerHttpSecurity.CsrfSpec::disable)
			.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
			.authorizeExchange(
					exchanges -> exchanges.pathMatchers(PERMIT_ALL).permitAll().anyExchange().authenticated())
			.oauth2ResourceServer(oAuth2ResourceServerSpec -> oAuth2ResourceServerSpec.jwt(Customizer.withDefaults()));

		return http.build();
	}

	/**
	 * Configures a security filter chain with no security applied.
	 *
	 * <p>
	 * This bean is activated when the `security.enabled` property is set to `false` or is
	 * not defined in the application properties (defaults to `true`). It configures
	 * WebFlux security to permit access to all paths without requiring authentication or
	 * authorization.
	 * </p>
	 * @param http the {@link ServerHttpSecurity} instance used to configure the WebFlux
	 * security filter chain
	 * @return a configured {@link SecurityWebFilterChain} with no security
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "security", name = "enabled", havingValue = "false", matchIfMissing = true)
	public SecurityWebFilterChain webFluxNoSecurity(ServerHttpSecurity http) {

		return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
			.authorizeExchange(exchanges -> exchanges.pathMatchers("/**").permitAll())
			.build();

	}

}
