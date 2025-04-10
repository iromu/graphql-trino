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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.iromu.trino.graphql.AppProperties;
import org.iromu.trino.graphql.data.relations.JoinDetector;
import org.iromu.trino.graphql.schema.GraphQLSchemaFixer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service responsible for interacting with the Trino metadata system. It retrieves and
 * caches catalogs, schemas, tables, columns, and join information. Uses
 * {@link JdbcTemplate} for SQL execution, and caches results locally using
 * {@link ObjectMapper}.
 *
 * @author Ivan Rodriguez
 */
@Service
@Slf4j
public class TrinoSchemaService {

	private final JdbcTemplate jdbcTemplate;

	private final ObjectMapper objectMapper;

	private final AppProperties app;

	private final GraphQLSchemaFixer fixer;

	private final JoinDetector joinDetector;

	/**
	 * Constructs a new {@code TrinoSchemaService} with all required dependencies.
	 * @param jdbcTemplate the JDBC template for SQL queries
	 * @param objectMapper the object mapper for caching metadata to JSON files
	 * @param app application configuration properties
	 * @param fixer helper for sanitizing/restoring schema and table names
	 * @param joinDetector optional join detection component
	 */
	public TrinoSchemaService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, AppProperties app,
			GraphQLSchemaFixer fixer, Optional<JoinDetector> joinDetector) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
		this.app = app;
		this.fixer = fixer;
		this.joinDetector = joinDetector == null ? null : joinDetector.orElse(null);
	}

	/**
	 * Retrieves all available catalogs from Trino, optionally using a cached version.
	 * @return a list of catalog names
	 */
	@SneakyThrows
	public List<String> getCatalogs() {
		// noinspection ResultOfMethodCallIgnored
		Paths.get(app.getSchemaFolder()).toFile().mkdirs();
		File file = Paths.get(app.getSchemaFolder(), "catalogs.json").toFile();
		if (!app.isIgnoreCache() && file.exists()) {
			return objectMapper.readValue(file, new TypeReference<>() {
			});
		}
		log.info("SHOW CATALOGS");
		List<String> catalogs = jdbcTemplate.queryForList("SHOW CATALOGS", String.class);
		if (app.isReplaceObjectsNameCharacters())
			catalogs.replaceAll(fixer::sanitizeSchema);
		objectMapper.writeValue(file, catalogs);
		return catalogs;
	}

	/**
	 * Retrieves all schemas for a given catalog, optionally using a cached version.
	 * @param _catalog the sanitized catalog name
	 * @return a list of schema names
	 */
	@SneakyThrows
	public List<String> getSchemas(String _catalog) {
		String catalog = fixer.sanitizeSchema(_catalog);

		// noinspection ResultOfMethodCallIgnored
		Paths.get(app.getSchemaFolder(), catalog).toFile().mkdirs();
		File file = Paths.get(app.getSchemaFolder(), catalog, "schemas.json").toFile();
		if (!app.isIgnoreCache() && file.exists()) {
			return objectMapper.readValue(file, new TypeReference<>() {
			});
		}
		log.info("SHOW SCHEMAS FROM {}", _catalog);
		try {
			List<String> schemas = jdbcTemplate
				.queryForList("SHOW SCHEMAS FROM " + fixer.restoreSanitizedSchema(_catalog), String.class);
			if (app.isReplaceObjectsNameCharacters())
				schemas.replaceAll(fixer::sanitizeSchema);
			objectMapper.writeValue(file, schemas);
			return schemas;
		}
		catch (Exception e) {
			log.error("{} {}", _catalog, e.getMessage());
			objectMapper.writeValue(file, new ArrayList<>());
			return new ArrayList<>();
		}
	}

	/**
	 * Retrieves all tables for a given catalog and schema, optionally using a cached
	 * version.
	 * @param _catalog the sanitized catalog name
	 * @param _schema the sanitized schema name
	 * @return a list of table names
	 */
	@SneakyThrows
	public List<String> getTables(String _catalog, String _schema) {
		String catalog = fixer.sanitizeSchema(_catalog);
		String schema = fixer.sanitizeSchema(_schema);

		// noinspection ResultOfMethodCallIgnored
		Paths.get(app.getSchemaFolder(), catalog, schema).toFile().mkdirs();
		File file = Paths.get(app.getSchemaFolder(), catalog, schema, "tables.json").toFile();
		if (!app.isIgnoreCache() && file.exists()) {
			return objectMapper.readValue(file, new TypeReference<>() {
			});
		}
		log.info("SHOW TABLES FROM {}.{}", _catalog, _schema);
		try {
			List<String> tables = jdbcTemplate.queryForList("SHOW TABLES FROM " + fixer.restoreSanitizedSchema(_catalog)
					+ "." + fixer.restoreSanitizedSchema(_schema), String.class);
			if (app.isReplaceObjectsNameCharacters())
				tables.replaceAll(fixer::sanitizeSchema);
			objectMapper.writeValue(file, tables);
			return tables;
		}
		catch (Exception e) {
			log.error("{}.{} {}", _catalog, _schema, e.getMessage());
			objectMapper.writeValue(file, new ArrayList<>());
			return new ArrayList<>();
		}
	}

	/**
	 * Attempts to retrieve detected joins for the given catalog.
	 * @param _catalog the sanitized catalog name
	 * @return a list of join relationships as strings, or an empty list if detection
	 * fails or is unavailable
	 */
	@SneakyThrows
	public List<String> getJoins(String _catalog) {
		if (joinDetector == null)
			return new ArrayList<>();
		String catalog = fixer.sanitizeSchema(_catalog);

		// noinspection ResultOfMethodCallIgnored
		Paths.get(app.getSchemaFolder(), catalog).toFile().mkdirs();
		File file = Paths.get(app.getSchemaFolder(), catalog, "joins.json").toFile();
		// if (!app.isIgnoreCache() && file.exists()) {
		// return objectMapper.readValue(file, new TypeReference<>() {
		// });
		// }
		log.info("DETECT JOINS {}", _catalog);
		try {
			List<String> tables = joinDetector.detect(_catalog);
			if (app.isReplaceObjectsNameCharacters())
				tables.replaceAll(fixer::sanitizeSchema);
			objectMapper.writeValue(file, tables);
			return tables;
		}
		catch (Exception e) {
			log.error("{} {}", _catalog, e.getMessage());
			objectMapper.writeValue(file, new ArrayList<>());
			return new ArrayList<>();
		}
	}

	/**
	 * Retrieves column metadata for a specific table, optionally using a cached version.
	 * @param _catalog the sanitized catalog name
	 * @param _schema the sanitized schema name
	 * @param _table the sanitized table name
	 * @return a list of maps, each representing a column's metadata
	 */
	@SneakyThrows
	public List<Map<String, Object>> getColumns(String _catalog, String _schema, String _table) {
		String catalog = fixer.sanitizeSchema(_catalog);
		String schema = fixer.sanitizeSchema(_schema);
		String table = fixer.sanitizeSchema(_table);

		// noinspection ResultOfMethodCallIgnored
		Paths.get(app.getSchemaFolder(), catalog, schema, table).toFile().mkdirs();
		File file = Paths.get(app.getSchemaFolder(), catalog, schema, table, "columns.json").toFile();
		if (!app.isIgnoreCache() && file.exists()) {
			return objectMapper.readValue(file, new TypeReference<>() {
			});
		}
		log.info("DESCRIBE {}.{}.{}", _catalog, _schema, _table);
		try {
			List<Map<String, Object>> columns = jdbcTemplate
				.queryForList("DESCRIBE " + fixer.restoreSanitizedSchema(_catalog) + "."
						+ fixer.restoreSanitizedSchema(_schema) + "." + fixer.restoreSanitizedSchema(_table));
			if (app.isReplaceObjectsNameCharacters())
				for (Map<String, Object> column : columns) {
					// Check if the map contains the "Column" key
					if (column.containsKey("Column")) {
						// Replace the value of the "Column" key
						column.put("Column", fixer.sanitizeSchema((String) column.get("Column")));
					}
				}

			objectMapper.writeValue(file, columns);
			return columns;
		}
		catch (Exception e) {
			log.error("{}.{}.{} {}", _catalog, _schema, _table, e.getMessage());
			objectMapper.writeValue(file, new ArrayList<>());
			return new ArrayList<>();
		}
	}

}
