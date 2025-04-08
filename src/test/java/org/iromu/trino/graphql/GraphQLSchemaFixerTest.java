package org.iromu.trino.graphql;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GraphQLSchemaFixerTest {
    private final GraphQLSchemaFixer fixer = new GraphQLSchemaFixer();

    @ParameterizedTest(name = "[{index}] sanitize: \"{0}\" → \"{1}\"")
    @CsvSource({
            // input, expected sanitized result
            "validName, validName",
            "_valid123, _valid123",
            "name@field, name_U0040_field",
            "🚀Rocket, _U1F680_Rocket",
            "user-name, user_U002D_name",
            "email!, email_U0021_",
            "#hashtag, _U0023_hashtag",
            "ççç, _U00E7__U00E7__U00E7_"
    })
    void sanitizeSchema(String input, String expectedSanitized) {
        String actualSanitized = fixer.sanitizeSchema(input);
        assertEquals(expectedSanitized, actualSanitized);
    }

    @ParameterizedTest(name = "[{index}] restore: \"{0}\" → \"{1}\"")
    @CsvSource({
            // sanitized, expected restored result
            "validName, validName",
            "name_U0040_field, name@field",
            "_U1F680_Rocket, 🚀Rocket",
            "user_U002D_name, user-name",
            "email_U0021_, email!",
            "_U0023_hashtag, #hashtag",
            "_U00E7__U00E7__U00E7_, ççç"
    })
    void restoreSanitizedSchema(String sanitized, String expectedRestored) {
        String actualRestored = fixer.restoreSanitizedSchema(sanitized);
        assertEquals(expectedRestored, actualRestored);

    }

    @ParameterizedTest(name = "[{index}] round-trip: \"{0}\"")
    @CsvSource({
            "validName",
            "_valid123",
            "email!",
            "🚀Rocket",
            "ççç"
    })
    public void testRoundTrip(String input) {
        String sanitized = fixer.sanitizeSchema(input);
        String restored = fixer.restoreSanitizedSchema(sanitized);
        assertEquals(input, restored);
    }
}
