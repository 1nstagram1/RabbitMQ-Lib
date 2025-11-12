package io.hydrodevelopments.celesmq.message;

import com.google.gson.JsonObject;
import io.hydrodevelopments.celesmq.RabbitMQClient;
import io.hydrodevelopments.celesmq.util.JsonSerializer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.logging.Logger;

/**
 * Request-Response pattern for RabbitMQ with automatic callback handling
 * Supports timeouts and provides CompletableFuture-based API
 */
public class MessageRequest {

  private static final Map<Integer, CompletableFuture<MessageResponse>> pendingRequests = new ConcurrentHashMap<>();
  private static final Random random = new Random();

  private final RabbitMQClient client;
  private final Logger logger;
  private final String replyQueue;
  private final BiFunction<String, String, CompletableFuture<Boolean>> sendFunction;

  private JsonObject data;
  private int taskId;
  private long timeout; // Must be explicitly set via timeout() method

  /**
   * Constructor with custom send function for smart routing
   */
  public MessageRequest(RabbitMQClient client, Logger logger, String replyQueue,
                        BiFunction<String, String, CompletableFuture<Boolean>> sendFunction) {
    this.client = client;
    this.logger = logger;
    this.replyQueue = replyQueue;
    this.sendFunction = sendFunction;
    this.data = new JsonObject();
    this.taskId = random.nextInt(100000);
  }

  /**
   * Legacy constructor for backward compatibility (uses exchange routing)
   */
  @Deprecated
  public MessageRequest(RabbitMQClient client, Logger logger, String replyQueue) {
    this(client, logger, replyQueue, (channel, message) ->
            client.publishToExchange(channel, "", message));
  }

  /**
   * Sets the action for this request
   */
  public MessageRequest action(String action) {
    data.addProperty("action", action);
    return this;
  }

  /**
   * Sets the timeout for this request
   */
  public MessageRequest timeout(long milliseconds) {
    this.timeout = milliseconds;
    return this;
  }

  /**
   * Sets a custom task ID
   */
  public MessageRequest taskId(int taskId) {
    this.taskId = taskId;
    return this;
  }

  /**
   * Adds a string parameter
   */
  public MessageRequest param(String key, String value) {
    data.addProperty(key, value);
    return this;
  }

  /**
   * Adds an integer parameter
   */
  public MessageRequest param(String key, int value) {
    data.addProperty(key, value);
    return this;
  }

  /**
   * Adds a long parameter
   */
  public MessageRequest param(String key, long value) {
    data.addProperty(key, value);
    return this;
  }

  /**
   * Adds a boolean parameter
   */
  public MessageRequest param(String key, boolean value) {
    data.addProperty(key, value);
    return this;
  }

  /**
   * Adds a double parameter
   */
  public MessageRequest param(String key, double value) {
    data.addProperty(key, value);
    return this;
  }

  /**
   * Adds any object parameter (will be serialized to JSON)
   */
  public MessageRequest param(String key, Object value) {
    if (value instanceof String) {
      data.addProperty(key, (String) value);
    } else if (value instanceof Number) {
      data.addProperty(key, (Number) value);
    } else if (value instanceof Boolean) {
      data.addProperty(key, (Boolean) value);
    } else {
      // Serialize complex objects
      String json = JsonSerializer.toJson(value);
      data.add(key, JsonSerializer.fromJson(json, JsonObject.class));
    }
    return this;
  }

  /**
   * Sets all data at once from a Map
   */
  public MessageRequest params(Map<String, Object> params) {
    params.forEach(this::param);
    return this;
  }

  /**
   * Sets all data at once from a Map (alias for params)
   */
  public MessageRequest data(Map<String, Object> data) {
    return params(data);
  }

  /**
   * Adds a UUID parameter (automatically converts to string)
   *
   * @param key the parameter key
   * @param value the UUID value
   * @return this request builder
   */
  public MessageRequest uuid(String key, UUID value) {
    if (value != null) {
      data.addProperty(key, value.toString());
    }
    return this;
  }

  /**
   * Conditionally adds a parameter if condition is true
   *
   * Useful for optional parameters:
   * <pre>
   * request.param("action", "query")
   *        .paramIf(filter != null, "filter", filter)
   *        .paramIf(includeOffline, "include_offline", true)
   * </pre>
   *
   * @param condition whether to add the parameter
   * @param key the parameter key
   * @param value the parameter value
   * @return this request builder
   */
  public MessageRequest paramIf(boolean condition, String key, Object value) {
    if (condition) {
      param(key, value);
    }
    return this;
  }

  /**
   * Conditionally adds a UUID parameter if condition is true
   *
   * @param condition whether to add the parameter
   * @param key the parameter key
   * @param value the UUID value
   * @return this request builder
   */
  public MessageRequest uuidIf(boolean condition, String key, UUID value) {
    if (condition) {
      uuid(key, value);
    }
    return this;
  }

  /**
   * Adds an enum parameter (automatically converts to string)
   *
   * @param key the parameter key
   * @param value the enum value
   * @return this request builder
   */
  public MessageRequest param(String key, Enum<?> value) {
    if (value != null) {
      data.addProperty(key, value.name());
    }
    return this;
  }

  /**
   * Conditionally adds an enum parameter if condition is true
   *
   * @param condition whether to add the parameter
   * @param key the parameter key
   * @param value the enum value
   * @return this request builder
   */
  public MessageRequest paramIf(boolean condition, String key, Enum<?> value) {
    if (condition && value != null) {
      data.addProperty(key, value.name());
    }
    return this;
  }

  /**
   * Adds an Instant parameter (converts to epoch milliseconds)
   *
   * @param key the parameter key
   * @param value the Instant value
   * @return this request builder
   */
  public MessageRequest instant(String key, Instant value) {
    if (value != null) {
      data.addProperty(key, value.toEpochMilli());
    }
    return this;
  }

  /**
   * Conditionally adds an Instant parameter if condition is true
   *
   * @param condition whether to add the parameter
   * @param key the parameter key
   * @param value the Instant value
   * @return this request builder
   */
  public MessageRequest instantIf(boolean condition, String key, Instant value) {
    if (condition) {
      instant(key, value);
    }
    return this;
  }

  /**
   * Adds a LocalDateTime parameter (converts to epoch milliseconds using system timezone)
   *
   * @param key the parameter key
   * @param value the LocalDateTime value
   * @return this request builder
   */
  public MessageRequest localDateTime(String key, LocalDateTime value) {
    if (value != null) {
      Instant instant = value.atZone(ZoneId.systemDefault()).toInstant();
      data.addProperty(key, instant.toEpochMilli());
    }
    return this;
  }

  /**
   * Conditionally adds a LocalDateTime parameter if condition is true
   *
   * @param condition whether to add the parameter
   * @param key the parameter key
   * @param value the LocalDateTime value
   * @return this request builder
   */
  public MessageRequest localDateTimeIf(boolean condition, String key, LocalDateTime value) {
    if (condition) {
      localDateTime(key, value);
    }
    return this;
  }

  /**
   * Adds a timestamp parameter (epoch milliseconds)
   *
   * @param key the parameter key
   * @param epochMillis the timestamp in milliseconds
   * @return this request builder
   */
  public MessageRequest timestamp(String key, long epochMillis) {
    data.addProperty(key, epochMillis);
    return this;
  }

  /**
   * Conditionally adds a timestamp parameter if condition is true
   *
   * @param condition whether to add the parameter
   * @param key the parameter key
   * @param epochMillis the timestamp in milliseconds
   * @return this request builder
   */
  public MessageRequest timestampIf(boolean condition, String key, long epochMillis) {
    if (condition) {
      data.addProperty(key, epochMillis);
    }
    return this;
  }

  /**
   * Gets the current request data
   */
  public JsonObject getData() {
    return data;
  }

  /**
   * Sends the request and returns a CompletableFuture
   * Supports both direct queue and exchange-based routing
   */
  public CompletableFuture<MessageResponse> sendTo(String channel) {
    CompletableFuture<MessageResponse> future = new CompletableFuture<>();

    // Add taskID and replyTo
    data.addProperty("taskID", taskId);
    data.addProperty("replyTo", replyQueue);

    // Store the future
    pendingRequests.put(taskId, future);

    long startTime = System.currentTimeMillis();

    // Send the message using the smart routing function
    String message = data.toString();
    sendFunction.apply(channel, message).thenAccept(success -> {
      if (!success) {
        future.completeExceptionally(new RuntimeException("Failed to send request"));
        pendingRequests.remove(taskId);
      }
    });

    // Set up timeout
    CompletableFuture.delayedExecutor(timeout, TimeUnit.MILLISECONDS).execute(() -> {
      CompletableFuture<MessageResponse> pending = pendingRequests.remove(taskId);
      if (pending != null && !pending.isDone()) {
        logger.warning("Request timeout for taskID " + taskId + " after " + timeout + "ms");
        String timeoutJson = JsonSerializer.toJson(Map.of("error", "timeout"));
        pending.complete(new MessageResponse(timeoutJson, ResponseStatus.TIMEOUT));
      }
    });

    // Log completion time on success
    future.thenAccept(response -> {
      long elapsed = System.currentTimeMillis() - startTime;
      if (elapsed > 100) { // Only log if > 100ms
        logger.info("Request " + data.get("action").getAsString() + " completed in " + elapsed + "ms");
      }
    });

    return future;
  }

  /**
   * Handles an incoming response message
   * This should be called by the message consumer when a reply is received
   */
  public static void handleResponse(int taskId, String jsonResponse, ResponseStatus status) {
    CompletableFuture<MessageResponse> future = pendingRequests.remove(taskId);
    if (future != null && !future.isDone()) {
      future.complete(new MessageResponse(jsonResponse, status));
    }
  }

  /**
   * Handles an incoming response message with automatic status detection
   */
  public static void handleResponse(int taskId, String jsonResponse) {
    MessageResponse response = new MessageResponse(jsonResponse);
    String statusStr = response.getString("status");
    ResponseStatus status = ResponseStatus.SUCCESS;

    if (statusStr != null) {
      try {
        status = ResponseStatus.fromString(statusStr);
      } catch (IllegalArgumentException e) {
        // Keep default SUCCESS status
      }
    }

    handleResponse(taskId, jsonResponse, status);
  }

  /**
   * Creates a new request builder with smart routing
   */
  public static MessageRequest create(RabbitMQClient client, Logger logger, String replyQueue,
                                      BiFunction<String, String, CompletableFuture<Boolean>> sendFunction) {
    return new MessageRequest(client, logger, replyQueue, sendFunction);
  }

  /**
   * Creates a new request builder (legacy - uses exchange routing)
   */
  @Deprecated
  public static MessageRequest create(RabbitMQClient client, Logger logger, String replyQueue) {
    return new MessageRequest(client, logger, replyQueue);
  }

  /**
   * Gets the number of pending requests
   */
  public static int getPendingCount() {
    return pendingRequests.size();
  }

  /**
   * Clears all pending requests (useful for cleanup)
   */
  public static void clearPending() {
    pendingRequests.clear();
  }
}