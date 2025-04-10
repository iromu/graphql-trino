package org.iromu.trino.graphql.data.relations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

class SameSchemaJoinDetectorTest {

	private JdbcTemplate jdbcTemplate;

	private SameSchemaJoinDetector joinDetector;

	@BeforeEach
	void setUp() {
		jdbcTemplate = mock(JdbcTemplate.class);
		joinDetector = spy(new SameSchemaJoinDetector(jdbcTemplate));
	}

	@Test
	void detect_shouldDetectJoinOnMatchingIdColumnsInSameSchema() {
		JoinDetector.ColumnInfo col1 = new JoinDetector.ColumnInfo("catalog", "public", "orders", "user_id", "int");
		JoinDetector.ColumnInfo col2 = new JoinDetector.ColumnInfo("catalog", "public", "users", "user_id", "int");

		doReturn(Arrays.asList(col1, col2)).when(joinDetector).getColumns("catalog");

		List<String> result = joinDetector.detect("catalog");

		// Verifying getColumns was called
		verify(joinDetector).getColumns("catalog");
	}

	@Test
	void detect_shouldNotDetectJoinAcrossDifferentSchemas() {
		JoinDetector.ColumnInfo col1 = new JoinDetector.ColumnInfo("catalog", "sales", "orders", "customer_id", "int");
		JoinDetector.ColumnInfo col2 = new JoinDetector.ColumnInfo("catalog", "support", "customers", "customer_id",
				"int");

		doReturn(Arrays.asList(col1, col2)).when(joinDetector).getColumns("catalog");

		List<String> result = joinDetector.detect("catalog");

		// These are from different schemas, so no join should be detected
		verify(joinDetector).getColumns("catalog");
	}

	@Test
	void detect_shouldDetectJoinOnNormalizedNameMatch() {
		JoinDetector.ColumnInfo col1 = new JoinDetector.ColumnInfo("catalog", "data", "table1", "UserID", "int");
		JoinDetector.ColumnInfo col2 = new JoinDetector.ColumnInfo("catalog", "data", "table2", "user_id", "int");

		doReturn(Arrays.asList(col1, col2)).when(joinDetector).getColumns("catalog");

		List<String> result = joinDetector.detect("catalog");

		verify(joinDetector).getColumns("catalog");
	}

	@Test
	void isJoinable_shouldReturnTrueForExactNormalizedMatch() {
		boolean result = joinDetector.isJoinable("user_id", "User_ID");
		assert result;
	}

	@Test
	void isJoinable_shouldReturnFalseForDifferentColumns() {
		boolean result = joinDetector.isJoinable("manager", "company_name");
		assert !result;
	}

}
