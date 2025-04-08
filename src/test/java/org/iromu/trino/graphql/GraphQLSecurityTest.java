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

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class GraphQLSecurityTest {

	@Autowired
	private WebTestClient webTestClient;

	private final String jwtToken = "Bearer eyJhbGciOi..."; // Optionally generate a valid test token

	@Test
	public void currentUser_shouldReturnUsername_whenAuthenticated() {
		String graphQLQuery = "{ \"query\": \"{ currentUser }\" }";

		webTestClient.post()
			.uri("/graphql")
			.header(HttpHeaders.AUTHORIZATION, jwtToken)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(graphQLQuery)
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.data.currentUser").value(Matchers.notNullValue());
	}

	@Test
	public void currentUser_shouldReturnUnauthorized_whenNoToken() {
		String graphQLQuery = "{ \"query\": \"{ currentUser }\" }";

		webTestClient.post()
			.uri("/graphql")
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(graphQLQuery)
			.exchange()
			.expectStatus().isUnauthorized();
	}

	@TestConfiguration
	public class TestSecurityConfig {

		@Bean
		public ReactiveJwtDecoder jwtDecoder() {
			return token -> Mono.just(
				Jwt.withTokenValue(token)
					.header("alg", "none")
					.claim("preferred_username", "test-user")
					.claim("scope", "read")
					.issuedAt(Instant.now())
					.expiresAt(Instant.now().plusSeconds(3600))
					.build()
			);
		}
	}

}
