package io.hydrodevelopments.celesmq.middleware;

import io.hydrodevelopments.celesmq.message.MessageResponse;

/**
 * Middleware interface for intercepting and modifying messages
 */
public interface MessageMiddleware {

  /**
   * Called before a message is sent
   *
   * @param message message about to be sent
   *
   * @return modified message or null to cancel sending
   */
  String beforeSend(String message);

  /**
   * Called after a message is received but before it's processed
   *
   * @param message received message
   *
   * @return modified MessageResponse or null to cancel processing
   */
  MessageResponse afterReceive(MessageResponse message);

  /**
   * Priority for middleware execution (higher = runs first)
   */
  default int getPriority() {
    return 0;
  }
}
