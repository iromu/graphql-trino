/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.iromu.trino.graphql.data;

import lombok.extern.slf4j.Slf4j;
import org.iromu.trino.graphql.schema.GraphQLSchemaFixer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service class responsible for executing SQL queries against Trino using JDBC. It
 * supports dynamic querying of tables with optional filters and schema sanitization.
 *
 * @author Ivan Rodriguez
 */
@Service
@Slf4j
public class TrinoQueryService {

	private final JdbcTemplate jdbcTemplate;

	private final GraphQLSchemaFixer fixer;

	/**
	 * Constructs a new {@code TrinoQueryService} with required dependencies.
	 * @param jdbcTemplate the {@code JdbcTemplate} used for executing SQL queries
	 * @param fixer the {@code GraphQLSchemaFixer} used for sanitizing and restoring
	 * schema/table names
	 */
	public TrinoQueryService(JdbcTemplate jdbcTemplate, GraphQLSchemaFixer fixer) {
		this.jdbcTemplate = jdbcTemplate;
		this.fixer = fixer;
	}

	/**
	 * Extracts the value from a filter map by checking a predefined list of allowed keys.
	 * @param filter a map representing a filter with various typed values
	 * @return the non-null value from the filter
	 * @throws IllegalArgumentException if no valid value is found
	 */
	private Object extractFilterValue(Map<String, Object> filter) {
		for (String key : List.of("stringValue", "intValue", "floatValue", "booleanValue", "dateValue")) {
			if (filter.get(key) != null) {
				return filter.get(key);
			}
		}
		throw new IllegalArgumentException("No valid value in filter: " + filter);
	}

	/**
	 * Executes a SQL query against a specific Trino table with optional filters and
	 * limit.
	 * @param _catalog the sanitized catalog name
	 * @param _schema the sanitized schema name
	 * @param _table the sanitized table name
	 * @param limit the maximum number of rows to return
	 * @param filters optional filters to apply as WHERE conditions
	 * @return a list of maps, each representing a row of data with sanitized field names
	 * @throws IllegalArgumentException if an unsupported operator is used in the filter
	 */
	public List<Map<String, Object>> queryTableWithFilters(String _catalog, String _schema, String _table, int limit,
			List<Map<String, Object>> filters) {

		String catalog = fixer.restoreSanitizedSchema(_catalog);
		String schema = fixer.restoreSanitizedSchema(_schema);
		String table = fixer.restoreSanitizedSchema(_table);

		// Construct the base SQL query (can be more dynamic if needed)
		StringBuilder query = new StringBuilder("SELECT t1.* FROM " + catalog + "." + schema + "." + table + " t1");

		if (filters != null && !filters.isEmpty()) {
			query.append(" WHERE ");

			// Iterate through filters and add conditions to the WHERE clause
			for (int i = 0; i < filters.size(); i++) {
				Map<String, Object> filter = filters.get(i);
				String field = fixer.restoreSanitizedSchema((String) filter.get("field"));
				String operator = (String) filter.get("operator");
				Object value = extractFilterValue(filter);

				if (i > 0) {
					query.append(" AND ");
				}

				// Handle different operators (this can be extended as needed)
				switch (operator.toLowerCase()) {
					case "eq":
						query.append(field).append(" = ").append(value);
						break;
					case "lt":
						query.append(field).append(" < '").append(value).append("'");
						break;
					case "gt":
						query.append(field).append(" > '").append(value).append("'");
						break;
					case "like":
						query.append(field).append(" LIKE '%").append(value).append("%'");
						break;
					default:
						throw new IllegalArgumentException("Unsupported operator: " + operator);
				}
			}
		}

		// Add LIMIT clause
		query.append(" LIMIT ").append(limit);
		log.info("{}", query); // Execute the query using the Trino service or another
		// database connector
		List<Map<String, Object>> maps = jdbcTemplate.queryForList(query.toString());
		for (Map<String, Object> map : maps) {
			for (String key : map.keySet()) {
				String sanitized = fixer.sanitizeSchema(key);
				if (!key.equals(sanitized)) {
					map.put(sanitized, map.get(key));
					map.remove(key);
				}
			}

		}
		return maps;
	}

	// public Stream<Map<String, Object>> queryTableWithFiltersStream(String catalog,
	// String schema, String table,
	// int limit, List<Map<String, Object>> filters) {
	// StringBuilder sql = new StringBuilder("SELECT * FROM ").append(catalog)
	// .append(".")
	// .append(schema)
	// .append(".")
	// .append(table);
	//
	// List<Object> params = new ArrayList<>();
	//
	// if (filters != null && !filters.isEmpty()) {
	// sql.append(" WHERE ");
	// for (int i = 0; i < filters.size(); i++) {
	// Map<String, Object> filter = filters.get(i);
	// String field = (String) filter.get("field");
	// String operator = (String) filter.get("operator");
	// Object value = extractFilterValue(filter);
	//
	// if (i > 0)
	// sql.append(" AND ");
	//
	// switch (operator.toLowerCase()) {
	// case "eq" -> {
	// sql.append(field).append(" = ?");
	// params.add(value);
	// }
	// case "lt" -> {
	// sql.append(field).append(" < ?");
	// params.add(value);
	// }
	// case "gt" -> {
	// sql.append(field).append(" > ?");
	// params.add(value);
	// }
	// case "like" -> {
	// sql.append(field).append(" LIKE ?");
	// params.add("%" + value + "%");
	// }
	// default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
	// }
	// }
	// }
	//
	// sql.append(" LIMIT ").append(limit);
	//
	// try {
	// Connection conn =
	// Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
	// PreparedStatement ps = conn.prepareStatement(sql.toString());
	// for (int i = 0; i < params.size(); i++) {
	// ps.setObject(i + 1, params.get(i));
	// }
	// ResultSet rs = ps.executeQuery();
	// ResultSetMetaData meta = rs.getMetaData();
	// int columnCount = meta.getColumnCount();
	//
	// Iterator<Map<String, Object>> iterator = new Iterator<>() {
	// boolean hasNext = rs.next();
	//
	// @Override
	// public boolean hasNext() {
	// return hasNext;
	// }
	//
	// @Override
	// public Map<String, Object> next() {
	// try {
	// Map<String, Object> row = new LinkedHashMap<>();
	// for (int i = 1; i <= columnCount; i++) {
	// row.put(meta.getColumnLabel(i), rs.getObject(i));
	// }
	// hasNext = rs.next();
	// return row;
	// }
	// catch (SQLException e) {
	// throw new RuntimeException(e);
	// }
	// }
	// };
	//
	// // Stream with auto-closing logic
	// Spliterator<Map<String, Object>> spliterator =
	// Spliterators.spliteratorUnknownSize(iterator,
	// Spliterator.ORDERED);
	// return StreamSupport.stream(spliterator, false).onClose(() -> {
	// try {
	// rs.close();
	// ps.close();
	// conn.close();
	// }
	// catch (SQLException e) {
	// throw new RuntimeException("Failed to close resources", e);
	// }
	// });
	//
	// }
	// catch (SQLException e) {
	// throw new RuntimeException("Failed to query data", e);
	// }
	// }

}
