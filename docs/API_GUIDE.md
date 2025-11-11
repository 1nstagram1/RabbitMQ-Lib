# CelesMQ Complete API Reference

Comprehensive reference guide for all classes and methods in the CelesMQ library.

## Table of Contents

### High-Level API (Recommended)
- [RabbitMQManager](#rabbitmqmanager) - Easy-to-use manager with action routing
- [MessageResponse](#messageresponse) - Type-safe message handling
- [MessageRequest](#messagerequest) - Request-response pattern
- [MessageRouter](#messagerouter) - Action-based routing
- [MessageBuilder](#messagebuilder) - Fluent message construction

### Configuration
- [RabbitMQConfig](#rabbitmqconfig) - Connection and channel configuration
- [Platform](#platform) - Platform abstraction (Spigot, BungeeCord, Velocity, Minestom, Sponge, Folia, NukkitX)

### Advanced Features
- [RPC](#rpc) - Remote procedure calls
- [EventBus](#eventbus) - Cross-server event system
- [MessageMiddleware](#messagemiddleware) - Message interception
- [BatchPublisher](#batchpublisher) - Batch operations
- [RabbitMQMetrics](#rabbitmqmetrics) - Performance monitoring

### Low-Level API
- [RabbitMQClient](#rabbitmqclient) - Direct RabbitMQ operations
- [JsonSerializer](#jsonserializer) - JSON utilities

---

## Important Notes

### Naming Conventions

The library is fully configurable with **no hardcoded values**. You must explicitly specify:

- Exchange names for EventBus
- Action names for EventBus and RPC
- Consumer names for RabbitMQConfig

**Best Practices:**

1. **Namespace everything** to avoid conflicts between applications:
   ```java
   // Create namespaced RPC
   RPC rpc = new RPC(mq, "myapp_rpc_call", "myapp_rpc_response");

   // Create namespaced EventBus
   EventBus eventBus = new EventBus(mq, "consumer-1", "myapp-events", "myapp_event", true);
   ```

2. **Use consistent prefixes** across your application (e.g., `"myapp_*"` for all internal actions)

3. **Avoid generic names** like `"events"`, `"rpc"`, etc. - be specific to your application

4. **Document your naming scheme** so all parts of your application use the same names

This allows multiple applications using this library to coexist on the same RabbitMQ server without conflicts.

---

## RabbitMQManager

High-level manager that provides an easy-to-use API with action routing, request-response patterns, and automatic channel management.

**Package:** `io.hydrogendevelopments.celesmq`

### Constructor

#### `RabbitMQManager(Platform platform, RabbitMQConfig config)`

Creates a new RabbitMQManager instance.

**Parameters:**
- `platform` - Platform instance (SpigotPlatform, BungeeCordPlatform, VelocityPlatform, MinestomPlatform, SpongePlatform, FoliaPlatform, or NukkitPlatfor)
- `config` - RabbitMQConfig with connection settings and channels

**Example:**
```java
// BungeeCord
RabbitMQConfig config = new RabbitMQConfig.Builder()
    .host("localhost")
    .port(5672)
    .username("guest")
    .password("guest")
    .consumerName("consumer-1")
    .addChannel("commands", "commands-exchange")
    .addChannel("events", "events-exchange")
    .autoSubscribe(true)
    .build();

RabbitMQManager mq = new RabbitMQManager(
    new BungeeCordPlatform(this),
    config
);
```

---

### Connection Methods

#### `boolean connect()`

Connects to RabbitMQ and sets up reply queue. Auto-subscribes to configured channels if enabled.

**Returns:** `true` if connection successful

**Example:**
```java
if (mq.connect()) {
    setupHandlers();
    getLogger().info("RabbitMQ ready!");
}
```

---

#### `void disconnect()`

Closes connection gracefully.

**Example:**
```java
@Override
public void onDisable() {
    mq.disconnect();
}
```

---

#### `boolean isConnected()`

Checks if connected to RabbitMQ.

**Returns:** `true` if connected

---

### Channel Management

#### `RabbitMQManager addChannel(String name, String exchange)`

Registers a channel for easy sending.

**Parameters:**
- `name` - Channel name (logical identifier)
- `exchange` - Exchange name in RabbitMQ

**Returns:** `this` for method chaining

**Example:**
```java
mq.addChannel("commands", "commands-exchange")
  .addChannel("events", "events-exchange")
  .addChannel("updates", "updates-exchange");
```

---

### Sending Messages

#### `CompletableFuture<Boolean> send(String channel, String message)`

Sends a message to a registered channel.

**Parameters:**
- `channel` - Channel name (must be registered)
- `message` - Message content

**Returns:** CompletableFuture<Boolean>

**Example:**
```java
mq.send("commands", "Hello World!")
  .thenAccept(success -> {
      if (success) {
          getLogger().info("Message sent!");
      }
  });
```

---

#### `CompletableFuture<Boolean> sendJson(String channel, Map<String, Object> data)`

Sends JSON data to a channel.

**Parameters:**
- `channel` - Channel name
- `data` - Map to serialize as JSON

**Returns:** CompletableFuture<Boolean>

**Example:**
```java
Map<String, Object> data = Map.of(
    "action", "player_join",
    "player", "Steve",
    "uuid", uuid.toString(),
    "server", "server-1"
);

mq.sendJson("events", data);
```

---

### Subscribing

#### `RabbitMQManager subscribe(String channel)`

Subscribes to a channel (exchange).

**Parameters:**
- `channel` - Exchange name

**Returns:** `this` for method chaining

**Example:**
```java
mq.subscribe("commands-exchange")
  .subscribe("events-exchange");
```

---

#### `RabbitMQManager subscribe(String channel, boolean syncToMainThread)`

Subscribes with thread control.

**Parameters:**
- `channel` - Exchange name
- `syncToMainThread` - Whether to sync handlers to main thread

**Returns:** `this` for method chaining

---

#### `RabbitMQManager subscribeTopic(String exchange, String pattern)`

Subscribes to topic exchange with pattern matching.

**Parameters:**
- `exchange` - Topic exchange name
- `pattern` - Routing key pattern (`*` = one word, `#` = zero or more words)

**Returns:** `this` for method chaining

**Example:**
```java
// Match all player events
mq.subscribeTopic("player-exchange", "player.#");

// Match specific events
mq.subscribeTopic("server-exchange", "server.*.status");
```

---

### Action Handlers

#### `RabbitMQManager on(String action, Consumer<MessageResponse> handler)`

Registers a handler for a specific action.

**Parameters:**
- `action` - Action name to match
- `handler` - Handler function

**Returns:** `this` for method chaining

**Example:**
```java
mq.on("player_join", msg -> {
    String player = msg.getString("player");
    String server = msg.getString("server");
    getLogger().info(player + " joined " + server);
});

mq.on("broadcast", msg -> {
    String message = msg.getString("message");
    getProxy().broadcast(message);
});

mq.on("kick", msg -> {
    int playerId = msg.getInt("player_id");
    String reason = msg.getString("reason", "Kicked");
    kickPlayer(playerId, reason);
});
```

---

#### `RabbitMQManager onDefault(Consumer<MessageResponse> handler)`

Registers a default handler for unmatched actions.

**Parameters:**
- `handler` - Default handler function

**Returns:** `this` for method chaining

**Example:**
```java
mq.onDefault(msg -> {
    getLogger().warning("Unhandled action: " + msg.getString("action"));
});
```

---

#### `RabbitMQManager onError(Consumer<Exception> handler)`

Registers an error handler.

**Parameters:**
- `handler` - Error handler function

**Returns:** `this` for method chaining

**Example:**
```java
mq.onError(error -> {
    getLogger().severe("Message error: " + error.getMessage());
});
```

---

### Request-Response

#### `MessageRequest request()`

Creates a new message request builder.

**Returns:** MessageRequest builder

**Example:**
```java
mq.request()
    .action("getPlayerData")
    .param("uuid", player.getUniqueId().toString())
    .timeout(3000)
    .sendTo("game-server")
    .thenAccept(response -> {
        if (response.isSuccess()) {
            int coins = response.getInt("coins");
            String rank = response.getString("rank");
            player.sendMessage("You have " + coins + " coins, rank: " + rank);
        }
    });
```

---

### Utility Methods

#### `RabbitMQClient getClient()`

Gets the underlying RabbitMQClient.

**Returns:** RabbitMQClient instance

---

#### `MessageRouter getRouter()`

Gets the message router.

**Returns:** MessageRouter instance

---

#### `String getReplyQueue()`

Gets the reply queue name.

**Returns:** Reply queue name

---

## MessageResponse

Type-safe wrapper for handling received messages. Provides convenient getters with default values.

**Package:** `io.hydrogendevelopments.celesmq.message`

### Constructor

Messages are created automatically by the library when messages are received.

---

### Getter Methods

#### `String getString(String key)`

Gets a string value.

**Parameters:**
- `key` - JSON key

**Returns:** String value or empty string if not found

---

#### `String getString(String key, String defaultValue)`

Gets a string with default value.

**Parameters:**
- `key` - JSON key
- `defaultValue` - Value to return if key not found

**Returns:** String value or default

**Example:**
```java
String player = msg.getString("player", "Unknown");
String rank = msg.getString("rank", "DEFAULT");
```

---

#### `int getInt(String key)`

Gets an integer value.

**Parameters:**
- `key` - JSON key

**Returns:** Integer value or 0 if not found

---

#### `int getInt(String key, int defaultValue)`

Gets an integer with default value.

**Example:**
```java
int level = msg.getInt("level", 1);
int coins = msg.getInt("coins", 0);
```

---

#### `long getLong(String key)`

Gets a long value.

**Returns:** Long value or 0L if not found

---

#### `long getLong(String key, long defaultValue)`

Gets a long with default value.

**Example:**
```java
long timestamp = msg.getLong("timestamp", 0L);
long playerId = msg.getLong("player_id", -1L);
```

---

#### `double getDouble(String key)`

Gets a double value.

**Returns:** Double value or 0.0 if not found

---

#### `double getDouble(String key, double defaultValue)`

Gets a double with default value.

**Example:**
```java
double balance = msg.getDouble("balance", 0.0);
```

---

#### `boolean getBoolean(String key)`

Gets a boolean value.

**Returns:** Boolean value or false if not found

---

#### `boolean getBoolean(String key, boolean defaultValue)`

Gets a boolean with default value.

**Example:**
```java
boolean premium = msg.getBoolean("premium", false);
boolean online = msg.getBoolean("online", false);
```

---

#### `JsonObject getObject(String key)`

Gets a nested JSON object.

**Returns:** JsonObject or null if not found

---

#### `JsonArray getArray(String key)`

Gets a JSON array.

**Returns:** JsonArray or null if not found

**Example:**
```java
JsonArray servers = msg.getArray("servers");
if (servers != null) {
    for (JsonElement server : servers) {
        String name = server.getAsString();
        getLogger().info("Server: " + name);
    }
}
```

---

#### `List<MessageResponse> getResponseList(String key)`

Gets a list of nested MessageResponse objects.

**Returns:** List of MessageResponse or empty list

**Example:**
```java
List<MessageResponse> friends = msg.getResponseList("friends");
for (MessageResponse friend : friends) {
    String name = friend.getString("name");
    boolean online = friend.getBoolean("online");
    getLogger().info(name + " - " + (online ? "Online" : "Offline"));
}
```

---

#### `boolean has(String key)`

Checks if a key exists.

**Returns:** `true` if key exists

**Example:**
```java
if (msg.has("special_data")) {
    processSpecialData(msg.getObject("special_data"));
}
```

---

### Response Status Methods

#### `boolean isSuccess()`

Checks if response indicates success.

**Returns:** `true` if success

---

#### `ResponseStatus getStatus()`

Gets the response status.

**Returns:** ResponseStatus object

**Built-in statuses:**
- `ResponseStatus.SUCCESS` - Request completed successfully
- `ResponseStatus.ERROR` - Error occurred
- `ResponseStatus.TIMEOUT` - Request timed out
- `ResponseStatus.NOT_FOUND` - Resource not found
- `ResponseStatus.INVALID` - Invalid request
- `ResponseStatus.PENDING` - Request pending

**Custom statuses:** Developers can create custom statuses using `ResponseStatus.custom("STATUS_NAME")`

---

#### `String getRawMessage()`

Gets the raw JSON message.

**Returns:** Original message string

---

## MessageRequest

Builder for creating request-response messages with automatic timeout handling.

**Package:** `io.hydrogendevelopments.celesmq.message`

### Builder Methods

#### `static MessageRequest create(RabbitMQClient client, Logger logger, String replyQueue)`

Creates a new request builder. Usually called via `RabbitMQManager.request()`.

---

#### `MessageRequest action(String action)`

Sets the action name.

**Parameters:**
- `action` - Action identifier

**Returns:** `this` for chaining

---

#### `MessageRequest param(String key, Object value)`

Adds a parameter.

**Parameters:**
- `key` - Parameter name
- `value` - Parameter value

**Returns:** `this` for chaining

**Example:**
```java
request()
    .action("createParty")
    .param("leader", player.getName())
    .param("leader_id", playerId)
    .param("max_size", 8)
    .param("public", false)
```

---

#### `MessageRequest timeout(long timeoutMs)`

Sets request timeout in milliseconds.

**Parameters:**
- `timeoutMs` - Timeout duration (required - no default)

**Returns:** `this` for chaining

---

#### `CompletableFuture<MessageResponse> sendTo(String exchange)`

Sends the request to an exchange.

**Parameters:**
- `exchange` - Target exchange name

**Returns:** CompletableFuture<MessageResponse>

**Example:**
```java
mq.request()
    .action("getPlayerCoins")
    .param("uuid", uuid)
    .timeout(2000)
    .sendTo("game-server")
    .thenAccept(response -> {
        if (response.getStatus().equals(ResponseStatus.TIMEOUT)) {
            player.sendMessage("Request timed out!");
            return;
        }

        if (response.isSuccess()) {
            int coins = response.getInt("coins");
            player.sendMessage("You have " + coins + " coins!");
        } else {
            player.sendMessage("Error: " + response.getString("error"));
        }
    })
    .exceptionally(throwable -> {
        getLogger().severe("Request failed: " + throwable.getMessage());
        return null;
    });
```

---

## ResponseStatus

Extensible response status class that allows both built-in and custom status types.

**Package:** `io.hydrogendevelopments.celesmq.message`

### Built-in Statuses

The library provides these standard response statuses:

```java
ResponseStatus.SUCCESS      // Request completed successfully
ResponseStatus.ERROR        // Error occurred during processing
ResponseStatus.TIMEOUT      // Request timed out
ResponseStatus.NOT_FOUND    // Resource not found
ResponseStatus.INVALID      // Invalid request
ResponseStatus.PENDING      // Request is pending
```

### Custom Statuses

Developers can create custom statuses for application-specific scenarios:

```java
// Create custom statuses
ResponseStatus rateLimited = ResponseStatus.custom("RATE_LIMITED");
ResponseStatus authRequired = ResponseStatus.custom("AUTH_REQUIRED");
ResponseStatus maintenance = ResponseStatus.custom("MAINTENANCE_MODE");

// Use in responses
MessageResponse response = new MessageResponse(json, rateLimited);

// Check custom statuses
if (response.getStatus().equals(rateLimited)) {
    // Handle rate limiting
}
```

### Methods

#### `static ResponseStatus custom(String name)`

Creates a custom response status.

**Parameters:**
- `name` - Status name (case-insensitive, will be converted to uppercase)

**Returns:** New ResponseStatus instance

**Example:**
```java
ResponseStatus customStatus = ResponseStatus.custom("PAYMENT_REQUIRED");
```

---

#### `static ResponseStatus fromString(String name)`

Parses a status name and returns the corresponding ResponseStatus. Returns a built-in status if the name matches, otherwise creates a custom status.

**Parameters:**
- `name` - Status name to parse

**Returns:** Matching built-in status or new custom status

**Example:**
```java
ResponseStatus status = ResponseStatus.fromString("SUCCESS"); // Returns ResponseStatus.SUCCESS
ResponseStatus custom = ResponseStatus.fromString("MY_STATUS"); // Creates custom status
```

---

#### `String getName()`

Gets the name of this status.

**Returns:** Status name (always uppercase)

---

#### `boolean isSuccess()`

Checks if this status represents success.

**Returns:** `true` if this is SUCCESS status

---

#### `boolean isError()`

Checks if this status represents an error.

**Returns:** `true` if this is ERROR status

---

#### `boolean isTimeout()`

Checks if this status represents a timeout.

**Returns:** `true` if this is TIMEOUT status

---

### Usage Examples

#### Built-in Status Checks

```java
mq.request()
    .action("getData")
    .timeout(5000)
    .sendTo("server")
    .thenAccept(response -> {
        if (response.getStatus().equals(ResponseStatus.TIMEOUT)) {
            // Handle timeout
        } else if (response.getStatus().equals(ResponseStatus.ERROR)) {
            // Handle error
        } else if (response.getStatus().isSuccess()) {
            // Process successful response
        }
    });
```

#### Custom Status for Domain Logic

```java
// Define custom statuses for your application
public class AppStatus {
    public static final ResponseStatus RATE_LIMITED = ResponseStatus.custom("RATE_LIMITED");
    public static final ResponseStatus BANNED = ResponseStatus.custom("BANNED");
    public static final ResponseStatus VIP_REQUIRED = ResponseStatus.custom("VIP_REQUIRED");
}

// Use in server responses
mq.on("purchase_item", msg -> {
    if (isRateLimited(player)) {
        String response = MessageBuilder.create("purchase_response")
            .add("status", "RATE_LIMITED")
            .add("error", "Too many requests")
            .build();
        mq.send("responses", response);
        return;
    }
    // Process purchase
});

// Handle custom statuses in client
response -> {
    if (response.getStatus().equals(AppStatus.RATE_LIMITED)) {
        player.sendMessage("You're doing that too fast!");
    }
}
```

---

## MessageRouter

Routes incoming messages to registered handlers based on action field.

**Package:** `io.hydrogendevelopments.celesmq.message`

### Methods

#### `MessageRouter on(String action, Consumer<MessageResponse> handler)`

Registers an action handler.

**Parameters:**
- `action` - Action name to match
- `handler` - Handler function

**Returns:** `this` for chaining

---

#### `MessageRouter onDefault(Consumer<MessageResponse> handler)`

Sets default handler for unmatched actions.

---

#### `MessageRouter onError(Consumer<Exception> handler)`

Sets error handler.

---

#### `void route(String message)`

Routes a message to appropriate handler. Called automatically by RabbitMQManager.

---

## MessageBuilder

Fluent builder for constructing JSON messages.

**Package:** `io.hydrogendevelopments.celesmq.message`

### Static Methods

#### `static MessageBuilder create(String action)`

Creates a new message builder with an action.

**Parameters:**
- `action` - Action name

**Returns:** MessageBuilder instance

---

### Builder Methods

#### `MessageBuilder add(String key, Object value)`

Adds a field to the message.

**Parameters:**
- `key` - Field name
- `value` - Field value (String, int, boolean, Map, List, etc.)

**Returns:** `this` for chaining

**Example:**
```java
String message = MessageBuilder.create("player_update")
    .add("player", "Steve")
    .add("uuid", uuid.toString())
    .add("level", 5)
    .add("premium", true)
    .add("server", "server-1")
    .add("timestamp", System.currentTimeMillis())
    .add("metadata", Map.of("coins", 100, "rank", "VIP"))
    .build();

mq.send("events", message);
```

---

#### `String build()`

Builds and returns the JSON string.

**Returns:** JSON message string

---

## RabbitMQConfig

Configuration for RabbitMQ connections, channels, and behavior.

**Package:** `io.hydrogendevelopments.celesmq.config`

### Builder

#### `RabbitMQConfig.Builder()`

Creates a new configuration builder. **All connection parameters are required** and must be explicitly configured:
- host: Required
- port: Required
- username: Required
- password: Required
- virtualHost: Required
- connectionTimeout: Required
- networkRecoveryInterval: Required
- automaticRecoveryEnabled: Required
- consumerName: Optional (auto-generated if not set)
- autoSubscribe: Optional (must be explicitly set if using channels)

---

### Connection Builder Methods

#### `Builder host(String host)`

Sets RabbitMQ server hostname.

---

#### `Builder port(int port)`

Sets server port (required - typically 5672 for standard RabbitMQ).

---

#### `Builder username(String username)`

Sets authentication username.

---

#### `Builder password(String password)`

Sets authentication password.

---

#### `Builder virtualHost(String virtualHost)`

Sets virtual host (required - typically "/" for default vhost).

---

#### `Builder connectionTimeout(int connectionTimeout)`

Sets connection timeout in milliseconds.

---

#### `Builder networkRecoveryInterval(int networkRecoveryInterval)`

Sets recovery interval in milliseconds.

---

#### `Builder automaticRecoveryEnabled(boolean enabled)`

Enables/disables automatic connection recovery.

---

### Channel Builder Methods

#### `Builder consumerName(String consumerName)`

Sets consumer/queue name for receiving messages.

**Parameters:**
- `consumerName` - Consumer identifier

**Example:**
```java
.consumerName("consumer-1")
.consumerName("proxy-server-1")
```

---

#### `Builder addChannel(String channelName, String exchangeName)`

Adds a channel (name -> exchange mapping).

**Parameters:**
- `channelName` - Logical channel name
- `exchangeName` - RabbitMQ exchange name

**Example:**
```java
.addChannel("commands", "commands-exchange")
.addChannel("events", "events-exchange")
.addChannel("updates", "updates-exchange")
```

---

#### `Builder channels(Map<String, String> channels)`

Adds multiple channels at once.

**Parameters:**
- `channels` - Map of channel name -> exchange name

**Example:**
```java
Map<String, String> channels = new HashMap<>();
channels.put("commands", "commands-exchange");
channels.put("events", "events-exchange");

config.channels(channels);
```

---

#### `Builder autoSubscribe(boolean autoSubscribe)`

Sets whether to automatically subscribe to configured channels on connect.

**Parameters:**
- `autoSubscribe` - Auto-subscribe flag (required if using channels)

---

### Complete Config Example

```java
RabbitMQConfig config = new RabbitMQConfig.Builder()
    .host("localhost")
    .port(5672)
    .username("guest")
    .password("guest")
    .virtualHost("/")
    .connectionTimeout(10000)
    .automaticRecoveryEnabled(true)
    .consumerName("consumer-1")
    .addChannel("commands", "commands-exchange")
    .addChannel("events", "events-exchange")
    .addChannel("updates", "updates-exchange")
    .autoSubscribe(true)
    .build();
```

---

### Loading from Config File

```java
// YAML: rabbitmq.channels.commands = "commands-exchange"
RabbitMQConfig config = new RabbitMQConfig.Builder()
    .host(getConfig().getString("rabbitmq.host"))
    .port(getConfig().getInt("rabbitmq.port"))
    .username(getConfig().getString("rabbitmq.username"))
    .password(getConfig().getString("rabbitmq.password"))
    .consumerName(getConfig().getString("rabbitmq.consumer"))
    .addChannel("commands", getConfig().getString("rabbitmq.channels.commands"))
    .addChannel("events", getConfig().getString("rabbitmq.channels.events"))
    .addChannel("updates", getConfig().getString("rabbitmq.channels.updates"))
    .autoSubscribe(getConfig().getBoolean("rabbitmq.autoSubscribe"))
    .build();
```

---

## Platform

Platform abstraction for all supported platforms (Spigot, BungeeCord, Velocity, Minestom, Sponge, Folia, NukkitX).

**Package:** `io.hydrogendevelopments.celesmq.platform`

### Implementations

#### `SpigotPlatform(JavaPlugin plugin)`

Platform for Spigot/Paper servers.

**Example:**
```java
Platform platform = new SpigotPlatform(this);
RabbitMQManager mq = new RabbitMQManager(platform, config);
```

---

#### `BungeeCordPlatform(Plugin plugin)`

Platform for BungeeCord proxies.

**Example:**
```java
Platform platform = new BungeeCordPlatform(this);
RabbitMQManager mq = new RabbitMQManager(platform, config);
```

---

#### `VelocityPlatform(Object plugin, ProxyServer server, Logger logger)`

Platform for Velocity proxies.

**Example:**
```java
Platform platform = new VelocityPlatform(this, proxyServer, julLogger);
RabbitMQManager mq = new RabbitMQManager(platform, config);
```

---

#### `MinestomPlatform()`

Platform for Minestom servers.

**Example:**
```java
Platform platform = new MinestomPlatform();
RabbitMQManager mq = new RabbitMQManager(platform, config);
```

---

#### `SpongePlatform(PluginContainer plugin, Server server, Logger logger)`

Platform for Sponge (Forge/Fabric plugin platform).

**Example:**
```java
Platform platform = new SpongePlatform(pluginContainer, server, logger);
RabbitMQManager mq = new RabbitMQManager(platform, config);
```

---

#### `FoliaPlatform(Plugin plugin)`

Platform for Folia (Paper's multithreaded fork).

**Example:**
```java
Platform platform = new FoliaPlatform(this);
RabbitMQManager mq = new RabbitMQManager(platform, config);
```

---

#### `NukkitPlatform(Plugin plugin)`

Platform for NukkitX/PowerNukkitX (Bedrock Edition servers).

**Example:**
```java
Platform platform = new NukkitPlatform(this);
RabbitMQManager mq = new RabbitMQManager(platform, config);
```

## RPC

Remote Procedure Call system for calling methods on other servers.

**Package:** `io.hydrogendevelopments.celesmq.rpc`

### Constructor

#### `RPC(RabbitMQManager manager, String rpcCallAction, String rpcResponseAction)`

Creates an RPC system with specified action names.

**Parameters:**
- `manager` - RabbitMQManager instance
- `rpcCallAction` - Action name for RPC calls (must be unique to your application)
- `rpcResponseAction` - Action name for RPC responses (must be unique to your application)

**Example:**
```java
// Create namespaced RPC to avoid conflicts with other applications
RPC rpc = new RPC(mq, "myapp_rpc_call", "myapp_rpc_response");
```

**Important:** Choose unique action names for your application to avoid conflicts.

---

### Methods

#### `void register(String procedureName, Function<MessageResponse, Object> handler)`

Registers a procedure that can be called remotely.

**Parameters:**
- `procedureName` - Procedure identifier
- `handler` - Function that processes request and returns result

**Example:**
```java
rpc.register("getOnlineCount", request -> {
    return getProxy().getOnlineCount();
});

rpc.register("getPlayerCoins", request -> {
    String uuid = request.getString("uuid");
    return playerData.get(uuid).getCoins();
});

rpc.register("addCoins", request -> {
    String uuid = request.getString("uuid");
    int amount = request.getInt("amount");
    PlayerData data = playerData.get(uuid);
    data.addCoins(amount);
    return "success";
});
```

---

#### `RPCRequest call(String targetServer, String procedureName)`

Creates an RPC request to call a remote procedure.

**Parameters:**
- `targetServer` - Target server/consumer name
- `procedureName` - Procedure to call

**Returns:** RPCRequest builder

**Example:**
```java
// Simple call
rpc.call("game-server", "getOnlineCount")
    .execute()
    .thenAccept(response -> {
        int count = response.getResultAsInt();
        getLogger().info("Players online: " + count);
    });

// Call with arguments
rpc.call("game-server", "getPlayerCoins")
    .arg("uuid", player.getUniqueId().toString())
    .timeout(2000)
    .execute()
    .thenAccept(response -> {
        if (response.isSuccess()) {
            int coins = response.getResultAsInt();
            player.sendMessage("You have " + coins + " coins!");
        }
    });

// Multiple parallel calls
CompletableFuture<Integer> server1 = rpc.call("server-1", "getOnlinePlayers")
    .execute()
    .thenApply(r -> r.getResultAsInt());

CompletableFuture<Integer> server2 = rpc.call("server-2", "getOnlinePlayers")
    .execute()
    .thenApply(r -> r.getResultAsInt());

CompletableFuture.allOf(server1, server2).thenRun(() -> {
    int total = server1.join() + server2.join();
    getLogger().info("Total players: " + total);
});
```

---

## EventBus

Cross-server event system that works on all platforms.

**Package:** `io.hydrogendevelopments.celesmq.event`

### Constructor

#### `EventBus(RabbitMQManager manager, String sourceIdentifier, String eventExchange, String eventAction, boolean ignoreOwnEvents)`

Creates an event bus with specified exchange and action names.

**Parameters:**
- `manager` - RabbitMQManager instance
- `sourceIdentifier` - Identifier for this server/consumer
- `eventExchange` - Exchange name for events (must be unique to your application)
- `eventAction` - Action name for event messages (must be unique to your application)
- `ignoreOwnEvents` - Whether to ignore events dispatched by this server (true to ignore, false to process)

**Example:**
```java
// Create namespaced EventBus that ignores its own events
EventBus eventBus = new EventBus(mq, "consumer-1", "myapp-events", "myapp_event", true);

// Or process all events including own
EventBus eventBus = new EventBus(mq, "consumer-1", "myapp-events", "myapp_event", false);
```

**Important:** Choose unique exchange and action names for your application to avoid conflicts.

---

### Methods

#### `<T extends CrossServerEvent> void on(String eventType, Consumer<T> handler)`

Registers an event handler.

**Parameters:**
- `eventType` - Event type identifier
- `handler` - Event handler function

**Example:**
```java
// Listen for your custom events
eventBus.on("player_join", (CrossServerEvent event) -> {
    String player = (String) event.getData("playerName");
    String server = event.getSourceServer();
    getLogger().info(player + " joined on " + server);
});

eventBus.on("coin_update", (CrossServerEvent event) -> {
    String uuid = (String) event.getData("playerUuid");
    int coins = (int) event.getData("coins");
    updatePlayerCoins(uuid, coins);
});
```

---

#### `void dispatch(CrossServerEvent event)`

Dispatches an event to all servers.

**Parameters:**
- `event` - Event to dispatch

**Example:**
```java
// Create and dispatch your custom events
public class PlayerJoinEvent extends CrossServerEvent {
    private final UUID playerUuid;
    private final String playerName;

    public PlayerJoinEvent(String source, UUID uuid, String name) {
        super(source);
        this.playerUuid = uuid;
        this.playerName = name;
        setData("playerUuid", uuid.toString());
        setData("playerName", name);
    }

    @Override
    public String getEventType() {
        return "player_join";
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
}

// Dispatch the event
PlayerJoinEvent event = new PlayerJoinEvent(
    "server-1",
    player.getUniqueId(),
    player.getName()
);
eventBus.dispatch(event);
```

---



### Custom Events

```java
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

// Listen for custom event
eventBus.on("coin_update", (CoinUpdateEvent event) -> {
    updateScoreboard(event.getPlayerUuid(), event.getNewBalance());
});

// Dispatch custom event
eventBus.dispatch(new CoinUpdateEvent("shop-server", uuid, 1000));
```

---

## MessageMiddleware

Interface for intercepting and modifying messages.

**Package:** `io.hydrogendevelopments.celesmq.middleware`

### Methods

#### `String beforeSend(String message)`

Called before a message is sent. Return null to cancel.

---

#### `MessageResponse afterReceive(MessageResponse message)`

Called after a message is received. Return null to cancel processing.

---

#### `int getPriority()`

Returns middleware priority (higher = runs first).

---

### Examples

```java
// Logging middleware
public class LoggingMiddleware implements MessageMiddleware {
    @Override
    public String beforeSend(String message) {
        logger.info("Sending: " + message);
        return message;
    }

    @Override
    public MessageResponse afterReceive(MessageResponse message) {
        logger.info("Received: " + message.getRawMessage());
        return message;
    }

    @Override
    public int getPriority() {
        return 0;
    }
}

// Authentication middleware
public class AuthMiddleware implements MessageMiddleware {
    private final String apiKey;

    public AuthMiddleware(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String beforeSend(String message) {
        // Add API key to outgoing messages
        return message.replace("}", ",\"apiKey\":\"" + apiKey + "\"}");
    }

    @Override
    public MessageResponse afterReceive(MessageResponse message) {
        // Validate incoming messages
        String key = message.getString("apiKey");
        if (!apiKey.equals(key)) {
            logger.warning("Invalid API key!");
            return null; // Cancel processing
        }
        return message;
    }

    @Override
    public int getPriority() {
        return 1000; // Run first
    }
}
```

---

## BatchPublisher

Batches messages for improved performance.

**Package:** `io.hydrogendevelopments.celesmq.batch`

### Constructor

#### `BatchPublisher(RabbitMQClient client, Logger logger)`

Creates a batch publisher.

---

### Methods

#### `BatchPublisher batchSize(int size)`

Sets batch size (messages per batch). **Required** - must be set before calling `start()`.

---

#### `BatchPublisher flushInterval(long intervalMs)`

Sets auto-flush interval in milliseconds. **Required** - must be set before calling `start()`.

---

#### `BatchPublisher start()`

Starts the batch publisher.

**Throws:** `IllegalStateException` if `batchSize` or `flushInterval` not configured.

---

#### `void stop()`

Stops and flushes remaining messages.

---

#### `CompletableFuture<Boolean> add(String exchange, String message)`

Adds a message to the batch.

---

### Example

```java
BatchPublisher batch = new BatchPublisher(mq.getClient(), getLogger())
    .batchSize(100)        // Send after 100 messages
    .flushInterval(1000)   // Or every 1 second
    .start();

// Add messages
for (ProxiedPlayer player : getProxy().getPlayers()) {
    String message = MessageBuilder.create("player_update")
        .add("uuid", player.getUniqueId().toString())
        .add("name", player.getName())
        .add("server", player.getServer().getInfo().getName())
        .build();

    batch.add("events-exchange", message);
}

// Cleanup
batch.stop(); // Flushes remaining messages
```

---

## RabbitMQMetrics

Tracks performance metrics for RabbitMQ operations.

**Package:** `io.hydrogendevelopments.celesmq.metrics`

### Constructor

#### `RabbitMQMetrics()`

Creates a new metrics tracker.

---

### Methods

#### `void recordSent(int bytes)`

Records a sent message.

---

#### `void recordReceived(int bytes)`

Records a received message.

---

#### `void recordProcessed(long processingTimeMs)`

Records a processed message with processing time.

---

#### `void recordFailed()`

Records a failed message.

---

#### `long getMessagesSent()`

Gets total messages sent.

---

#### `long getMessagesReceived()`

Gets total messages received.

---

#### `long getMessagesProcessed()`

Gets total messages processed.

---

#### `long getMessagesFailed()`

Gets total messages failed.

---

#### `double getAverageProcessingTime()`

Gets average processing time in milliseconds.

---

#### `long getPeakProcessingTime()`

Gets peak processing time in milliseconds.

---

#### `double getMessagesPerSecond()`

Gets messages per second rate.

---

#### `double getThroughputBytesPerSecond()`

Gets throughput in bytes per second.

---

#### `double getSuccessRate()`

Gets success rate (0.0 to 1.0).

---

#### `String getSummary()`

Gets formatted summary of all metrics.

---

### Example

```java
RabbitMQMetrics metrics = new RabbitMQMetrics();

// Track incoming messages
mq.on("*", msg -> {
long start = System.currentTimeMillis();
    metrics.recordReceived(msg.getRawMessage().length());

  try {
handleMessage(msg);
        metrics.recordProcessed(System.currentTimeMillis() - start);
  } catch (Exception e) {
  metrics.recordFailed();
    }
      });

// View metrics
getLogger().info(metrics.getSummary());

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
```

---

## RabbitMQClient

Low-level client for direct RabbitMQ operations. **For most use cases, use RabbitMQManager instead.**

**Package:** `io.hydrogendevelopments.celesmq`

### Constructor

#### `RabbitMQClient(Platform platform, RabbitMQConfig config)`

Creates a new client.

---

#### `RabbitMQClient(JavaPlugin plugin, RabbitMQConfig config)` (Deprecated)

Legacy constructor for Spigot. Use Platform version instead.

---

### Connection Methods

#### `boolean connect()`

Connects to RabbitMQ.

---

#### `void disconnect()`

Disconnects from RabbitMQ.

---

#### `boolean isConnected()`

Checks connection status.

---

### Publishing Methods

#### `CompletableFuture<Boolean> publishToQueue(String queueName, String message)`

Publishes to a queue.

---

#### `CompletableFuture<Boolean> publishToQueue(String queueName, String message, boolean persistent)`

Publishes with persistence option.

---

#### `CompletableFuture<Boolean> publishToExchange(String exchange, String routingKey, String message)`

Publishes to an exchange.

---

#### `CompletableFuture<Boolean> publishToExchange(String exchange, String routingKey, String message, String type, boolean persistent)`

Publishes with full options.

---

#### `CompletableFuture<Boolean> broadcast(String exchange, String message)`

Broadcasts to fanout exchange.

---

### Consuming Methods

#### `boolean consumeQueue(String queueName, MessageListener listener)`

Consumes from a queue.

---

#### `boolean consumeQueue(String queueName, MessageListener listener, boolean autoAck, boolean syncToMainThread)`

Consumes with full options.

---

#### `boolean subscribeToBroadcast(String exchange, MessageListener listener)`

Subscribes to broadcasts.

---

#### `boolean subscribeToBroadcast(String exchange, MessageListener listener, boolean syncToMainThread)`

Subscribes with thread control.

---

#### `boolean subscribeToTopic(String exchange, String pattern, MessageListener listener)`

Subscribes to topic exchange.

---

#### `boolean subscribeToTopic(String exchange, String pattern, MessageListener listener, boolean syncToMainThread)`

Subscribes with thread control.

---

## JsonSerializer

Utility for JSON serialization using Gson.

**Package:** `io.hydrogendevelopments.celesmq.util`

### Static Methods

#### `String toJson(Object object)`

Serializes object to JSON.

---

#### `String toPrettyJson(Object object)`

Serializes with pretty printing.

---

#### `<T> T fromJson(String json, Class<T> classOfT)`

Deserializes JSON to object.

---

#### `<T> T fromJson(String json, Type typeOfT)`

Deserializes with Type (for generics).

---

## Complete Example

```java
public class MyProxyPlugin extends Plugin {
  private RabbitMQManager mq;
  private RPC rpc;
  private EventBus eventBus;

  @Override
  public void onEnable() {
    // Load config
    RabbitMQConfig config = new RabbitMQConfig.Builder()
      .host(getConfig().getString("rabbitmq.host"))
      .port(getConfig().getInt("rabbitmq.port"))
      .username(getConfig().getString("rabbitmq.username"))
      .password(getConfig().getString("rabbitmq.password"))
      .consumerName(getConfig().getString("rabbitmq.consumer"))
      .addChannel("commands", getConfig().getString("rabbitmq.channels.commands"))
      .addChannel("events", getConfig().getString("rabbitmq.channels.events"))
      .addChannel("updates", getConfig().getString("rabbitmq.channels.updates"))
      .autoSubscribe(true)
      .build();

    // Create manager
    mq = new RabbitMQManager(new BungeeCordPlatform(this), config);

    if (mq.connect()) {
      setupRPC();
      setupEvents();
      setupHandlers();
      getLogger().info("RabbitMQ ready!");
    }
  }

  private void setupRPC() {
    rpc = new RPC(mq, "myapp_rpc_call", "myapp_rpc_response");
    rpc.register("getOnlineCount", req -> getProxy().getOnlineCount());
  }

  private void setupEvents() {
    eventBus = new EventBus(mq, getConfig().getString("rabbitmq.consumer"),
      "myapp-events", "myapp_event", true);
    eventBus.on("player_join", this::handlePlayerJoin);
  }

  private void setupHandlers() {
    mq.on("broadcast", msg -> {
      getProxy().broadcast(msg.getString("message"));
    });

    mq.on("kick", msg -> {
      int playerId = msg.getInt("player_id");
      String reason = msg.getString("reason", "Kicked");
      kickPlayer(playerId, reason);
    });
  }

  @Override
  public void onDisable() {
    if (mq != null) {
      mq.disconnect();
    }
  }
}
```

---

## Support

For more examples and documentation:
- [Easy Start Guide](EASY_START.md)
- [Advanced Features Guide](ADVANCED_FEATURES.md)
- [Config Examples](CONFIG_EXAMPLES.md)
- [Main README](../README.md)
