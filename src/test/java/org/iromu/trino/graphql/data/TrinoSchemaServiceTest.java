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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.iromu.trino.graphql.AppProperties;
import org.iromu.trino.graphql.schema.GraphQLSchemaFixer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Rodriguez
 */
class TrinoSchemaServiceTest {

	private JdbcTemplate jdbcTemplate;

	private ObjectMapper objectMapper;

	private AppProperties appProperties;

	private GraphQLSchemaFixer fixer;

	private TrinoSchemaService service;

	@BeforeEach
	void setUp() {
		jdbcTemplate = mock(JdbcTemplate.class);
		objectMapper = mock(ObjectMapper.class);
		appProperties = mock(AppProperties.class);
		fixer = mock(GraphQLSchemaFixer.class);

		when(appProperties.getSchemaFolder()).thenReturn("target/test-schema-cache");
		when(appProperties.isIgnoreCache()).thenReturn(true);
		when(appProperties.isReplaceObjectsNameCharacters()).thenReturn(false);

		service = new TrinoSchemaService(jdbcTemplate, objectMapper, appProperties, fixer, null);
	}

	@Test
	void testGetCatalogs() throws Exception {
		List<String> expectedCatalogs = List.of("catalog1", "catalog2");
		when(jdbcTemplate.queryForList("SHOW CATALOGS", String.class)).thenReturn(expectedCatalogs);

		List<String> result = service.getCatalogs();

		assertEquals(expectedCatalogs, result);
		verify(objectMapper).writeValue(any(File.class), eq(expectedCatalogs));
	}

	@Test
	void testGetSchemas() throws Exception {
		when(fixer.sanitizeSchema("myCatalog")).thenReturn("myCatalog");
		when(fixer.restoreSanitizedSchema("myCatalog")).thenReturn("myCatalog");

		List<String> schemas = List.of("public", "internal");
		when(jdbcTemplate.queryForList("SHOW SCHEMAS FROM myCatalog", String.class)).thenReturn(schemas);

		List<String> result = service.getSchemas("myCatalog");

		assertEquals(schemas, result);
		verify(objectMapper).writeValue(any(File.class), eq(schemas));
	}

	@Test
	void testGetTables() throws Exception {
		when(fixer.sanitizeSchema("catalog")).thenReturn("catalog");
		when(fixer.sanitizeSchema("schema")).thenReturn("schema");
		when(fixer.restoreSanitizedSchema("catalog")).thenReturn("catalog");
		when(fixer.restoreSanitizedSchema("schema")).thenReturn("schema");

		List<String> tables = List.of("users", "orders");
		when(jdbcTemplate.queryForList("SHOW TABLES FROM catalog.schema", String.class)).thenReturn(tables);

		List<String> result = service.getTables("catalog", "schema");

		assertEquals(tables, result);
		verify(objectMapper).writeValue(any(File.class), eq(tables));
	}

	@Test
	void testGetColumns() throws Exception {
		when(fixer.sanitizeSchema("catalog")).thenReturn("catalog");
		when(fixer.sanitizeSchema("schema")).thenReturn("schema");
		when(fixer.sanitizeSchema("table")).thenReturn("table");

		when(fixer.restoreSanitizedSchema("catalog")).thenReturn("catalog");
		when(fixer.restoreSanitizedSchema("schema")).thenReturn("schema");
		when(fixer.restoreSanitizedSchema("table")).thenReturn("table");

		Map<String, Object> column = new HashMap<>();
		column.put("Column", "id");
		column.put("Type", "varchar");

		List<Map<String, Object>> columns = List.of(column);

		when(jdbcTemplate.queryForList("DESCRIBE catalog.schema.table")).thenReturn(columns);

		List<Map<String, Object>> result = service.getColumns("catalog", "schema", "table");

		assertEquals(columns, result);
		verify(objectMapper).writeValue(any(File.class), eq(columns));
	}

	@Test
	void testGetSchemas_ExceptionShouldReturnEmptyList() throws Exception {
		when(fixer.sanitizeSchema("invalid")).thenReturn("invalid");
		when(fixer.restoreSanitizedSchema("invalid")).thenReturn("invalid");
		when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenThrow(new RuntimeException("fail"));

		List<String> result = service.getSchemas("invalid");

		assertTrue(result.isEmpty());
		verify(objectMapper).writeValue(any(File.class), eq(List.of()));
	}

}
