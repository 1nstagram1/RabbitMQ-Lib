package com.example.bungeecord;

import io.hydrogendevelopments.celesmq.RabbitMQManager;
import io.hydrogendevelopments.celesmq.config.RabbitMQConfig;
import io.hydrogendevelopments.celesmq.event.EventBus;
import io.hydrogendevelopments.celesmq.platform.BungeeCordPlatform;
import io.hydrogendevelopments.celesmq.rpc.RPC;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Example BungeeCord plugin demonstrating RabbitMQ cross-server communication
 *
 * This example shows:
 * - Connecting BungeeCord proxy to RabbitMQ
 * - Listening for events from Spigot servers
 * - Calling RPC methods on Spigot servers
 * - Broadcasting messages to all connected servers
 */
public class ExampleBungeeCordPlugin extends Plugin {

    private RabbitMQManager mq;
    private EventBus eventBus;
    private RPC rpc;
    private Configuration config;

    @Override
    public void onEnable() {
        // Load configuration
        loadConfig();

        // Initialize RabbitMQ connection
        if (!initializeRabbitMQ()) {
            getLogger().severe("Failed to initialize RabbitMQ connection. Plugin disabled.");
            return;
        }

        // Setup event listeners
        setupEvents();

        // Setup RPC
        setupRPC();

        getLogger().info("ExampleBungeeCord plugin enabled successfully!");
        getLogger().info("Listening for events from Spigot servers...");
    }

    @Override
    public void onDisable() {
        if (mq != null) {
            mq.disconnect();
            getLogger().info("Disconnected from RabbitMQ");
        }
    }

    private void loadConfig() {
        try {
            // Create plugin folder if it doesn't exist
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }

            File configFile = new File(getDataFolder(), "config.yml");

            // Copy default config if it doesn't exist
            if (!configFile.exists()) {
                try (InputStream in = getResourceAsStream("config.yml")) {
                    Files.copy(in, configFile.toPath());
                }
            }

            config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(configFile);

        } catch (IOException e) {
            getLogger().severe("Failed to load config: " + e.getMessage());
        }
    }

    private boolean initializeRabbitMQ() {
        try {
            // Build RabbitMQ configuration with explicit parameters
            RabbitMQConfig rabbitConfig = new RabbitMQConfig.Builder()
                    .host(config.getString("rabbitmq.host"))
                    .port(config.getInt("rabbitmq.port"))
                    .username(config.getString("rabbitmq.username"))
                    .password(config.getString("rabbitmq.password"))
                    .virtualHost(config.getString("rabbitmq.virtualHost"))
                    .connectionTimeout(config.getInt("rabbitmq.connectionTimeout"))
                    .networkRecoveryInterval(config.getInt("rabbitmq.networkRecoveryInterval"))
                    .automaticRecoveryEnabled(config.getBoolean("rabbitmq.automaticRecoveryEnabled"))
                    .consumerName(config.getString("rabbitmq.consumer"))
                    .autoSubscribe(false)
                    .build();

            // Create RabbitMQ manager with BungeeCord platform
            mq = new RabbitMQManager(
                    new BungeeCordPlatform(this, getProxy()),
                    rabbitConfig
            );

            getLogger().info("Successfully connected to RabbitMQ!");
            return true;

        } catch (Exception e) {
            getLogger().severe("Failed to connect to RabbitMQ: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void setupEvents() {
        // Create EventBus with custom exchange and action names
        // Set ignoreOwnEvents to true since we only want to receive events from Spigot servers
        eventBus = new EventBus(
                mq,
                config.getString("rabbitmq.consumer"),
                "network-events",
                "network_event",
                true
        );

        // Listen for player join events from Spigot servers
        eventBus.on("player_join", event -> {
            String playerName = (String) event.getData("playerName");
            String serverName = event.getSourceServer();

            getLogger().info("Player " + playerName + " joined server " + serverName);

            // You could do something here like:
            // - Update a database
            // - Send a welcome message
            // - Trigger other cross-server events
        });

        // Listen for player quit events
        eventBus.on("player_quit", event -> {
            String playerName = (String) event.getData("playerName");
            String serverName = event.getSourceServer();

            getLogger().info("Player " + playerName + " left server " + serverName);
        });

        // Listen for custom events
        eventBus.on("server_status", event -> {
            String serverName = event.getSourceServer();
            int playerCount = ((Number) event.getData("playerCount")).intValue();
            String status = (String) event.getData("status");

            getLogger().info("Server " + serverName + " status: " + status + " (" + playerCount + " players)");
        });

        getLogger().info("Event listeners registered successfully");
    }

    private void setupRPC() {
        // Create RPC with custom action names
        rpc = new RPC(mq, "network_rpc_call", "network_rpc_response");

        // Register RPC procedures that Spigot servers can call
        rpc.register("getProxyPlayerCount", req -> getProxy().getOnlineCount());

        rpc.register("getServerList", req -> {
            return getProxy().getServers().keySet().toString();
        });

        rpc.register("broadcastMessage", req -> {
            String message = req.getString("message");
            getProxy().broadcast(message);
            return "Broadcast sent";
        });

        getLogger().info("RPC procedures registered: getProxyPlayerCount, getServerList, broadcastMessage");
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
                        getLogger().info("Server " + serverName + " has " + playerCount + " players online");
                    } else {
                        getLogger().warning("RPC call failed: " + response.getError());
                    }
                });
    }

    /**
     * Example: Broadcast a network-wide event
     */
    public void broadcastNetworkEvent(String eventType, String message) {
        EventBus.GenericCrossServerEvent event = new EventBus.GenericCrossServerEvent(
                config.getString("rabbitmq.consumer"),
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
