# CelesMQ Examples

This directory contains example plugins demonstrating cross-server communication using CelesMQ across different Minecraft platforms.

## Examples Overview

| Example | Platform | Description |
|---------|----------|-------------|
| [example-plugin](./example-plugin) | Spigot/Paper | Backend game server that broadcasts events and handles RPC calls |
| [example-bungeecord](./example-bungeecord) | BungeeCord | Proxy server that listens for events and communicates with game servers |
| [example-velocity](./example-velocity) | Velocity | Modern proxy server with the same capabilities as BungeeCord |

## Architecture

```
┌─────────────────┐
│   BungeeCord    │
│   or Velocity   │◄────┐
│     Proxy       │     │
└────────┬────────┘     │
         │              │
         │   RabbitMQ   │
         │   Message    │
         │   Broker     │
         │              │
┌────────▼────────┐     │
│  Spigot Server  ├─────┘
│   (survival-1)  │
└─────────────────┘
         │
┌────────▼────────┐
│  Spigot Server  │
│   (survival-2)  │
└─────────────────┘
```

## Cross-Server Communication Examples

### Scenario 1: Player Join Event

**Flow:**
1. Player joins Spigot server `survival-1`
2. Spigot broadcasts `player_join` event via EventBus
3. BungeeCord/Velocity proxy receives the event
4. Proxy logs the join and can trigger additional actions

**Spigot Code:**
```java
// When player joins
EventBus.GenericCrossServerEvent event = new EventBus.GenericCrossServerEvent(
    "survival-1",
    "player_join",
    new JsonObject()
);
event.setData("playerName", player.getName());
event.setData("uuid", player.getUniqueId().toString());
eventBus.dispatch(event);
```

**Proxy Code (BungeeCord/Velocity):**
```java
// Listen for player joins
eventBus.on("player_join", event -> {
    String playerName = (String) event.getData("playerName");
    String serverName = event.getSourceServer();
    logger.info("Player {} joined server {}", playerName, serverName);
});
```

### Scenario 2: RPC Call from Proxy to Spigot

**Flow:**
1. Proxy wants to know how many players are on `survival-1`
2. Proxy calls RPC method `getOnlineCount` on `survival-1`
3. Spigot receives the call, executes the procedure
4. Spigot returns the player count
5. Proxy receives the response

**Proxy Code:**
```java
// Call RPC on Spigot server
rpc.call("survival-1", "getOnlineCount")
   .timeout(5000)
   .execute()
   .thenAccept(response -> {
       if (response.isSuccess()) {
           int count = response.getResultAsInt();
           logger.info("Server has {} players", count);
       }
   });
```

**Spigot Code:**
```java
// Register RPC procedure
rpc.register("getOnlineCount", req -> Bukkit.getOnlinePlayers().size());
```

### Scenario 3: Broadcast from Proxy to All Spigot Servers

**Flow:**
1. Proxy wants to broadcast a network-wide message
2. Proxy broadcasts event via EventBus
3. All Spigot servers receive the event
4. Each Spigot server displays the message to its players

**Proxy Code:**
```java
// Broadcast network event
EventBus.GenericCrossServerEvent event = new EventBus.GenericCrossServerEvent(
    "velocity-proxy-1",
    "network_broadcast",
    new JsonObject()
);
event.setData("message", "Server restarting in 5 minutes!");
eventBus.dispatch(event);
```

**Spigot Code:**
```java
// Listen for network broadcasts
eventBus.on("network_broadcast", event -> {
    String message = (String) event.getData("message");
    Bukkit.broadcastMessage(ChatColor.RED + "[Network] " + message);
});
```

## Configuration

All examples require RabbitMQ configuration. Each platform has its own config format:

### Spigot (config.yml)
```yaml
rabbitmq:
  host: "localhost"
  port: 5672
  username: "guest"
  password: "guest"
  virtualHost: "/"
  connectionTimeout: 10000
  networkRecoveryInterval: 5000
  automaticRecoveryEnabled: true
  consumer: "survival-1"  # Unique name for this server
```

### BungeeCord (config.yml)
```yaml
rabbitmq:
  host: "localhost"
  port: 5672
  username: "guest"
  password: "guest"
  virtualHost: "/"
  connectionTimeout: 10000
  networkRecoveryInterval: 5000
  automaticRecoveryEnabled: true
  consumer: "bungeecord-proxy-1"  # Unique name for this proxy
```

### Velocity (config.properties)
```properties
rabbitmq.host=localhost
rabbitmq.port=5672
rabbitmq.username=guest
rabbitmq.password=guest
rabbitmq.virtualHost=/
rabbitmq.connectionTimeout=10000
rabbitmq.networkRecoveryInterval=5000
rabbitmq.automaticRecoveryEnabled=true
rabbitmq.consumer=velocity-proxy-1
```

## Important: Consumer Names

Each server/proxy MUST have a **unique consumer name**. This is critical for:
- Preventing message duplication
- Identifying event sources
- Routing RPC calls correctly

Example naming scheme:
- Spigot servers: `survival-1`, `survival-2`, `creative-1`, `lobby-1`
- Proxies: `bungeecord-proxy-1`, `velocity-proxy-1`

## Building the Examples

### All Examples
```bash
cd examples
mvn clean package
```

### Individual Example
```bash
cd examples/example-plugin
mvn clean package
```

## Running the Examples

### Prerequisites
1. Install and start RabbitMQ:
   ```bash
   docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
   ```

2. Configure all plugins with the same RabbitMQ connection details

3. Ensure each server/proxy has a unique `consumer` name

### Setup
1. Build all examples: `mvn clean package`
2. Copy JARs to respective servers:
   - `example-plugin/target/*.jar` → Spigot `plugins/` folder
   - `example-bungeecord/target/*.jar` → BungeeCord `plugins/` folder
   - `example-velocity/target/*.jar` → Velocity `plugins/` folder
3. Configure each plugin (see Configuration section)
4. Start servers and observe cross-server communication

## Testing Cross-Server Communication

### Test 1: Event Broadcasting
1. Join a Spigot server
2. Check proxy logs for "Player X joined server Y" message
3. This confirms EventBus is working

### Test 2: RPC Calls
1. On Spigot, use a command that triggers RPC (if implemented)
2. Or call RPC programmatically from proxy
3. Verify response is received

### Test 3: Network Broadcast
1. Send a network-wide event from proxy
2. Verify all Spigot servers receive it
3. Check that message appears on all servers

## Common Issues

### Events not being received
- **Check consumer names**: Each server must have a unique name
- **Check exchange names**: EventBus must use the same exchange name (`network-events` in examples)
- **Check action names**: EventBus must use the same action name (`network_event` in examples)
- **Check RabbitMQ connection**: Verify all servers can connect to RabbitMQ

### RPC calls timing out
- **Check consumer names**: Target server must have the correct consumer name
- **Check RPC action names**: Must match between caller and receiver (`network_rpc_call`, `network_rpc_response`)
- **Check procedure is registered**: Ensure the RPC procedure exists on the target server
- **Check timeout value**: 5000ms might not be enough for slow servers

### Connection errors
- **RabbitMQ not running**: Start RabbitMQ server
- **Wrong credentials**: Check username/password in config
- **Network issues**: Ensure servers can reach RabbitMQ host
- **Port blocked**: Check firewall rules for port 5672

## Advanced Usage

### Custom Events
Create your own event types:
```java
eventBus.on("economy_update", event -> {
    String playerName = (String) event.getData("player");
    double balance = ((Number) event.getData("balance")).doubleValue();
    // Handle economy update
});
```

### Custom RPC Procedures
Register custom procedures:
```java
rpc.register("getPlayerData", req -> {
    String playerName = req.getString("player");
    // Return player data as Object
    return getPlayerData(playerName);
});
```

### Message Batching
For high-throughput scenarios:
```java
BatchPublisher batch = new BatchPublisher(mq.getClient(), getLogger())
    .batchSize(100)
    .flushInterval(1000)
    .start();

batch.add("network-events", message);
```

## Version Compatibility

These examples are for **CelesMQ 2.0.0+** which requires explicit configuration of all parameters.

### Migration from 1.x
If upgrading from 1.x, you MUST:
1. Add `ignoreOwnEvents` parameter to all `EventBus` constructors
2. Explicitly configure ALL `RabbitMQConfig.Builder` parameters
3. Add `.timeout()` calls to all `MessageRequest` and RPC calls
4. Add `.batchSize()` and `.flushInterval()` to all `BatchPublisher` instances

## Further Reading

- [Main Documentation](../docs/README.md)
- [API Guide](../docs/API_GUIDE.md)
- [Advanced Features](../docs/ADVANCED_FEATURES.md)
- [Configuration Examples](../docs/CONFIG_EXAMPLES.md)

## Support

For issues or questions:
- Open an issue on [GitHub](https://github.com/1nstagram1/CelesMQ/issues)
- Check the [documentation](../docs/)
