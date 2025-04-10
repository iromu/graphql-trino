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

package org.iromu.trino.graphql.data;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * GraphQL resolver for querying Trino schema metadata such as catalogs, schemas, and
 * tables. Acts as a controller in the Spring Boot context and handles GraphQL queries.
 *
 * @author Ivan Rodriguez
 */
@Controller
public class TrinoGraphQLResolver {

	private final TrinoSchemaService trinoSchemaService;

	/**
	 * Constructs a new {@code TrinoGraphQLResolver} with the provided
	 * {@code TrinoSchemaService}.
	 * @param trinoSchemaService the service used to interact with Trino schema
	 * information
	 */
	public TrinoGraphQLResolver(TrinoSchemaService trinoSchemaService) {
		this.trinoSchemaService = trinoSchemaService;
	}

	/**
	 * Retrieves a list of all available catalogs in Trino.
	 * @return a list of catalog names
	 */
	@QueryMapping
	public List<String> catalogs() {
		return trinoSchemaService.getCatalogs();
	}

	/**
	 * Retrieves a list of schemas within the specified catalog.
	 * @param catalog the name of the catalog to query
	 * @return a list of schema names in the given catalog
	 */
	@QueryMapping
	public List<String> schemas(@Argument String catalog) {
		return trinoSchemaService.getSchemas(catalog);
	}

	/**
	 * Retrieves a list of tables within the specified catalog and schema.
	 * @param catalog the name of the catalog
	 * @param schema the name of the schema within the catalog
	 * @return a list of table names in the given schema and catalog
	 */
	@QueryMapping
	public List<String> tables(@Argument String catalog, @Argument String schema) {
		return trinoSchemaService.getTables(catalog, schema);
	}

}
