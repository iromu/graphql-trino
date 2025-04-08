package org.iromu.trino.graphql;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GraphQLSchemaFixer {
    private static final Pattern ENCODED_CHAR_PATTERN = Pattern.compile("_U([0-9A-Fa-f]{4,6})_");
    private static final Pattern VALID_CHAR_PATTERN = Pattern.compile("^[_A-Za-z][_0-9A-Za-z]*$");

    public String sanitizeSchema(String input) {
        if (VALID_CHAR_PATTERN.matcher(input).matches()) return input;

        StringBuilder sanitized = new StringBuilder();
        input.codePoints().forEach(cp -> {
            if ((cp >= 'A' && cp <= 'Z') ||
                    (cp >= 'a' && cp <= 'z') ||
                    (cp >= '0' && cp <= '9') ||
                    cp == '_') {
                sanitized.appendCodePoint(cp);
            } else {
                sanitized.append("_U").append(String.format("%04X", cp)).append("_");
            }
        });
        return sanitized.toString();
    }

    public String restoreSanitizedSchema(String input) {
        Matcher matcher = ENCODED_CHAR_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();

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
