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

import org.iromu.trino.graphql.schema.GraphQLSchemaFixer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Rodriguez
 */
class TrinoQueryServiceTest {

	private JdbcTemplate jdbcTemplate;

	private GraphQLSchemaFixer fixer;

	private TrinoQueryService service;

	@BeforeEach
	void setUp() {
		jdbcTemplate = mock(JdbcTemplate.class);
		fixer = mock(GraphQLSchemaFixer.class);
		service = new TrinoQueryService(jdbcTemplate, fixer);
	}

	@Test
	void testQueryTableWithFilters() {
		// Given
		String catalog = "test_catalog";
		String schema = "test_schema";
		String table = "test_table";
		int limit = 10;

		List<Map<String, Object>> filters = List.of(Map.of("field", "name", "operator", "eq", "stringValue", "Alice"),
				Map.of("field", "age", "operator", "gt", "intValue", 25));

		when(fixer.restoreSanitizedSchema(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(fixer.sanitizeSchema(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		List<Map<String, Object>> fakeResults = List.of(new HashMap<>(Map.of("name", "Alice", "age", 30)));

		when(jdbcTemplate.queryForList(anyString())).thenReturn(fakeResults);

		// When
		List<Map<String, Object>> result = service.queryTableWithFilters(catalog, schema, table, limit, filters);

		// Then
		assertEquals(1, result.size());
		assertEquals("Alice", result.get(0).get("name"));

		ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).queryForList(sqlCaptor.capture());

		String generatedSql = sqlCaptor.getValue();
		assertTrue(generatedSql.contains("SELECT t1.* FROM test_catalog.test_schema.test_table t1"));
		assertTrue(generatedSql.contains("name = Alice"));
		assertTrue(generatedSql.contains("age > '25'"));
		assertTrue(generatedSql.contains("LIMIT 10"));
	}

	@Test
	void testExtractFilterValueThrowsOnInvalidFilter() {
		Map<String, Object> invalidFilter = Map.of("field", "age", "operator", "eq");

		var ex = assertThrows(IllegalArgumentException.class,
				() -> service.queryTableWithFilters("cat", "sch", "tbl", 1, List.of(invalidFilter)));

		assertTrue(ex.getMessage().contains("No valid value in filter"));
	}

}
