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

package org.iromu.trino.graphql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TrinoSchemaService {

	private final JdbcTemplate jdbcTemplate;

	private final ObjectMapper objectMapper;

	private final AppProperties appProperties;

	private final GraphQLSchemaFixer fixer;

	public TrinoSchemaService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, AppProperties appProperties,
							  GraphQLSchemaFixer fixer) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
		this.appProperties = appProperties;
		this.fixer = fixer;
	}

	// Get all catalogs
	@SneakyThrows
	public List<String> getCatalogs() {
		Paths.get(appProperties.getSchemaFolder()).toFile().mkdirs();
		File file = Paths.get(appProperties.getSchemaFolder(), "catalogs.json").toFile();
		if (file.exists()) {
			return objectMapper.readValue(file, new TypeReference<>() {
			});
		}
		log.info("SHOW CATALOGS");
		List<String> catalogs = jdbcTemplate.queryForList("SHOW CATALOGS", String.class);
		catalogs.replaceAll(s -> fixer.sanitizeSchema(s));
		objectMapper.writeValue(file, catalogs);
		return catalogs;
	}

	// Get all schemas in a specific catalog
	@SneakyThrows
	public List<String> getSchemas(String _catalog) {
		String catalog = fixer.sanitizeSchema(_catalog);

		Paths.get(appProperties.getSchemaFolder(), catalog).toFile().mkdirs();
		File file = Paths.get(appProperties.getSchemaFolder(), catalog, "schemas.json").toFile();
		if (file.exists()) {
			return objectMapper.readValue(file, new TypeReference<>() {
			});
		}
		log.info("SHOW SCHEMAS FROM {}", _catalog);
		try {
			List<String> schemas = jdbcTemplate
				.queryForList("SHOW SCHEMAS FROM " + fixer.restoreSanitizedSchema(_catalog), String.class);
			schemas.replaceAll(s -> fixer.sanitizeSchema(s));
			objectMapper.writeValue(file, schemas);
			return schemas;
		} catch (Exception e) {
			log.error("{} {}", _catalog, e.getMessage());
			objectMapper.writeValue(file, new ArrayList<>());
			return new ArrayList<>();
		}
	}

	// Get all tables in a specific catalog and schema
	@SneakyThrows
	public List<String> getTables(String _catalog, String _schema) {
		String catalog = fixer.sanitizeSchema(_catalog);
		String schema = fixer.sanitizeSchema(_schema);

		Paths.get(appProperties.getSchemaFolder(), catalog, schema).toFile().mkdirs();
		File file = Paths.get(appProperties.getSchemaFolder(), catalog, schema, "tables.json").toFile();
		if (file.exists()) {
			return objectMapper.readValue(file, new TypeReference<>() {
			});
		}
		log.info("SHOW TABLES FROM {}.{}", _catalog, _schema);
		try {
			List<String> tables = jdbcTemplate.queryForList("SHOW TABLES FROM " + fixer.restoreSanitizedSchema(_catalog)
					+ "." + fixer.restoreSanitizedSchema(_schema), String.class);
			tables.replaceAll(s -> fixer.sanitizeSchema(s));
			objectMapper.writeValue(file, tables);
			return tables;
		} catch (Exception e) {
			log.error("{}.{} {}", _catalog, _schema, e.getMessage());
			objectMapper.writeValue(file, new ArrayList<>());
			return new ArrayList<>();
		}
	}

	// Get columns for a table
	@SneakyThrows
	public List<Map<String, Object>> getColumns(String _catalog, String _schema, String _table) {
		String catalog = fixer.sanitizeSchema(_catalog);
		String schema = fixer.sanitizeSchema(_schema);
		String table = fixer.sanitizeSchema(_table);

		Paths.get(appProperties.getSchemaFolder(), catalog, schema, table).toFile().mkdirs();
		File file = Paths.get(appProperties.getSchemaFolder(), catalog, schema, table, "columns.json").toFile();
		if (file.exists()) {
			return objectMapper.readValue(file, new TypeReference<>() {
			});
		}
		log.info("DESCRIBE {}.{}.{}", _catalog, _schema, _table);
		try {
			List<Map<String, Object>> columns = jdbcTemplate
				.queryForList("DESCRIBE " + fixer.restoreSanitizedSchema(_catalog) + "."
					+ fixer.restoreSanitizedSchema(_schema) + "." + fixer.restoreSanitizedSchema(_table));
			for (Map<String, Object> column : columns) {
				// Check if the map contains the "Column" key
				if (column.containsKey("Column")) {
					// Replace the value of the "Column" key
					column.put("Column", fixer.sanitizeSchema((String) column.get("Column")));
				}
			}

			objectMapper.writeValue(file, columns);
			return columns;
		} catch (Exception e) {
			log.error("{}.{}.{} {}", _catalog, _schema, _table, e.getMessage());
			objectMapper.writeValue(file, new ArrayList<>());
			return new ArrayList<>();
		}
	}

}
