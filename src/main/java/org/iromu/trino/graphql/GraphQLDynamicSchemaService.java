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

import graphql.Scalars;
import graphql.language.SchemaDefinition;
import graphql.schema.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static graphql.Scalars.GraphQLString;
import static org.iromu.trino.graphql.GraphQLSchemaFixer.VALID_CHAR_PATTERN;

@Service
@Slf4j
public class GraphQLDynamicSchemaService {

	private final TrinoSchemaService trinoSchemaService;

	private final TrinoQueryService trinoQueryService;

	private final GraphQLSchemaFixer fixer;

	public GraphQLDynamicSchemaService(TrinoSchemaService trinoSchemaService, TrinoQueryService trinoQueryService,
									   GraphQLSchemaFixer fixer) {
		this.trinoSchemaService = trinoSchemaService;
		this.trinoQueryService = trinoQueryService;
		this.fixer = fixer;
	}

	public GraphQLSchema generateSchema() {
		GraphQLSchema.Builder schemaBuilder = GraphQLSchema.newSchema();
		GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
		GraphQLObjectType.Builder subscriptionBuilder = GraphQLObjectType.newObject().name("Subscription");

		// Store created types to register later
		Set<GraphQLType> additionalTypes = new HashSet<>();

		queryBuilder.field(GraphQLFieldDefinition.newFieldDefinition()
			.name("catalogs")
			.type(GraphQLList.list(GraphQLString))
			.dataFetcher(env -> trinoSchemaService.getCatalogs())
			.build());

		for (String catalog : trinoSchemaService.getCatalogs()) {
			// if ("system".equals(catalog)) {
			// continue;
			// }

			if (!VALID_CHAR_PATTERN.matcher(catalog).matches())
				continue;
			for (String schema : trinoSchemaService.getSchemas(catalog)) {
				// if ("information_schema".equals(schema)) {
				// continue;
				// }
				if (!VALID_CHAR_PATTERN.matcher(schema).matches())
					continue;
				for (String table : trinoSchemaService.getTables(catalog, schema)) {
					if (!VALID_CHAR_PATTERN.matcher(table).matches())
						continue;
					// Create a unique name for each table type (avoid collisions)
					String typeName = catalog + "_" + schema + "_" + table;
					String queryFieldName = catalog + "_" + schema + "_" + table;

					// Define GraphQLObjectType for the table
					GraphQLObjectType tableType = createTableType(catalog, schema, table, typeName);
					if (tableType.getFieldDefinitions().isEmpty())
						continue;
					additionalTypes.add(tableType);

					// Add a field for each table with filter arguments
					for (GraphQLObjectType.Builder builder : List.of(queryBuilder, subscriptionBuilder)) {

						builder.field(GraphQLFieldDefinition.newFieldDefinition()
							.name(queryFieldName)
							.type(GraphQLList.list(GraphQLTypeReference.typeRef(typeName)))
							.argument(GraphQLArgument.newArgument().name("limit").type(Scalars.GraphQLInt))
							.argument(GraphQLArgument.newArgument()
									.name("filters") // Add filters argument
									.type(GraphQLList.list(FILTER_INPUT_TYPE)) // Accept a
								// list of
								// filters
							)
							.dataFetcher(env -> {
								Integer limit = env.getArgument("limit") != null ? env.getArgument("limit") : 1000;
								List<Map<String, Object>> filters = env.getArgument("filters");

								// Fetch and filter data based on filters
								return trinoQueryService.queryTableWithFilters(catalog, schema, table, limit, filters);
							})
							.build());
					}

				}
			}
		}

		schemaBuilder.query(queryBuilder.build());
		schemaBuilder.subscription(subscriptionBuilder.build());
		schemaBuilder.additionalTypes(additionalTypes);
		schemaBuilder.definition(SchemaDefinition.newSchemaDefinition().build());
		return schemaBuilder.build();
	}

	public static final GraphQLInputObjectType FILTER_INPUT_TYPE = GraphQLInputObjectType.newInputObject()
		.name("FilterInput")
		.field(GraphQLInputObjectField.newInputObjectField()
			.name("field")
			.description("Column name")
			.type(GraphQLNonNull.nonNull(Scalars.GraphQLString)))
		.field(GraphQLInputObjectField.newInputObjectField()
			.name("operator")
			.description("Filter operation (e.g.: eq, lt, gt, like)")
			.type(GraphQLNonNull.nonNull(Scalars.GraphQLString)))

		// Flexible value fields (only one should be used per input)
		.field(GraphQLInputObjectField.newInputObjectField().name("stringValue").type(Scalars.GraphQLString))
		.field(GraphQLInputObjectField.newInputObjectField().name("intValue").type(Scalars.GraphQLInt))
		.field(GraphQLInputObjectField.newInputObjectField().name("floatValue").type(Scalars.GraphQLFloat))
		.field(GraphQLInputObjectField.newInputObjectField().name("booleanValue").type(Scalars.GraphQLBoolean))
		.field(GraphQLInputObjectField.newInputObjectField().name("dateValue").type(Scalars.GraphQLString)) // ISO
		// 8601
		// date
		// string
		.build();

	private GraphQLObjectType createTableType(String catalog, String schema, String table, String typeName) {
		GraphQLObjectType.Builder typeBuilder = GraphQLObjectType.newObject().name(typeName);

		for (Map<String, Object> column : trinoSchemaService.getColumns(catalog, schema, table)) {
			String columnName = (String) column.get("Column");
			String columnType = (String) column.get("Type");

			if (!VALID_CHAR_PATTERN.matcher(columnName).matches())
				continue;
			typeBuilder.field(GraphQLFieldDefinition.newFieldDefinition()
				.name(columnName)
				.type(mapColumnType(columnType))
				.build());
		}

		return typeBuilder.build();
	}

	private GraphQLOutputType mapColumnType(String trinoType) {
		return switch (trinoType.toLowerCase()) {
			case "integer", "bigint", "int" -> Scalars.GraphQLInt;
			case "double", "float" -> Scalars.GraphQLFloat;
			case "boolean" -> Scalars.GraphQLBoolean;
			default -> GraphQLString;
		};
	}

}
