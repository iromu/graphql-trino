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

import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebFluxTest(GraphQLSchemaEndpoint.class)
class GraphQLSchemaEndpointTest {

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private GraphQLSchema graphQLSchema;

	@BeforeEach
	void setup() {
		// Prevent NPE by mocking getCodeRegistry()
		GraphQLCodeRegistry codeRegistry = mock(GraphQLCodeRegistry.class);
		when(graphQLSchema.getCodeRegistry()).thenReturn(codeRegistry);
	}

	@Test
	void testGetSchema() {
		// Just to avoid NPE during schema printing
		// In real tests, you might still want to replace the schemaPrinter with a custom
		// one

		webTestClient.get()
			.uri("/schema.graphqls")
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType("text/plain;charset=UTF-8")
			.expectBody(String.class)
			.value(body -> {
				assertNotNull(body);
				assertTrue(body.contains("type") || body.contains("schema"));
			});
	}

}
