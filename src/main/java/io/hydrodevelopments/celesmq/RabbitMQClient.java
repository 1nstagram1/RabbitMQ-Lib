package io.hydrodevelopments.celesmq;

import io.hydrodevelopments.celesmq.config.RabbitMQConfig;
import io.hydrodevelopments.celesmq.connection.RabbitMQConnectionManager;
import io.hydrodevelopments.celesmq.listener.MessageListener;
import io.hydrodevelopments.celesmq.messaging.RabbitMQConsumer;
import io.hydrodevelopments.celesmq.messaging.RabbitMQPublisher;
import io.hydrodevelopments.celesmq.platform.Platform;
import io.hydrodevelopments.celesmq.platform.SpigotPlatform;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Main API class for RabbitMQ integration in Minecraft plugins This class provides a simple interface to interact with
 * RabbitMQ Supports Spigot/Paper, BungeeCord, and Velocity platforms
 */
public class RabbitMQClient {

  private final RabbitMQConnectionManager connectionManager;
  private final RabbitMQPublisher publisher;
  private final RabbitMQConsumer consumer;

  /**
   * Creates a new RabbitMQ client instance for Spigot/Paper.
   *
   * @param plugin the JavaPlugin instance
   * @param config RabbitMQ configuration
   *
   * @deprecated Use {@link #RabbitMQClient(Platform, RabbitMQConfig)} for better multi-platform support.
   */
  @Deprecated(since = "2.0.0", forRemoval = true) public RabbitMQClient(JavaPlugin plugin, RabbitMQConfig config) {
    this(new SpigotPlatform(plugin), config);
  }

  /**
   * Creates a new RabbitMQ client instance with platform abstraction
   *
   * @param platform the Platform instance (SpigotPlatform, BungeeCordPlatform, or VelocityPlatform or any other supported platform)
   * @param config   RabbitMQ configuration
   */
  public RabbitMQClient(Platform platform, RabbitMQConfig config) {
    this.connectionManager = new RabbitMQConnectionManager(platform, config);
    this.publisher = new RabbitMQPublisher(connectionManager);
    this.consumer = new RabbitMQConsumer(connectionManager);
  }

  /**
   * Connects to the RabbitMQ server
   *
   * @return true if connection successful, false otherwise
   */
  public boolean connect() {
    return connectionManager.connect();
  }

  /**
   * Disconnects from the RabbitMQ server
   */
  public void disconnect() {
    connectionManager.disconnect();
  }

  /**
   * Checks if client is connected
   *
   * @return true if connected, false otherwise
   */
  public boolean isConnected() {
    return connectionManager.isConnected();
  }

  // ========== Publishing Methods ==========

  /**
   * Publishes a message to a queue
   *
   * @param queueName name of the queue
   * @param message   message content
   *
   * @return CompletableFuture indicating success/failure
   */
  public CompletableFuture<Boolean> publishToQueue(String queueName, String message) {
    return publisher.publishToQueue(queueName, message);
  }

  /**
   * Publishes a persistent message to a queue
   *
   * @param queueName  name of the queue
   * @param message    message content
   * @param persistent whether message should be persistent
   *
   * @return CompletableFuture indicating success/failure
   */
  public CompletableFuture<Boolean> publishToQueue(String queueName, String message, boolean persistent) {
    return publisher.publishToQueue(queueName, message, persistent);
  }

  /**
   * Publishes a message to an exchange
   *
   * @param exchangeName name of the exchange
   * @param routingKey   routing key
   * @param message      message content
   *
   * @return CompletableFuture indicating success/failure
   */
  public CompletableFuture<Boolean> publishToExchange(String exchangeName, String routingKey, String message) {
    return publisher.publishToExchange(exchangeName, routingKey, message);
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
    return publisher.publishToExchange(exchangeName, routingKey, message, exchangeType, persistent);
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
    return publisher.publishWithHeaders(exchangeName, routingKey, message, headers);
  }

  /**
   * Broadcasts a message to all consumers (fanout pattern)
   *
   * @param exchangeName name of the fanout exchange
   * @param message      message content
   *
   * @return CompletableFuture indicating success/failure
   */
  public CompletableFuture<Boolean> broadcast(String exchangeName, String message) {
    return publisher.broadcast(exchangeName, message);
  }

  // ========== Consuming Methods ==========

  /**
   * Starts consuming messages from a queue
   *
   * @param queueName name of the queue
   * @param listener  callback for handling messages
   *
   * @return true if consumer started successfully
   */
  public boolean consumeQueue(String queueName, MessageListener listener) {
    return consumer.consume(queueName, listener, false);
  }

  /**
   * Starts consuming messages from a queue with auto-acknowledgment
   *
   * @param queueName name of the queue
   * @param listener  callback for handling messages
   * @param autoAck   whether to automatically acknowledge messages
   *
   * @return true if consumer started successfully
   */
  public boolean consumeQueue(String queueName, MessageListener listener, boolean autoAck) {
    return consumer.consume(queueName, listener, autoAck);
  }

  /**
   * Starts consuming messages from a queue with sync option
   *
   * @param queueName        name of the queue
   * @param listener         callback for handling messages
   * @param autoAck          whether to automatically acknowledge messages
   * @param syncToMainThread whether to execute listener on main Minecraft thread
   *
   * @return true if consumer started successfully
   */
  public boolean consumeQueue(String queueName, MessageListener listener, boolean autoAck, boolean syncToMainThread) {
    return consumer.consume(queueName, listener, autoAck, syncToMainThread);
  }

  /**
   * Subscribes to a broadcast exchange (fanout pattern)
   *
   * @param exchangeName name of the exchange
   * @param listener     callback for handling messages
   *
   * @return true if subscription successful
   */
  public boolean subscribeToBroadcast(String exchangeName, MessageListener listener) {
    return consumer.subscribeToBroadcast(exchangeName, listener);
  }

  /**
   * Subscribes to a broadcast exchange with sync option
   *
   * @param exchangeName     name of the exchange
   * @param listener         callback for handling messages
   * @param syncToMainThread whether to execute listener on main thread
   *
   * @return true if subscription successful
   */
  public boolean subscribeToBroadcast(String exchangeName, MessageListener listener, boolean syncToMainThread) {
    return consumer.subscribeToBroadcast(exchangeName, listener, syncToMainThread);
  }

  /**
   * Subscribes to a topic exchange with pattern matching
   *
   * @param exchangeName      name of the topic exchange
   * @param routingKeyPattern routing key pattern (e.g., "server.*.events")
   * @param listener          callback for handling messages
   *
   * @return true if subscription successful
   */
  public boolean subscribeToTopic(String exchangeName, String routingKeyPattern, MessageListener listener) {
    return consumer.subscribeToTopic(exchangeName, routingKeyPattern, listener);
  }

  /**
   * Subscribes to a topic exchange with pattern matching and sync option
   *
   * @param exchangeName      name of the topic exchange
   * @param routingKeyPattern routing key pattern (e.g., "server.*.events")
   * @param listener          callback for handling messages
   * @param syncToMainThread  whether to execute listener on main thread
   *
   * @return true if subscription successful
   */
  public boolean subscribeToTopic(String exchangeName,
    String routingKeyPattern,
    MessageListener listener,
    boolean syncToMainThread) {
    return consumer.subscribeToTopic(exchangeName, routingKeyPattern, listener, syncToMainThread);
  }

  // ========== Utility Methods ==========

  /**
   * Gets the connection manager
   *
   * @return RabbitMQConnectionManager instance
   */
  public RabbitMQConnectionManager getConnectionManager() {
    return connectionManager;
  }

  /**
   * Gets the publisher
   *
   * @return RabbitMQPublisher instance
   */
  public RabbitMQPublisher getPublisher() {
    return publisher;
  }

  /**
   * Gets the consumer
   *
   * @return RabbitMQConsumer instance
   */
  public RabbitMQConsumer getConsumer() {
    return consumer;
  }

  /**
   * Gets all active consumers
   *
   * @return map of queue/exchange names to consumer tags
   */
  public Map<String, String> getActiveConsumers() {
    return consumer.getActiveConsumers();
  }
}
