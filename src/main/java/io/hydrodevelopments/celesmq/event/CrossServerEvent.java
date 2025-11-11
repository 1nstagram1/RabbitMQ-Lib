package io.hydrodevelopments.celesmq.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for cross-server events Platform-agnostic event system that works on Spigot, BungeeCord, and Velocity
 */
public abstract class CrossServerEvent {

  private final String eventId;
  private final String sourceServer;
  private final long timestamp;
  private final Map<String, Object> data;
  private boolean cancelled = false;

  public CrossServerEvent(String sourceServer) {
    this.eventId = UUID.randomUUID().toString();
    this.sourceServer = sourceServer;
    this.timestamp = System.currentTimeMillis();
    this.data = new HashMap<>();
  }

  public String getEventId() {
    return eventId;
  }

  public String getSourceServer() {
    return sourceServer;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public void setData(String key, Object value) {
    data.put(key, value);
  }

  public Object getData(String key) {
    return data.get(key);
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

  /**
   * Gets the event type name for routing
   */
  public abstract String getEventType();

  /**
   * Serializes the event to a map for transmission
   */
  public Map<String, Object> serialize() {
    Map<String, Object> map = new HashMap<>();
    map.put("eventId", eventId);
    map.put("eventType", getEventType());
    map.put("sourceServer", sourceServer);
    map.put("timestamp", timestamp);
    map.put("cancelled", cancelled);
    map.put("data", data);
    return map;
  }

  /**
   * Deserializes common event data
   */
  protected void deserialize(Map<String, Object> map) {
    if (map.containsKey("cancelled")) {
      this.cancelled = (boolean) map.get("cancelled");
    }
    if (map.containsKey("data") && map.get("data") instanceof Map) {
      @SuppressWarnings("unchecked") Map<String, Object> dataMap = (Map<String, Object>) map.get("data");
      this.data.putAll(dataMap);
    }
  }
}
