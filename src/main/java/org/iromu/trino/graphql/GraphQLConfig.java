package org.iromu.trino.graphql;

import graphql.schema.GraphQLSchema;
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphQLConfig {

    private final GraphQLDynamicSchemaService schemaService;

    public GraphQLConfig(GraphQLDynamicSchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @Bean
    public GraphQLSchema schema() {
        return schemaService.generateSchema();
    }

    // @Bean
    // public GraphQL graphQL(GraphQLSchema schema) {
    // return GraphQL.newGraphQL(schema).build();
    // }

    @Bean
    public GraphQlSourceBuilderCustomizer federationTransform(GraphQLSchema schema) {
        return builder -> builder.configureGraphQl(graphQLBuilder -> graphQLBuilder.schema(schema));
    }

}
