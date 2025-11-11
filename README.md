# CelesMQ | RabbitMQ made easier!

[![](https://jitpack.io/v/1nstagram1/CelesMQ.svg)](https://jitpack.io/#1nstagram1/CelesMQ)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A comprehensive RabbitMQ integration library for Minecraft platforms (Spigot/Paper, BungeeCord, Velocity, Minestom, Sponge, Folia, NukkitX) that enables seamless cross-server communication and event handling.

RabbitMQ is a powerful message broker that allows different servers and services to communicate asynchronously. This library simplifies RabbitMQ usage in Minecraft plugins with a high-level API, type-safe message handling, and built-in request-response patterns.

## 🤔 Why Choose This Library?

- **🚀 Simpler than Redis** - High-level API with action routing and request-response patterns
- **🎯 Type-Safe** - MessageResponse with built-in getters (no manual JSON parsing!)
- **⚡ Request-Response Built-in** - Automatic callbacks with timeout handling
- **🎨 Fluent API** - Chain methods for clean, readable code
- **📦 Zero Boilerplate** - RabbitMQManager handles all the complexity
- **🔧 Platform Agnostic** - Same API across 7+ platforms (Spigot/Paper, BungeeCord, Velocity, Minestom, Sponge, Folia, 
  NukkitX)

### Quick Comparison

**Traditional Way:**
```java
String json = getMessage();
JSONObject obj = new JSONParser().parse(json);
int value = ((Long) obj.get("value")).intValue(); // 😰
```

**Our Way:**
```java
mq.on("action", msg -> {
    int value = msg.getInt("value"); // 🤩
});
```

## Features

### Core Features
- **Easy-to-use RabbitMQManager** - High-level API for simple usage
- **MessageResponse** - Type-safe JSON handling with convenient getters
- **Request-Response Pattern** - Built-in request/reply with CompletableFuture
- **Action-Based Routing** - Clean message handling organization
- **MessageBuilder** - Fluent builder for constructing messages
- Automatic connection recovery and reconnection
- Thread-safe operations with async support
- Multiple messaging patterns (Queue, Fanout, Topic, Work queues)
- JSON serialization utilities
- Configurable connection settings
- Minecraft-aware threading (sync to main thread when needed)
- Comprehensive error handling
- **SSL/TLS Encryption** - Secure connections with configurable encryption options

### Advanced Features 🔥
- **RPC (Remote Procedure Calls)** - Call methods on other servers like local functions!
- **Cross-Server Event System** - Platform-agnostic events that work everywhere
- **Message Middleware** - Intercept, modify, filter, and transform messages
- **Batch Operations** - Efficient bulk message sending with automatic batching
- **Metrics & Monitoring** - Built-in performance tracking and statistics
- **Custom Events** - Create and dispatch your own cross-server events
- **Middleware Chain** - Multiple middleware with priority support

**See [ADVANCED_FEATURES.md](docs/ADVANCED_FEATURES.md) for complete guide!**

## Requirements

- Java 17 or higher
- One of the following platforms:
  - Spigot/Paper 1.20.4 or higher
  - BungeeCord 1.20 or higher
  - Velocity 3.3.0 or higher
  - Minestom 1.20.2+ (Modern Server)
  - Sponge 11.0.0+ (Forge/Fabric)
  - Folia (Paper's multithreaded fork)
  - NukkitX/PowerNukkitX (Bedrock Edition)
- RabbitMQ Server 3.x

## Installation

### Maven (via JitPack)

Add the JitPack repository and dependency to your plugin's `pom.xml`:

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
        <artifactId>CelesMQ</artifactId>
        <version>2.0.0</version>
    </dependency>
</dependencies>
```

**Note:** Replace `2.0.0` with the latest release version or use `main-SNAPSHOT` for the latest development build.

### Gradle (via JitPack)

Add JitPack repository and dependency to your `build.gradle`:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.1nstagram1:CelesMQ:2.0.0'
}
```

### Build from Source

```bash
git clone https://github.com/1nstagram1/CelesMQ.git
cd CelesMQ
mvn clean install
```

## 🎯 Super Easy Quick Start (Recommended)

### Option 1: Config-Based

```yaml
# config.yml
rabbitmq:
  host: localhost
  port: 5672
  username: guest
  password: guest
  consumer: "consumer-1"     # Your consumer name!
  autoSubscribe: true        # Auto-subscribe to channels!
  channels:
    commands: "commands-exchange"
    events: "events-exchange"
    updates: "updates-exchange"
```

```java
public class MyBungeePlugin extends Plugin {
    private RabbitMQManager mq;

    @Override
    public void onEnable() {
        // Load from config - just like your original code!
        RabbitMQConfig config = new RabbitMQConfig.Builder()
            .host(getConfig().getString("rabbitmq.host"))
            .port(getConfig().getInt("rabbitmq.port"))
            .username(getConfig().getString("rabbitmq.username"))
            .password(getConfig().getString("rabbitmq.password"))
            .consumerName(getConfig().getString("rabbitmq.consumer"))
            .addChannel("commands", getConfig().getString("rabbitmq.channels.commands"))
            .addChannel("events", getConfig().getString("rabbitmq.channels.events"))
            .addChannel("updates", getConfig().getString("rabbitmq.channels.updates"))
            .autoSubscribe(true)  // Auto-subscribe!
            .build();

        mq = new RabbitMQManager(new BungeeCordPlatform(this), config);

        if (mq.connect()) {
            // Channels already loaded and subscribed!
            // Just register your handlers!
            mq.on("player_join", msg -> {
                String player = msg.getString("player");
                getLogger().info(player + " joined!");
            });

            mq.on("broadcast", msg -> {
                getProxy().broadcast(msg.getString("message"));
            });

            // Send to configured channels
            mq.send("commands", MessageBuilder.create("alert")
                .add("message", "Server started!")
                .build());
        }
    }
}
```

**See [CONFIG_EXAMPLES.md](docs/CONFIG_EXAMPLES.md) for complete config-based setup!**

### Option 2: Programmatic Setup

```java
RabbitMQConfig config = new RabbitMQConfig.Builder()
    .host("localhost").port(5672)
    .username("guest").password("guest")
    .consumerName("my-server")
    .addChannel("commands", "commands-exchange")
    .addChannel("updates", "updates-exchange")
    .autoSubscribe(true)
    .build();

mq = new RabbitMQManager(new BungeeCordPlatform(this), config);
mq.connect();

// Handlers
mq.on("player_join", msg -> { /* ... */ });
```

**See [EASY_START.md](docs/EASY_START.md) for complete examples and tutorials!**

## 🚀 Advanced Features Preview

```java
// RPC - Call methods on other servers!
rpc.call("game-server", "getPlayerCoins")
    .arg("uuid", uuid)
    .execute()
    .thenAccept(response -> {
        int coins = response.getResultAsInt();
    });

// Events - Cross-server event system with your custom events
eventBus.on("player_join", (CrossServerEvent event) -> {
    String playerName = (String) event.getData("playerName");
    broadcast(playerName + " joined!");
});

// Batch - Efficient bulk operations
batch.batchSize(100).start();
batch.add("exchange", message); // Sent efficiently in batches

// Metrics - Built-in monitoring
metrics.getSummary(); // Get performance stats
```

**Check out [ADVANCED_FEATURES.md](docs/ADVANCED_FEATURES.md) for RPC, Events, Middleware, Batching, and Metrics!**

## Quick Start (Traditional API)

### 1. Create Configuration

```java
RabbitMQConfig config = new RabbitMQConfig.Builder()
    .host("localhost")
    .port(5672)
    .username("guest")
    .password("guest")
    .virtualHost("/")
    .connectionTimeout(10000)
    .automaticRecoveryEnabled(true)
    .build();
```

### 2. Initialize Client

#### Spigot/Paper Plugin

```java
public class MyPlugin extends JavaPlugin {
    private RabbitMQClient rabbitMQClient;

    @Override
    public void onEnable() {
        rabbitMQClient = new RabbitMQClient(this, config);

        if (rabbitMQClient.connect()) {
            getLogger().info("Connected to RabbitMQ!");
        }
    }

    @Override
    public void onDisable() {
        if (rabbitMQClient != null) {
            rabbitMQClient.disconnect();
        }
    }
}
```

#### BungeeCord Plugin

```java
import io.hydrodevelopments.celesmq.RabbitMQClient;
import platform.io.hydrodevelopments.celesmq.BungeeCordPlatform;
import net.md_5.bungee.api.plugin.Plugin;

public class MyBungeePlugin extends Plugin {
  private RabbitMQClient rabbitMQClient;

  @Override public void onEnable() {
    BungeeCordPlatform platform = new BungeeCordPlatform(this);
    rabbitMQClient = new RabbitMQClient(platform, config);

    if (rabbitMQClient.connect()) {
      getLogger().info("Connected to RabbitMQ!");
    }
  }

  @Override public void onDisable() {
    if (rabbitMQClient != null) {
      rabbitMQClient.disconnect();
    }
  }
}
```

#### Velocity Plugin

```java
import com.google.inject.Inject;
import io.hydrodevelopments.celesmq.RabbitMQClient;
import platform.io.hydrodevelopments.celesmq.VelocityPlatform;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(id = "myvelocityplugin", name = "My Velocity Plugin", version = "1.0.0") public class MyVelocityPlugin {
  private final ProxyServer proxyServer;
  private final Logger logger;
  private RabbitMQClient rabbitMQClient;

  @Inject public MyVelocityPlugin(ProxyServer proxyServer, Logger logger) {
    this.proxyServer = proxyServer;
    this.logger = logger;
  }

  @Subscribe public void onProxyInitialization(ProxyInitializeEvent event) {
    // Convert SLF4J logger to java.util.logging.Logger
    java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(logger.getName());

    VelocityPlatform platform = new VelocityPlatform(this, proxyServer, julLogger);
    rabbitMQClient = new RabbitMQClient(platform, config);

    if (rabbitMQClient.connect()) {
      logger.info("Connected to RabbitMQ!");
    }
  }

  @Subscribe public void onProxyShutdown(ProxyShutdownEvent event) {
    if (rabbitMQClient != null) {
      rabbitMQClient.disconnect();
    }
  }
}
```

### 3. Publishing Messages

#### Simple Queue Publishing

```java
rabbitMQClient.publishToQueue("my-queue", "Hello World!")
    .thenAccept(success -> {
        if (success) {
            getLogger().info("Message sent!");
        }
    });
```

#### Broadcasting to All Servers

```java
Map<String, Object> event = new HashMap<>();
event.put("type", "player_join");
event.put("player", "Steve");
event.put("server", "lobby");

String json = JsonSerializer.toJson(event);
rabbitMQClient.broadcast("player.events", json);
```

#### Topic-Based Publishing

```java
rabbitMQClient.publishToExchange(
    "chat.exchange",
    "chat.global",
    "Hello everyone!",
    "topic",
    false
);
```

### 4. Consuming Messages

#### Simple Queue Consumer

```java
rabbitMQClient.consumeQueue("my-queue", message -> {
    getLogger().info("Received: " + message);
}, false, true); // manual ack, sync to main thread
```

#### Broadcast Subscriber

```java
rabbitMQClient.subscribeToBroadcast("player.events", message -> {
    Map<String, Object> event = JsonSerializer.fromJson(message, Map.class);
    String playerName = (String) event.get("player");
    Bukkit.broadcastMessage(playerName + " joined another server!");
}, true); // sync to main thread
```

#### Topic Subscriber

```java
rabbitMQClient.subscribeToTopic(
    "chat.exchange",
    "chat.*", // matches chat.global, chat.admin, etc.
    message -> {
        getLogger().info("Chat message: " + message);
    },
    true
);
```

## Documentation

### 📘 [Complete API Methods Guide](docs/API_GUIDE.md)

Comprehensive reference with all classes, methods, parameters, and examples. Covers:

- Detailed method documentation with parameters and return types

- Code examples for every method

- Common patterns and use cases

- Best practices and error handling

- Performance tips

## API Reference

### RabbitMQClient

Main API class for interacting with RabbitMQ.

#### Connection Methods

- `connect()` - Establishes connection to RabbitMQ server
- `disconnect()` - Closes connection gracefully
- `isConnected()` - Checks if client is connected

#### Publishing Methods

- `publishToQueue(String queueName, String message)` - Publish to a queue
- `publishToQueue(String queueName, String message, boolean persistent)` - Publish with persistence
- `publishToExchange(String exchange, String routingKey, String message)` - Publish to exchange
- `publishToExchange(String exchange, String routingKey, String message, String type, boolean persistent)` - Full options
- `publishWithHeaders(String exchange, String routingKey, String message, Map<String, Object> headers)` - With headers
- `broadcast(String exchange, String message)` - Fanout broadcast

#### Consuming Methods

- `consumeQueue(String queueName, MessageListener listener)` - Consume from queue
- `consumeQueue(String queueName, MessageListener listener, boolean autoAck)` - With auto-ack option
- `consumeQueue(String queueName, MessageListener listener, boolean autoAck, boolean syncToMainThread)` - Full options
- `subscribeToBroadcast(String exchange, MessageListener listener)` - Subscribe to broadcasts
- `subscribeToBroadcast(String exchange, MessageListener listener, boolean syncToMainThread)` - With sync option
- `subscribeToTopic(String exchange, String pattern, MessageListener listener)` - Topic subscription
- `subscribeToTopic(String exchange, String pattern, MessageListener listener, boolean syncToMainThread)` - With sync

### JsonSerializer

Utility class for JSON operations using Gson.

- `toJson(Object obj)` - Serialize object to JSON
- `toPrettyJson(Object obj)` - Serialize with pretty printing
- `fromJson(String json, Class<T> type)` - Deserialize from JSON
- `fromJson(String json, Type type)` - Deserialize with Type

## Usage Patterns

### Cross-Server Player Tracking

```java
// Server A - Publish join event
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Map<String, Object> data = Map.of(
        "event", "join",
        "player", event.getPlayer().getName(),
        "uuid", event.getPlayer().getUniqueId().toString()
    );
    rabbitMQClient.broadcast("player.events", JsonSerializer.toJson(data));
}

// Server B - Subscribe to events
rabbitMQClient.subscribeToBroadcast("player.events", message -> {
    Map<String, Object> data = JsonSerializer.fromJson(message, Map.class);
    Bukkit.broadcastMessage(data.get("player") + " joined " + data.get("server"));
}, true);
```

### Cross-Server Chat

```java
// Publishing server
rabbitMQClient.publishToExchange(
    "chat.exchange",
    "chat.global",
    JsonSerializer.toJson(Map.of(
        "player", playerName,
        "message", chatMessage
    )),
    "topic",
    false
);

// Subscribing servers
rabbitMQClient.subscribeToTopic("chat.exchange", "chat.#", message -> {
    Map<String, Object> data = JsonSerializer.fromJson(message, Map.class);
    Bukkit.broadcastMessage("[Global] <" + data.get("player") + "> " + data.get("message"));
}, true);
```

### Work Queue Pattern

```java
// Producer - Add tasks to queue
Map<String, Object> task = Map.of(
    "type", "backup",
    "world", "world",
    "priority", "high"
);
rabbitMQClient.publishToQueue("server.tasks", JsonSerializer.toJson(task), true);

// Worker - Process tasks
rabbitMQClient.consumeQueue("server.tasks", message -> {
    Map<String, Object> task = JsonSerializer.fromJson(message, Map.class);
    // Process task...
    getLogger().info("Processing: " + task.get("type"));
}, false, false); // Manual ack, async processing
```

### Proxy Server Communication

#### BungeeCord - Player Server Switching

```java
// Listen for player server switch events
getProxy().getPluginManager().registerListener(this, new Listener() {
    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        Map<String, Object> data = Map.of(
            "event", "server_switch",
            "player", player.getName(),
            "uuid", player.getUniqueId().toString(),
            "from", event.getFrom() != null ? event.getFrom().getName() : "null",
            "to", player.getServer().getInfo().getName()
        );
        rabbitMQClient.broadcast("proxy.events", JsonSerializer.toJson(data));
    }
});
```

#### Velocity - Cross-Proxy Messaging

```java
// Subscribe to cross-proxy messages
rabbitMQClient.subscribeToBroadcast("proxy.messages", message -> {
    Map<String, Object> data = JsonSerializer.fromJson(message, Map.class);
    String targetPlayer = (String) data.get("player");
    String msg = (String) data.get("message");

    proxyServer.getPlayer(targetPlayer).ifPresent(player -> {
        player.sendMessage(Component.text(msg));
    });
}, false); // Velocity is async-first, no need to sync

// Send a cross-proxy message
Map<String, Object> data = Map.of(
    "player", "PlayerName",
    "message", "Hello from another proxy!"
);
rabbitMQClient.broadcast("proxy.messages", JsonSerializer.toJson(data));
```

## Configuration

### Plugin Configuration Example

```yaml
rabbitmq:
  host: localhost
  port: 5672
  username: guest
  password: guest
  virtualHost: /
  connectionTimeout: 10000
  networkRecoveryInterval: 5000
  automaticRecoveryEnabled: true
```

### Loading Configuration

```java
RabbitMQConfig config = new RabbitMQConfig.Builder()
    .host(getConfig().getString("rabbitmq.host"))
    .port(getConfig().getInt("rabbitmq.port"))
    .username(getConfig().getString("rabbitmq.username"))
    .password(getConfig().getString("rabbitmq.password"))
    .virtualHost(getConfig().getString("rabbitmq.virtualHost"))
    .connectionTimeout(getConfig().getInt("rabbitmq.connectionTimeout"))
    .networkRecoveryInterval(getConfig().getInt("rabbitmq.networkRecoveryInterval"))
    .automaticRecoveryEnabled(getConfig().getBoolean("rabbitmq.automaticRecoveryEnabled"))
    .build();
```

## Platform-Specific Notes

### Spigot/Paper
- Use `syncToMainThread = true` when calling Bukkit API methods in message listeners
- The library will automatically schedule tasks on the main thread using Bukkit's scheduler

### BungeeCord
- BungeeCord is mostly async, so `syncToMainThread` parameter has minimal effect
- All BungeeCord API calls are generally thread-safe
- Use the proxy's scheduler for delayed or repeating tasks

### Velocity
- Velocity is fully async and doesn't have a main thread concept
- The `syncToMainThread` parameter will execute tasks immediately
- All Velocity API operations are thread-safe by design
- Prefer async operations for optimal performance

## Best Practices

1. **Always disconnect on plugin disable**
   ```java
   @Override
   public void onDisable() {
       rabbitMQClient.disconnect();
   }
   ```

2. **Use async operations for publishing**
   - Publishing is already async by default
   - Use CompletableFuture callbacks for results

3. **Sync to main thread for Bukkit API calls (Spigot/Paper only)**
   ```java
   rabbitMQClient.consumeQueue("queue", message -> {
       // This runs on main thread, safe for Bukkit API
   }, false, true); // Last parameter = true
   ```

4. **Platform abstraction for multi-platform plugins**
   ```java
   Platform platform = new SpigotPlatform(plugin);
   // OR
   Platform platform = new BungeeCordPlatform(plugin);
   // OR
   Platform platform = new VelocityPlatform(plugin, proxyServer, logger);

   RabbitMQClient client = new RabbitMQClient(platform, config);
   ```

5. **Handle deserialization errors**
   ```java
   Map<String, Object> data = JsonSerializer.fromJson(message, Map.class);
   if (data == null) {
       getLogger().warning("Failed to parse message");
       return;
   }
   ```

6. **Use manual acknowledgment for critical messages**
   ```java
   rabbitMQClient.consumeQueue("important-queue", message -> {
       // Process message...
   }, false, false); // autoAck = false for manual ack
   ```

## Example Plugin

See the [examples/example-plugin](examples/example-plugin) directory for a complete working example demonstrating:
- Player join/quit events across servers
- Cross-server announcements
- Work queue processing
- Configuration management

## Troubleshooting

### Connection Failed

- Verify RabbitMQ server is running: `systemctl status rabbitmq-server`
- Check host and port configuration
- Ensure firewall allows connections on port 5672
- Verify username/password credentials

### Messages Not Being Received

- Check that exchanges and queues are properly declared
- Verify routing keys match between publisher and consumer
- Check RabbitMQ management UI for queue bindings
- Ensure consumers are active: `rabbitMQClient.getActiveConsumers()`

### Thread Safety Issues

- Always use `syncToMainThread = true` when calling Bukkit API
- Use async operations for heavy processing
- Don't block the main thread with synchronous operations

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions:
- Open an issue on GitHub
- Check existing issues for solutions
- Review the example plugin for implementation guidance
