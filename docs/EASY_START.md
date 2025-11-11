# Easy Start Guide - RabbitMQ Made Simple

This guide shows you how to use the super easy RabbitMQManager API that makes RabbitMQ simpler than Redis!

## Table of Contents
- [Quick Setup](#quick-setup)
- [Sending Messages](#sending-messages)
- [Receiving Messages with Actions](#receiving-messages-with-actions)
- [Request-Response Pattern](#request-response-pattern)
- [Type-Safe Message Handling](#type-safe-message-handling)
- [Complete Examples](#complete-examples)

## Quick Setup

### BungeeCord Example

```java
import io.hydrodevelopments.celesmq.RabbitMQManager;
import config.io.hydrodevelopments.celesmq.RabbitMQConfig;
import platform.io.hydrodevelopments.celesmq.BungeeCordPlatform;

public class MyBungeePlugin extends Plugin {
  private RabbitMQManager mq;

  @Override public void onEnable() {
    // Configure
    RabbitMQConfig config =
      new RabbitMQConfig.Builder().host("localhost").port(5672).username("guest").password("guest").build();

    // Initialize
    BungeeCordPlatform platform = new BungeeCordPlatform(this);
    mq = new RabbitMQManager(platform, config);

    // Connect and setup
    if (mq.connect()) {
      setupChannels();
      setupHandlers();
      getLogger().info("RabbitMQ ready!");
    }
  }

  private void setupChannels() {
    // Add channels for easy sending
    mq.addChannel("commands", "commands-exchange")
      .addChannel("updates", "updates-exchange")
      .addChannel("events", "events-exchange");
  }

  private void setupHandlers() {
    // Subscribe to messages
    mq.subscribe("commands-exchange");

    // Handle specific actions
    mq.on("player_join", msg -> {
      String player = msg.getString("player");
      String server = msg.getString("server");
      getLogger().info(player + " joined " + server);
    });

    mq.on("broadcast", msg -> {
      String message = msg.getString("message");
      getProxy().broadcast(message);
    });
  }

  @Override public void onDisable() {
    if (mq != null) {
      mq.disconnect();
    }
  }
}
```

## Sending Messages

### Simple Send

```java
// Send a simple message
mq.send("commands", "Hello World!");
```

### Send JSON Data

```java
// Send structured data
Map<String, Object> data = Map.of(
    "action", "player_join",
    "player", "Steve",
    "uuid", player.getUniqueId().toString(),
    "server", "server-1"
);
mq.sendJson("commands", data);
```

### Using MessageBuilder

```java
import message.io.hydrodevelopments.celesmq.MessageBuilder;

// Build complex messages easily
String message = MessageBuilder.create("alert")
  .add("player", player.getName())
  .add("level", 5)
  .add("premium", true)
  .add("metadata", Map.of("coins", 100, "rank", "VIP"))
  .build();

mq.

send("events",message);
```

## Receiving Messages with Actions

### Register Action Handlers

```java
// Handle player connections
mq.on("player_connect", msg -> {
    String player = msg.getString("player");
    int id = msg.getInt("id");
    boolean staff = msg.getBoolean("staff", false);

    getLogger().info("Player " + player + " (ID: " + id + ") connected");
    if (staff) {
        notifyStaff(player);
    }
});

// Handle staff messages
mq.on("staffchat", msg -> {
    String sender = msg.getString("player");
    String message = msg.getString("message");
    String server = msg.getString("server");

    broadcastToStaff("[" + server + "] " + sender + ": " + message);
});

// Handle kicks
mq.on("kick", msg -> {
    int playerId = msg.getInt("player_id");
    String reason = msg.getString("reason", "Kicked from server");

    User user = findUser(playerId);
    if (user != null) {
        user.disconnect(reason);
    }
});
```

### Default Handler

```java
// Handle all unmatched messages
mq.onDefault(msg -> {
    getLogger().info("Received: " + msg.getRawMessage());
});
```

### Error Handler

```java
// Handle errors
mq.onError(error -> {
    getLogger().severe("Message error: " + error.getMessage());
});
```

## Request-Response Pattern

Send requests and wait for responses with automatic timeout handling!

### Basic Request

```java
// Send request and wait for response
mq.request()
    .action("getPlayerData")
    .param("uuid", player.getUniqueId().toString())
    .sendTo("game-server")
    .thenAccept(response -> {
        if (response.isSuccess()) {
            int coins = response.getInt("coins");
            String rank = response.getString("rank");
            getLogger().info("Player has " + coins + " coins, rank: " + rank);
        }
    });
```

### Request with Timeout

```java
// Timeout is required - set it based on your needs
mq.request()
    .action("getServer")
    .param("name", "server-1")
    .timeout(2000) // 2 seconds
    .sendTo("game-server")
    .thenAccept(response -> {
        if (response.getStatus().equals(ResponseStatus.TIMEOUT)) {
            getLogger().warning("Request timed out!");
            return;
        }

        boolean online = response.getBoolean("online");
        int players = response.getInt("player_count");
        getLogger().info("Server online: " + online + ", players: " + players);
    });
```

### Complex Request

```java
// Send complex request with multiple parameters
mq.request()
    .action("createParty")
    .param("leader", player.getName())
    .param("leader_id", playerId)
    .param("max_size", 8)
    .param("public", false)
    .sendTo("game-server")
    .thenAccept(response -> {
        String status = response.getString("status");
        if ("SUCCESS".equals(status)) {
            String partyId = response.getString("party_id");
            player.sendMessage("Party created! ID: " + partyId);
        } else {
            String error = response.getString("error", "Unknown error");
            player.sendMessage("Failed to create party: " + error);
        }
    })
    .exceptionally(throwable -> {
        getLogger().severe("Request failed: " + throwable.getMessage());
        return null;
    });
```

## Type-Safe Message Handling

### Extract Data Safely

```java
mq.on("player_update", msg -> {
    // Get values with defaults
    int id = msg.getInt("id", -1);
    String name = msg.getString("name", "Unknown");
    long timestamp = msg.getLong("timestamp", 0L);
    double balance = msg.getDouble("balance", 0.0);
    boolean online = msg.getBoolean("online", false);

    // Check if key exists
    if (msg.has("special_data")) {
        JsonObject special = msg.getObject("special_data");
        // Process special data
    }

    // Get arrays
    JsonArray servers = msg.getArray("servers");
    if (servers != null) {
        for (JsonElement server : servers) {
            getLogger().info("Server: " + server.getAsString());
        }
    }

    // Get nested objects as MessageResponse
    List<MessageResponse> history = msg.getResponseList("login_history");
    for (MessageResponse login : history) {
        String server = login.getString("server");
        long time = login.getLong("timestamp");
        getLogger().info("Login: " + server + " at " + time);
    }
});
```

## Complete Examples

### BungeeCord Staff Chat System

```java
public class StaffChatExample extends Plugin {
    private RabbitMQManager mq;

    @Override
    public void onEnable() {
        RabbitMQConfig config = new RabbitMQConfig.Builder()
            .host("localhost")
            .port(5672)
            .username("guest")
            .password("guest")
            .build();

        mq = new RabbitMQManager(new BungeeCordPlatform(this), config);

        if (mq.connect()) {
            mq.addChannel("staff", "staff-exchange")
               .subscribe("staff-exchange")
               .on("staffchat", this::handleStaffChat)
               .on("staff_join", this::handleStaffJoin);
        }
    }

    private void handleStaffChat(MessageResponse msg) {
        String player = msg.getString("player");
        String message = msg.getString("message");
        String server = msg.getString("server");

        String formatted = ChatColor.GRAY + "[" + ChatColor.RED + "Staff" +
                          ChatColor.GRAY + "] " + ChatColor.AQUA + player +
                          ChatColor.GRAY + " @ " + server + ": " +
                          ChatColor.WHITE + message;

        for (ProxiedPlayer staff : getProxy().getPlayers()) {
            if (staff.hasPermission("staff.chat")) {
                staff.sendMessage(formatted);
            }
        }
    }

    private void handleStaffJoin(MessageResponse msg) {
        String player = msg.getString("player");
        String server = msg.getString("server");

        for (ProxiedPlayer staff : getProxy().getPlayers()) {
            if (staff.hasPermission("staff.notify")) {
                staff.sendMessage(ChatColor.GREEN + player + " joined " + server);
            }
        }
    }

    public void sendStaffMessage(String player, String message, String server) {
        String msg = MessageBuilder.create("staffchat")
            .add("player", player)
            .add("message", message)
            .add("server", server)
            .add("timestamp", System.currentTimeMillis())
            .build();

        mq.send("staff", msg);
    }
}
```

### Player Data Request Example

```java
public void loadPlayerData(ProxiedPlayer player) {
    mq.request()
        .action("getPlayerData")
        .param("uuid", player.getUniqueId().toString())
        .param("name", player.getName())
        .timeout(3000)
        .sendTo("game-server")
        .thenAccept(response -> {
            if (!response.isSuccess()) {
                player.sendMessage(ChatColor.RED + "Failed to load your data!");
                return;
            }

            // Extract player data
            int coins = response.getInt("coins");
            String rank = response.getString("rank", "DEFAULT");
            long lastLogin = response.getLong("last_login");
            List<MessageResponse> friends = response.getResponseList("friends");

            // Apply data
            player.sendMessage(ChatColor.GREEN + "Welcome back!");
            player.sendMessage(ChatColor.YELLOW + "Coins: " + coins);
            player.sendMessage(ChatColor.YELLOW + "Rank: " + rank);
            player.sendMessage(ChatColor.YELLOW + "Friends online: " +
                              friends.stream()
                                     .filter(f -> f.getBoolean("online"))
                                     .count());
        })
        .exceptionally(error -> {
            player.sendMessage(ChatColor.RED + "Error loading data!");
            getLogger().severe("Failed to load data for " + player.getName());
            return null;
        });
}
```

### Cross-Server Announcement

```java
public void announceToAll(String message) {
    String msg = MessageBuilder.create("announcement")
        .add("message", message)
        .add("sender", "Console")
        .add("timestamp", System.currentTimeMillis())
        .add("priority", "HIGH")
        .build();

    // Send to all servers
    mq.send("updates", msg);
    mq.send("server-a", msg);
    mq.send("server-b", msg);
}
```

## Why This is Easier Than Redis

1. **Type-Safe Access**: No manual JSON parsing or type casting
2. **Built-in Request-Response**: Automatic callback and timeout handling
3. **Action Routing**: Clean, organized message handling
4. **Fluent API**: Chain methods for readable code
5. **Platform Agnostic**: Works the same on all supported platforms (Spigot, BungeeCord, Velocity, Minestom, Sponge, Folia, NukkitX)
6. **Error Handling**: Built-in error handlers and timeouts
7. **Zero Boilerplate**: No manual connection management or channel setup

RabbitMQ made simple! Get started today and streamline your server communication.
