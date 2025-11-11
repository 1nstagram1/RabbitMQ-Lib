package io.hydrodevelopments.celesmq.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import io.hydrodevelopments.celesmq.connection.RabbitMQConnectionManager;
import io.hydrodevelopments.celesmq.listener.MessageListener;
import io.hydrodevelopments.celesmq.platform.Platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles consuming messages from RabbitMQ queues
 */
public class RabbitMQConsumer {

  private final RabbitMQConnectionManager connectionManager;
  private final Platform platform;
  private final Logger logger;
  private final Map<String, String> activeConsumers;

  public RabbitMQConsumer(RabbitMQConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
    this.platform = connectionManager.getPlatform();
    this.logger = platform.getLogger();
    this.activeConsumers = new HashMap<>();
  }

  /**
   * Starts consuming messages from a queue
   *
   * @param queueName name of the queue to consume from
   * @param listener  callback for handling messages
   * @param autoAck   whether to automatically acknowledge messages
   *
   * @return true if consumer started successfully, false otherwise
   */
  public boolean consume(String queueName, MessageListener listener, boolean autoAck) {
    return consume(queueName, listener, autoAck, false);
  }

  /**
   * Starts consuming messages from a queue with sync option
   *
   * @param queueName        name of the queue to consume from
   * @param listener         callback for handling messages
   * @param autoAck          whether to automatically acknowledge messages
   * @param syncToMainThread whether to execute listener on main Minecraft thread
   *
   * @return true if consumer started successfully, false otherwise
   */
  public boolean consume(String queueName, MessageListener listener, boolean autoAck, boolean syncToMainThread) {
    try {
      Channel channel = connectionManager.createChannel();

      // Declare queue (idempotent)
      channel.queueDeclare(queueName, true, false, false, null);

      // Set QoS prefetch count
      channel.basicQos(1);

      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

        Runnable task = () -> {
          try {
            listener.onMessageReceived(message);

            // Manual acknowledgment if autoAck is false
            if (!autoAck) {
              channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
          } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing message from queue: " + queueName, e);
            if (!autoAck) {
              try {
                // Reject and requeue message on error
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
              } catch (IOException ioException) {
                logger.log(Level.SEVERE, "Failed to nack message", ioException);
              }
            }
          }
        };

        // Execute on main thread if requested, otherwise run async
        if (syncToMainThread) {
          platform.runSync(task);
        } else {
          task.run();
        }
      };

      // Start consuming
      String consumerTag = channel.basicConsume(queueName, autoAck, deliverCallback, consumerTag1 -> {
        logger.info("Consumer cancelled: " + consumerTag1);
        activeConsumers.remove(queueName);
      });

      activeConsumers.put(queueName, consumerTag);
      logger.info("Started consuming from queue: " + queueName + " (consumer tag: " + consumerTag + ")");
      return true;

    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to start consuming from queue: " + queueName, e);
      return false;
    }
  }

  /**
   * Subscribes to a fanout exchange (broadcast pattern)
   *
   * @param exchangeName name of the exchange
   * @param listener     callback for handling messages
   *
   * @return true if subscription successful, false otherwise
   */
  public boolean subscribeToBroadcast(String exchangeName, MessageListener listener) {
    return subscribeToBroadcast(exchangeName, listener, false);
  }

  /**
   * Subscribes to a fanout exchange with sync option
   *
   * @param exchangeName     name of the exchange
   * @param listener         callback for handling messages
   * @param syncToMainThread whether to execute listener on main thread
   *
   * @return true if subscription successful, false otherwise
   */
  public boolean subscribeToBroadcast(String exchangeName, MessageListener listener, boolean syncToMainThread) {
    try {
      Channel channel = connectionManager.createChannel();

      // Declare fanout exchange
      channel.exchangeDeclare(exchangeName, "fanout", true);

      // Create temporary exclusive queue
      String queueName = channel.queueDeclare().getQueue();

      // Bind queue to exchange
      channel.queueBind(queueName, exchangeName, "");

      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

        Runnable task = () -> {
          try {
            listener.onMessageReceived(message);
          } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing broadcast message", e);
          }
        };

        if (syncToMainThread) {
          platform.runSync(task);
        } else {
          task.run();
        }
      };

      String consumerTag = channel.basicConsume(queueName, true, deliverCallback, tag -> {
        logger.info("Broadcast consumer cancelled: " + tag);
        activeConsumers.remove(exchangeName);
      });

      activeConsumers.put(exchangeName, consumerTag);
      logger.info("Subscribed to broadcast exchange: " + exchangeName);
      return true;

    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to subscribe to broadcast exchange: " + exchangeName, e);
      return false;
    }
  }

  /**
   * Subscribes to a topic exchange with pattern matching
   *
   * @param exchangeName      name of the topic exchange
   * @param routingKeyPattern routing key pattern (e.g., "server.*.events")
   * @param listener          callback for handling messages
   *
   * @return true if subscription successful, false otherwise
   */
  public boolean subscribeToTopic(String exchangeName, String routingKeyPattern, MessageListener listener) {
    return subscribeToTopic(exchangeName, routingKeyPattern, listener, false);
  }

  /**
   * Subscribes to a topic exchange with pattern matching and sync option
   *
   * @param exchangeName      name of the topic exchange
   * @param routingKeyPattern routing key pattern (e.g., "server.*.events")
   * @param listener          callback for handling messages
   * @param syncToMainThread  whether to execute listener on main thread
   *
   * @return true if subscription successful, false otherwise
   */
  public boolean subscribeToTopic(String exchangeName,
    String routingKeyPattern,
    MessageListener listener,
    boolean syncToMainThread) {
    try {
      Channel channel = connectionManager.createChannel();

      // Declare topic exchange
      channel.exchangeDeclare(exchangeName, "topic", true);

      // Create queue
      String queueName = channel.queueDeclare().getQueue();

      // Bind queue to exchange with routing pattern
      channel.queueBind(queueName, exchangeName, routingKeyPattern);

      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

        Runnable task = () -> {
          try {
            listener.onMessageReceived(message);
          } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing topic message", e);
          }
        };

        if (syncToMainThread) {
          platform.runSync(task);
        } else {
          task.run();
        }
      };

      String consumerTag = channel.basicConsume(queueName, true, deliverCallback, tag -> {
        logger.info("Topic consumer cancelled: " + tag);
        activeConsumers.remove(exchangeName + ":" + routingKeyPattern);
      });

      activeConsumers.put(exchangeName + ":" + routingKeyPattern, consumerTag);
      logger.info("Subscribed to topic exchange: " + exchangeName + " with pattern: " + routingKeyPattern);
      return true;

    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to subscribe to topic exchange", e);
      return false;
    }
  }

  /**
   * Gets the map of active consumers
   *
   * @return map of queue/exchange names to consumer tags
   */
  public Map<String, String> getActiveConsumers() {
    return new HashMap<>(activeConsumers);
  }
}
