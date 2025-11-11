# Configuration Examples

This guide shows how to configure RabbitMQ from config files instead of hardcoding values.

## YAML Configuration (BungeeCord/Spigot)

### config.yml
```yaml
rabbitmq:
  host: localhost
  port: 5672
  username: guest
  password: guest
  virtualHost: /
  connectionTimeout: 10000
  automaticRecoveryEnabled: true

  # Consumer name (queue name for receiving messages)
  consumer: "consumer-1"

  # Auto-subscribe to these channels on connect
  autoSubscribe: true

  # Channels (name -> exchange mapping)
  channels:
    commands: "commands-exchange"
    events: "events-exchange"
    updates: "updates-exchange"
    notifications: "notifications-exchange"
```

## Loading from Config

### BungeeCord Example

```java
import io.hydrodevelopments.celesmq.RabbitMQManager;
import config.io.hydrodevelopments.celesmq.RabbitMQConfig;
import platform.io.hydrodevelopments.celesmq.BungeeCordPlatform;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;

public class MyBungeePlugin extends Plugin {
  private RabbitMQManager mq;

  @Override public void onEnable() {
    // Load config
    Configuration config = getConfig();

    // Build RabbitMQ config from YAML
    RabbitMQConfig rabbitConfig = new RabbitMQConfig.Builder().host(config.getString("rabbitmq.host"))
      .port(config.getInt("rabbitmq.port"))
      .username(config.getString("rabbitmq.username"))
      .password(config.getString("rabbitmq.password"))
      .virtualHost(config.getString("rabbitmq.virtualHost"))
      .connectionTimeout(config.getInt("rabbitmq.connectionTimeout"))
      .automaticRecoveryEnabled(config.getBoolean("rabbitmq.automaticRecoveryEnabled"))
      .consumerName(config.getString("rabbitmq.consumer"))
      .addChannel("commands", config.getString("rabbitmq.channels.commands"))
      .addChannel("events", config.getString("rabbitmq.channels.events"))
      .addChannel("updates", config.getString("rabbitmq.channels.updates"))
      .addChannel("notifications", config.getString("rabbitmq.channels.notifications"))
      .autoSubscribe(config.getBoolean("rabbitmq.autoSubscribe"))
      .build();

    // Create manager - channels already loaded!
    mq = new RabbitMQManager(new BungeeCordPlatform(this), rabbitConfig);

    if (mq.connect()) {
      // Channels are already loaded and subscribed!
      // Just register your handlers
      setupHandlers();
      getLogger().info("RabbitMQ ready!");
    }
  }

  private void setupHandlers() {
    // Register action handlers
    mq.on("player_join", msg -> {
      String player = msg.getString("player");
      getLogger().info(player + " joined!");
    });

    mq.on("broadcast", msg -> {
      getProxy().broadcast(msg.getString("message"));
    });
  }

  @Override public void onDisable() {
    if (mq != null) {
      mq.disconnect();
    }
  }
}
```

### Spigot/Paper Example

```java
import io.hydrodevelopments.celesmq.RabbitMQManager;
import config.io.hydrodevelopments.celesmq.RabbitMQConfig;
import platform.io.hydrodevelopments.celesmq.SpigotPlatform;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
  private RabbitMQManager mq;

  @Override public void onEnable() {
    // Save default config
    saveDefaultConfig();
    FileConfiguration config = getConfig();

    // Build from config
    RabbitMQConfig rabbitConfig = new RabbitMQConfig.Builder().host(config.getString("rabbitmq.host"))
      .port(config.getInt("rabbitmq.port"))
      .username(config.getString("rabbitmq.username"))
      .password(config.getString("rabbitmq.password"))
      .consumerName(config.getString("rabbitmq.consumer"))
      .addChannel("commands", config.getString("rabbitmq.channels.commands"))
      .addChannel("updates", config.getString("rabbitmq.channels.updates"))
      .autoSubscribe(config.getBoolean("rabbitmq.autoSubscribe"))
      .build();

    mq = new RabbitMQManager(new SpigotPlatform(this), rabbitConfig);

    if (mq.connect()) {
      setupHandlers();
    }
  }

  private void setupHandlers() {
    mq.on("alert", msg -> {
      Bukkit.broadcastMessage(msg.getString("message"));
    });
  }
}
```

## Loading Channels Dynamically

```java
// Load all channels from config section
Configuration channelsConfig = config.getSection("rabbitmq.channels");
RabbitMQConfig.Builder builder = new RabbitMQConfig.Builder()
    .host(config.getString("rabbitmq.host"))
    .port(config.getInt("rabbitmq.port"))
    .username(config.getString("rabbitmq.username"))
    .password(config.getString("rabbitmq.password"))
    .consumerName(config.getString("rabbitmq.consumer"));

// Add all channels from config
for (String channelName : channelsConfig.getKeys()) {
    String exchange = channelsConfig.getString(channelName);
    builder.addChannel(channelName, exchange);
}

RabbitMQConfig rabbitConfig = builder.build();
```

## Using Map for Channels

```java
// Load channels as a Map
Map<String, String> channels = new HashMap<>();
channels.put("commands", config.getString("rabbitmq.channels.commands"));
channels.put("events", config.getString("rabbitmq.channels.events"));
channels.put("updates", config.getString("rabbitmq.channels.updates"));

RabbitMQConfig rabbitConfig = new RabbitMQConfig.Builder()
    .host(config.getString("rabbitmq.host"))
    .port(config.getInt("rabbitmq.port"))
    .username(config.getString("rabbitmq.username"))
    .password(config.getString("rabbitmq.password"))
    .consumerName(config.getString("rabbitmq.consumer"))
    .channels(channels)  // Add all at once!
    .autoSubscribe(true)
    .build();
```

## Manual Control (No Auto-Subscribe)

```java
RabbitMQConfig config = new RabbitMQConfig.Builder()
    .host("localhost")
    .port(5672)
    .username("guest")
    .password("guest")
    .consumerName("my-consumer")
    .addChannel("commands", "commands-exchange")
    .addChannel("updates", "updates-exchange")
    .autoSubscribe(false)  // Don't auto-subscribe!
    .build();

RabbitMQManager mq = new RabbitMQManager(platform, config);

if (mq.connect()) {
    // Manually subscribe to specific channels
    mq.subscribe("commands-exchange")
       .on("player_join", msg -> { /* ... */ });

    // Don't subscribe to updates-exchange if you don't need it
}
```

## Complete Real-World Example

```java
public class CorePlugin extends Plugin {
    private RabbitMQManager mq;
    private RPC rpc;
    private EventBus eventBus;

    @Override
    public void onEnable() {
        saveConfig();
        Configuration config = getConfig();

        // Load everything from config
        RabbitMQConfig rabbitConfig = buildFromConfig(config);

        mq = new RabbitMQManager(new BungeeCordPlatform(this), rabbitConfig);

        if (mq.connect()) {
            setupRPC();
            setupEvents();
            setupHandlers();

            getLogger().info("Connected as: " + rabbitConfig.getConsumerName());
            getLogger().info("Channels: " + rabbitConfig.getChannels().keySet());
        }
    }

    private RabbitMQConfig buildFromConfig(Configuration config) {
        RabbitMQConfig.Builder builder = new RabbitMQConfig.Builder()
            .host(config.getString("rabbitmq.host"))
            .port(config.getInt("rabbitmq.port"))
            .username(config.getString("rabbitmq.username"))
            .password(config.getString("rabbitmq.password"))
            .virtualHost(config.getString("rabbitmq.virtualHost", "/"))
            .consumerName(config.getString("rabbitmq.consumer"))
            .autoSubscribe(config.getBoolean("rabbitmq.autoSubscribe", true));

        // Load channels
        Configuration channels = config.getSection("rabbitmq.channels");
        if (channels != null) {
            for (String name : channels.getKeys()) {
                builder.addChannel(name, channels.getString(name));
            }
        }

        return builder.build();
    }

    private void setupRPC() {
        rpc = new RPC(mq, "myapp_rpc_call", "myapp_rpc_response");
        rpc.register("getOnlineCount", req -> getProxy().getOnlineCount());
        rpc.register("getServerInfo", req -> getServerInfo());
    }

    private void setupEvents() {
        eventBus = new EventBus(mq, getConfig().getString("rabbitmq.consumer"),
                                "myapp-events", "myapp_event", true);
        eventBus.on("player_join", this::handlePlayerJoin);
        eventBus.on("player_leave", this::handlePlayerLeave);
    }

    private void setupHandlers() {
        mq.on("command", msg -> {
            String cmd = msg.getString("command");
            getProxy().getPluginManager().dispatchCommand(getProxy().getConsole(), cmd);
        });

        mq.on("alert", msg -> {
            getProxy().broadcast(msg.getString("message"));
        });

        mq.on("kick", msg -> {
            int playerId = msg.getInt("player_id");
            kickPlayer(playerId, msg.getString("reason"));
        });
    }
}
```

This example demonstrates a complete RabbitMQ setup in a BungeeCord plugin, loading configuration from a YAML file and setting up RPC, event handling, and message handlers.
