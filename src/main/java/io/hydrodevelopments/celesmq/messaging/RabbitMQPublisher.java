package io.hydrodevelopments.celesmq.messaging;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import io.hydrodevelopments.celesmq.connection.RabbitMQConnectionManager;
import io.hydrodevelopments.celesmq.platform.Platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles publishing messages to RabbitMQ exchanges and queues
 */
public class RabbitMQPublisher {

  private final RabbitMQConnectionManager connectionManager;
  private final Platform platform;
  private final Logger logger;

  public RabbitMQPublisher(RabbitMQConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
    this.platform = connectionManager.getPlatform();
    this.logger = platform.getLogger();
  }

  /**
   * Publishes a message to a specific queue
   *
   * @param queueName name of the queue
   * @param message   message content
   *
   * @return CompletableFuture indicating success/failure
   */
  public CompletableFuture<Boolean> publishToQueue(String queueName, String message) {
    return publishToQueue(queueName, message, false);
  }

  /**
   * Publishes a message to a specific queue with durability option
   *
   * @param queueName  name of the queue
   * @param message    message content
   * @param persistent whether message should be persistent
   *
   * @return CompletableFuture indicating success/failure
   */
  public CompletableFuture<Boolean> publishToQueue(String queueName, String message, boolean persistent) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();

    platform.runAsync(() -> {
      try {
        Channel channel = connectionManager.getChannel();

        // Declare queue, idempotent operation
        channel.queueDeclare(queueName, true, false, false, null);

        // Prepare message properties
        AMQP.BasicProperties props =
          persistent ? MessageProperties.PERSISTENT_TEXT_PLAIN : MessageProperties.TEXT_PLAIN;

        // Publish message to default exchange with queue name as routing key
        channel.basicPublish("", queueName, props, message.getBytes(StandardCharsets.UTF_8));

        logger.info("Message published to queue: " + queueName);
        future.complete(true);

      } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to publish message to queue: " + queueName, e);
        future.complete(false);
      }
    });

    return future;
  }

  /**
   * Publishes a message to an exchange with a routing key
   *
   * @param exchangeName name of the exchange
   * @param routingKey   routing key
   * @param message      message content
   *
   * @return CompletableFuture indicating success/failure
   */
  public CompletableFuture<Boolean> publishToExchange(String exchangeName, String routingKey, String message) {
    return publishToExchange(exchangeName, routingKey, message, "direct", false);
  }

  /**
   * Publishes a message to an exchange with full options
   *
   * @param exchangeName name of the exchange
   * @param routingKey   routing key
   * @param message      message content
   * @param exchangeType type of exchange (direct, fanout, topic, headers)
   * @param persistent   whether message should be persistent
   *
   * @return CompletableFuture indicating success/failure
   */
  public CompletableFuture<Boolean> publishToExchange(String exchangeName,
    String routingKey,
    String message,
    String exchangeType,
    boolean persistent) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();

    platform.runAsync(() -> {
      try {
        Channel channel = connectionManager.getChannel();

        // Declare exchange, idempotent operation
        channel.exchangeDeclare(exchangeName, exchangeType, true);

        // Prepare message properties
        AMQP.BasicProperties props =
          persistent ? MessageProperties.PERSISTENT_TEXT_PLAIN : MessageProperties.TEXT_PLAIN;

        // Publish message
        channel.basicPublish(exchangeName, routingKey, props, message.getBytes(StandardCharsets.UTF_8));

        logger.info("Message published to exchange: " + exchangeName + " with routing key: " + routingKey);
        future.complete(true);

      } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to publish message to exchange: " + exchangeName, e);
        future.complete(false);
      }
    });

    return future;
  }

  /**
   * Publishes a message with custom headers
   *
   * @param exchangeName name of the exchange
   * @param routingKey   routing key
   * @param message      message content
   * @param headers      custom headers
   *
   * @return CompletableFuture indicating success/failure
   */
  public CompletableFuture<Boolean> publishWithHeaders(String exchangeName,
    String routingKey,
    String message,
    Map<String, Object> headers) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();

    platform.runAsync(() -> {
      try {
        Channel channel = connectionManager.getChannel();

        // Declare exchange
        channel.exchangeDeclare(exchangeName, "direct", true);

        // Build properties with headers
        AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
        propsBuilder.headers(headers);
        propsBuilder.contentType("text/plain");
        propsBuilder.deliveryMode(2); // persistent

        // Publish message
        channel.basicPublish(exchangeName, routingKey, propsBuilder.build(), message.getBytes(StandardCharsets.UTF_8));

        logger.info("Message with headers published to exchange: " + exchangeName);
        future.complete(true);

      } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to publish message with headers", e);
        future.complete(false);
      }
    });

    return future;
  }

  /**
   * Publishes a message for fanout (broadcast) pattern
   *
   * @param exchangeName name of the fanout exchange
   * @param message      message content
   *
   * @return CompletableFuture indicating success/failure
   */
  public CompletableFuture<Boolean> broadcast(String exchangeName, String message) {
    return publishToExchange(exchangeName, "", message, "fanout", false);
  }
}
