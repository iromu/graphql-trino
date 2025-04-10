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

package org.iromu.trino.graphql.schema;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ivan Rodriguez
 */
class GraphQLSchemaFixerTest {

	private final GraphQLSchemaFixer fixer = new GraphQLSchemaFixer();

	@ParameterizedTest(name = "[{index}] sanitize: \"{0}\" → \"{1}\"")
	@CsvSource({
			// input, expected sanitized result
			"validName, validName", "1name, _U0031_name", "_valid123, _valid123", "name@field, name_U0040_field",
			"🚀Rocket, _U1F680_Rocket", "user-name, user_U002D_name", "email!, email_U0021_",
			"#hashtag, _U0023_hashtag", "ççç, _U00E7__U00E7__U00E7_" })
	void sanitizeSchema(String input, String expectedSanitized) {
		String actualSanitized = fixer.sanitizeSchema(input);
		assertEquals(expectedSanitized, actualSanitized);
	}

	@ParameterizedTest(name = "[{index}] restore: \"{0}\" → \"{1}\"")
	@CsvSource({
			// sanitized, expected restored result
			"validName, validName", "_U0031_name, 1name", "name_U0040_field, name@field", "_U1F680_Rocket, 🚀Rocket",
			"user_U002D_name, user-name", "email_U0021_, email!", "_U0023_hashtag, #hashtag",
			"_U00E7__U00E7__U00E7_, ççç" })
	void restoreSanitizedSchema(String sanitized, String expectedRestored) {
		String actualRestored = fixer.restoreSanitizedSchema(sanitized);
		assertEquals(expectedRestored, actualRestored);

	}

	@ParameterizedTest(name = "[{index}] round-trip: \"{0}\"")
	@CsvSource({ "validName", "_valid123", "1name, _U0031_name", "email!", "🚀Rocket", "ççç" })
	public void testRoundTrip(String input) {
		String sanitized = fixer.sanitizeSchema(input);
		String restored = fixer.restoreSanitizedSchema(sanitized);
		assertEquals(input, restored);
	}

}
