package se.kth.infosys.login.couchbase;

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

	public JsonElement serialize(AbstractRegisteredService src, 
			Type typeOfSrc, JsonSerializationContext context) {
		JsonObject result = new JsonObject();
		result.add("type", new JsonPrimitive(src.getClass().getName()));
		result.add("properties", context.serialize(src, src.getClass()));
		return result;
	}

	public AbstractRegisteredService deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		JsonObject jsonObject = json.getAsJsonObject();
		String type = jsonObject.get("type").getAsString();
		JsonElement element = jsonObject.get("properties");
		try {
			return context.deserialize(element, Class.forName(type));
		} catch (ClassNotFoundException e) {
			throw new JsonParseException("Unknown element type: " + type, e);
		}
	}
}
