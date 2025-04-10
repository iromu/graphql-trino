package org.iromu.trino.graphql.data;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Abstract base class for detecting join relationships between tables in a Trino catalog.
 * <p>
 * Subclasses must implement the {@link #detect(String)} method to define how joins are
 * inferred. This class also provides a utility method to retrieve column metadata from
 * the Trino information schema.
 * </p>
 *
 * <p>
 * It filters out columns that are not commonly involved in joins (e.g., dates, booleans,
 * JSON, etc.) and excludes system/internal schemas from the results.
 * </p>
 *
 * @author Ivan Rodriguez
 */
public abstract class JoinDetector {

	private final JdbcTemplate jdbcTemplate;

	/**
	 * Constructs a JoinDetector using the provided JDBC template.
	 * @param jdbcTemplate the JDBC template to use for querying the Trino metadata
	 */
	public JoinDetector(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Abstract method to be implemented by subclasses to detect possible join
	 * relationships in the provided catalog.
	 * @param catalog the catalog to scan
	 * @return a list of join description strings (implementation dependent)
	 */
	public abstract List<String> detect(String catalog);

	/**
	 * Retrieves a filtered list of column metadata from the specified Trino catalog.
	 * <p>
	 * This method queries the {@code information_schema.columns} table, excluding system
	 * schemas and non-joinable data types such as dates, JSON, arrays, blobs, decimals,
	 * etc.
	 * </p>
	 * @param catalog the catalog to query
	 * @return a list of {@link ColumnInfo} objects representing filtered columns
	 */
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
				""", catalog);

		return jdbcTemplate.query(query,
				(rs, rowNum) -> new ColumnInfo(rs.getString("table_catalog"), rs.getString("table_schema"),
						rs.getString("table_name"), rs.getString("column_name"), rs.getString("data_type")));
	}

	/**
	 * POJO representing a column and its metadata. Used internally to support join
	 * detection logic.
	 */
	public static class ColumnInfo {

		/** Catalog the column belongs to. */
		String catalog;

		/** Schema the column belongs to. */
		String schema;

		/** Table the column belongs to. */
		String table;

		/** Name of the column. */
		String column;

		/** Data type of the column. */
		String dataType;

		/**
		 * Constructs a ColumnInfo object with the given metadata.
		 * @param catalog the catalog name
		 * @param schema the schema name
		 * @param table the table name
		 * @param column the column name
		 * @param dataType the column's data type
		 */
		ColumnInfo(String catalog, String schema, String table, String column, String dataType) {
			this.catalog = catalog;
			this.schema = schema;
			this.table = table;
			this.column = column;
			this.dataType = dataType;
		}

		/**
		 * Returns a string representation of the fully qualified column name with type
		 * info.
		 * @return the full name in the format
		 * <code>catalog.schema.table.column (type)</code>
		 */
		String fullName() {
			return catalog + "." + schema + "." + table + "." + column + " (" + dataType + ") ";
		}

		/**
		 * Returns the fully qualified table name.
		 * @return the table name in the format <code>catalog.schema.table</code>
		 */
		String tableName() {
			return catalog + "." + schema + "." + table;
		}

	}

}
