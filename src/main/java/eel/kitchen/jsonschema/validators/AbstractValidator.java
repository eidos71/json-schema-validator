/*
 * Copyright (c) 2011, Francis Galiegue <fgaliegue@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eel.kitchen.jsonschema.validators;

import eel.kitchen.util.CollectionUtils;
import eel.kitchen.util.NodeType;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractValidator
    implements Validator
{
    protected static final JsonNode EMPTY_SCHEMA;

    protected JsonNode schema;
    protected final Map<String, EnumSet<NodeType>> fieldMap
        = new HashMap<String, EnumSet<NodeType>>();
    protected final List<String> messages = new LinkedList<String>();
    protected boolean setupDone = false, validSchema = false;

    static {
        try {
            EMPTY_SCHEMA = new ObjectMapper().readTree("{}");
        } catch (IOException e) {
            throw  new ExceptionInInitializerError();
        }
    }

    @Override
    public final Validator setSchema(final JsonNode schema)
    {
        messages.clear();
        setupDone = false;
        validSchema = false;
        this.schema = schema;
        return this;
    }

    @Override
    public final boolean setup()
    {
        if (!setupDone) {
            validSchema = doSetup();
            setupDone = true;
        }

        return validSchema;
    }

    protected boolean doSetup()
    {
        return isWellFormed();
    }

    @Override
    public boolean validate(final JsonNode node)
    {
        return false;
    }

    @Override
    public SchemaProvider getSchemaProvider()
    {
        return new EmptySchemaProvider();
    }

    @Override
    public final List<String> getMessages()
    {
        return Collections.unmodifiableList(messages);
    }

    protected final void registerField(final String name, final NodeType type)
    {
        if (fieldMap.containsKey(name)) {
            fieldMap.get(name).add(type);
            return;
        }

        fieldMap.put(name, EnumSet.of(type));
    }

    private boolean isWellFormed()
    {
        boolean ret = true;
        EnumSet<NodeType> expected;
        NodeType actual;

        final Set<String> fieldnames = fieldMap.keySet();
        fieldnames.retainAll(CollectionUtils.toSet(schema.getFieldNames()));

        for (final String field: fieldnames) {
            expected = fieldMap.get(field);
            actual = NodeType.getNodeType(schema.get(field));
            if (!expected.contains(actual)) {
                ret = false;
                messages.add(String.format(
                    "%s is of type %s, " + "expected %s", field, actual,
                    expected));
            }
        }

        return ret;
    }
}
