/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.watcher.input;

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class InputRegistry {

    private final ImmutableMap<String, InputFactory> factories;

    @Inject
    public InputRegistry(Map<String, InputFactory> factories) {
        this.factories = ImmutableMap.copyOf(factories);
    }

    /**
     * Reads the contents of parser to create the correct Input
     *
     * @param parser    The parser containing the input definition
     * @return          A new input instance from the parser
     * @throws java.io.IOException
     */
    public ExecutableInput parse(String watchId, XContentParser parser) throws IOException {
        String type = null;

        if (parser.currentToken() != XContentParser.Token.START_OBJECT) {
            throw new InputException("could not parse input for watch [{}]. expected an object representing the input, but found [{}] instead", watchId, parser.currentToken());
        }

        XContentParser.Token token;
        ExecutableInput input = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                type = parser.currentName();
            } else if (type == null) {
                throw new InputException("could not parse input for watch [{}]. expected field indicating the input type, but found [{}] instead", watchId, token);
            } else if (token == XContentParser.Token.START_OBJECT) {
                InputFactory factory = factories.get(type);
                if (factory == null) {
                    throw new InputException("could not parse input for watch [{}]. unknown input type [{}]", watchId, type);
                }
                input = factory.parseExecutable(watchId, parser);
            } else {
                throw new InputException("could not parse input for watch [{}]. expected an object representing input [{}], but found [{}] instead", watchId, type, token);
            }
        }

        if (input == null) {
            throw new InputException("could not parse input for watch [{}]. expected field indicating the input type, but found an empty object instead", watchId, token);
        }

        return input;
    }

}
