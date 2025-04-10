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

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility component for sanitizing and restoring GraphQL schema names.
 *
 * <p>
 * This class provides methods to sanitize schema names by replacing invalid characters
 * with a Unicode escape sequence and restoring sanitized schema names back to their
 * original form.
 * </p>
 *
 * <p>
 * The sanitation process ensures that schema names conform to a valid naming convention
 * by replacing characters that would otherwise be invalid (such as special characters)
 * with encoded Unicode representations.
 * </p>
 *
 * @author Ivan Rodriguez
 */
@Component
public class GraphQLSchemaFixer {

	/**
	 * Pattern to match encoded Unicode characters in the form _UXXXX_
	 */
	private static final Pattern ENCODED_CHAR_PATTERN = Pattern.compile("_U([0-9A-Fa-f]{4,6})_");

	/**
	 * Pattern to validate characters allowed in GraphQL schema names
	 */
	public static final Pattern VALID_CHAR_PATTERN = Pattern.compile("^[_A-Za-z][_0-9A-Za-z]*$");

	/**
	 * Sanitizes the input string by replacing invalid characters with their Unicode
	 * escape sequences.
	 *
	 * <p>
	 * This method ensures that schema names start with a valid character (letter or
	 * underscore) and replaces any invalid character with a Unicode escape sequence of
	 * the form _UXXXX_ where XXXX is the Unicode code point of the character.
	 * </p>
	 * @param input the schema name to sanitize
	 * @return a sanitized version of the schema name, where invalid characters are
	 * replaced by Unicode escapes
	 */
	public String sanitizeSchema(String input) {
		if (input == null || input.isEmpty())
			return input;

		StringBuilder sanitized = new StringBuilder();

		int[] codePoints = input.codePoints().toArray();
		for (int i = 0; i < codePoints.length; i++) {
			int cp = codePoints[i];

			// Special handling for the first character
			if (i == 0 && !((cp >= 'A' && cp <= 'Z') || (cp >= 'a' && cp <= 'z') || cp == '_')) {
				sanitized.append("_U").append(String.format("%04X", cp)).append("_");
			}
			else if ((cp >= 'A' && cp <= 'Z') || (cp >= 'a' && cp <= 'z') || (cp >= '0' && cp <= '9') || cp == '_') {
				sanitized.appendCodePoint(cp);
			}
			else {
				sanitized.append("_U").append(String.format("%04X", cp)).append("_");
			}
		}

		return sanitized.toString();
	}

	/**
	 * Restores a sanitized schema name by decoding any Unicode escape sequences back to
	 * the original characters.
	 *
	 * <p>
	 * This method reverses the sanitation process, replacing any occurrences of the
	 * Unicode escape sequences (e.g., _UXXXX_) with their corresponding characters.
	 * </p>
	 * @param input the sanitized schema name to restore
	 * @return the restored schema name with Unicode escape sequences replaced by original
	 * characters
	 */
	public String restoreSanitizedSchema(String input) {
		Matcher matcher = ENCODED_CHAR_PATTERN.matcher(input);
		StringBuilder result = new StringBuilder();

		while (matcher.find()) {
			String hex = matcher.group(1);
			int codePoint = Integer.parseInt(hex, 16);
			String replacement = new String(Character.toChars(codePoint));
			matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
		}

		matcher.appendTail(result);
		return result.toString();
	}

}
