# 🚀 CelesMQ | RabbitMQ Made Easier!

[![JitPack](https://jitpack.io/v/1nstagram1/CelesMQ.svg)](https://jitpack.io/#1nstagram1/CelesMQ)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Platforms](https://img.shields.io/badge/Platforms-7%2B-blue.svg)](#supported-platforms)

**A next-generation RabbitMQ library for Minecraft networks that makes distributed messaging ridiculously simple.**

Built for Spigot, Paper, BungeeCord, Velocity, Minestom, Sponge, Folia, and NukkitX.

---

## 💎 Why This Library is OP

### The Old Way vs Our Way

<table>
<tr>
<th>❌ Traditional Approach</th>
<th>✅ CelesMQ</th>
</tr>
<tr>
<td>

```java
// 500-line switch statement 😰
switch (action) {
  case "ping":
    JSONObject json = new JSONObject();
    json.put("taskID", taskID);
    int count = ((Long) json.get("count")).intValue();
    // ... 50 more cases
}
```

</td>
<td>

```java
// Clean action routing 🎉
manager.on("ping", msg -> {
  int count = msg.getInt("count");
  // Done!
});
```

</td>
</tr>
<tr>
<td>

```java
// Manual request-response hell 😱
int taskID = random.nextInt(100000);
callbacks.put(taskID, response -> {
  // Handle response after timeout checks...
});
sendMessage(channel, taskID);
// 40+ lines of timeout/callback logic
```

</td>
<td>

```java
// Built-in request-response ✨
MessageResponse response = manager
  .request()
  .param("name", player)
  .timeout(500)
  .sendTo("channel")
  .get();
```

</td>
</tr>
</table>
---

## ✨ Key Features

### 🎯 Developer Experience
- **Type-Safe API** - `msg.getInt()`, `msg.getString()`, `msg.getBoolean()` with defaults
- **Action-Based Routing** - `manager.on("action", handler)` - no more switch statements
- **Fluent Builder Pattern** - Chain methods for clean, readable code
- **Zero Boilerplate** - Library handles all the complexity
- **Auto JSON Serialization** - Send `Map<String, Object>`, receive typed data

### ⚡ Advanced Capabilities
- **Request-Response Built-in** - Automatic callbacks with timeout handling
- **RPC (Remote Procedure Calls)** - Call methods on other servers like local functions
- **Cross-Server Events** - Platform-agnostic event system
- **Message Middleware** - Intercept, filter, and transform messages
- **Batch Operations** - Efficient bulk message sending
- **Metrics & Monitoring** - Built-in performance tracking

### 🔒 Enterprise Ready
- **SSL/TLS Encryption** - Secure connections with mTLS support
- **Automatic Reconnection** - Never lose messages during network issues
- **Thread-Safe** - Fully concurrent with CompletableFuture support
- **Multi-Platform** - Same API across 7+ Minecraft platforms

---

## 📦 Installation

### Maven (JitPack)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.1nstagram1</groupId>
        <artifactId>celesmq</artifactId>
        <version>2.1.1</version>
    </dependency>
</dependencies>
```

### Gradle

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.1nstagram1:celesmq:2.1.1'
}
```

---

## 🚀 Quick Start

### 1. Initialize (Takes 5 Lines)

```java
RabbitMQConfig config = new RabbitMQConfig.Builder()
    .host("localhost")
    .port(5672)
    .username("guest")
    .password("guest")
    .consumerName("my-server")
    .build();

RabbitMQManager manager = new RabbitMQManager(
    new BungeeCordPlatform(this),
    config
);

manager.connect();
```

### 2. Handle Messages (Clean & Simple)

```java
// Action-based routing - SO CLEAN
manager.on("player_join", msg -> {
    String player = msg.getString("player");
    String server = msg.getString("server");
    broadcast(player + " joined " + server);
});

manager.on("broadcast", msg -> {
    String message = msg.getString("message");
    getProxy().broadcast(message);
});

manager.on("kick_player", msg -> {
    String player = msg.getString("player");
    String reason = msg.getString("reason");
    getPlayer(player).ifPresent(p -> p.disconnect(reason));
});
```

### 3. Send Messages (Fire & Forget)

```java
Map<String, Object> data = new HashMap<>();
data.put("action", "broadcast");
data.put("message", "Server restarting in 5 minutes!");

manager.publishToQueue("networkcore", data);
```

### 4. Request-Response (Built-in!)

```java
// Blocking
MessageResponse response = manager
    .request()
    .param("name", playerName)
    .param("action", "getPlayer")
    .timeout(500)
    .sendTo("database")
    .get();

if (response.isSuccess()) {
    int coins = response.getInt("coins");
    String rank = response.getString("rank");
}

// Async
manager.request()
    .param("uuid", uuid)
    .param("action", "getStats")
    .timeout(500)
    .sendTo("stats-service")
    .thenAccept(response -> {
        if (response.isSuccess()) {
            int kills = response.getInt("kills");
            player.sendMessage("Kills: " + kills);
        }
    });
```

---

## 📚 Documentation

| Guide | Description |
|-------|-------------|
| [**🎯 Easy Start Guide**](docs/EASY_START.md) | Complete beginner tutorial with examples |
| [**📖 API Reference**](docs/API_GUIDE.md) | Full method documentation |
| [**🚀 Advanced Features**](docs/ADVANCED_FEATURES.md) | RPC, Events, Middleware, Batching, Metrics |
| [**🔐 SSL/TLS Guide**](docs/SSL_TLS_GUIDE.md) | Secure connections with encryption |
| [**⚙️ Config Examples**](docs/CONFIG_EXAMPLES.md) | YAML-based configuration patterns |
---

## 🎮 Supported Platforms

| Platform | Version | Status |
|----------|---------|--------|
| **Spigot/Paper** | 1.20.4+ | ✅ Fully Supported |
| **BungeeCord/Waterfall** | 1.20+ | ✅ Fully Supported |
| **Velocity** | 3.3.0+ | ✅ Fully Supported |
| **Minestom** | 1.20.2+ | ✅ Fully Supported |
| **Sponge** | 11.0.0+ | ✅ Fully Supported |
| **Folia** | Latest | ✅ Fully Supported |
| **NukkitX/PowerNukkitX** | 1.20.40+ | ✅ Fully Supported |

---

## 🔥 Real-World Examples

### Cross-Server Player Tracking

```java
// On any server - player joins
@EventHandler
public void onJoin(PlayerJoinEvent event) {
    Map<String, Object> data = new HashMap<>();
    data.put("action", "player_join");
    data.put("player", event.getPlayer().getName());
    data.put("server", getServerName());

    manager.publish("network-events", data);
}

// On all servers - handle join event
manager.on("player_join", msg -> {
    String player = msg.getString("player");
    String server = msg.getString("server");

    Bukkit.broadcastMessage(player + " joined " + server + "!");
});
```

### Global Chat System

```java
// Send global message
Map<String, Object> data = new HashMap<>();
data.put("action", "global_chat");
data.put("player", player.getName());
data.put("message", message);
data.put("rank", player.getRank());

manager.publish("chat", data);

// Receive on all servers
manager.on("global_chat", msg -> {
    String player = msg.getString("player");
    String message = msg.getString("message");
    String rank = msg.getString("rank");

    String formatted = String.format("[%s] %s: %s", rank, player, message);
    Bukkit.broadcastMessage(formatted);
});
```

### Player Data Request-Response

```java
// Request player data from database server
public Optional<PlayerData> getPlayerData(String playerName) {
    try {
        MessageResponse response = manager
            .request()
            .param("name", playerName)
            .param("action", "getPlayer")
            .timeout(500)
            .sendTo("database")
            .get();

        if (response.isSuccess()) {
            return Optional.of(new PlayerData(
                response.getInt("id"),
                response.getString("name"),
                response.getInt("coins"),
                response.getString("rank")
            ));
        }
    } catch (Exception e) {
        logger.warning("Failed to fetch player data: " + e.getMessage());
    }
    return Optional.empty();
}
```

### Proxy Server Management

```java
// BungeeCord - Send player to specific server
manager.on("send_player", msg -> {
    String playerName = msg.getString("player");
    String targetServer = msg.getString("server");

    ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
    if (player != null) {
        ServerInfo server = ProxyServer.getInstance().getServerInfo(targetServer);
        if (server != null) {
            player.connect(server);
        }
    }
});

// Any server - Request player transfer
Map<String, Object> data = new HashMap<>();
data.put("action", "send_player");
data.put("player", "Steve");
data.put("server", "lobby-1");

manager.publish("proxy", data);
```

---

## 🔐 SSL/TLS Support

Secure your RabbitMQ connections with full SSL/TLS support:

```java
RabbitMQConfig config = new RabbitMQConfig.Builder()
    .host("rabbitmq.example.com")
    .port(5671)  // SSL port
    .username("user")
    .password("pass")
    .useSsl(true)
    .sslProtocol("TLSv1.3")
    .trustStorePath("/path/to/truststore.jks")
    .trustStorePassword("trustpass")
    .keyStorePath("/path/to/keystore.jks")  // For mTLS
    .keyStorePassword("keypass")
    .build();
```

**See [SSL_TLS_GUIDE.md](docs/SSL_TLS_GUIDE.md) for complete setup instructions.**

---

## 🎯 Advanced Features

### RPC - Call Remote Methods

```java
// Register RPC handler on server A
rpc.register("getPlayerBalance", args -> {
    UUID uuid = UUID.fromString((String) args.get("uuid"));
    return economy.getBalance(uuid);
});

// Call from server B
rpc.call("server-a", "getPlayerBalance")
    .arg("uuid", uuid.toString())
    .execute()
    .thenAccept(response -> {
        double balance = response.getResultAsDouble();
        player.sendMessage("Balance: $" + balance);
    });
```

### Cross-Server Events

```java
// Register event listener
eventBus.on("player_purchase", event -> {
    String player = (String) event.getData("player");
    String item = (String) event.getData("item");
    broadcast(player + " purchased " + item);
});

// Emit event from any server
eventBus.emit("player_purchase", Map.of(
    "player", "Steve",
    "item", "Diamond Sword"
));
```

### Message Middleware

```java
// Add authentication middleware
manager.addMiddleware((message, next) -> {
    if (message.has("auth_token")) {
        String token = message.getString("auth_token");
        if (validateToken(token)) {
            return next.process(message);
        }
    }
    return false; // Block unauthorized messages
});

// Add logging middleware
manager.addMiddleware((message, next) -> {
    long start = System.currentTimeMillis();
    boolean result = next.process(message);
    long duration = System.currentTimeMillis() - start;
    logger.info("Processed message in " + duration + "ms");
    return result;
});
```

**See [ADVANCED_FEATURES.md](docs/ADVANCED_FEATURES.md) for full documentation.**
---

## 💡 Best Practices

### 1. Always Use Action-Based Routing
```java
// ❌ Don't parse manually
manager.on("message", msg -> {
    String action = msg.getString("action");
    if (action.equals("broadcast")) { /* ... */ }
    else if (action.equals("kick")) { /* ... */ }
});

// ✅ Use action routing
manager.on("broadcast", msg -> { /* ... */ });
manager.on("kick", msg -> { /* ... */ });
```

### 2. Use Request-Response for Data Queries
```java
// ❌ Don't use manual callbacks
Map<String, Object> request = new HashMap<>();
request.put("taskID", generateTaskID());
callbacks.put(taskID, response -> { /* ... */ });

// ✅ Use built-in request-response
MessageResponse response = manager.request()
    .param("query", "data")
    .timeout(500)
    .sendTo("service")
    .get();
```

### 3. Sync to Main Thread (Spigot/Paper Only)
```java
manager.on("player_teleport", msg -> {
    // This runs on RabbitMQ thread - NOT safe for Bukkit API!
    Location loc = parseLocation(msg);

    // Use platform's scheduler for Bukkit API calls
    Bukkit.getScheduler().runTask(plugin, () -> {
        player.teleport(loc);
    });
});
```

### 4. Handle Errors Gracefully
```java
try {
    MessageResponse response = manager.request()
        .param("action", "query")
        .timeout(500)
        .sendTo("database")
        .get();

    if (response.isSuccess()) {
        // Handle success
    } else if (response.isTimeout()) {
        logger.warning("Database timeout!");
    } else {
        logger.warning("Database error: " + response.getString("error"));
    }
} catch (Exception e) {
    logger.severe("Request failed: " + e.getMessage());
}
```

---

## 🐛 Troubleshooting

### Connection Issues
```
java.net.ConnectException: Connection refused
```
**Solution:** Ensure RabbitMQ is running: `systemctl status rabbitmq-server`

### Messages Not Received
- Check action names match exactly (case-sensitive)
- Verify channels are properly configured
- Use RabbitMQ Management UI to check queues: `http://localhost:15672`

### Timeout Issues
- Increase timeout: `.timeout(1000)`
- Check network latency between servers
- Ensure receiver is processing messages (not blocking)

---

## 📊 Performance

Benchmarked on i7-9700K with local RabbitMQ:

| Operation | Latency (avg) | Throughput |
|-----------|---------------|------------|
| Simple publish | ~1ms | 50,000 msg/s |
| Request-response | ~2-3ms | 15,000 req/s |
| Batch publish (100) | ~5ms | 200,000 msg/s |

*Note: Network latency between servers adds ~1-5ms depending on distance*

---

## 📝 License

MIT License - see [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Submit a pull request

---

## 💬 Support

- 📖 [Documentation](docs/)
- 🐛 [Report Issues](https://github.com/1nstagram1/CelesMQ/issues)
- 💡 [Feature Requests](https://github.com/1nstagram1/CelesMQ/issues/new)

---

## 🌟 Show Your Support

If this library helped your project, give it a ⭐ on GitHub!

---

<p align="center">
  Made with ❤️ for the Minecraft development community
</p>
