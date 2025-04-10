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

package org.iromu.trino.graphql.schema;

import org.iromu.trino.graphql.AppProperties;
import org.iromu.trino.graphql.data.TrinoQueryService;
import org.iromu.trino.graphql.data.TrinoSchemaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.List;
import java.util.Map;

/**
 * @author Ivan Rodriguez
 */
@AutoConfigureGraphQlTester
@GraphQlTest
@Import({ GraphQLConfig.class, GraphQLDynamicSchemaService.class, GraphQLSchemaFixer.class, AppProperties.class })
public class GraphQLDynamicSchemaTest {

	@Autowired
	private GraphQlTester graphQlTester;

	@Test
	void testQueryTableData() {
		String query = """
				    query {
				        hive_sales_orders(limit: 2) {
				            order_id
				            amount
				            status
				        }
				    }
				""";

		graphQlTester.document(query)
			.execute()
			.path("data.hive_sales_orders[0].order_id")
			.entity(Integer.class)
			.isEqualTo(1)
			.path("data.hive_sales_orders[0].amount")
			.entity(Double.class)
			.isEqualTo(100.0)
			.path("data.hive_sales_orders[0].status")
			.entity(String.class)
			.isEqualTo("Completed")
			.path("data.hive_sales_orders[1].order_id")
			.entity(Integer.class)
			.isEqualTo(2)
			.path("data.hive_sales_orders[1].amount")
			.entity(Double.class)
			.isEqualTo(50.0)
			.path("data.hive_sales_orders[1].status")
			.entity(String.class)
			.isEqualTo("Pending");
	}

	@TestConfiguration
	static class TestTrinoSchemaConfiguration {

		@Bean
		@Primary
		public TrinoSchemaService testTrinoSchemaService() {
			return new TrinoSchemaService(null, null, null, new GraphQLSchemaFixer(), null) {
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

		@Bean
		@Primary
		public TrinoQueryService testTrinoQueryService() {
			return new TrinoQueryService(null, new GraphQLSchemaFixer()) {

				@Override
				public List<Map<String, Object>> queryTableWithFilters(String catalog, String schema, String table,
						int limit, List<Map<String, Object>> filters) {
					return List.of(Map.of("order_id", 1, "amount", 100.0, "status", "Completed"),
							Map.of("order_id", 2, "amount", 50.0, "status", "Pending"));
				}

			};
		}

	}

}
