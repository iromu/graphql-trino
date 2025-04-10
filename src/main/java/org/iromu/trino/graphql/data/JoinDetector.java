package org.iromu.trino.graphql.data;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * @author Ivan Rodriguez
 */
public abstract class JoinDetector {

	private final JdbcTemplate jdbcTemplate;

	public JoinDetector(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public abstract List<String> detect(String catalog);

	public static class ColumnInfo {

		String catalog;

		String schema;

		String table;

		String column;

		String dataType;

		ColumnInfo(String catalog, String schema, String table, String column, String dataType) {
			this.catalog = catalog;
			this.schema = schema;
			this.table = table;
			this.column = column;
			this.dataType = dataType;
		}

		String fullName() {
			return catalog + "." + schema + "." + table + "." + column + " (" + dataType + ") ";
		}

		String tableName() {
			return catalog + "." + schema + "." + table;
		}

	}

	public List<ColumnInfo> getColumns(String catalog) {

		String query = String.format("""
				    SELECT table_catalog, table_schema, table_name, column_name, data_type
				    FROM %s.information_schema.columns
				    WHERE table_schema NOT IN ('information_schema', 'pg_catalog', 'system', 'sys')
				      AND data_type NOT IN (
				        'date', 'timestamp', 'timestamp with time zone', 'boolean', 'json', 'jsonb', 'interval',
				        'blob', 'array', 'map', 'row', 'struct', 'decimal', 'float', 'real'
				      )
				      AND data_type NOT LIKE 'varchar%%'
				""", catalog); // Use catalog variable in the query

		// Query Trino to get column metadata
		return jdbcTemplate.query(query,
				(rs, rowNum) -> new ColumnInfo(rs.getString("table_catalog"), rs.getString("table_schema"),
						rs.getString("table_name"), rs.getString("column_name"), rs.getString("data_type")));
	}

}
