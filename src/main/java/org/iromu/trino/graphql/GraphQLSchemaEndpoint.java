package org.iromu.trino.graphql;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GraphQLSchemaEndpoint {

    private final GraphQLSchema graphQLSchema;

    private final SchemaPrinter schemaPrinter;

    @Autowired
    public GraphQLSchemaEndpoint(GraphQLSchema graphQLSchema) {
        this.graphQLSchema = graphQLSchema;
        this.schemaPrinter = new SchemaPrinter(SchemaPrinter.Options.defaultOptions()
                .includeScalarTypes(true)
                .includeSchemaDefinition(true)
                .includeDirectives(true));
    }

    @GetMapping(value = "/schema.graphqls", produces = "text/plain")
    public String getSchema() {
        return schemaPrinter.print(graphQLSchema);
    }

}
