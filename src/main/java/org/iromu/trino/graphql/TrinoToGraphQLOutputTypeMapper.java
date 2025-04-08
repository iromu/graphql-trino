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
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

public class TrinoToGraphQLOutputTypeMapper {

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
			return Scalars.GraphQLString;  // Mapping decimal to String for simplicity
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

	private static GraphQLOutputType handleArray(String trinoType) {
		String innerType = extractInner(trinoType);
		return GraphQLList.list(mapType(innerType));
	}

	private static GraphQLOutputType handleMap() {
		return GraphQLList.list(getKeyValueObjectType());
	}

	private static String extractInner(String type) {
		int start = type.indexOf('(') + 1;
		int end = type.lastIndexOf(')');
		return type.substring(start, end).trim();
	}

	private static GraphQLOutputType getKeyValueObjectType() {
		return GraphQLObjectType.newObject()
			.name("KeyValue")
			.field(f -> f.name("key").type(Scalars.GraphQLString))
			.field(f -> f.name("value").type(Scalars.GraphQLString))
			.build();
	}
}
