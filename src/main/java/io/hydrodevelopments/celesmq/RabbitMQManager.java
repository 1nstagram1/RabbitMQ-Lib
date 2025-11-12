package io.hydrodevelopments.celesmq;

import io.hydrodevelopments.celesmq.config.RabbitMQConfig;
import io.hydrodevelopments.celesmq.message.MessagePublisher;
import io.hydrodevelopments.celesmq.message.MessageRouter;
import io.hydrodevelopments.celesmq.message.MessageResponse;
import io.hydrodevelopments.celesmq.message.MessageRequest;
import io.hydrodevelopments.celesmq.platform.Platform;
import io.hydrodevelopments.celesmq.util.JsonSerializer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * High-level manager for RabbitMQ that provides an easy-to-use API
 * Handles multiple channels, routing, and request-response patterns automatically
 */
public class RabbitMQManager {
    private final Platform platform;
    private final RabbitMQClient client;
    private final MessageRouter router;
    private final RabbitMQConfig config;
    private final String replyQueue;
    private final Map<String, String> channels = new HashMap<>();
    private final Set<String> directChannels = new HashSet<>();

    /**
     * Creates a new RabbitMQManager
     * @param platform the platform instance
     * @param config RabbitMQ configuration
     */
    public RabbitMQManager(Platform platform, RabbitMQConfig config) {
        this.platform = platform;
        this.config = config;
        this.client = new RabbitMQClient(platform, config);
        this.router = new MessageRouter(platform.getLogger());

        // Use configured consumer name or generate one
        this.replyQueue = config.getConsumerName() != null ?
                config.getConsumerName() :
                "reply-" + UUID.randomUUID().toString().substring(0, 8);

        // Load channels from config
        this.channels.putAll(config.getChannels());
        this.directChannels.addAll(config.getDirectChannels());
    }

    /**
     * Connects to RabbitMQ and sets up the reply queue consumer
     * @return true if connection successful
     */
    public boolean connect() {
        if (!client.connect()) {
            return false;
        }

        // Set up reply queue consumer for request-response pattern
        client.consumeQueue(replyQueue, message -> {
            router.route(message);
        }, true, false);

        platform.getLogger().info("RabbitMQManager connected with consumer: " + replyQueue);

        // Log configured channels
        if (!channels.isEmpty()) {
            platform.getLogger().info("Loaded " + channels.size() + " exchange-based channel(s)");
            channels.forEach((name, exchange) ->
                    platform.getLogger().info("  - " + name + " -> " + exchange + " (exchange)")
            );
        }

        if (!directChannels.isEmpty()) {
            platform.getLogger().info("Loaded " + directChannels.size() + " direct queue channel(s)");
            directChannels.forEach(queue ->
                    platform.getLogger().info("  - " + queue + " (direct queue)")
            );
        }

        // Auto-subscribe to configured channels if enabled
        if (config.isAutoSubscribe()) {
            // Subscribe to exchange-based channels
            for (Map.Entry<String, String> entry : channels.entrySet()) {
                String exchange = entry.getValue();
                subscribe(exchange);
                platform.getLogger().info("Auto-subscribed to exchange: " + exchange);
            }

            // Subscribe to direct queue channels
            for (String queue : directChannels) {
                client.consumeQueue(queue, message -> {
                    router.route(message);
                }, true, false);
                platform.getLogger().info("Auto-subscribed to direct queue: " + queue);
            }
        }

        return true;
    }

    /**
     * Disconnects from RabbitMQ
     */
    public void disconnect() {
        client.disconnect();
    }

    /**
     * Registers a direct queue channel (no exchange, direct routing)
     * Compatible with old RabbitMQ pattern.
     *
     * @param queueName queue name
     * @return this manager for chaining
     */
    public RabbitMQManager addChannel(String queueName) {
        directChannels.add(queueName);
        platform.getLogger().info("Added direct queue channel: " + queueName);
        return this;
    }

    /**
     * Registers an exchange-based channel for broadcast/fanout routing
     * @param name channel name
     * @param exchange exchange name
     * @return this manager for chaining
     */
    public RabbitMQManager addChannel(String name, String exchange) {
        channels.put(name, exchange);
        platform.getLogger().info("Added exchange channel: " + name + " -> " + exchange);
        return this;
    }

    /**
     * Sends a message to a registered channel (supports both direct queue and exchange routing)
     * @param channel channel name
     * @param message message to send
     * @return CompletableFuture indicating success
     */
    public CompletableFuture<Boolean> send(String channel, String message) {
        // Check if it's a direct queue channel
        if (directChannels.contains(channel)) {
            return client.publishToQueue(channel, message);
        }

        // Check if it's an exchange-based channel
        String exchange = channels.get(channel);
        if (exchange != null) {
            return client.broadcast(exchange, message);
        }

        // Channel not found
        platform.getLogger().warning("Unknown channel: " + channel);
        return CompletableFuture.completedFuture(false);
    }

    /**
     * Sends a JSON message to a registered channel
     * @param channel channel name
     * @param data map of data to send as JSON
     * @return CompletableFuture indicating success
     */
    public CompletableFuture<Boolean> sendJson(String channel, Map<String, Object> data) {
        return send(channel, JsonSerializer.toJson(data));
    }

    /**
     * Creates a fluent message publisher for sending messages with a builder pattern
     * @return new MessagePublisher builder
     */
    public MessagePublisher publish() {
        return new MessagePublisher(this::sendJson);
    }

    /**
     * Creates a request builder for request-response pattern
     * @return new MessageRequest builder
     */
    public MessageRequest request() {
        return MessageRequest.create(client, platform.getLogger(), replyQueue, this::send);
    }

    /**
     * Subscribes to a channel with a consumer
     * @param channel channel name (will be created as exchange)
     * @param syncToMainThread whether to sync to main thread
     * @return this manager for chaining
     */
    public RabbitMQManager subscribe(String channel, boolean syncToMainThread) {
        client.subscribeToBroadcast(channel, message -> {
            router.route(message);
        }, syncToMainThread);
        return this;
    }

    /**
     * Subscribes to a channel with default async behavior
     * @param channel channel name (will be created as exchange)
     * @return this manager for chaining
     */
    public RabbitMQManager subscribe(String channel) {
        return subscribe(channel, false);
    }

    /**
     * Subscribes to a topic exchange with pattern
     * @param exchange exchange name
     * @param pattern routing key pattern
     * @param syncToMainThread whether to sync to main thread
     * @return this manager for chaining
     */
    public RabbitMQManager subscribeTopic(String exchange, String pattern, boolean syncToMainThread) {
        client.subscribeToTopic(exchange, pattern, message -> {
            router.route(message);
        }, syncToMainThread);
        return this;
    }

    /**
     * Subscribes to a topic exchange with default async behavior
     * @param exchange exchange name
     * @param pattern routing key pattern
     * @return this manager for chaining
     */
    public RabbitMQManager subscribeTopic(String exchange, String pattern) {
        return subscribeTopic(exchange, pattern, false);
    }

    /**
     * Registers a handler for a specific action
     * @param action action name
     * @param handler handler function
     * @return this manager for chaining
     */
    public RabbitMQManager on(String action, java.util.function.Consumer<MessageResponse> handler) {
        router.on(action, handler);
        return this;
    }

    /**
     * Registers a default handler for unmatched actions
     * @param handler handler function
     * @return this manager for chaining
     */
    public RabbitMQManager onDefault(java.util.function.Consumer<MessageResponse> handler) {
        router.onDefault(handler);
        return this;
    }

    /**
     * Registers an error handler
     * @param handler error handler function
     * @return this manager for chaining
     */
    public RabbitMQManager onError(java.util.function.Consumer<Exception> handler) {
        router.onError(handler);
        return this;
    }

    /**
     * Gets the underlying RabbitMQClient
     * @return RabbitMQClient instance
     */
    public RabbitMQClient getClient() {
        return client;
    }

    /**
     * Gets the MessageRouter
     * @return MessageRouter instance
     */
    public MessageRouter getRouter() {
        return router;
    }

    /**
     * Gets the reply queue name
     * @return reply queue name
     */
    public String getReplyQueue() {
        return replyQueue;
    }

    /**
     * Checks if connected
     * @return true if connected
     */
    public boolean isConnected() {
        return client.isConnected();
    }
}