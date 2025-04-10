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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor;

/**
 * Spring configuration class to set up RSocket security.
 *
 * <p>
 * This class configures RSocket security settings, enabling or disabling security based
 * on the application's configuration. It defines two beans for handling RSocket
 * authentication and authorization:
 * </p>
 *
 * <ul>
 * <li>A secured RSocket interceptor if security is enabled (via
 * `security.enabled=true`).</li>
 * <li>An unsecured RSocket interceptor if security is disabled (via
 * `security.enabled=false`).</li>
 * </ul>
 *
 * <p>
 * By default, the security settings are applied based on the `security.enabled` property
 * in the application properties.
 * </p>
 *
 * @author Ivan Rodriguez
 */
@Configuration
@EnableRSocketSecurity
public class RSocketSecurityConfig {

	/**
	 * Configures an RSocket interceptor with security enabled.
	 *
	 * <p>
	 * This bean is activated when the `security.enabled` property is set to `true`. It
	 * configures the RSocket security to authorize any exchange (request) and allows the
	 * use of JWT-based authentication. The `permitAll()` method allows all routes to be
	 * accessed without authentication by default, but this can be customized to enforce
	 * security on specific routes.
	 * </p>
	 * @param rsocket the RSocketSecurity instance for configuring security
	 * @return a configured RSocket payload interceptor with security enabled
	 */
	@Bean
	@ConditionalOnProperty(prefix = "security", name = "enabled", havingValue = "true", matchIfMissing = false)
	public PayloadSocketAcceptorInterceptor rsocketAuth(RSocketSecurity rsocket) {
		rsocket.authorizePayload(auth ->
		// auth.route("public.").permitAll().anyExchange().authenticated()
		auth.anyExchange().permitAll()).jwt(Customizer.withDefaults());

		return rsocket.build();
	}

	/**
	 * Configures an RSocket interceptor with no security.
	 *
	 * <p>
	 * This bean is activated when the `security.enabled` property is set to `false` or is
	 * not defined in the application properties (defaults to `true`). It allows all
	 * exchanges without requiring any authentication or authorization.
	 * </p>
	 * @param rsocket the RSocketSecurity instance for configuring no security
	 * @return a configured RSocket payload interceptor with no security
	 */
	@Bean
	@ConditionalOnProperty(prefix = "security", name = "enabled", havingValue = "false", matchIfMissing = true)
	public PayloadSocketAcceptorInterceptor rsocketNoAuth(RSocketSecurity rsocket) {
		return rsocket.authorizePayload(authorizePayloadsSpec -> authorizePayloadsSpec.anyExchange().permitAll())
			.build();
	}

}
