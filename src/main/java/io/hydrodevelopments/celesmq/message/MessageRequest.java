package io.hydrodevelopments.celesmq.message;

import com.google.gson.JsonObject;
import io.hydrodevelopments.celesmq.RabbitMQClient;
import io.hydrodevelopments.celesmq.util.JsonSerializer;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Request-Response pattern for RabbitMQ with automatic callback handling Supports timeouts and provides
 * CompletableFuture-based API
 */
public class MessageRequest {

  private static final Map<Integer, CompletableFuture<MessageResponse>> pendingRequests = new ConcurrentHashMap<>();
  private static final Random random = new Random();

  private final RabbitMQClient client;
  private final Logger logger;
  private final String replyQueue;

  private JsonObject data;
  private int taskId;
  private long timeout;

  public MessageRequest(RabbitMQClient client, Logger logger, String replyQueue) {
    this.client = client;
    this.logger = logger;
    this.replyQueue = replyQueue;
    this.data = new JsonObject();
    this.taskId = random.nextInt(100000);
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
   * Gets the current request data
   */
  public JsonObject getData() {
    return data;
  }

  /**
   * Sends the request and returns a CompletableFuture
   */
  public CompletableFuture<MessageResponse> sendTo(String exchange) {
    return sendTo(exchange, "");
  }

  /**
   * Sends the request to a specific exchange with routing key
   */
  public CompletableFuture<MessageResponse> sendTo(String exchange, String routingKey) {
    CompletableFuture<MessageResponse> future = new CompletableFuture<>();

    // Add taskID and replyTo
    data.addProperty("taskID", taskId);
    data.addProperty("replyTo", replyQueue);

    // Store the future
    pendingRequests.put(taskId, future);

    long startTime = System.currentTimeMillis();

    // Send the message
    String message = data.toString();
    client.publishToExchange(exchange, routingKey, message).thenAccept(success -> {
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
      if (elapsed > 100) {
        logger.info("Request " + data.get("action").getAsString() + " completed in " + elapsed + "ms");
      }
    });

    return future;
  }

  /**
   * Handles an incoming response message This should be called by the message consumer when a reply is received
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
        // Default to SUCCESS if unknown
      }
    }

    handleResponse(taskId, jsonResponse, status);
  }

  /**
   * Creates a new request builder
   */
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
