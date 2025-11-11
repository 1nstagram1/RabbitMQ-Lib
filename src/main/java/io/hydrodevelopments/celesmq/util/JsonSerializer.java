package io.hydrodevelopments.celesmq.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;

/**
 * Utility class for JSON serialization and deserialization
 */
public class JsonSerializer {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private static final Gson COMPACT_GSON = new Gson();

  /**
   * Serializes an object to JSON string
   *
   * @param object the object to serialize
   *
   * @return JSON string representation
   */
  public static String toJson(Object object) {
    return COMPACT_GSON.toJson(object);
  }

  /**
   * Serializes an object to pretty-printed JSON string
   *
   * @param object the object to serialize
   *
   * @return pretty-printed JSON string
   */
  public static String toPrettyJson(Object object) {
    return GSON.toJson(object);
  }

  /**
   * Deserializes JSON string to an object
   *
   * @param json     JSON string
   * @param classOfT class type to deserialize to
   * @param <T>      type parameter
   *
   * @return deserialized object or null if parsing fails
   */
  public static <T> T fromJson(String json, Class<T> classOfT) {
    try {
      return GSON.fromJson(json, classOfT);
    } catch (JsonSyntaxException e) {
      return null;
    }
  }

  /**
   * Deserializes JSON string to an object with Type
   *
   * @param json    JSON string
   * @param typeOfT type to deserialize to
   * @param <T>     type parameter
   *
   * @return deserialized object or null if parsing fails
   */
  public static <T> T fromJson(String json, Type typeOfT) {
    try {
      return GSON.fromJson(json, typeOfT);
    } catch (JsonSyntaxException e) {
      return null;
    }
  }

  /**
   * Gets the Gson instance
   *
   * @return Gson instance
   */
  public static Gson getGson() {
    return GSON;
  }

  /**
   * Gets the compact Gson instance (no pretty printing)
   *
   * @return compact Gson instance
   */
  public static Gson getCompactGson() {
    return COMPACT_GSON;
  }
}
