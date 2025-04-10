package org.iromu.trino.graphql.data.relations;

import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link JoinDetector} implementation that detects possible join relationships between
 * tables within the same schema in a given Trino catalog.
 * <p>
 * This detector focuses on columns with the same name that exist in multiple tables
 * within the same schema. It applies normalization and basic heuristics to infer
 * potential joins, particularly targeting common naming conventions like *_id fields.
 * </p>
 *
 * <p>
 * The results are printed to the console and returned as a list of strings (currently
 * unused, can be enhanced to include detailed join metadata).
 * </p>
 *
 * <pre>{@code
 * Example:
 *   table_a.user_id ↔ table_b.user_id
 *   table_a.id → table_b.user_id
 * }</pre>
 *
 * <p>
 * Marked as {@code @Primary} to make it the default bean when multiple
 * {@code JoinDetector}s are present.
 * </p>
 *
 * @author Ivan Rodriguez
 */
@Primary
@Component
public class SameSchemaJoinDetector extends JoinDetector {

	/**
	 * Constructs a SameSchemaJoinDetector with the given JDBC template.
	 * @param jdbcTemplate JDBC template used to query metadata from Trino
	 */
	public SameSchemaJoinDetector(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}

	/**
	 * Detects join relationships between tables within the same schema of a given
	 * catalog.
	 * <p>
	 * The detection is based on:
	 * <ul>
	 * <li>Identical column names across multiple tables within a schema</li>
	 * <li>Heuristics for common foreign key naming conventions (e.g., *_id)</li>
	 * </ul>
	 * @param catalog the catalog to scan
	 * @return an empty list (extendable to return detailed join information)
	 */
	public List<String> detect(String catalog) {
		// Map to store columns by column name, also grouping by schema
		Map<String, Map<String, List<ColumnInfo>>> schemaColumnMap = new HashMap<>();

		for (ColumnInfo col : getColumns(catalog)) {
			// if (col.isJoinableColumn()) { // Only process columns that are joinable
			schemaColumnMap.computeIfAbsent(col.schema, k -> new HashMap<>()) // Group by
																				// schema
																				// first
				.computeIfAbsent(col.column, k -> new ArrayList<>()) // Group by column
																		// name within
																		// schema
				.add(col);
			// }
		}

		System.out.println("🔍 Possible Join Candidates for Catalog: " + catalog + "\n");

		// Detect joins within the same schema based on exact column name matches
		for (Map.Entry<String, Map<String, List<ColumnInfo>>> schemaEntry : schemaColumnMap.entrySet()) {
			String schema = schemaEntry.getKey();
			Map<String, List<ColumnInfo>> columnMap = schemaEntry.getValue();

			// For each column name, check if there are multiple columns with the same
			// name within the schema
			for (Map.Entry<String, List<ColumnInfo>> columnEntry : columnMap.entrySet()) {
				String columnName = columnEntry.getKey();
				List<ColumnInfo> columnList = columnEntry.getValue();

				// If there are multiple tables with the same column name within the same
				// schema, it's a potential join
				if (columnList.size() > 1) {
					// Avoid matching columns that are too different (e.g., `s_manager` vs
					// `s_company_name`)
					for (int i = 0; i < columnList.size(); i++) {
						for (int j = i + 1; j < columnList.size(); j++) {
							ColumnInfo col1 = columnList.get(i);
							ColumnInfo col2 = columnList.get(j);

							// Apply smarter name matching logic (e.g., don't match
							// `s_manager` with `s_company_name`)
							if (isJoinable(col1.column, col2.column)) {
								System.out.println("🧩 " + col1.fullName() + " ↔ " + col2.fullName());
							}
						}
					}
				}
			}
		}
		return new ArrayList<>();
	}

	/**
	 * Determines if two column names are likely to be involved in a join relationship.
	 * <p>
	 * Normalizes names (removes underscores and converts to lowercase) and supports
	 * matching of common foreign key conventions (e.g., {@code user_id} with
	 * {@code user_id}).
	 * </p>
	 * @param column1 the first column name
	 * @param column2 the second column name
	 * @return {@code true} if the columns are likely joinable, {@code false} otherwise
	 */
	boolean isJoinable(String column1, String column2) {
		// Normalize the column names (remove underscores and convert to lowercase)
		String normalizedColumn1 = normalizeColumnName(column1);
		String normalizedColumn2 = normalizeColumnName(column2);

		// Check if the columns are exactly the same after normalization
		if (normalizedColumn1.equals(normalizedColumn2)) {
			return true;
		}

		// Additional checks for common join patterns (e.g., '_id' suffixes)
		if (column1.endsWith("_id") && column2.endsWith("_id")) {
			String base1 = column1.substring(0, column1.length() - 3); // Remove "_id"
			String base2 = column2.substring(0, column2.length() - 3); // Remove "_id"
			return base1.equalsIgnoreCase(base2); // Compare the base names
		}

		// Return false if no meaningful match is found
		return false;
	}

	/**
	 * Utility to normalize column names for comparison purposes.
	 * @param columnName the original column name
	 * @return a normalized version with underscores removed and converted to lowercase
	 */
	private String normalizeColumnName(String columnName) {
		return columnName.replace("_", "").toLowerCase();
	}

}
