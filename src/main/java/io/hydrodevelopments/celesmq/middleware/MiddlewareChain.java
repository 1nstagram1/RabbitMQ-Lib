package io.hydrodevelopments.celesmq.middleware;

import io.hydrodevelopments.celesmq.message.MessageResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages and executes middleware chain for message processing
 */
public class MiddlewareChain {

  private final List<MessageMiddleware> middlewares = new ArrayList<>();
  private final Logger logger;

  public MiddlewareChain(Logger logger) {
    this.logger = logger;
  }

  /**
   * Adds middleware to the chain
   */
  public MiddlewareChain add(MessageMiddleware middleware) {
    middlewares.add(middleware);
    // Sort by priority, higher priority first
    middlewares.sort(Comparator.comparingInt(MessageMiddleware::getPriority).reversed());
    return this;
  }

  /**
   * Removes middleware from the chain
   */
  public MiddlewareChain remove(MessageMiddleware middleware) {
    middlewares.remove(middleware);
    return this;
  }

  /**
   * Processes a message before sending
   *
   * @return processed message or null if cancelled
   */
  public String processSend(String message) {
    String current = message;
    for (MessageMiddleware middleware : middlewares) {
      try {
        current = middleware.beforeSend(current);
        if (current == null) {
          logger.info("Message send cancelled by middleware: " + middleware.getClass().getSimpleName());
          return null;
        }
      } catch (Exception e) {
        logger.severe("Error in middleware " + middleware.getClass().getSimpleName() + ": " + e.getMessage());
      }
    }
    return current;
  }

  /**
   * Processes a message after receiving
   *
   * @return processed response or null if cancelled
   */
  public MessageResponse processReceive(MessageResponse response) {
    MessageResponse current = response;
    for (MessageMiddleware middleware : middlewares) {
      try {
        current = middleware.afterReceive(current);
        if (current == null) {
          logger.info("Message receive cancelled by middleware: " + middleware.getClass().getSimpleName());
          return null;
        }
      } catch (Exception e) {
        logger.severe("Error in middleware " + middleware.getClass().getSimpleName() + ": " + e.getMessage());
      }
    }
    return current;
  }

  /**
   * Gets the number of middlewares
   */
  public int getMiddlewareCount() {
    return middlewares.size();
  }

  /**
   * Clears all middlewares
   */
  public MiddlewareChain clearAll() {
    middlewares.clear();
    return this;
  }

  /**
   * Built-in logging middleware
   */
  public static class LoggingMiddleware implements MessageMiddleware {
    private final Logger logger;

    public LoggingMiddleware(Logger logger) {
      this.logger = logger;
    }

    @Override public String beforeSend(String message) {
      logger.info("Sending: " + message.substring(0, Math.min(100, message.length())));
      return message;
    }

    @Override public MessageResponse afterReceive(MessageResponse message) {
      logger.info("Received: " + message.getRawMessage().substring(0, Math.min(100, message.getRawMessage().length())));
      return message;
    }

    @Override public int getPriority() {
      return -1000; // Run last
    }
  }

  /**
   * Built-in filtering middleware
   */
  public static class FilterMiddleware implements MessageMiddleware {
    private final java.util.function.Predicate<String> filter;

    public FilterMiddleware(java.util.function.Predicate<String> filter) {
      this.filter = filter;
    }

    @Override public String beforeSend(String message) {
      return filter.test(message) ? message : null;
    }

    @Override public MessageResponse afterReceive(MessageResponse message) {
      return filter.test(message.getRawMessage()) ? message : null;
    }
  }

  /**
   * Built-in message transformation middleware
   */
  public static class TransformMiddleware implements MessageMiddleware {
    private final java.util.function.Function<String, String> transformer;

    public TransformMiddleware(java.util.function.Function<String, String> transformer) {
      this.transformer = transformer;
    }

    @Override public String beforeSend(String message) {
      return transformer.apply(message);
    }

    @Override public MessageResponse afterReceive(MessageResponse message) {
      String transformed = transformer.apply(message.getRawMessage());
      return new MessageResponse(transformed);
    }
  }
}
