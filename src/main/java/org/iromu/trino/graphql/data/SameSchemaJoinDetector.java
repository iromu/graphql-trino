package org.iromu.trino.graphql.data;

import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivan Rodriguez
 */
@Primary
@Component
public class SameSchemaJoinDetector extends JoinDetector {

	public SameSchemaJoinDetector(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}

	// Method to detect joins for a specific catalog
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

	// Method to check if two columns are likely joinable based on name patterns
	private boolean isJoinable(String column1, String column2) {
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

	// Normalize column name by removing underscores and converting to lowercase
	private String normalizeColumnName(String columnName) {
		return columnName.replace("_", "").toLowerCase();
	}

}
