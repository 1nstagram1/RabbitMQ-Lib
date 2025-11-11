package io.hydrodevelopments.celesmq.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.hydrodevelopments.celesmq.util.JsonSerializer;

import java.util.List;
import java.util.Map;

/**
 * Fluent builder for constructing JSON messages easily
 */
public class MessageBuilder {

  private final JsonObject data;

  public MessageBuilder() {
    this.data = new JsonObject();
  }

  /**
   * Sets the action for this message
   */
  public MessageBuilder action(String action) {
    data.addProperty("action", action);
    return this;
  }

  /**
   * Adds a string property
   */
  public MessageBuilder add(String key, String value) {
    data.addProperty(key, value);
    return this;
  }

  /**
   * Adds an integer property
   */
  public MessageBuilder add(String key, int value) {
    data.addProperty(key, value);
    return this;
  }

  /**
   * Adds a long property
   */
  public MessageBuilder add(String key, long value) {
    data.addProperty(key, value);
    return this;
  }

  /**
   * Adds a boolean property
   */
  public MessageBuilder add(String key, boolean value) {
    data.addProperty(key, value);
    return this;
  }

  /**
   * Adds a double property
   */
  public MessageBuilder add(String key, double value) {
    data.addProperty(key, value);
    return this;
  }

  /**
   * Adds a list as JSON array
   */
  public MessageBuilder add(String key, List<?> list) {
    JsonArray array = new JsonArray();
    for (Object item : list) {
      if (item instanceof String) {
        array.add((String) item);
      } else if (item instanceof Number) {
        array.add((Number) item);
      } else if (item instanceof Boolean) {
        array.add((Boolean) item);
      } else {
        array.add(JsonSerializer.toJson(item));
      }
    }
    data.add(key, array);
    return this;
  }

  /**
   * Adds a map as JSON object
   */
  public MessageBuilder add(String key, Map<String, ?> map) {
    JsonObject obj = new JsonObject();
    map.forEach((k, v) -> {
      if (v instanceof String) {
        obj.addProperty(k, (String) v);
      } else if (v instanceof Number) {
        obj.addProperty(k, (Number) v);
      } else if (v instanceof Boolean) {
        obj.addProperty(k, (Boolean) v);
      } else {
        String json = JsonSerializer.toJson(v);
        obj.add(k, JsonSerializer.fromJson(json, JsonObject.class));
      }
    });
    data.add(key, obj);
    return this;
  }

  /**
   * Adds any object property (will be serialized to JSON)
   */
  public MessageBuilder add(String key, Object value) {
    if (value instanceof String) {
      data.addProperty(key, (String) value);
    } else if (value instanceof Number) {
      data.addProperty(key, (Number) value);
    } else if (value instanceof Boolean) {
      data.addProperty(key, (Boolean) value);
    } else if (value instanceof List) {
      add(key, (List<?>) value);
    } else if (value instanceof Map) {
      add(key, (Map<String, ?>) value);
    } else {
      String json = JsonSerializer.toJson(value);
      data.add(key, JsonSerializer.fromJson(json, JsonObject.class));
    }
    return this;
  }

  /**
   * Adds multiple properties from a map
   */
  public MessageBuilder addAll(Map<String, Object> properties) {
    properties.forEach(this::add);
    return this;
  }

  /**
   * Removes a property
   */
  public MessageBuilder remove(String key) {
    data.remove(key);
    return this;
  }

  /**
   * Checks if a property exists
   */
  public boolean has(String key) {
    return data.has(key);
  }

  /**
   * Gets the underlying JsonObject
   */
  public JsonObject getData() {
    return data;
  }

  /**
   * Builds and returns the JSON string
   */
  public String build() {
    return data.toString();
  }

  /**
   * Builds and returns a pretty-printed JSON string
   */
  public String buildPretty() {
    return JsonSerializer.toPrettyJson(data);
  }

  /**
   * Creates a new MessageBuilder
   */
  public static MessageBuilder create() {
    return new MessageBuilder();
  }

  /**
   * Creates a new MessageBuilder with an action
   */
  public static MessageBuilder create(String action) {
    return new MessageBuilder().action(action);
  }
}
