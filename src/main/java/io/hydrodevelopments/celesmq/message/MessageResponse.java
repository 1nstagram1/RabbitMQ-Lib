package io.hydrodevelopments.celesmq.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hydrodevelopments.celesmq.util.JsonSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Type-safe wrapper for JSON message responses with convenient getter methods Inspired by ResultSet pattern for easy
 * data extraction
 */
public class MessageResponse {
  private final JsonObject data;
  private final String rawMessage;
  private final ResponseStatus status;

  public MessageResponse(String json) {
    this(json, ResponseStatus.SUCCESS);
  }

  public MessageResponse(String json, ResponseStatus status) {
    this.rawMessage = json;
    this.status = status;
    try {
      JsonElement element = JsonParser.parseString(json);
      this.data = element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON: " + json, e);
    }
  }

  /**
   * Gets an integer value safely with default of 0
   */
  public int getInt(String key) {
    return getInt(key, 0);
  }

  /**
   * Gets an integer value with custom default
   */
  public int getInt(String key, int defaultValue) {
    try {
      if (!data.has(key)) {
        return defaultValue;
      }
      JsonElement element = data.get(key);
      if (element.isJsonNull()) {
        return defaultValue;
      }
      return element.getAsInt();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * Gets a long value safely with default of 0
   */
  public long getLong(String key) {
    return getLong(key, 0L);
  }

  /**
   * Gets a long value with custom default
   */
  public long getLong(String key, long defaultValue) {
    try {
      if (!data.has(key)) {
        return defaultValue;
      }
      JsonElement element = data.get(key);
      if (element.isJsonNull()) {
        return defaultValue;
      }
      return element.getAsLong();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * Gets a double value safely with default of 0.0
   */
  public double getDouble(String key) {
    return getDouble(key, 0.0);
  }

  /**
   * Gets a double value with custom default
   */
  public double getDouble(String key, double defaultValue) {
    try {
      if (!data.has(key)) {
        return defaultValue;
      }
      JsonElement element = data.get(key);
      if (element.isJsonNull()) {
        return defaultValue;
      }
      return element.getAsDouble();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * Gets a boolean value safely with default of false
   */
  public boolean getBoolean(String key) {
    return getBoolean(key, false);
  }

  /**
   * Gets a boolean value with custom default
   */
  public boolean getBoolean(String key, boolean defaultValue) {
    try {
      if (!data.has(key)) {
        return defaultValue;
      }
      JsonElement element = data.get(key);
      if (element.isJsonNull()) {
        return defaultValue;
      }
      return element.getAsBoolean();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * Gets a string value safely, returns null if not found
   */
  public String getString(String key) {
    return getString(key, null);
  }

  /**
   * Gets a string value with custom default
   */
  public String getString(String key, String defaultValue) {
    try {
      if (!data.has(key)) {
        return defaultValue;
      }
      JsonElement element = data.get(key);
      if (element.isJsonNull()) {
        return defaultValue;
      }
      return element.getAsString();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * Gets a JSON array
   */
  public JsonArray getArray(String key) {
    try {
      if (!data.has(key)) {
        return null;
      }
      JsonElement element = data.get(key);
      if (element.isJsonNull()) {
        return null;
      }
      return element.getAsJsonArray();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Gets a JSON object
   */
  public JsonObject getObject(String key) {
    try {
      if (!data.has(key)) {
        return null;
      }
      JsonElement element = data.get(key);
      if (element.isJsonNull()) {
        return null;
      }
      return element.getAsJsonObject();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Gets a list of MessageResponse objects from a JSON array
   */
  public List<MessageResponse> getResponseList(String key) {
    List<MessageResponse> responses = new ArrayList<>();
    try {
      JsonArray array = getArray(key);
      if (array != null) {
        for (JsonElement element : array) {
          responses.add(new MessageResponse(element.toString(), status));
        }
      }
    } catch (Exception e) {
      // Return empty list on error
    }
    return responses;
  }

  /**
   * Checks if a key exists in the response
   */
  public boolean has(String key) {
    return data.has(key) && !data.get(key).isJsonNull();
  }

  /**
   * Checks if a key exists and is not null (alias for has)
   */
  public boolean containsKey(String key) {
    return has(key);
  }

  /**
   * Deserializes the data to a specific type
   */
  public <T> T as(Class<T> type) {
    return JsonSerializer.fromJson(rawMessage, type);
  }

  /**
   * Gets the underlying JsonObject
   */
  public JsonObject getData() {
    return data;
  }

  /**
   * Gets the raw JSON string
   */
  public String getRawMessage() {
    return rawMessage;
  }

  /**
   * Gets the response status
   */
  public ResponseStatus getStatus() {
    return status;
  }

  /**
   * Checks if the response is successful
   */
  public boolean isSuccess() {
    return status.equals(ResponseStatus.SUCCESS);
  }

  /**
   * Checks if the response is an error
   */
  public boolean isError() {
    return status.equals(ResponseStatus.ERROR);
  }

}
