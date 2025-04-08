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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class TrinoToGraphQLOutputTypeMapperComplexTest {

	@Test
	@DisplayName("Should map array(varchar) to GraphQLList of String")
	void testArrayType() {
		GraphQLOutputType gqlType = TrinoToGraphQLOutputTypeMapper.mapType("array(varchar)");
		assertInstanceOf(GraphQLList.class, gqlType);
		GraphQLList list = (GraphQLList) gqlType;
		assertEquals(Scalars.GraphQLString, list.getWrappedType());
	}

	@Test
	@DisplayName("Should map map(varchar,varchar) to GraphQLList of KeyValue")
	void testMapType() {
		GraphQLOutputType gqlType = TrinoToGraphQLOutputTypeMapper.mapType("map(varchar,varchar)");
		assertInstanceOf(GraphQLList.class, gqlType);
		GraphQLList list = (GraphQLList) gqlType;
		assertInstanceOf(GraphQLObjectType.class, list.getWrappedType());
		GraphQLObjectType objType = (GraphQLObjectType) list.getWrappedType();
		assertEquals("KeyValue", objType.getName());
	}

}
