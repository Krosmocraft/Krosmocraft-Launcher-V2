package fr.krosmocraft.launcher.core.game;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import fr.krosmocraft.launcher.core.game.VersionProfile.Argument;
import fr.krosmocraft.launcher.core.game.VersionProfile.Rule;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

final class ArgumentAdapter implements JsonDeserializer<Argument> {

	private static final Type RULES = new TypeToken<List<Rule>>() {}.getType();

	@Override
	public Argument deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) throws JsonParseException {
		if (json.isJsonPrimitive()) {
			return new Argument(List.of(json.getAsString()), List.of());
		}
		JsonObject obj = json.getAsJsonObject();
		List<String> values = new ArrayList<>();
		JsonElement value = obj.get("value");
		if (value.isJsonArray()) {
			value.getAsJsonArray().forEach(e -> values.add(e.getAsString()));
		} else {
			values.add(value.getAsString());
		}
		List<Rule> rules = obj.has("rules") ? ctx.deserialize(obj.get("rules"), RULES) : List.of();
		return new Argument(List.copyOf(values), rules);
	}
}
