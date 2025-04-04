package org.iromu.trino.graphql;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TrinoSchemaService {

    private final JdbcTemplate jdbcTemplate;

    public TrinoSchemaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Get all catalogs
    public List<String> getCatalogs() {
        return jdbcTemplate.queryForList("SHOW CATALOGS", String.class);
    }

    // Get all schemas in a specific catalog
    public List<String> getSchemas(String catalog) {
        return jdbcTemplate.queryForList("SHOW SCHEMAS FROM " + catalog, String.class);
    }

    // Get all tables in a specific catalog and schema
    public List<String> getTables(String catalog, String schema) {
        return jdbcTemplate.queryForList("SHOW TABLES FROM " + catalog + "." + schema, String.class);
    }

    // Get columns for a table
    public List<Map<String, Object>> getColumns(String catalog, String schema, String table) {
        return jdbcTemplate.queryForList("DESCRIBE " + catalog + "." + schema + "." + table);
    }

}
