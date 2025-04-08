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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.List;
import java.util.Map;

@AutoConfigureGraphQlTester
@GraphQlTest(controllers = TrinoGraphQLResolver.class)
class TrinoGraphQLResolverTest {

	@Autowired
	private GraphQlTester graphQlTester;

	@Test
	void testGetCatalogsQuery() {
		String query = """
				    query {
				        catalogs
				    }
				""";

		graphQlTester.document(query)
				.execute()
				.path("data.catalogs")
				.entityList(String.class)
				.contains("hive", "mysql"); // Verify expected catalogs
	}

	@Test
	void testGetSchemasQuery() {
		String query = """
				    query {
				        schemas(catalog: "hive")
				    }
				""";

		graphQlTester.document(query)
				.execute()
				.path("data.schemas")
				.entityList(String.class)
				.contains("default", "sales"); // Verify expected catalogs
	}

	@Test
	void testGetTablesQuery() {
		String query = """
				    query {
				        tables(catalog: "hive", schema: "sales")
				    }
				""";

		graphQlTester.document(query).execute().path("data.tables").entityList(String.class).contains("orders"); // Verify
		// expected
		// table
		// name
	}

	@TestConfiguration
	static class TestTrinoSchemaConfiguration {

		@Bean
		@Primary
		public TrinoSchemaService testTrinoSchemaService() {
			return new TrinoSchemaService(null, null, null, new GraphQLSchemaFixer()) {
				@Override
				public List<String> getCatalogs() {
					return List.of("hive", "mysql");
				}

				@Override
				public List<String> getSchemas(String catalog) {
					return List.of("default", "sales");
				}

				@Override
				public List<String> getTables(String catalog, String schema) {
					return List.of("orders");
				}

				@Override
				public List<Map<String, Object>> getColumns(String catalog, String schema, String table) {
					return List.of(Map.of("Column", "order_id", "Type", "integer"),
							Map.of("Column", "amount", "Type", "double"),
							Map.of("Column", "status", "Type", "varchar"));
				}
			};
		}

	}

}
