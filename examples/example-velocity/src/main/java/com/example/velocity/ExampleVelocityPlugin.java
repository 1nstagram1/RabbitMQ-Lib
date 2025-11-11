package com.example.velocity;

import com.google.inject.Inject;
import io.hydrogendevelopments.celesmq.RabbitMQManager;
import io.hydrogendevelopments.celesmq.config.RabbitMQConfig;
import io.hydrogendevelopments.celesmq.event.EventBus;
import io.hydrogendevelopments.celesmq.platform.VelocityPlatform;
import io.hydrogendevelopments.celesmq.rpc.RPC;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Example Velocity plugin demonstrating RabbitMQ cross-server communication
 *
 * This example shows:
 * - Connecting Velocity proxy to RabbitMQ
 * - Listening for events from Spigot servers
 * - Calling RPC methods on Spigot servers
 * - Broadcasting messages to all connected servers
 */
@Plugin(
        id = "example-velocity-plugin",
        name = "Example Velocity Plugin",
        version = "1.0.0",
        description = "Example Velocity plugin demonstrating RabbitMQ cross-server communication",
        authors = {"1nstagram"}
)
public class ExampleVelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private RabbitMQManager mq;
    private EventBus eventBus;
    private RPC rpc;
    private Properties config;

    @Inject
    public ExampleVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Load configuration
        loadConfig();

        // Initialize RabbitMQ connection
        if (!initializeRabbitMQ()) {
            logger.error("Failed to initialize RabbitMQ connection. Plugin disabled.");
            return;
        }

        // Setup event listeners
        setupEvents();

        // Setup RPC
        setupRPC();

        logger.info("ExampleVelocity plugin enabled successfully!");
        logger.info("Listening for events from Spigot servers...");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (mq != null) {
            mq.disconnect();
            logger.info("Disconnected from RabbitMQ");
        }
    }

    private void loadConfig() {
        try {
            // Create plugin folder if it doesn't exist
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            Path configFile = dataDirectory.resolve("config.properties");

            // Copy default config if it doesn't exist
            if (!Files.exists(configFile)) {
                try (InputStream in = getClass().getResourceAsStream("/config.properties")) {
                    Files.copy(in, configFile);
                }
            }

            // Load properties
            config = new Properties();
            try (InputStream in = Files.newInputStream(configFile)) {
                config.load(in);
            }

        } catch (IOException e) {
            logger.error("Failed to load config: " + e.getMessage());
        }
    }

    private boolean initializeRabbitMQ() {
        try {
            // Build RabbitMQ configuration with explicit parameters
            RabbitMQConfig rabbitConfig = new RabbitMQConfig.Builder()
                    .host(config.getProperty("rabbitmq.host"))
                    .port(Integer.parseInt(config.getProperty("rabbitmq.port")))
                    .username(config.getProperty("rabbitmq.username"))
                    .password(config.getProperty("rabbitmq.password"))
                    .virtualHost(config.getProperty("rabbitmq.virtualHost"))
                    .connectionTimeout(Integer.parseInt(config.getProperty("rabbitmq.connectionTimeout")))
                    .networkRecoveryInterval(Integer.parseInt(config.getProperty("rabbitmq.networkRecoveryInterval")))
                    .automaticRecoveryEnabled(Boolean.parseBoolean(config.getProperty("rabbitmq.automaticRecoveryEnabled")))
                    .consumerName(config.getProperty("rabbitmq.consumer"))
                    .autoSubscribe(false)
                    .build();

            // Create RabbitMQ manager with Velocity platform
            mq = new RabbitMQManager(
                    new VelocityPlatform(server, logger),
                    rabbitConfig
            );

            logger.info("Successfully connected to RabbitMQ!");
            return true;

        } catch (Exception e) {
            logger.error("Failed to connect to RabbitMQ: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void setupEvents() {
        // Create EventBus with custom exchange and action names
        // Set ignoreOwnEvents to true since we only want to receive events from Spigot servers
        eventBus = new EventBus(
                mq,
                config.getProperty("rabbitmq.consumer"),
                "network-events",
                "network_event",
                true
        );

        // Listen for player join events from Spigot servers
        eventBus.on("player_join", event -> {
            String playerName = (String) event.getData("playerName");
            String serverName = event.getSourceServer();

            logger.info("Player {} joined server {}", playerName, serverName);

            // You could do something here like:
            // - Update a database
            // - Send a welcome message
            // - Trigger other cross-server events
        });

        // Listen for player quit events
        eventBus.on("player_quit", event -> {
            String playerName = (String) event.getData("playerName");
            String serverName = event.getSourceServer();

            logger.info("Player {} left server {}", playerName, serverName);
        });

        // Listen for custom events
        eventBus.on("server_status", event -> {
            String serverName = event.getSourceServer();
            int playerCount = ((Number) event.getData("playerCount")).intValue();
            String status = (String) event.getData("status");

            logger.info("Server {} status: {} ({} players)", serverName, status, playerCount);
        });

        logger.info("Event listeners registered successfully");
    }

    private void setupRPC() {
        // Create RPC with custom action names
        rpc = new RPC(mq, "network_rpc_call", "network_rpc_response");

        // Register RPC procedures that Spigot servers can call
        rpc.register("getProxyPlayerCount", req -> server.getPlayerCount());

        rpc.register("getServerList", req -> {
            return server.getAllServers().stream()
                    .map(s -> s.getServerInfo().getName())
                    .toList().toString();
        });

        rpc.register("broadcastMessage", req -> {
            String message = req.getString("message");
            server.getAllPlayers().forEach(player ->
                    player.sendMessage(net.kyori.adventure.text.Component.text(message)));
            return "Broadcast sent";
        });

        logger.info("RPC procedures registered: getProxyPlayerCount, getServerList, broadcastMessage");
    }

    /**
     * Example: Call RPC on a Spigot server to get player count
     */
    public void getSpigotServerPlayerCount(String serverName) {
        rpc.call(serverName, "getOnlineCount")
                .timeout(5000)
                .execute()
                .thenAccept(response -> {
                    if (response.isSuccess()) {
                        int playerCount = response.getResultAsInt();
                        logger.info("Server {} has {} players online", serverName, playerCount);
                    } else {
                        logger.warn("RPC call failed: {}", response.getError());
                    }
                });
    }

    /**
     * Example: Broadcast a network-wide event
     */
    public void broadcastNetworkEvent(String eventType, String message) {
        EventBus.GenericCrossServerEvent event = new EventBus.GenericCrossServerEvent(
                config.getProperty("rabbitmq.consumer"),
                eventType,
                new com.google.gson.JsonObject()
        );
        event.setData("message", message);
        event.setData("timestamp", System.currentTimeMillis());

        eventBus.dispatch(event);
    }

    public RabbitMQManager getRabbitMQ() {
        return mq;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public RPC getRpc() {
        return rpc;
    }
}
