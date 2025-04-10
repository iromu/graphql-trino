package org.iromu.trino.graphql.data;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of the {@link JoinDetector} for automatically detecting
 * potential join relationships between tables in a given Trino catalog.
 * <p>
 * This component analyzes the column names across all tables in the specified catalog to
 * suggest join candidates. It uses naming conventions (e.g., foreign key suffix `_id`)
 * and column name similarities to infer potential joins.
 * </p>
 *
 * <p>
 * Example detection includes:
 * </p>
 * <ul>
 * <li>Columns ending in <code>_id</code> pointing to tables with a corresponding
 * <code>id</code> column</li>
 * <li>Columns with the same name existing across multiple tables</li>
 * </ul>
 *
 * <p>
 * Heuristics are applied to reduce false positives by checking normalized column name
 * matches and known suffixes.
 * </p>
 *
 * @author Ivan Rodriguez
 */
@Component
public class DefaultJoinDetector extends JoinDetector {

	/**
	 * Constructs a {@code DefaultJoinDetector} with the provided {@link JdbcTemplate}.
	 * @param jdbcTemplate the JDBC template used to query schema information
	 */
	public DefaultJoinDetector(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}

	/**
	 * Detects potential join relationships in the given catalog.
	 * <p>
	 * This method looks for:
	 * <ul>
	 * <li>Columns ending in <code>_id</code> that may reference <code>id</code> columns
	 * in other tables</li>
	 * <li>Columns with matching or similar names across tables</li>
	 * </ul>
	 * The results are printed to the console as part of the detection process.
	 * @param catalog the catalog to scan for possible joins
	 * @return a list of join strings (currently returns an empty list as the
	 * implementation prints instead)
	 */
	public List<String> detect(String catalog) {
		List<ColumnInfo> columns = getColumns(catalog);

		// Map to store columns by column name
		Map<String, List<ColumnInfo>> columnMap = new HashMap<>();
		for (ColumnInfo col : columns) {
			columnMap.computeIfAbsent(col.column, k -> new ArrayList<>()).add(col);
		}

		System.out.println("🔍 Possible Join Candidates for Catalog: " + catalog + "\n");

		for (ColumnInfo col : columns) {
			if (col.column.endsWith("_id")) {
				String refName = col.column.substring(0, col.column.length() - 3); // strip
				// "_id"
				for (Map.Entry<String, List<ColumnInfo>> entry : columnMap.entrySet()) {
					for (ColumnInfo candidate : entry.getValue()) {
						boolean nameMatch = candidate.table.equalsIgnoreCase(refName)
								|| candidate.table.equalsIgnoreCase(refName + "s");
						if (nameMatch && candidate.column.equalsIgnoreCase("id")) {
							System.out.println("🧩 " + col.fullName() + " → " + candidate.fullName());
						}
					}
				}
			}
		}

		// Detect joins based on exact column name matches and similar names
		for (Map.Entry<String, List<ColumnInfo>> entry : columnMap.entrySet()) {
			String columnName = entry.getKey();
			List<ColumnInfo> columnList = entry.getValue();

			// If there are multiple tables with the same column name, it's a potential
			// join
			if (columnList.size() > 1) {
				// Avoid matching columns that are too different (e.g., `s_manager` vs
				// `s_company_name`)
				for (int i = 0; i < columnList.size(); i++) {
					for (int j = i + 1; j < columnList.size(); j++) {
						ColumnInfo col1 = columnList.get(i);
						ColumnInfo col2 = columnList.get(j);

						// Apply smarter name matching logic (for example, don't match
						// `s_manager` with `s_company_name`)
						if (isJoinable(col1.column, col2.column)) {
							System.out.println("🧩 " + col1.fullName() + " ↔ " + col2.fullName());
						}
					}
				}
			}
		}
		return new ArrayList<>();
	}

	/**
	 * Determines if two columns are likely joinable based on their names.
	 * @param column1 the name of the first column
	 * @param column2 the name of the second column
	 * @return {@code true} if the columns are considered joinable, {@code false}
	 * otherwise
	 */
	private boolean isJoinable(String column1, String column2) {
		// Here we apply better heuristics to avoid false positives
		// Example: Avoid matching `manager` with `company_name`, but allow `user_id` with
		// `user_id`

		// Check if the columns have the same base name (e.g., `user_id` vs `user_id`)
		if (column1.equalsIgnoreCase(column2)) {
			return true;
		}

		// Normalize the column names (remove underscores and convert to lowercase)
		String normalizedColumn1 = normalizeColumnName(column1);
		String normalizedColumn2 = normalizeColumnName(column2);

		// Check if the columns are exactly the same after normalization
		if (normalizedColumn1.equals(normalizedColumn2)) {
			return true;
		}

		// Additional checks for common join patterns (e.g., '_id' suffixes)
		if (column1.endsWith("_id") && column2.endsWith("_id")) {
			return column1.substring(0, column1.length() - 3)
				.equalsIgnoreCase(column2.substring(0, column2.length() - 3));
		}

		// Add more custom logic for your data model if needed
		return false;
	} // Normalize column name by removing underscores and converting to lowercase

	/**
	 * Normalizes a column name by removing underscores and converting to lowercase.
	 * @param columnName the original column name
	 * @return a normalized version of the column name
	 */
	private String normalizeColumnName(String columnName) {
		return columnName.replace("_", "").toLowerCase();
	}

}
