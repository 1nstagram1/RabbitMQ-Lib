package io.hydrodevelopments.celesmq.listener;

/**
 * Functional interface for handling received messages
 */
@FunctionalInterface public interface MessageListener {

  /**
   * Called when a message is received
   *
   * @param message the received message content
   */
  void onMessageReceived(String message);
}
