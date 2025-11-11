package io.hydrodevelopments.celesmq.message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Routes incoming messages to registered action handlers Provides a clean way to organize message handling logic
 */
public class MessageRouter {

  private final Map<String, Consumer<MessageResponse>> handlers = new ConcurrentHashMap<>();
  private final Logger logger;
  private Consumer<MessageResponse> defaultHandler;
  private Consumer<Exception> errorHandler;

  public MessageRouter(Logger logger) {
    this.logger = logger;
  }

  /**
   * Registers a handler for a specific action
   */
  public MessageRouter on(String action, Consumer<MessageResponse> handler) {
    handlers.put(action.toLowerCase(), handler);
    return this;
  }

  /**
   * Registers a default handler for unmatched actions
   */
  public MessageRouter onDefault(Consumer<MessageResponse> handler) {
    this.defaultHandler = handler;
    return this;
  }

  /**
   * Registers an error handler for exceptions during message processing
   */
  public MessageRouter onError(Consumer<Exception> handler) {
    this.errorHandler = handler;
    return this;
  }

  /**
   * Routes a raw message string
   */
  public void route(String message) {
    try {
      MessageResponse response = new MessageResponse(message);
      route(response);
    } catch (Exception e) {
      handleError(e);
    }
  }

  /**
   * Routes a MessageResponse
   */
  public void route(MessageResponse response) {
    try {
      // Check for taskID and handle as response
      if (response.has("taskID")) {
        int taskId = response.getInt("taskID");
        MessageRequest.handleResponse(taskId, response.getRawMessage());
      }

      // Check for action and route
      if (response.has("action")) {
        String action = response.getString("action").toLowerCase();
        Consumer<MessageResponse> handler = handlers.get(action);

        if (handler != null) {
          handler.accept(response);
        } else if (defaultHandler != null) {
          defaultHandler.accept(response);
        } else {
          logger.warning("No handler registered for action: " + action);
        }
      } else if (!response.has("taskID")) {
        // If no action and no taskID, route to default handler
        if (defaultHandler != null) {
          defaultHandler.accept(response);
        }
      }
    } catch (Exception e) {
      handleError(e);
    }
  }

  /**
   * Handles errors during message processing
   */
  private void handleError(Exception e) {
    if (errorHandler != null) {
      errorHandler.accept(e);
    } else {
      logger.log(Level.SEVERE, "Error processing message", e);
    }
  }

  /**
   * Unregisters a handler for a specific action
   */
  public MessageRouter remove(String action) {
    handlers.remove(action.toLowerCase());
    return this;
  }

  /**
   * Clears all registered handlers
   */
  public MessageRouter clearAll() {
    handlers.clear();
    defaultHandler = null;
    errorHandler = null;
    return this;
  }

  /**
   * Gets the number of registered handlers
   */
  public int getHandlerCount() {
    return handlers.size();
  }

  /**
   * Checks if a handler is registered for an action
   */
  public boolean hasHandler(String action) {
    return handlers.containsKey(action.toLowerCase());
  }

  /**
   * Creates a new MessageRouter
   */
  public static MessageRouter create(Logger logger) {
    return new MessageRouter(logger);
  }
}
