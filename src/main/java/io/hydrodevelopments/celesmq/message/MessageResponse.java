package io.hydrodevelopments.celesmq.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hydrodevelopments.celesmq.util.JsonSerializer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Type-safe wrapper for JSON message responses with convenient getter methods
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
            if (!data.has(key)) return defaultValue;
            JsonElement element = data.get(key);
            if (element.isJsonNull()) return defaultValue;
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
            if (!data.has(key)) return defaultValue;
            JsonElement element = data.get(key);
            if (element.isJsonNull()) return defaultValue;
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
            if (!data.has(key)) return defaultValue;
            JsonElement element = data.get(key);
            if (element.isJsonNull()) return defaultValue;
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
            if (!data.has(key)) return defaultValue;
            JsonElement element = data.get(key);
            if (element.isJsonNull()) return defaultValue;
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
            if (!data.has(key)) return defaultValue;
            JsonElement element = data.get(key);
            if (element.isJsonNull()) return defaultValue;
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
            if (!data.has(key)) return null;
            JsonElement element = data.get(key);
            if (element.isJsonNull()) return null;
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
            if (!data.has(key)) return null;
            JsonElement element = data.get(key);
            if (element.isJsonNull()) return null;
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

    /**
     * Checks if the response timed out
     */
    public boolean isTimeout() {
        return status.equals(ResponseStatus.TIMEOUT);
    }

    /**
     * Gets the raw JSON string (alias for getRawMessage)
     */
    public String getRawJson() {
        return rawMessage;
    }

    /**
     * Gets the application-specific status string for switch statements
     * <p>
     * This method checks for a "status" field in the response data.
     * If found, returns it as uppercase. If not found, maps the
     * ResponseStatus to a string.
     * <p>
     * Useful for application-level status codes like:
     * - "LANG_MESSAGE"
     * - "PLAYER_NOT_FOUND"
     * - "ALREADY_TARGET"
     * - etc.
     *
     * @return status string (uppercase) or "SUCCESS"/"ERROR"/"TIMEOUT" from ResponseStatus
     * @example <pre>
     * switch (response.getStatusString()) {
     *     case "SUCCESS":
     *         // Handle success
     *         break;
     *     case "LANG_MESSAGE":
     *         // Handle language message
     *         break;
     *     case "PLAYER_NOT_FOUND":
     *         // Handle not found
     *         break;
     * }
     * </pre>
     */
    public String getStatusString() {
        // Check if response has custom application status
        if (has("status")) {
            String appStatus = getString("status");
            if (appStatus != null && !appStatus.isEmpty()) {
                return appStatus.toUpperCase();
            }
        }

        // Fall back to ResponseStatus
        if (status.equals(ResponseStatus.SUCCESS)) return "SUCCESS";
        if (status.equals(ResponseStatus.ERROR)) return "ERROR";
        if (status.equals(ResponseStatus.TIMEOUT)) return "TIMEOUT";
        if (status.equals(ResponseStatus.NOT_FOUND)) return "NOT_FOUND";
        if (status.equals(ResponseStatus.INVALID)) return "INVALID";
        if (status.equals(ResponseStatus.PENDING)) return "PENDING";

        // Default to status name
        return status.getName();
    }

    /**
     * Gets the application-specific status as an enum
     *
     * @param enumClass the enum class to convert to
     * @param <E>       the enum type
     * @return enum value or null if status doesn't match
     */
    public <E extends Enum<E>> E getStatusEnum(Class<E> enumClass) {
        try {
            String statusStr = getStatusString();
            return statusStr != null ? Enum.valueOf(enumClass, statusStr) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Gets the application-specific status as an enum with default value
     *
     * @param enumClass    the enum class to convert to
     * @param defaultValue the default enum value if status doesn't match
     * @param <E>          the enum type
     * @return enum value or defaultValue if status doesn't match
     */
    public <E extends Enum<E>> E getStatusEnum(Class<E> enumClass, E defaultValue) {
        E result = getStatusEnum(enumClass);
        return result != null ? result : defaultValue;
    }


    /**
     * Gets a list of strings from a JSON array
     *
     * @param key the key to retrieve
     * @return list of strings, or empty list if not found or not an array
     */
    public List<String> getStringList(String key) {
        List<String> result = new ArrayList<>();
        try {
            JsonArray array = getArray(key);
            if (array != null) {
                for (JsonElement element : array) {
                    if (!element.isJsonNull()) {
                        result.add(element.getAsString());
                    }
                }
            }
        } catch (Exception e) {
            // Return empty list on error
        }
        return result;
    }

    /**
     * Gets a list of integers from a JSON array
     *
     * @param key the key to retrieve
     * @return list of integers, or empty list if not found or not an array
     */
    public List<Integer> getIntList(String key) {
        List<Integer> result = new ArrayList<>();
        try {
            JsonArray array = getArray(key);
            if (array != null) {
                for (JsonElement element : array) {
                    if (!element.isJsonNull()) {
                        result.add(element.getAsInt());
                    }
                }
            }
        } catch (Exception e) {
            // Return empty list on error
        }
        return result;
    }

    /**
     * Gets a list of longs from a JSON array
     *
     * @param key the key to retrieve
     * @return list of longs, or empty list if not found or not an array
     */
    public List<Long> getLongList(String key) {
        List<Long> result = new ArrayList<>();
        try {
            JsonArray array = getArray(key);
            if (array != null) {
                for (JsonElement element : array) {
                    if (!element.isJsonNull()) {
                        result.add(element.getAsLong());
                    }
                }
            }
        } catch (Exception e) {
            // Return empty list on error
        }
        return result;
    }

    /**
     * Gets a UUID value from a string field
     *
     * @param key the key to retrieve
     * @return UUID or null if not found or invalid format
     */
    public UUID getUUID(String key) {
        try {
            String str = getString(key);
            return str != null ? UUID.fromString(str) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Gets a UUID value with custom default
     *
     * @param key          the key to retrieve
     * @param defaultValue the default UUID to return if not found
     * @return UUID or defaultValue if not found or invalid
     */
    public UUID getUUID(String key, UUID defaultValue) {
        UUID result = getUUID(key);
        return result != null ? result : defaultValue;
    }

    /**
     * Gets a required string value (throws if missing)
     *
     * @param key the key to retrieve
     * @return the string value
     * @throws IllegalArgumentException if key is missing or null
     */
    public String requireString(String key) {
        if (!has(key)) {
            throw new IllegalArgumentException("Required field '" + key + "' is missing");
        }
        String value = getString(key);
        if (value == null) {
            throw new IllegalArgumentException("Required field '" + key + "' is null");
        }
        return value;
    }

    /**
     * Gets a required integer value (throws if missing)
     *
     * @param key the key to retrieve
     * @return the integer value
     * @throws IllegalArgumentException if key is missing
     */
    public int requireInt(String key) {
        if (!has(key)) {
            throw new IllegalArgumentException("Required field '" + key + "' is missing");
        }
        return getInt(key);
    }

    /**
     * Gets a required long value (throws if missing)
     *
     * @param key the key to retrieve
     * @return the long value
     * @throws IllegalArgumentException if key is missing
     */
    public long requireLong(String key) {
        if (!has(key)) {
            throw new IllegalArgumentException("Required field '" + key + "' is missing");
        }
        return getLong(key);
    }

    /**
     * Gets a required boolean value (throws if missing)
     *
     * @param key the key to retrieve
     * @return the boolean value
     * @throws IllegalArgumentException if key is missing
     */
    public boolean requireBoolean(String key) {
        if (!has(key)) {
            throw new IllegalArgumentException("Required field '" + key + "' is missing");
        }
        return getBoolean(key);
    }

    /**
     * Converts the response data to a Map
     *
     * @return Map representation of the response data
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            map.put(entry.getKey(), jsonElementToObject(entry.getValue()));
        }
        return map;
    }

    /**
     * Helper method to convert JsonElement to Java Object
     */
    private Object jsonElementToObject(JsonElement element) {
        if (element.isJsonNull()) {
            return null;
        } else if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            } else if (element.getAsJsonPrimitive().isNumber()) {
                // Try to determine the best number type
                Number num = element.getAsNumber();
                if (num.doubleValue() == num.longValue()) {
                    return num.longValue();
                }
                return num.doubleValue();
            } else {
                return element.getAsString();
            }
        } else if (element.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement e : element.getAsJsonArray()) {
                list.add(jsonElementToObject(e));
            }
            return list;
        } else if (element.isJsonObject()) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), jsonElementToObject(entry.getValue()));
            }
            return map;
        }
        return null;
    }

    /**
     * Gets an enum value from a string field
     *
     * @param key       the key to retrieve
     * @param enumClass the enum class
     * @param <E>       the enum type
     * @return enum value or null if not found or invalid
     */
    public <E extends Enum<E>> E getEnum(String key, Class<E> enumClass) {
        try {
            String str = getString(key);
            return str != null ? Enum.valueOf(enumClass, str.toUpperCase()) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Gets an enum value with custom default
     *
     * @param key          the key to retrieve
     * @param enumClass    the enum class
     * @param defaultValue the default enum value
     * @param <E>          the enum type
     * @return enum value or defaultValue if not found or invalid
     */
    public <E extends Enum<E>> E getEnum(String key, Class<E> enumClass, E defaultValue) {
        E result = getEnum(key, enumClass);
        return result != null ? result : defaultValue;
    }

    /**
     * Gets an Instant from epoch milliseconds
     *
     * @param key the key to retrieve
     * @return Instant or null if not found
     */
    public Instant getInstant(String key) {
        try {
            long epochMillis = getLong(key);
            return epochMillis != 0 ? Instant.ofEpochMilli(epochMillis) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets an Instant with custom default
     *
     * @param key          the key to retrieve
     * @param defaultValue the default Instant
     * @return Instant or defaultValue if not found
     */
    public Instant getInstant(String key, Instant defaultValue) {
        Instant result = getInstant(key);
        return result != null ? result : defaultValue;
    }

    /**
     * Gets a LocalDateTime from epoch milliseconds (system timezone)
     *
     * @param key the key to retrieve
     * @return LocalDateTime or null if not found
     */
    public LocalDateTime getLocalDateTime(String key) {
        try {
            Instant instant = getInstant(key);
            return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets a LocalDateTime with custom default
     *
     * @param key          the key to retrieve
     * @param defaultValue the default LocalDateTime
     * @return LocalDateTime or defaultValue if not found
     */
    public LocalDateTime getLocalDateTime(String key, LocalDateTime defaultValue) {
        LocalDateTime result = getLocalDateTime(key);
        return result != null ? result : defaultValue;
    }

    /**
     * Gets an Optional String
     *
     * @param key the key to retrieve
     * @return Optional containing the value or empty
     */
    public Optional<String> getOptionalString(String key) {
        return Optional.ofNullable(getString(key));
    }

    /**
     * Gets an OptionalInt
     *
     * @param key the key to retrieve
     * @return OptionalInt containing the value or empty
     */
    public OptionalInt getOptionalInt(String key) {
        if (!has(key)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(getInt(key));
    }

    /**
     * Gets an OptionalLong
     *
     * @param key the key to retrieve
     * @return OptionalLong containing the value or empty
     */
    public OptionalLong getOptionalLong(String key) {
        if (!has(key)) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(getLong(key));
    }

    /**
     * Gets an OptionalDouble
     *
     * @param key the key to retrieve
     * @return OptionalDouble containing the value or empty
     */
    public OptionalDouble getOptionalDouble(String key) {
        if (!has(key)) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(getDouble(key));
    }

    /**
     * Gets an Optional UUID
     *
     * @param key the key to retrieve
     * @return Optional containing the UUID or empty
     */
    public Optional<UUID> getOptionalUUID(String key) {
        return Optional.ofNullable(getUUID(key));
    }

    /**
     * Gets a required non-empty string (throws if missing or empty)
     *
     * @param key the key to retrieve
     * @return the non-empty string value
     * @throws IllegalArgumentException if key is missing, null, or empty
     */
    public String requireNonEmpty(String key) {
        String value = requireString(key);
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Required field '" + key + "' is empty");
        }
        return value;
    }

    /**
     * Gets a required positive integer (throws if not > 0)
     *
     * @param key the key to retrieve
     * @return the positive integer value
     * @throws IllegalArgumentException if key is missing or value <= 0
     */
    public int requirePositive(String key) {
        int value = requireInt(key);
        if (value <= 0) {
            throw new IllegalArgumentException("Required field '" + key + "' must be positive, got: " + value);
        }
        return value;
    }

    /**
     * Gets a required non-negative integer (throws if < 0)
     *
     * @param key the key to retrieve
     * @return the non-negative integer value
     * @throws IllegalArgumentException if key is missing or value < 0
     */
    public int requireNonNegative(String key) {
        int value = requireInt(key);
        if (value < 0) {
            throw new IllegalArgumentException("Required field '" + key + "' must be non-negative, got: " + value);
        }
        return value;
    }

    /**
     * Gets a required integer within range (throws if out of bounds)
     *
     * @param key the key to retrieve
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @return the integer value within range
     * @throws IllegalArgumentException if key is missing or value out of range
     */
    public int requireInRange(String key, int min, int max) {
        int value = requireInt(key);
        if (value < min || value > max) {
            throw new IllegalArgumentException("Required field '" + key + "' must be between " + min + " and " + max + ", got: " + value);
        }
        return value;
    }

    /**
     * Gets a required UUID (throws if missing or invalid)
     *
     * @param key the key to retrieve
     * @return the UUID value
     * @throws IllegalArgumentException if key is missing or UUID invalid
     */
    public UUID requireUUID(String key) {
        if (!has(key)) {
            throw new IllegalArgumentException("Required field '" + key + "' is missing");
        }
        UUID value = getUUID(key);
        if (value == null) {
            throw new IllegalArgumentException("Required field '" + key + "' is not a valid UUID");
        }
        return value;
    }

    /**
     * Gets a required enum value (throws if missing or invalid)
     *
     * @param key       the key to retrieve
     * @param enumClass the enum class
     * @param <E>       the enum type
     * @return the enum value
     * @throws IllegalArgumentException if key is missing or enum invalid
     */
    public <E extends Enum<E>> E requireEnum(String key, Class<E> enumClass) {
        if (!has(key)) {
            throw new IllegalArgumentException("Required field '" + key + "' is missing");
        }
        E value = getEnum(key, enumClass);
        if (value == null) {
            throw new IllegalArgumentException("Required field '" + key + "' is not a valid " + enumClass.getSimpleName());
        }
        return value;
    }

    /**
     * Gets a nested value using dot notation
     * <p>
     * Example: "player.stats.kills" will navigate player -> stats -> kills
     *
     * @param path the dot-separated path
     * @return the value as string or null if not found
     */
    public String getNestedString(String path) {
        JsonElement element = navigatePath(path);
        if (element != null && element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return null;
    }

    /**
     * Gets a nested integer value using dot notation
     *
     * @param path the dot-separated path
     * @return the value as int or 0 if not found
     */
    public int getNestedInt(String path) {
        JsonElement element = navigatePath(path);
        if (element != null && element.isJsonPrimitive()) {
            try {
                return element.getAsInt();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Gets a nested long value using dot notation
     *
     * @param path the dot-separated path
     * @return the value as long or 0 if not found
     */
    public long getNestedLong(String path) {
        JsonElement element = navigatePath(path);
        if (element != null && element.isJsonPrimitive()) {
            try {
                return element.getAsLong();
            } catch (Exception e) {
                return 0L;
            }
        }
        return 0L;
    }

    /**
     * Gets a nested boolean value using dot notation
     *
     * @param path the dot-separated path
     * @return the value as boolean or false if not found
     */
    public boolean getNestedBoolean(String path) {
        JsonElement element = navigatePath(path);
        if (element != null && element.isJsonPrimitive()) {
            try {
                return element.getAsBoolean();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Gets a nested double value using dot notation
     *
     * @param path the dot-separated path
     * @return the value as double or 0.0 if not found
     */
    public double getNestedDouble(String path) {
        JsonElement element = navigatePath(path);
        if (element != null && element.isJsonPrimitive()) {
            try {
                return element.getAsDouble();
            } catch (Exception e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * Helper method to navigate a dot-separated path in the JSON structure
     *
     * @param path the dot-separated path (e.g., "player.stats.kills")
     * @return the JsonElement at that path or null if not found
     */
    private JsonElement navigatePath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        JsonElement current = data;

        for (String part : parts) {
            if (current == null || !current.isJsonObject()) {
                return null;
            }
            current = current.getAsJsonObject().get(part);
        }

        return current;
    }

}