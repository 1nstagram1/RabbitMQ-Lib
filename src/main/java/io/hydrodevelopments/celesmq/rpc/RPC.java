package io.hydrodevelopments.celesmq.rpc;

import io.hydrodevelopments.celesmq.RabbitMQManager;
import io.hydrodevelopments.celesmq.message.MessageResponse;
import io.hydrodevelopments.celesmq.message.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Remote Procedure Call (RPC) system for cross-server method invocation Call methods on other servers as if they were
 * local!
 */
public class RPC {

  private final RabbitMQManager manager;
  private final Map<String, Function<RPCRequest, Object>> procedures = new ConcurrentHashMap<>();
  private final String rpcCallAction;
  private final String rpcResponseAction;

  /**
   * Creates an RPC system with specified action names
   *
   * @param manager           RabbitMQManager instance
   * @param rpcCallAction     Action name for RPC calls
   * @param rpcResponseAction Action name for RPC responses
   */
  public RPC(RabbitMQManager manager, String rpcCallAction, String rpcResponseAction) {
    this.manager = manager;
    this.rpcCallAction = rpcCallAction;
    this.rpcResponseAction = rpcResponseAction;
    setupRPCHandler();
  }

  private void setupRPCHandler() {
    manager.on(rpcCallAction, msg -> {
      String procedure = msg.getString("procedure");
      int callId = msg.getInt("callId");
      String replyTo = msg.getString("replyTo");

      Function<RPCRequest, Object> handler = procedures.get(procedure);
      if (handler != null) {
        try {
          RPCRequest request = new RPCRequest(msg);
          Object result = handler.apply(request);

          // Send response
          String response = MessageBuilder.create(rpcResponseAction)
            .add("callId", callId)
            .add("success", true)
            .add("result", result != null ? result.toString() : "")
            .build();

          manager.getClient().publishToQueue(replyTo, response);
        } catch (Exception e) {
          // Send error response
          String response = MessageBuilder.create(rpcResponseAction)
            .add("callId", callId)
            .add("success", false)
            .add("error", e.getMessage())
            .build();

          manager.getClient().publishToQueue(replyTo, response);
        }
      }
    });
  }

  /**
   * Registers a procedure that can be called remotely
   *
   * @param name    procedure name
   * @param handler function that handles the call
   *
   * @return this for chaining
   */
  public RPC register(String name, Function<RPCRequest, Object> handler) {
    procedures.put(name, handler);
    return this;
  }

  /**
   * Calls a remote procedure
   *
   * @param server    server to call (exchange name)
   * @param procedure procedure name
   *
   * @return builder for the call
   */
  public RPCCallBuilder call(String server, String procedure) {
    return new RPCCallBuilder(manager, server, procedure, rpcCallAction);
  }

  /**
   * Unregisters a procedure
   */
  public RPC unregister(String name) {
    procedures.remove(name);
    return this;
  }

  /**
   * Gets the number of registered procedures
   */
  public int getProcedureCount() {
    return procedures.size();
  }

  /**
   * Checks if a procedure is registered
   */
  public boolean hasRegistered(String name) {
    return procedures.containsKey(name);
  }

  /**
   * Builder for RPC calls
   */
  public static class RPCCallBuilder {
    private final RabbitMQManager manager;
    private final String server;
    private final String procedure;
    private final String rpcCallAction;
    private final Map<String, Object> args = new ConcurrentHashMap<>();
    private long timeout; // Must be explicitly set via timeout() method

    public RPCCallBuilder(RabbitMQManager manager, String server, String procedure, String rpcCallAction) {
      this.manager = manager;
      this.server = server;
      this.procedure = procedure;
      this.rpcCallAction = rpcCallAction;
    }

    public RPCCallBuilder arg(String key, Object value) {
      args.put(key, value);
      return this;
    }

    public RPCCallBuilder timeout(long millis) {
      this.timeout = millis;
      return this;
    }

    public CompletableFuture<RPCResponse> execute() {
      return manager.request()
        .action(rpcCallAction)
        .param("procedure", procedure)
        .params(args)
        .timeout(timeout)
        .sendTo(server)
        .thenApply(RPCResponse::new);
    }
  }

  /**
   * RPC Request wrapper
   */
  public static class RPCRequest {
    private final MessageResponse message;

    public RPCRequest(MessageResponse message) {
      this.message = message;
    }

    public int getInt(String key) {
      return message.getInt(key);
    }

    public long getLong(String key) {
      return message.getLong(key);
    }

    public String getString(String key) {
      return message.getString(key);
    }

    public boolean getBoolean(String key) {
      return message.getBoolean(key);
    }

    public double getDouble(String key) {
      return message.getDouble(key);
    }

    public boolean has(String key) {
      return message.has(key);
    }

    public MessageResponse getMessage() {
      return message;
    }
  }

  /**
   * RPC Response wrapper
   */
  public static class RPCResponse {
    private final MessageResponse message;

    public RPCResponse(MessageResponse message) {
      this.message = message;
    }

    public boolean isSuccess() {
      return message.getBoolean("success");
    }

    public String getResult() {
      return message.getString("result");
    }

    public String getError() {
      return message.getString("error");
    }

    public int getResultAsInt() {
      try {
        return Integer.parseInt(getResult());
      } catch (NumberFormatException e) {
        return 0;
      }
    }

    public long getResultAsLong() {
      try {
        return Long.parseLong(getResult());
      } catch (NumberFormatException e) {
        return 0L;
      }
    }

    public boolean getResultAsBoolean() {
      return Boolean.parseBoolean(getResult());
    }

    public MessageResponse getMessage() {
      return message;
    }
  }
}
