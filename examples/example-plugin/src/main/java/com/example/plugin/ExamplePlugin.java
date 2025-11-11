package com.example.plugin;

import io.hydrogendevelopments.celesmq.RabbitMQClient;
import io.hydrogendevelopments.celesmq.config.RabbitMQConfig;
import io.hydrogendevelopments.celesmq.util.JsonSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Example plugin demonstrating RabbitMQ library usage
 */
public class ExamplePlugin extends JavaPlugin implements Listener {

    private RabbitMQClient rabbitMQClient;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize RabbitMQ configuration from config.yml
        RabbitMQConfig config = new RabbitMQConfig.Builder()
                .host(getConfig().getString("rabbitmq.host", "localhost"))
                .port(getConfig().getInt("rabbitmq.port", 5672))
                .username(getConfig().getString("rabbitmq.username", "guest"))
                .password(getConfig().getString("rabbitmq.password", "guest"))
                .virtualHost(getConfig().getString("rabbitmq.virtualHost", "/"))
                .connectionTimeout(getConfig().getInt("rabbitmq.connectionTimeout", 10000))
                .networkRecoveryInterval(getConfig().getInt("rabbitmq.networkRecoveryInterval", 5000))
                .automaticRecoveryEnabled(getConfig().getBoolean("rabbitmq.automaticRecoveryEnabled", true))
                .build();

        // Create RabbitMQ client
        rabbitMQClient = new RabbitMQClient(this, config);

        // Connect to RabbitMQ
        if (rabbitMQClient.connect()) {
            getLogger().info("Successfully connected to RabbitMQ!");

            // Register event listeners
            Bukkit.getPluginManager().registerEvents(this, this);

            // Example: Subscribe to player events from other servers
            rabbitMQClient.subscribeToBroadcast("player.events", this::handlePlayerEvent, true);

            // Example: Subscribe to chat messages with topic pattern
            rabbitMQClient.subscribeToTopic("chat.exchange", "chat.#", this::handleChatMessage, true);

            // Example: Consume work queue
            rabbitMQClient.consumeQueue("player.tasks", this::handlePlayerTask, false, true);

        } else {
            getLogger().severe("Failed to connect to RabbitMQ!");
        }
    }

    @Override
    public void onDisable() {
        if (rabbitMQClient != null) {
            rabbitMQClient.disconnect();
            getLogger().info("Disconnected from RabbitMQ");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Create player join event message
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("event", "player_join");
        eventData.put("player", player.getName());
        eventData.put("uuid", player.getUniqueId().toString());
        eventData.put("server", getConfig().getString("server.name", "lobby"));
        eventData.put("timestamp", System.currentTimeMillis());

        // Publish to broadcast exchange
        String json = JsonSerializer.toJson(eventData);
        rabbitMQClient.broadcast("player.events", json)
                .thenAccept(success -> {
                    if (success) {
                        getLogger().info("Broadcasted player join event for " + player.getName());
                    }
                });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Create player quit event message
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("event", "player_quit");
        eventData.put("player", player.getName());
        eventData.put("uuid", player.getUniqueId().toString());
        eventData.put("server", getConfig().getString("server.name", "lobby"));
        eventData.put("timestamp", System.currentTimeMillis());

        // Publish to broadcast exchange
        String json = JsonSerializer.toJson(eventData);
        rabbitMQClient.broadcast("player.events", json)
                .thenAccept(success -> {
                    if (success) {
                        getLogger().info("Broadcasted player quit event for " + player.getName());
                    }
                });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("announce")) {
            if (args.length == 0) {
                sender.sendMessage("Usage: /announce <message>");
                return true;
            }

            String message = String.join(" ", args);

            // Create announcement message
            Map<String, Object> announcement = new HashMap<>();
            announcement.put("type", "announcement");
            announcement.put("message", message);
            announcement.put("sender", sender.getName());
            announcement.put("server", getConfig().getString("server.name", "lobby"));
            announcement.put("timestamp", System.currentTimeMillis());

            // Broadcast announcement to all servers
            String json = JsonSerializer.toJson(announcement);
            rabbitMQClient.broadcast("server.announcements", json)
                    .thenAccept(success -> {
                        if (success) {
                            sender.sendMessage("Announcement sent to all servers!");
                        } else {
                            sender.sendMessage("Failed to send announcement!");
                        }
                    });

            return true;
        }

        if (command.getName().equalsIgnoreCase("rabbitmq")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("status")) {
                sender.sendMessage("RabbitMQ Status: " +
                        (rabbitMQClient.isConnected() ? "Connected" : "Disconnected"));
                sender.sendMessage("Active Consumers: " + rabbitMQClient.getActiveConsumers().size());
                return true;
            }
        }

        return false;
    }

    // Handler for player events from other servers
    private void handlePlayerEvent(String message) {
        Map<String, Object> eventData = JsonSerializer.fromJson(message, Map.class);
        if (eventData != null) {
            String event = (String) eventData.get("event");
            String player = (String) eventData.get("player");
            String server = (String) eventData.get("server");

            getLogger().info("Received player event: " + event + " - " + player + " on " + server);

            // Broadcast to online players
            String broadcastMessage = String.format("[%s] %s %s",
                    server,
                    player,
                    event.equals("player_join") ? "joined" : "left");

            Bukkit.broadcastMessage(broadcastMessage);
        }
    }

    // Handler for chat messages
    private void handleChatMessage(String message) {
        Map<String, Object> chatData = JsonSerializer.fromJson(message, Map.class);
        if (chatData != null) {
            String player = (String) chatData.get("player");
            String chatMessage = (String) chatData.get("message");
            String server = (String) chatData.get("server");

            // Display cross-server chat
            String formattedMessage = String.format("[%s] <%s> %s", server, player, chatMessage);
            Bukkit.broadcastMessage(formattedMessage);
        }
    }

    // Handler for player tasks
    private void handlePlayerTask(String message) {
        Map<String, Object> task = JsonSerializer.fromJson(message, Map.class);
        if (task != null) {
            String taskType = (String) task.get("type");
            String playerName = (String) task.get("player");

            getLogger().info("Processing task: " + taskType + " for player: " + playerName);

            // Process different task types
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                switch (taskType) {
                    case "give_reward":
                        // Give player a reward
                        player.sendMessage("You received a reward!");
                        break;
                    case "kick":
                        String reason = (String) task.get("reason");
                        player.kickPlayer(reason != null ? reason : "Kicked by admin");
                        break;
                    case "teleport":
                        // Handle teleport request
                        player.sendMessage("Teleport request received!");
                        break;
                    default:
                        getLogger().warning("Unknown task type: " + taskType);
                }
            }
        }
    }
}
