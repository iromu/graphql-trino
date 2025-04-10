package org.iromu.trino.graphql.data.relations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class DefaultJoinDetectorTest {

	private JdbcTemplate jdbcTemplate;

	private DefaultJoinDetector joinDetector;

	private ByteArrayOutputStream outContent;

	@BeforeEach
	void setUp() {
		jdbcTemplate = mock(JdbcTemplate.class);
		joinDetector = spy(new DefaultJoinDetector(jdbcTemplate));

		// Redirect console output
		outContent = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outContent));
	}

	@Test
	void detect_shouldPrintForeignKeyToPrimaryKeyJoin() {
		var fk = new JoinDetector.ColumnInfo("cat", "public", "orders", "user_id", "int");
		var pk = new JoinDetector.ColumnInfo("cat", "public", "users", "id", "int");

		doReturn(List.of(fk, pk)).when(joinDetector).getColumns("cat");

		joinDetector.detect("cat");

		String output = outContent.toString();
		assertTrue(output.contains("orders.user_id") && output.contains("users.id"),
				"Should detect and print join between orders.user_id → users.id");
	}

	@Test
	void detect_shouldPrintJoinOnSameNamedColumnsAcrossTables() {
		var col1 = new JoinDetector.ColumnInfo("cat", "public", "employees", "dept_id", "int");
		var col2 = new JoinDetector.ColumnInfo("cat", "public", "departments", "dept_id", "int");

		doReturn(List.of(col1, col2)).when(joinDetector).getColumns("cat");

		joinDetector.detect("cat");

		String output = outContent.toString();
		assertTrue(output.contains("employees.dept_id") && output.contains("departments.dept_id"),
				"Should detect and print symmetric join on dept_id");
	}

	@Test
	void detect_shouldIgnoreUnrelatedColumns() {
		var col1 = new JoinDetector.ColumnInfo("cat", "hr", "users", "manager", "varchar");
		var col2 = new JoinDetector.ColumnInfo("cat", "hr", "companies", "company_name", "varchar");

		doReturn(List.of(col1, col2)).when(joinDetector).getColumns("cat");

		joinDetector.detect("cat");

		String output = outContent.toString();
		assertFalse(output.contains("↔") || output.contains("→"), "Should not detect join between unrelated columns");
	}

	@Test
	void isJoinable_shouldHandleNormalizedMatching() {
		assertTrue(invokeIsJoinable("User_ID", "userId"));
		assertFalse(invokeIsJoinable("manager", "company_name"));
	}

	// Helper to invoke private isJoinable method using reflection
	private boolean invokeIsJoinable(String col1, String col2) {
		try {
			var method = DefaultJoinDetector.class.getDeclaredMethod("isJoinable", String.class, String.class);
			method.setAccessible(true);
			return (boolean) method.invoke(joinDetector, col1, col2);
		}
		catch (Exception e) {
			throw new RuntimeException("Reflection failed", e);
		}
	}

}
