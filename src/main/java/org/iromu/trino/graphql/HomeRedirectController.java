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

package org.iromu.trino.graphql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.URI;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Configuration class responsible for setting up a redirection from the root URL ("/") to
 * the GraphiQL interface.
 *
 * <p>
 * This class defines a Spring WebFlux router function to handle HTTP GET requests to the
 * root path ("/") and redirects the user to the GraphiQL interface. The redirection is
 * temporary, ensuring that the user is redirected to the appropriate GraphiQL path
 * ("/graphiql").
 * </p>
 *
 * @author Ivan Rodriguez
 */
@Configuration
public class HomeRedirectController {

	/**
	 * Bean definition for the router function that handles redirection to GraphiQL.
	 *
	 * <p>
	 * This method defines the behavior of routing HTTP GET requests made to the root path
	 * ("/") to a temporary redirect to the "/graphiql" path. The redirection is performed
	 * using {@link ServerResponse#temporaryRedirect(URI)} and the URI is dynamically
	 * constructed using the current request URI.
	 * </p>
	 * @return a {@link RouterFunction} that routes HTTP GET requests to the root path and
	 * redirects them to "/graphiql"
	 */
	@Bean
	public RouterFunction<ServerResponse> redirectToGraphiql() {
		return route(GET("/"),
				req -> ServerResponse.temporaryRedirect(URI.create(req.uri().getPath() + "graphiql")).build());
	}

}
