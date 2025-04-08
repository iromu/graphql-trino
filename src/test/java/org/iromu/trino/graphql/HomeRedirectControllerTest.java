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

import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

public class HomeRedirectControllerTest {

	@Test
	void testRedirectToGraphiql() {
		// Create an instance of the router function
		HomeRedirectController controller = new HomeRedirectController();
		RouterFunction<ServerResponse> route = controller.redirectToGraphiql();

		// Bind the WebTestClient to the route
		WebTestClient client = WebTestClient.bindToRouterFunction(route).build();

		// Perform the test
		client.get()
			.uri("/")
			.exchange()
			.expectStatus()
			.isTemporaryRedirect()
			.expectHeader()
			.valueEquals("Location", "/graphiql");
	}

}
