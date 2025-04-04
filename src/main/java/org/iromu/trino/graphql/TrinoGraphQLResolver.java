package org.iromu.trino.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 *
 */
@Controller
public class TrinoGraphQLResolver {

    private final TrinoSchemaService trinoSchemaService;

    public TrinoGraphQLResolver(TrinoSchemaService trinoSchemaService) {
        this.trinoSchemaService = trinoSchemaService;
    }

    // List all catalogs in Trino
    @QueryMapping
    public List<String> catalogs() {
        return trinoSchemaService.getCatalogs();
    }

    // List schemas in a specific catalog
    @QueryMapping
    public List<String> schemas(@Argument String catalog) {
        return trinoSchemaService.getSchemas(catalog);
    }

    // List tables in a specific catalog and schema
    @QueryMapping
    public List<String> tables(@Argument String catalog, @Argument String schema) {
        return trinoSchemaService.getTables(catalog, schema);
    }

}
