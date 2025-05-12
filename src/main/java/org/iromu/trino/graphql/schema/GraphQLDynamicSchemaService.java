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

import graphql.Scalars;
import graphql.language.SchemaDefinition;
import graphql.schema.*;
import lombok.extern.slf4j.Slf4j;
import org.iromu.trino.graphql.AppProperties;
import org.iromu.trino.graphql.data.TrinoQueryService;
import org.iromu.trino.graphql.data.TrinoSchemaService;
import org.iromu.trino.graphql.data.TrinoToGraphQLOutputTypeMapper;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.iromu.trino.graphql.schema.GraphQLSchemaFixer.VALID_CHAR_PATTERN;

/**
 * Service responsible for dynamically generating a {@link GraphQLSchema} from Trino
 * metadata.
 * <p>
 * This service builds GraphQL query and subscription fields for each table in Trino,
 * exposing them as strongly-typed GraphQL types. It supports filtering via a flexible
 * input type and maps Trino data types to corresponding GraphQL types.
 * </p>
 *
 * <p>
 * It integrates with {@link TrinoSchemaService} to read catalog/schema/table/column
 * structure and with {@link TrinoQueryService} to provide data fetching at query time.
 * </p>
 *
 * <p>
 * Filter operations are also supported using a custom enum and input type definition.
 * </p>
 *
 * @author Ivan Rodriguez
 */
@Service
@Slf4j
public class GraphQLDynamicSchemaService {

	private final TrinoSchemaService trinoSchemaService;

	private final TrinoQueryService trinoQueryService;

	private final AppProperties app;

	/**
	 * Constructs a dynamic schema service with injected dependencies.
	 * @param trinoSchemaService service for accessing Trino catalog/schema/table metadata
	 * @param trinoQueryService service responsible for executing queries with filtering
	 * @param app application properties containing configuration flags
	 */
	public GraphQLDynamicSchemaService(TrinoSchemaService trinoSchemaService, TrinoQueryService trinoQueryService,
			AppProperties app) {
		this.trinoSchemaService = trinoSchemaService;
		this.trinoQueryService = trinoQueryService;
		this.app = app;
	}

	/**
	 * Dynamically builds a {@link GraphQLSchema} from Trino metadata.
	 * <p>
	 * For every valid table in every valid schema and catalog, a corresponding
	 * {@link graphql.schema.GraphQLObjectType} is created and exposed via GraphQL.
	 * Filtering is supported via a {@code filters} argument and optional {@code limit}.
	 * </p>
	 * @return the fully constructed {@link GraphQLSchema}
	 */
	public GraphQLSchema generateSchema() {
		GraphQLSchema.Builder schemaBuilder = GraphQLSchema.newSchema();
		GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
		GraphQLObjectType.Builder subscriptionBuilder = GraphQLObjectType.newObject().name("Subscription");

		// Store created types to register later
		Set<GraphQLType> additionalTypes = new HashSet<>();

		queryBuilder.field(GraphQLFieldDefinition.newFieldDefinition()
			.name("catalogs")
			.type(GraphQLList.list(Scalars.GraphQLString))
			.dataFetcher(env -> trinoSchemaService.getCatalogs())
			.build());

		for (String catalog : trinoSchemaService.getCatalogs()) {
			if (app.getExcludeCatalogs() != null && app.getExcludeSchemas().contains(catalog)) {
				continue;
			}
			if (app.getIncludeCatalogs() != null && !app.getIncludeCatalogs().contains(catalog)) {
				continue;
			}
			if (app.isIgnoreObjectsWithWrongCharacters() && !VALID_CHAR_PATTERN.matcher(catalog).matches()) {
				continue;
			}
			for (String schema : trinoSchemaService.getSchemas(catalog)) {
				if (app.getExcludeSchemas() != null && app.getExcludeSchemas().contains(catalog)) {
					continue;
				}
				if (app.isIgnoreObjectsWithWrongCharacters() && !VALID_CHAR_PATTERN.matcher(schema).matches()) {
					continue;
				}
				for (String table : trinoSchemaService.getTables(catalog, schema)) {
					if (app.isIgnoreObjectsWithWrongCharacters() && !VALID_CHAR_PATTERN.matcher(table).matches())
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
							.description("Catalog: " + catalog + ", Schema: " + schema + ", Table: " + table)
							.type(GraphQLList.list(GraphQLTypeReference.typeRef(typeName)))
							.argument(GraphQLArgument.newArgument()
								.name("limit")
								.type(Scalars.GraphQLInt)
								.description("Limit number of rows"))
							.argument(GraphQLArgument.newArgument()
								.name("filters") // Add filters argument
								.description("Filter selection")
								.type(GraphQLList.list(FILTER_INPUT_TYPE)) // Accept a
							// list of
							// filters
							)
							.dataFetcher(env -> {
								Integer limit = env.getArgument("limit") != null ? env.getArgument("limit") : 1000;
								List<Map<String, Object>> filters = env.getArgument("filters");

								// Fetch and filter data based on filters
								// noinspection DataFlowIssue
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

	/**
	 * Enum type defining supported filtering operations.
	 * <p>
	 * Used by the {@link #FILTER_INPUT_TYPE} to represent SQL-like operators such as
	 * {@code EQ}, {@code GT}, {@code LIKE}, etc.
	 * </p>
	 */
	public static final GraphQLEnumType OPERATOR_ENUM = GraphQLEnumType.newEnum()
		.name("FilterOperator")
		.description("SQL-compatible filter operations")
		.value("EQ", "eq", "Equal to (=)")
		.value("NEQ", "neq", "Not equal to (!= or <>)")
		.value("GT", "gt", "Greater than (>)")
		.value("GTE", "gte", "Greater than or equal (>=)")
		.value("LT", "lt", "Less than (<)")
		.value("LTE", "lte", "Less than or equal (<=)")
		.value("LIKE", "like", "String match (LIKE)")
		.value("NOT_LIKE", "not_like", "Not string match (NOT LIKE)")
		.value("IN", "in", "In list (IN)")
		.value("NOT_IN", "not_in", "Not in list (NOT IN)")
		.value("IS_NULL", "is_null", "Is NULL")
		.value("IS_NOT_NULL", "is_not_null", "Is NOT NULL")
		.value("BETWEEN", "between", "Between values")
		.value("NOT_BETWEEN", "not_between", "Not between values")
		.build();

	/**
	 * Defines an input type used to apply filters to table queries.
	 *
	 * <p>
	 * Supports a variety of value types (string, int, float, etc.) as well as value lists
	 * for operations like {@code IN} and {@code BETWEEN}.
	 * </p>
	 */
	public static final GraphQLInputObjectType FILTER_INPUT_TYPE = GraphQLInputObjectType.newInputObject()
		.name("FilterInput")
		.field(GraphQLInputObjectField.newInputObjectField()
			.name("field")
			.description("Column name")
			.type(GraphQLNonNull.nonNull(Scalars.GraphQLString)))
		.field(GraphQLInputObjectField.newInputObjectField()
			.name("operator")
			.description("Filter operation")
			.type(GraphQLNonNull.nonNull(OPERATOR_ENUM)))

		// Flexible value fields (only one should be used per input)
		.field(GraphQLInputObjectField.newInputObjectField().name("stringValue").type(Scalars.GraphQLString))
		.field(GraphQLInputObjectField.newInputObjectField().name("intValue").type(Scalars.GraphQLInt))
		.field(GraphQLInputObjectField.newInputObjectField().name("floatValue").type(Scalars.GraphQLFloat))
		.field(GraphQLInputObjectField.newInputObjectField().name("booleanValue").type(Scalars.GraphQLBoolean))
		.field(GraphQLInputObjectField.newInputObjectField().name("dateValue").type(Scalars.GraphQLString)) // ISO
		// 8601
		// date
		// string
		.field(GraphQLInputObjectField.newInputObjectField()
			.name("values")
			.description("List of values for IN, BETWEEN, etc.")
			.type(GraphQLList.list(Scalars.GraphQLString)))
		.build();

	/**
	 * Creates a {@link GraphQLObjectType} for a Trino table.
	 *
	 * <p>
	 * Each column in the table is mapped to a GraphQL field with a matching type using
	 * {@link TrinoToGraphQLOutputTypeMapper}.
	 * </p>
	 * @param catalog the catalog the table belongs to
	 * @param schema the schema the table belongs to
	 * @param table the table name
	 * @param typeName the unique GraphQL type name for this table
	 * @return the generated {@link GraphQLObjectType}
	 */
	private GraphQLObjectType createTableType(String catalog, String schema, String table, String typeName) {
		GraphQLObjectType.Builder typeBuilder = GraphQLObjectType.newObject().name(typeName);

		for (Map<String, Object> column : trinoSchemaService.getColumns(catalog, schema, table)) {
			String columnName = (String) column.get("Column");
			String columnType = (String) column.get("Type");

			if (app.isIgnoreObjectsWithWrongCharacters() && !VALID_CHAR_PATTERN.matcher(columnName).matches())
				continue;
			typeBuilder.field(GraphQLFieldDefinition.newFieldDefinition()
				.name(columnName)
				.description("Trino type: " + columnType)
				.type(TrinoToGraphQLOutputTypeMapper.mapType(columnType))
				.build());
		}

		return typeBuilder.build();
	}

}
