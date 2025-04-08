package org.iromu.trino.graphql;

import graphql.Scalars;
import graphql.schema.GraphQLOutputType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TrinoToGraphQLOutputTypeMapperTest {

	@ParameterizedTest(name = "Trino type \"{0}\" should map to GraphQL type \"{1}\"")
	@CsvSource({
		"boolean, GraphQLBoolean",
		"tinyint, GraphQLInt",
		"smallint, GraphQLInt",
		"integer, GraphQLInt",
		"bigint, GraphQLString",
		"real, GraphQLFloat",
		"double, GraphQLFloat",
		"varchar, GraphQLString",
		"char, GraphQLString",
		"varbinary, GraphQLString",
		"json, GraphQLString",
		"uuid, GraphQLString",
		"ipaddress, GraphQLString",
		"date, GraphQLString",
		"time, GraphQLString",
		"timestamp, GraphQLString",
		"interval, GraphQLString",
		"'decimal(10,2)', GraphQLString"
	})
	void testPrimitiveTypes(String trinoType, String expectedGraphQLType) {
		GraphQLOutputType gqlType = TrinoToGraphQLOutputTypeMapper.mapType(trinoType);

		switch (expectedGraphQLType) {
			case "GraphQLBoolean":
				assertEquals(Scalars.GraphQLBoolean, gqlType);
				break;
			case "GraphQLInt":
				assertEquals(Scalars.GraphQLInt, gqlType);
				break;
			case "GraphQLFloat":
				assertEquals(Scalars.GraphQLFloat, gqlType);
				break;
			case "GraphQLString":
				assertEquals(Scalars.GraphQLString, gqlType);
				break;
			default:
				throw new IllegalArgumentException("Unexpected GraphQL type: " + expectedGraphQLType);
		}
	}
}
