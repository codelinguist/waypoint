package com.waypoint.planning.futurevalue.web.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;

/**
 * Jackson's default behavior for a JSON number deserialized into an integer
 * field truncates toward zero (3.5 becomes 3) and narrows an out-of-range
 * integral value instead of failing. That would silently narrow an invalid
 * {@code projectionMonths} input, so this deserializer rejects any JSON value
 * that is not a whole number fitting in a 32-bit integer instead.
 */
final class WholeNumberDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (!node.isIntegralNumber()) {
            throw InvalidFormatException.from(parser, "must be a whole number", node.asText(), Integer.class);
        }
        if (!node.canConvertToInt()) {
            throw InvalidFormatException.from(parser, "must fit in a 32-bit integer", node.asText(), Integer.class);
        }
        return node.intValue();
    }
}
