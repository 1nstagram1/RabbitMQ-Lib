# Advanced Features Guide

This guide covers the advanced features that make this library incredibly powerful for production environments.

## Table of Contents
- [RPC (Remote Procedure Calls)](#rpc-remote-procedure-calls)
- [Cross-Server Event System](#cross-server-event-system)
- [Message Middleware](#message-middleware)
- [Batch Operations](#batch-operations)
- [Metrics and Monitoring](#metrics-and-monitoring)

## RPC (Remote Procedure Calls)

Call methods on other servers as if they were local! No more manual request-response handling.

### Setup

```java
// Create RPC with your own action names
RPC rpc = new RPC(mq, "myapp_rpc_call", "myapp_rpc_response");

// Register procedures that can be called remotely
rpc.register("getPlayerCoins", request -> {
    String uuid = request.getString("uuid");
    // Query database or cache
    return playerCoins.get(uuid);
});

rpc.register("addCoins", request -> {
    String uuid = request.getString("uuid");
    int amount = request.getInt("amount");
    // Add coins logic
    playerCoins.put(uuid, playerCoins.get(uuid) + amount);
    return "success";
});

rpc.register("getOnlinePlayers", request -> {
    return ProxyServer.getInstance().getOnlineCount();
});
```

### Calling Remote Procedures

```java
// Call a remote procedure and get the result
rpc.call("events", "getPlayerCoins")
    .arg("uuid", player.getUniqueId().toString())
    .timeout(2000)
    .execute()
    .thenAccept(response -> {
        if (response.isSuccess()) {
            int coins = response.getResultAsInt();
            player.sendMessage("You have " + coins + " coins!");
        } else {
            player.sendMessage("Error: " + response.getError());
        }
    });

// Add coins on another server
rpc.call("events", "addCoins")
    .arg("uuid", player.getUniqueId().toString())
    .arg("amount", 100)
    .execute()
    .thenAccept(response -> {
        if (response.isSuccess()) {
            player.sendMessage("Added 100 coins!");
        }
    });

// Get data from multiple servers
CompletableFuture<Integer> lobby1 = rpc.call("server-1", "getOnlinePlayers")
    .execute()
    .thenApply(r -> r.getResultAsInt());

CompletableFuture<Integer> lobby2 = rpc.call("server-2", "getOnlinePlayers")
    .execute()
    .thenApply(r -> r.getResultAsInt());

CompletableFuture.allOf(lobby1, lobby2).thenRun(() -> {
    int total = lobby1.join() + lobby2.join();
    System.out.println("Total players: " + total);
});
```

## Cross-Server Event System

Platform-agnostic event system that works on all supported platforms (Spigot, BungeeCord, Velocity, Minestom, Sponge, Folia, NukkitX)!

### Setup

```java
// Create EventBus with your own exchange and action names
// Last parameter: true = ignore own events, false = process all events
EventBus eventBus = new EventBus(mq, "consumer-1", "myapp-events", "myapp_event", true);

// Listen for your custom events
eventBus.on("player_join", (CrossServerEvent event) -> {
    String player = (String) event.getData("playerName");
    String server = event.getSourceServer();
    getLogger().info(player + " joined the network on " + server);
});

eventBus.on("player_switch_server", (CrossServerEvent event) -> {
    String playerName = (String) event.getData("playerName");
    String fromServer = (String) event.getData("fromServer");
    String toServer = (String) event.getData("toServer");
    getLogger().info(playerName + " moved from " + fromServer + " to " + toServer);
});
```

### Dispatching Events

```java
// Create your own event class
public class PlayerJoinEvent extends CrossServerEvent {
    public PlayerJoinEvent(String source, UUID uuid, String name) {
        super(source);
        setData("playerUuid", uuid.toString());
        setData("playerName", name);
    }

    @Override
    public String getEventType() {
        return "player_join";
    }
}

// Dispatch the event
PlayerJoinEvent event = new PlayerJoinEvent(
    "server-1",
    player.getUniqueId(),
    player.getName()
);
eventBus.dispatch(event);

// Create a server switch event
public class PlayerSwitchEvent extends CrossServerEvent {
    public PlayerSwitchEvent(String source, UUID uuid, String name, String from, String to) {
        super(source);
        setData("playerUuid", uuid.toString());
        setData("playerName", name);
        setData("fromServer", from);
        setData("toServer", to);
    }

    @Override
    public String getEventType() {
        return "player_switch_server";
    }
}

// Dispatch server switch
PlayerSwitchEvent switchEvent = new PlayerSwitchEvent(
    "consumer-1",
    player.getUniqueId(),
    player.getName(),
    fromServer,
    toServer
);
eventBus.dispatch(switchEvent);
```

### Custom Events

```java
// Create your own event
public class CoinUpdateEvent extends CrossServerEvent {
    private final UUID playerUuid;
    private final int newBalance;

    public CoinUpdateEvent(String source, UUID uuid, int balance) {
        super(source);
        this.playerUuid = uuid;
        this.newBalance = balance;
        setData("playerUuid", uuid.toString());
        setData("newBalance", balance);
    }

    @Override
    public String getEventType() {
        return "coin_update";
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public int getNewBalance() { return newBalance; }
}

// Listen for it
eventBus.on("coin_update", (CoinUpdateEvent event) -> {
    updateScoreboard(event.getPlayerUuid(), event.getNewBalance());
});

// Dispatch it
eventBus.dispatch(new CoinUpdateEvent("shop-server", uuid, 1000));
```

## Message Middleware

Intercept and modify messages before they're sent or after they're received!

### Built-in Middleware

```java
MiddlewareChain middleware = new MiddlewareChain(logger);

// Add logging middleware
middleware.add(new MiddlewareChain.LoggingMiddleware(logger));

// Add filtering middleware
middleware.add(new MiddlewareChain.FilterMiddleware(msg ->
    !msg.contains("debug") // Filter out debug messages
));

// Add transformation middleware
middleware.add(new MiddlewareChain.TransformMiddleware(msg -> {
    // Add timestamp to every message
    return msg.replace("{", ",\"timestamp\":" + System.currentTimeMillis() + "}");
}));
```

### Custom Middleware

```java
// Create custom middleware
public class AuthMiddleware implements MessageMiddleware {
    private final String apiKey;

    public AuthMiddleware(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String beforeSend(String message) {
        // Add auth header to outgoing messages
        return message.replace("{", ",\"apiKey\":\"" + apiKey + "\"}");
    }

    @Override
    public MessageResponse afterReceive(MessageResponse message) {
        // Validate incoming messages
        String key = message.getString("apiKey");
        if (!apiKey.equals(key)) {
            logger.warning("Invalid API key in message!");
            return null; // Cancel processing
        }
        return message;
    }

    @Override
    public int getPriority() {
        return 1000; // Run first
    }
}

middleware.add(new AuthMiddleware("my-secret-key"));
```

### Rate Limiting Middleware

```java
public class RateLimitMiddleware implements MessageMiddleware {
    private final Map<String, Long> lastSent = new ConcurrentHashMap<>();
    private final long minInterval = 100; // 100ms between messages

    @Override
    public String beforeSend(String message) {
        String key = extractKey(message); // Your logic
        long now = System.currentTimeMillis();
        Long last = lastSent.get(key);

        if (last != null && (now - last) < minInterval) {
            return null; // Rate limited, cancel
        }

        lastSent.put(key, now);
        return message;
    }

    @Override
    public MessageResponse afterReceive(MessageResponse message) {
        return message; // No filtering on receive
    }
}
```

## Batch Operations

Batch multiple messages together for improved performance and reduced network overhead.

### Setup

```java
BatchPublisher batch = new BatchPublisher(mq.getClient(), logger)
    .batchSize(100) // Send after 100 messages
    .flushInterval(1000) // Or every 1 second
    .start();
```

### Using Batch Publisher

```java
// Add messages to batch
batch.add("commands-exchange", "{\"action\":\"update\",\"data\":1}")
     .thenAccept(success -> {
         if (success) {
             logger.info("Message sent in batch!");
         }
     });

// Batch sends automatically when:
// 1. Batch size reached (100 messages)
// 2. Flush interval elapsed (1 second)
// 3. Manual flush called

// Manual flush
batch.flush();

// Cleanup
batch.stop(); // Flushes remaining messages and stops the batch publisher
```

### Real-World Example: Bulk Player Updates

```java
// Efficiently update 1000 players
for (ProxiedPlayer player : getProxy().getPlayers()) {
    String message = MessageBuilder.create("player_update")
        .add("uuid", player.getUniqueId().toString())
        .add("name", player.getName())
        .add("server", player.getServer().getInfo().getName())
        .build();

    batch.add("events-exchange", message);
}
// All messages sent efficiently in batches!
```

## Metrics and Monitoring

Track performance and health of your RabbitMQ operations.

### Setup

```java
RabbitMQMetrics metrics = new RabbitMQMetrics();

// Integrate with your message handling
mq.on("*", msg -> {
    long start = System.currentTimeMillis();
    metrics.recordReceived(msg.getRawMessage().length());

    try {
        // Process message
        handleMessage(msg);
        metrics.recordProcessed(System.currentTimeMillis() - start);
    } catch (Exception e) {
        metrics.recordFailed();
    }
});
```

### Viewing Metrics

```java
// Get summary
System.out.println(metrics.getSummary());

// Output:
// RabbitMQ Metrics:
//   Messages Sent: 1523
//   Messages Received: 1487
//   Messages Processed: 1480
//   Messages Failed: 7
//   Success Rate: 99.53%
//   Average Processing Time: 12.45 ms
//   Peak Processing Time: 245 ms
//   Messages/Second: 24.78
//   Throughput: 128.5 KB/s
//   Uptime: 60 seconds

// Individual metrics
long received = metrics.getMessagesReceived();
double avgTime = metrics.getAverageProcessingTime();
double successRate = metrics.getSuccessRate();
```

### Metrics Dashboard Command

```java
public class MetricsCommand implements CommandExecutor {
    private final RabbitMQMetrics metrics;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equals("reset")) {
            metrics.reset();
            sender.sendMessage("Metrics reset!");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== RabbitMQ Metrics ===");
        sender.sendMessage(ChatColor.YELLOW + "Received: " + metrics.getMessagesReceived());
        sender.sendMessage(ChatColor.YELLOW + "Processed: " + metrics.getMessagesProcessed());
        sender.sendMessage(ChatColor.YELLOW + "Failed: " + metrics.getMessagesFailed());
        sender.sendMessage(ChatColor.GREEN + "Success Rate: " +
                          String.format("%.2f%%", metrics.getSuccessRate() * 100));
        sender.sendMessage(ChatColor.AQUA + "Avg Time: " +
                          String.format("%.2fms", metrics.getAverageProcessingTime()));
        sender.sendMessage(ChatColor.AQUA + "Throughput: " +
                          String.format("%.2f KB/s", metrics.getThroughputBytesPerSecond() / 1024));

        return true;
    }
}
```

## Combining Features

### Complete Production Setup

```java
public class MyProxyPlugin extends Plugin {
    private RabbitMQManager mq;
    private RPC rpc;
    private EventBus eventBus;
    private BatchPublisher batch;
    private RabbitMQMetrics metrics;

    @Override
    public void onEnable() {
        // Setup
        RabbitMQConfig config = new RabbitMQConfig.Builder()
            .host("localhost")
            .port(5672)
            .username("guest")
            .password("guest")
            .build();

        mq = new RabbitMQManager(new BungeeCordPlatform(this), config);

        if (mq.connect()) {
            setupAdvancedFeatures();
        }
    }

    private void setupAdvancedFeatures() {
        // Metrics
        metrics = new RabbitMQMetrics();

        // Middleware
        MiddlewareChain middleware = new MiddlewareChain(getLogger());
        middleware.add(new MiddlewareChain.LoggingMiddleware(getLogger()));
        middleware.add(new RateLimitMiddleware());

        // RPC
        rpc = new RPC(mq, "myapp_rpc_call", "myapp_rpc_response");
        rpc.register("getOnlineCount", req ->
            getProxy().getOnlineCount());
        rpc.register("getPlayerServer", req ->
            getPlayerServerName(req.getString("uuid")));

        // Events
        eventBus = new EventBus(mq, "consumer-1", "myapp-events", "myapp_event", true);
        eventBus.on("player_join_network", this::handlePlayerJoin);

        // Batch
        batch = new BatchPublisher(mq.getClient(), getLogger())
            .batchSize(50)
            .flushInterval(500)
            .start();

        // Handlers
        mq.on("bulk_update", msg -> {
            long start = System.currentTimeMillis();
            metrics.recordReceived(msg.getRawMessage().length());

            try {
                handleBulkUpdate(msg);
                metrics.recordProcessed(System.currentTimeMillis() - start);
            } catch (Exception e) {
                metrics.recordFailed();
            }
        });
    }
}
```

These advanced features make your RabbitMQ integration robust, efficient, and production-ready! Explore and combine them to build powerful distributed systems.
