# RabbitMQ-Lib API Methods Guide

Complete reference guide for all classes and methods in the RabbitMQ-Lib library.

## Table of Contents

- [RabbitMQClient](#rabbitmqclient)

- [RabbitMQConfig](#rabbitmqconfig)

- [RabbitMQConnectionManager](#rabbitmqconnectionmanager)

- [RabbitMQPublisher](#rabbitmqpublisher)

- [RabbitMQConsumer](#rabbitmqconsumer)

- [JsonSerializer](#jsonserializer)

- [MessageListener](#messagelistener)

---

## RabbitMQClient

Main API class for interacting with RabbitMQ. This is the primary class you'll use in your plugin.

**Package:** `com.rabbitmq.minecraft`

### Constructor

#### `RabbitMQClient(JavaPlugin plugin, RabbitMQConfig config)`

Creates a new RabbitMQ client instance.

**Parameters:**

- `plugin` - Your JavaPlugin instance

- `config` - RabbitMQConfig configuration object

**Example:**

```java
RabbitMQConfig config = new RabbitMQConfig.Builder()
    .host("localhost")
    .port(5672)
    .build();
RabbitMQClient client = new RabbitMQClient(this, config);
```

---

### Connection Methods

#### `boolean connect()`

Establishes connection to the RabbitMQ server.

**Returns:** `true` if connection successful, `false` otherwise

**Example:**
```java
if (client.connect()) {
    getLogger().info("Connected to RabbitMQ!");
} else {
    getLogger().severe("Failed to connect!");
}
```

---

#### `void disconnect()`

Closes the RabbitMQ connection gracefully. Call this in your plugin's `onDisable()`.

**Example:**
```java
@Override
public void onDisable() {
    client.disconnect();
}
```

---

#### `boolean isConnected()`

 

Checks if the client is currently connected to RabbitMQ.

 

**Returns:** `true` if connected, `false` otherwise

 

**Example:**

```java

if (client.isConnected()) {

    client.publishToQueue("test", "Hello");

}

```

 

---

 

### Publishing Methods

 

#### `CompletableFuture<Boolean> publishToQueue(String queueName, String message)`

 

Publishes a message to a queue with default settings (non-persistent).

 

**Parameters:**

- `queueName` - Name of the queue

- `message` - Message content (String)

 

**Returns:** CompletableFuture that resolves to `true` if successful

 

**Example:**

```java

client.publishToQueue("player-events", "Player joined")

    .thenAccept(success -> {

        if (success) {

            getLogger().info("Message sent!");

        }

    });

```

 

---

 

#### `CompletableFuture<Boolean> publishToQueue(String queueName, String message, boolean persistent)`

 

Publishes a message to a queue with persistence option.

 

**Parameters:**

- `queueName` - Name of the queue

- `message` - Message content

- `persistent` - If true, message survives RabbitMQ restarts

 

**Returns:** CompletableFuture<Boolean>

 

**Example:**

```java

// Send persistent message

client.publishToQueue("important-tasks", taskJson, true)

    .thenAccept(success -> {

        getLogger().info("Critical task queued");

    });

```

 

---

 

#### `CompletableFuture<Boolean> publishToExchange(String exchangeName, String routingKey, String message)`

 

Publishes a message to an exchange with a routing key (direct exchange by default).

 

**Parameters:**

- `exchangeName` - Name of the exchange

- `routingKey` - Routing key for message routing

- `message` - Message content

 

**Returns:** CompletableFuture<Boolean>

 

**Example:**

```java

client.publishToExchange("server-exchange", "server.lobby", "Player count: 45")

    .thenAccept(success -> {

        getLogger().info("Published to exchange");

    });

```

 

---

 

#### `CompletableFuture<Boolean> publishToExchange(String exchangeName, String routingKey, String message, String exchangeType, boolean persistent)`

 

Publishes a message to an exchange with full customization.

 

**Parameters:**

- `exchangeName` - Name of the exchange

- `routingKey` - Routing key

- `message` - Message content

- `exchangeType` - Type: "direct", "fanout", "topic", or "headers"

- `persistent` - Message persistence flag

 

**Returns:** CompletableFuture<Boolean>

 

**Example:**

```java

// Publish to topic exchange with persistence

client.publishToExchange(

    "chat-exchange",

    "chat.global",

    messageJson,

    "topic",

    true

).thenAccept(success -> {

    getLogger().info("Chat message published");

});

```

 

---

 

#### `CompletableFuture<Boolean> publishWithHeaders(String exchangeName, String routingKey, String message, Map<String, Object> headers)`

 

Publishes a message with custom headers.

 

**Parameters:**

- `exchangeName` - Name of the exchange

- `routingKey` - Routing key

- `message` - Message content

- `headers` - Map of header key-value pairs

 

**Returns:** CompletableFuture<Boolean>

 

**Example:**

```java

Map<String, Object> headers = new HashMap<>();

headers.put("priority", "high");

headers.put("source", "lobby-server");

headers.put("timestamp", System.currentTimeMillis());

 

client.publishWithHeaders(

    "events",

    "player.action",

    eventJson,

    headers

);

```

 

---

 

#### `CompletableFuture<Boolean> broadcast(String exchangeName, String message)`

 

Broadcasts a message to all consumers subscribed to a fanout exchange.

 

**Parameters:**

- `exchangeName` - Name of the fanout exchange

- `message` - Message content

 

**Returns:** CompletableFuture<Boolean>

 

**Example:**

```java

// Broadcast server shutdown warning to all servers

String announcement = JsonSerializer.toJson(

    Map.of("type", "shutdown", "seconds", 60)

);

client.broadcast("server-announcements", announcement);

```

 

---

 

### Consuming Methods

 

#### `boolean consumeQueue(String queueName, MessageListener listener)`

 

Starts consuming messages from a queue with manual acknowledgment.

 

**Parameters:**

- `queueName` - Name of the queue

- `listener` - MessageListener to handle received messages

 

**Returns:** `true` if consumer started successfully

 

**Example:**

```java

client.consumeQueue("player-tasks", message -> {

    getLogger().info("Received: " + message);

    // Process message

});

```

 

---

 

#### `boolean consumeQueue(String queueName, MessageListener listener, boolean autoAck)`

 

Starts consuming with acknowledgment control.

 

**Parameters:**

- `queueName` - Name of the queue

- `listener` - MessageListener callback

- `autoAck` - If true, automatically acknowledge; if false, manual ack

 

**Returns:** `true` if successful

 

**Example:**

```java

// Manual acknowledgment for critical messages

client.consumeQueue("critical-tasks", message -> {

    try {

        processTask(message);

        // Message is acknowledged automatically on success

    } catch (Exception e) {

        // Message is rejected and requeued on error

        getLogger().severe("Failed to process: " + e.getMessage());

    }

}, false); // autoAck = false

```

 

---

 

#### `boolean consumeQueue(String queueName, MessageListener listener, boolean autoAck, boolean syncToMainThread)`

 

Full control over message consumption with thread synchronization.

 

**Parameters:**

- `queueName` - Name of the queue

- `listener` - MessageListener callback

- `autoAck` - Automatic acknowledgment flag

- `syncToMainThread` - If true, runs listener on Minecraft main thread

 

**Returns:** `true` if successful

 

**Example:**

```java

// Consume with Bukkit API calls

client.consumeQueue("player-commands", message -> {

    // Safe to use Bukkit API here

    Player player = Bukkit.getPlayer(playerName);

    if (player != null) {

        player.sendMessage("Command executed!");

    }

}, false, true); // Manual ack, sync to main thread

```

 

---

 

#### `boolean subscribeToBroadcast(String exchangeName, MessageListener listener)`

 

Subscribes to a fanout exchange to receive broadcast messages.

 

**Parameters:**

- `exchangeName` - Name of the fanout exchange

- `listener` - MessageListener callback

 

**Returns:** `true` if subscription successful

 

**Example:**

```java

// Subscribe to global announcements

client.subscribeToBroadcast("global-announcements", message -> {

    Map<String, Object> data = JsonSerializer.fromJson(message, Map.class);

    String announcement = (String) data.get("message");

    Bukkit.broadcastMessage("[Network] " + announcement);

});

```

 

---

 

#### `boolean subscribeToBroadcast(String exchangeName, MessageListener listener, boolean syncToMainThread)`

 

Subscribe to broadcasts with thread control.

 

**Parameters:**

- `exchangeName` - Name of the fanout exchange

- `listener` - MessageListener callback

- `syncToMainThread` - Thread synchronization flag

 

**Returns:** `true` if successful

 

**Example:**

```java

client.subscribeToBroadcast("player-events", message -> {

    // Process on main thread

    handlePlayerEvent(message);

}, true);

```

 

---

 

#### `boolean subscribeToTopic(String exchangeName, String routingKeyPattern, MessageListener listener)`

 

Subscribes to a topic exchange with pattern matching.

 

**Parameters:**

- `exchangeName` - Name of the topic exchange

- `routingKeyPattern` - Routing key pattern (supports wildcards: `*` and `#`)

- `listener` - MessageListener callback

 

**Returns:** `true` if successful

 

**Pattern Wildcards:**

- `*` - Matches exactly one word

- `#` - Matches zero or more words

 

**Example:**

```java

// Subscribe to all chat messages

client.subscribeToTopic("chat-exchange", "chat.#", message -> {

    getLogger().info("Chat: " + message);

});

 

// Subscribe to specific server types

client.subscribeToTopic("server-events", "server.*.status", message -> {

    // Matches: server.lobby.status, server.survival.status, etc.

});

```

 

---

 

#### `boolean subscribeToTopic(String exchangeName, String routingKeyPattern, MessageListener listener, boolean syncToMainThread)`

 

Subscribe to topics with thread control.

 

**Parameters:**

- `exchangeName` - Name of the topic exchange

- `routingKeyPattern` - Routing key pattern

- `listener` - MessageListener callback

- `syncToMainThread` - Thread synchronization flag

 

**Returns:** `true` if successful

 

**Example:**

```java

client.subscribeToTopic(

    "player-events",

    "player.*.join",

    message -> {

        // Safe Bukkit API usage

        processPlayerJoin(message);

    },

    true

);

```

 

---

 

### Utility Methods

 

#### `RabbitMQConnectionManager getConnectionManager()`

 

Gets the underlying connection manager.

 

**Returns:** RabbitMQConnectionManager instance

 

**Example:**

```java

RabbitMQConnectionManager manager = client.getConnectionManager();

RabbitMQConfig config = manager.getConfig();

```

 

---

 

#### `RabbitMQPublisher getPublisher()`

 

Gets the publisher instance for advanced usage.

 

**Returns:** RabbitMQPublisher instance

 

---

 

#### `RabbitMQConsumer getConsumer()`

 

Gets the consumer instance for advanced usage.

 

**Returns:** RabbitMQConsumer instance

 

---

 

#### `Map<String, String> getActiveConsumers()`

 

Gets all active consumers.

 

**Returns:** Map of queue/exchange names to consumer tags

 

**Example:**

```java

Map<String, String> consumers = client.getActiveConsumers();

getLogger().info("Active consumers: " + consumers.size());

for (String queue : consumers.keySet()) {

    getLogger().info("  - " + queue + ": " + consumers.get(queue));

}

```

 

---

 

## RabbitMQConfig

 

Configuration class for RabbitMQ connection settings.

 

**Package:** `com.rabbitmq.minecraft.config`

 

### Builder Pattern

 

#### `RabbitMQConfig.Builder()`

 

Creates a new configuration builder with default values.

 

**Default Values:**

- host: "localhost"

- port: 5672

- username: "guest"

- password: "guest"

- virtualHost: "/"

- connectionTimeout: 10000ms

- networkRecoveryInterval: 5000ms

- automaticRecoveryEnabled: true

 

---

 

### Builder Methods

 

#### `Builder host(String host)`

 

Sets the RabbitMQ server hostname or IP address.

 

**Parameters:**

- `host` - Hostname or IP (e.g., "localhost", "192.168.1.100")

 

**Returns:** Builder instance for chaining

 

---

 

#### `Builder port(int port)`

 

Sets the RabbitMQ server port.

 

**Parameters:**

- `port` - Port number (default: 5672)

 

**Returns:** Builder instance

 

---

 

#### `Builder username(String username)`

 

Sets the authentication username.

 

**Parameters:**

- `username` - RabbitMQ username

 

**Returns:** Builder instance

 

---

 

#### `Builder password(String password)`

 

Sets the authentication password.

 

**Parameters:**

- `password` - RabbitMQ password

 

**Returns:** Builder instance

 

---

 

#### `Builder virtualHost(String virtualHost)`

 

Sets the virtual host (namespace) to use.

 

**Parameters:**

- `virtualHost` - Virtual host name (default: "/")

 

**Returns:** Builder instance

 

---

 

#### `Builder connectionTimeout(int connectionTimeout)`

 

Sets the connection timeout in milliseconds.

 

**Parameters:**

- `connectionTimeout` - Timeout in milliseconds

 

**Returns:** Builder instance

 

---

 

#### `Builder networkRecoveryInterval(int networkRecoveryInterval)`

 

Sets the interval between recovery attempts.

 

**Parameters:**

- `networkRecoveryInterval` - Interval in milliseconds

 

**Returns:** Builder instance

 

---

 

#### `Builder automaticRecoveryEnabled(boolean automaticRecoveryEnabled)`

 

Enables or disables automatic connection recovery.

 

**Parameters:**

- `automaticRecoveryEnabled` - Recovery flag

 

**Returns:** Builder instance

 

---

 

#### `RabbitMQConfig build()`

 

Builds and returns the configuration object.

 

**Returns:** Configured RabbitMQConfig instance

 

**Complete Example:**

```java

RabbitMQConfig config = new RabbitMQConfig.Builder()

    .host("rabbitmq.example.com")

    .port(5672)

    .username("minecraft")

    .password("secure_password")

    .virtualHost("/production")

    .connectionTimeout(15000)

    .networkRecoveryInterval(3000)

    .automaticRecoveryEnabled(true)

    .build();

```

 

---

 

### Getter Methods

 

#### `String getHost()`

Returns the configured hostname.

 

#### `int getPort()`

Returns the configured port.

 

#### `String getUsername()`

Returns the username.

 

#### `String getPassword()`

Returns the password.

 

#### `String getVirtualHost()`

Returns the virtual host.

 

#### `int getConnectionTimeout()`

Returns the connection timeout in milliseconds.

 

#### `int getNetworkRecoveryInterval()`

Returns the network recovery interval.

 

#### `boolean isAutomaticRecoveryEnabled()`

Returns whether automatic recovery is enabled.

 

---

 

## RabbitMQConnectionManager

 

Manages RabbitMQ connections with automatic recovery.

 

**Package:** `com.rabbitmq.minecraft.connection`

 

### Constructor

 

#### `RabbitMQConnectionManager(JavaPlugin plugin, RabbitMQConfig config)`

 

Creates a new connection manager.

 

**Parameters:**

- `plugin` - JavaPlugin instance

- `config` - RabbitMQConfig configuration

 

---

 

### Methods

 

#### `boolean connect()`

 

Establishes connection to RabbitMQ server with automatic recovery setup.

 

**Returns:** `true` if successful

 

---

 

#### `void disconnect()`

 

Closes the connection gracefully.

 

---

 

#### `Channel getChannel()`

 

Gets the current channel, creating one if necessary.

 

**Returns:** Channel instance

 

**Throws:** IOException if channel creation fails

 

---

 

#### `Channel createChannel()`

 

Creates a new channel.

 

**Returns:** New Channel instance

 

**Throws:** IOException if connection not established

 

---

 

#### `boolean isConnected()`

 

Checks if connection is active.

 

**Returns:** `true` if connected

 

---

 

#### `JavaPlugin getPlugin()`

 

Gets the plugin instance.

 

**Returns:** JavaPlugin instance

 

---

 

#### `RabbitMQConfig getConfig()`

 

Gets the configuration.

 

**Returns:** RabbitMQConfig instance

 

---

 

## RabbitMQPublisher

 

Handles publishing messages to RabbitMQ.

 

**Package:** `com.rabbitmq.minecraft.messaging`

 

**Note:** Typically accessed through RabbitMQClient. All methods are asynchronous.

 

### Constructor

 

#### `RabbitMQPublisher(RabbitMQConnectionManager connectionManager)`

 

Creates a new publisher instance.

 

---

 

### Methods

 

All publishing methods return `CompletableFuture<Boolean>` and run asynchronously.

 

See [RabbitMQClient Publishing Methods](#publishing-methods) for detailed documentation.

 

---

 

## RabbitMQConsumer

 

Handles consuming messages from RabbitMQ.

 

**Package:** `com.rabbitmq.minecraft.messaging`

 

**Note:** Typically accessed through RabbitMQClient.

 

### Constructor

 

#### `RabbitMQConsumer(RabbitMQConnectionManager connectionManager)`

 

Creates a new consumer instance.

 

---

 

### Methods

 

#### `Map<String, String> getActiveConsumers()`

 

Gets all active consumers.

 

**Returns:** Map of queue/exchange names to consumer tags

 

See [RabbitMQClient Consuming Methods](#consuming-methods) for consumption method documentation.

 

---

 

## JsonSerializer

 

Utility class for JSON serialization and deserialization using Gson.

 

**Package:** `com.rabbitmq.minecraft.util`

 

### Static Methods

 

#### `String toJson(Object object)`

 

Serializes an object to compact JSON string.

 

**Parameters:**

- `object` - Object to serialize

 

**Returns:** JSON string

 

**Example:**

```java

Map<String, Object> data = Map.of(

    "player", "Steve",

    "action", "join",

    "timestamp", System.currentTimeMillis()

);

 

String json = JsonSerializer.toJson(data);

// Output: {"player":"Steve","action":"join","timestamp":1234567890}

```

 

---

 

#### `String toPrettyJson(Object object)`

 

Serializes an object to pretty-printed JSON string.

 

**Parameters:**

- `object` - Object to serialize

 

**Returns:** Pretty-printed JSON string

 

**Example:**

```java

String json = JsonSerializer.toPrettyJson(data);

// Output:

// {

//   "player": "Steve",

//   "action": "join",

//   "timestamp": 1234567890

// }

```

 

---

 

#### `<T> T fromJson(String json, Class<T> classOfT)`

 

Deserializes JSON string to an object.

 

**Parameters:**

- `json` - JSON string

- `classOfT` - Class type to deserialize to

 

**Returns:** Deserialized object or `null` if parsing fails

 

**Example:**

```java

// Deserialize to Map

Map<String, Object> data = JsonSerializer.fromJson(

    jsonString,

    Map.class

);

 

// Deserialize to custom class

PlayerData player = JsonSerializer.fromJson(

    jsonString,

    PlayerData.class

);

```

 

---

 

#### `<T> T fromJson(String json, Type typeOfT)`

 

Deserializes JSON with Type information (for generics).

 

**Parameters:**

- `json` - JSON string

- `typeOfT` - Type to deserialize to

 

**Returns:** Deserialized object or `null` if parsing fails

 

**Example:**

```java

// Deserialize to List<String>

Type listType = new TypeToken<List<String>>(){}.getType();

List<String> list = JsonSerializer.fromJson(jsonString, listType);

 

// Deserialize to Map<String, PlayerData>

Type mapType = new TypeToken<Map<String, PlayerData>>(){}.getType();

Map<String, PlayerData> map = JsonSerializer.fromJson(jsonString, mapType);

```

 

---

 

#### `Gson getGson()`

 

Gets the Gson instance with pretty printing.

 

**Returns:** Gson instance

 

---

 

#### `Gson getCompactGson()`

 

Gets the compact Gson instance (no pretty printing).

 

**Returns:** Gson instance

 

---

 

## MessageListener

 

Functional interface for handling received messages.

 

**Package:** `com.rabbitmq.minecraft.listener`

 

### Method

 

#### `void onMessageReceived(String message)`

 

Called when a message is received.

 

**Parameters:**

- `message` - The received message content

 

**Example:**

```java

MessageListener listener = message -> {

    getLogger().info("Received: " + message);

 

    Map<String, Object> data = JsonSerializer.fromJson(message, Map.class);

    if (data != null) {

        processMessage(data);

    }

};

 

client.consumeQueue("my-queue", listener);

```

 

**Lambda Example:**

```java

client.consumeQueue("events", msg -> handleEvent(msg));

```

 

**Method Reference Example:**

```java

client.consumeQueue("tasks", this::processTask);

```

 

---

 

## Common Patterns and Examples

 

### Pattern 1: Point-to-Point Queue

 

```java

// Producer

client.publishToQueue("work-queue", taskJson, true);

 

// Consumer

client.consumeQueue("work-queue", message -> {

    processWork(message);

}, false, false); // Manual ack, async

```

 

---

 

### Pattern 2: Publish-Subscribe (Fanout)

 

```java

// Publisher

client.broadcast("notifications", notificationJson);

 

// Subscribers (on multiple servers)

client.subscribeToBroadcast("notifications", message -> {

    showNotification(message);

}, true); // Sync to main thread

```

 

---

 

### Pattern 3: Topic-Based Routing

 

```java

// Publisher

client.publishToExchange(

    "logs",

    "server.lobby.error",

    logJson,

    "topic",

    false

);

 

// Subscriber 1: All logs

client.subscribeToTopic("logs", "server.#", this::logAll);

 

// Subscriber 2: Only errors

client.subscribeToTopic("logs", "server.*.error", this::logErrors);

 

// Subscriber 3: Only lobby

client.subscribeToTopic("logs", "server.lobby.#", this::logLobby);

```

 

---

 

### Pattern 4: Request-Reply

 

```java

// Requester

String requestId = UUID.randomUUID().toString();

Map<String, Object> request = Map.of(

    "id", requestId,

    "action", "getPlayerCount"

);

 

client.publishToQueue("requests", JsonSerializer.toJson(request));

 

// Listen for reply

client.consumeQueue("replies." + requestId, reply -> {

    Map<String, Object> response = JsonSerializer.fromJson(reply, Map.class);

    int count = (int) response.get("count");

    getLogger().info("Player count: " + count);

});

 

// Responder

client.consumeQueue("requests", message -> {

    Map<String, Object> req = JsonSerializer.fromJson(message, Map.class);

    String id = (String) req.get("id");

 

    Map<String, Object> response = Map.of(

        "id", id,

        "count", Bukkit.getOnlinePlayers().size()

    );

 

    client.publishToQueue("replies." + id, JsonSerializer.toJson(response));

});

```

 

---

 

## Best Practices

 

### 1. Always Disconnect in onDisable

 

```java

@Override

public void onDisable() {

    if (rabbitMQClient != null) {

        rabbitMQClient.disconnect();

    }

}

```

 

### 2. Use Sync for Bukkit API

 

```java

// CORRECT

client.consumeQueue("player-teleport", message -> {

    Player player = Bukkit.getPlayer(playerName);

    player.teleport(location); // Safe on main thread

}, false, true); // syncToMainThread = true

 

// INCORRECT - May cause ConcurrentModificationException

client.consumeQueue("player-teleport", message -> {

    Player player = Bukkit.getPlayer(playerName);

    player.teleport(location); // UNSAFE - not on main thread

}, false, false);

```

 

### 3. Handle JSON Parsing Errors

 

```java

client.consumeQueue("events", message -> {

    Map<String, Object> data = JsonSerializer.fromJson(message, Map.class);

    if (data == null) {

        getLogger().warning("Failed to parse message: " + message);

        return;

    }

 

    processEvent(data);

});

```

 

### 4. Use Persistent Messages for Critical Data

 

```java

// Critical data - survives RabbitMQ restart

client.publishToQueue("player-bans", banJson, true);

 

// Temporary data - no persistence needed

client.publishToQueue("chat-messages", chatJson, false);

```

 

### 5. Use Manual Ack for Important Processing

 

```java

client.consumeQueue("important-tasks", message -> {

    try {

        processImportantTask(message);

        // Automatically acknowledged on success

    } catch (Exception e) {

        // Automatically rejected and requeued on error

        throw e;

    }

}, false); // autoAck = false

```

 

---

 

## Error Handling

 

### Connection Failures

 

```java

if (!client.connect()) {

    getLogger().severe("Failed to connect to RabbitMQ!");

    getLogger().severe("Check your configuration and RabbitMQ server status");

    // Optionally disable plugin features

}

```

 

### Publish Failures

 

```java

client.publishToQueue("test", message)

    .thenAccept(success -> {

        if (!success) {

            getLogger().severe("Failed to publish message!");

        }

    })

    .exceptionally(throwable -> {

        getLogger().severe("Error publishing: " + throwable.getMessage());

        return null;

    });

```

 

### Consumer Failures

 

```java

boolean started = client.consumeQueue("test", message -> {

    try {

        processMessage(message);

    } catch (Exception e) {

        getLogger().severe("Error processing message: " + e.getMessage());

        e.printStackTrace();

    }

});

 

if (!started) {

    getLogger().severe("Failed to start consumer!");

}

```

 

---

 

## Performance Tips

 

1. **Reuse RabbitMQClient** - Create once, use throughout plugin lifecycle

2. **Use Async Processing** - Set `syncToMainThread = false` for heavy tasks

3. **Batch Messages** - Combine multiple small messages when possible

4. **Use Appropriate Exchange Types** - Choose based on routing needs

5. **Monitor Active Consumers** - Check `getActiveConsumers()` periodically

 

---

 

## Support

 

For more examples and support:

- Check the [example plugin](../examples/example-plugin/)

- Review the [README](../README.md)

- Open an issue on GitHub
