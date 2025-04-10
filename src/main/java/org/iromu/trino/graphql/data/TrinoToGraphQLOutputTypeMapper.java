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

import graphql.Scalars;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

/**
 * Maps Trino SQL types to GraphQL output types. Handles primitive types as well as
 * complex types like arrays and maps.
 *
 * <p>
 * Note: Complex types like {@code map} are represented as GraphQL lists of objects with
 * {@code key} and {@code value} fields.
 *
 * @author Ivan Rodriguez
 */
public class TrinoToGraphQLOutputTypeMapper {

	// Define the KeyValue type once as a static final field to avoid redefining it
	private static final GraphQLOutputType KEY_VALUE_TYPE = GraphQLObjectType.newObject()
		.name("KeyValue")
		.field(f -> f.name("key").type(Scalars.GraphQLString))
		.field(f -> f.name("value").type(Scalars.GraphQLString))
		.build();

	/**
	 * Maps a Trino SQL type string to a corresponding GraphQL output type.
	 * @param trinoType the SQL type from Trino (e.g., "varchar", "array(bigint)")
	 * @return the corresponding {@link GraphQLOutputType}
	 */
	public static GraphQLOutputType mapType(String trinoType) {
		trinoType = trinoType.trim().toLowerCase();

		// Handle complex types first
		if (trinoType.startsWith("array(")) {
			return handleArray(trinoType);
		}

		if (trinoType.startsWith("map(")) {
			return handleMap();
		}

		if (trinoType.startsWith("decimal(")) {
			return Scalars.GraphQLString; // Mapping decimal to String for simplicity
		}

		// Base types using switch
		return switch (trinoType) {
			case "boolean" -> Scalars.GraphQLBoolean;
			case "tinyint", "smallint", "integer", "int" -> Scalars.GraphQLInt;
			case "bigint" -> Scalars.GraphQLString;
			case "real", "double" -> Scalars.GraphQLFloat;
			case "varchar", "char", "varbinary", "json", "uuid", "ipaddress", "date", "time", "timestamp", "interval" ->
				Scalars.GraphQLString;
			default -> Scalars.GraphQLString; // fallback
		};
	}

	/**
	 * Handles mapping of Trino {@code array(<type>)} to GraphQL {@code [<type>]}.
	 * @param trinoType the full array type string (e.g., "array(varchar)")
	 * @return the GraphQL list type corresponding to the array's inner type
	 */
	private static GraphQLOutputType handleArray(String trinoType) {
		String innerType = extractInner(trinoType);
		return GraphQLList.list(mapType(innerType));
	}

	/**
	 * Handles Trino {@code map(key_type, value_type)} by mapping to a GraphQL list of
	 * KeyValue objects.
	 * @return a {@link GraphQLList} of {@link GraphQLObjectType} "KeyValue"
	 */
	private static GraphQLOutputType handleMap() {
		return GraphQLList.list(KEY_VALUE_TYPE);
	}

	/**
	 * Extracts the inner type from a parameterized type string. For example:
	 * {@code array(bigint)} -> {@code bigint}
	 * @param type the full type string
	 * @return the inner type string
	 */
	private static String extractInner(String type) {
		int start = type.indexOf('(') + 1;
		int end = type.lastIndexOf(')');
		return type.substring(start, end).trim();
	}

}
