package se.kth.infosys.login.couchbase;

/*
 * Copyright (C) 2013 KTH, Kungliga tekniska hogskolan, http://www.kth.se
 *
 * This file is part of cas-server-integration-couchbase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.reflect.Type;

import org.jasig.cas.services.AbstractRegisteredService;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


/**
 * Helper class for serializing derivatives of the abstract class
 * AbstractRegisteredService in a way that allows us to re-instantiate
 * the the object using the proper class. It will store the object "type"
 * of the class in the JSON object together with a "properties" field
 * containing the values of the object properties.
 */
public class AbstractRegisteredServiceJsonSerializer
implements JsonSerializer<AbstractRegisteredService>, JsonDeserializer<AbstractRegisteredService> {

    /**
     * @param src CAS Service.
     * @param typeOfSrc (ignored).
     * @param context Gson serialization context.
     * @see JsonSerializer#serialize(Object, Type, JsonSerializationContext)
     * @return JSON serialized version of a CAS service.
     */
    public final JsonElement serialize(
            final AbstractRegisteredService src, final Type typeOfSrc, final JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.add("type", new JsonPrimitive(src.getClass().getName()));
        result.add("properties", context.serialize(src, src.getClass()));
        return result;
    }

    /**
     * @see JsonDeserializer#deserialize(JsonElement, Type, JsonDeserializationContext)
     * @param json JSON encoded representation of CAS Service.
     * @param typeOfT (ignored).
     * @param context Gson de-serialization context.
     * @return de-serialized version of a CAS service.
     */
    public final AbstractRegisteredService deserialize(
            final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();
        JsonElement element = jsonObject.get("properties");
        try {
            return context.deserialize(element, Class.forName(type));
        } catch (final ClassNotFoundException e) {
            throw new JsonParseException("Unknown element type: " + type, e);
        }
    }
}
