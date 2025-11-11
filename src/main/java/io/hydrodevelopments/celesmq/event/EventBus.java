package io.hydrodevelopments.celesmq.event;

import io.hydrodevelopments.celesmq.RabbitMQManager;
import io.hydrodevelopments.celesmq.message.MessageBuilder;
import io.hydrodevelopments.celesmq.message.MessageResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Cross-server event bus for dispatching and listening to events across the network Platform-agnostic event system that
 * works everywhere!
 */
public class EventBus {

  private final RabbitMQManager manager;
  private final Logger logger;
  private final String serverName;
  private final Map<String, List<Consumer<CrossServerEvent>>> listeners = new ConcurrentHashMap<>();
  private final String eventExchange;
  private final String eventAction;
  private final boolean ignoreOwnEvents;

  /**
   * Creates an EventBus with specified exchange and action names
   *
   * @param manager         RabbitMQManager instance
   * @param serverName      Server identifier
   * @param eventExchange   Exchange name for events
   * @param eventAction     Action name for event messages
   * @param ignoreOwnEvents Whether to ignore events from this server
   */
  public EventBus(RabbitMQManager manager,
    String serverName,
    String eventExchange,
    String eventAction,
    boolean ignoreOwnEvents) {
    this.manager = manager;
    this.logger = manager.getClient().getConnectionManager().getPlatform().getLogger();
    this.serverName = serverName;
    this.eventExchange = eventExchange;
    this.eventAction = eventAction;
    this.ignoreOwnEvents = ignoreOwnEvents;
    setupEventListener();
  }

  private void setupEventListener() {
    // Subscribe to the event exchange
    manager.subscribe(eventExchange);

    // Handle incoming events
    manager.on(eventAction, this::handleIncomingEvent);
  }

  private void handleIncomingEvent(MessageResponse msg) {
    String eventType = msg.getString("eventType");
    String sourceServer = msg.getString("sourceServer");

    // Optionally ignore own events based on configuration
    if (ignoreOwnEvents && sourceServer.equals(serverName)) {
      return;
    }

    List<Consumer<CrossServerEvent>> eventListeners = listeners.get(eventType);
    if (eventListeners != null && !eventListeners.isEmpty()) {
      try {
        // Create a generic event from the data
        GenericCrossServerEvent event = new GenericCrossServerEvent(sourceServer, eventType, msg.getData());

        // Call all listeners
        for (Consumer<CrossServerEvent> listener : eventListeners) {
          try {
            listener.accept(event);
          } catch (Exception e) {
            logger.severe("Error in event listener for " + eventType + ": " + e.getMessage());
          }
        }
      } catch (Exception e) {
        logger.severe("Error handling cross-server event: " + e.getMessage());
      }
    }
  }

  /**
   * Registers a listener for a specific event type
   *
   * @param eventType event type to listen for
   * @param listener  listener function
   *
   * @return this for chaining
   */
  public <T extends CrossServerEvent> EventBus on(String eventType, Consumer<T> listener) {
    listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add((Consumer<CrossServerEvent>) listener);
    return this;
  }

  /**
   * Dispatches an event to all servers
   *
   * @param event the event to dispatch
   *
   * @return this for chaining
   */
  public EventBus dispatch(CrossServerEvent event) {
    String message = MessageBuilder.create(eventAction)
      .add("eventType", event.getEventType())
      .add("eventId", event.getEventId())
      .add("sourceServer", event.getSourceServer())
      .add("timestamp", event.getTimestamp())
      .add("cancelled", event.isCancelled())
      .add("data", event.getData())
      .build();

    manager.send(eventExchange, message);
    return this;
  }

  /**
   * Unregisters all listeners for an event type
   */
  public EventBus unregister(String eventType) {
    listeners.remove(eventType);
    return this;
  }

  /**
   * Gets the number of listeners for an event type
   */
  public int getListenerCount(String eventType) {
    List<Consumer<CrossServerEvent>> list = listeners.get(eventType);
    return list != null ? list.size() : 0;
  }

  /**
   * Gets total number of registered listeners
   */
  public int getTotalListeners() {
    return listeners.values().stream().mapToInt(List::size).sum();
  }

  /**
   * Clears all listeners
   */
  public EventBus clearAll() {
    listeners.clear();
    return this;
  }

  /**
   * Generic cross-server event for dynamic event types
   */
  public static class GenericCrossServerEvent extends CrossServerEvent {
    private final String eventType;

    public GenericCrossServerEvent(String sourceServer, String eventType, com.google.gson.JsonObject data) {
      super(sourceServer);
      this.eventType = eventType;

      // Copy data from JsonObject
      data.keySet().forEach(key -> {
        setData(key, data.get(key).toString());
      });
    }

    @Override public String getEventType() {
      return eventType;
    }
  }
}
