package io.hydrodevelopments.celesmq.batch;

import io.hydrodevelopments.celesmq.RabbitMQClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Batches multiple messages together for efficient sending Reduces network overhead and improves throughput
 */
public class BatchPublisher {

  private final RabbitMQClient client;
  private final Logger logger;
  private final List<BatchedMessage> messageQueue = new ArrayList<>();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  private int batchSize;
  private long flushInterval;
  private boolean started = false;

  public BatchPublisher(RabbitMQClient client, Logger logger) {
    this.client = client;
    this.logger = logger;
  }

  /**
   * Sets the batch size (number of messages to accumulate before sending)
   */
  public BatchPublisher batchSize(int size) {
    this.batchSize = size;
    return this;
  }

  /**
   * Sets the flush interval (milliseconds to wait before sending even if batch not full)
   */
  public BatchPublisher flushInterval(long millis) {
    this.flushInterval = millis;
    return this;
  }

  /**
   * Starts the batch publisher
   *
   * @throws IllegalStateException if batchSize or flushInterval not configured
   */
  public BatchPublisher start() {
    if (!started) {
      if (batchSize <= 0) {
        throw new IllegalStateException("Batch size must be configured before starting");
      }
      if (flushInterval <= 0) {
        throw new IllegalStateException("Flush interval must be configured before starting");
      }
      started = true;
      scheduler.scheduleAtFixedRate(this::flush, flushInterval, flushInterval, TimeUnit.MILLISECONDS);
      logger.info("BatchPublisher started (batch size: " + batchSize + ", interval: " + flushInterval + "ms)");
    }
    return this;
  }

  /**
   * Adds a message to the batch
   */
  public synchronized CompletableFuture<Boolean> add(String exchange, String message) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    messageQueue.add(new BatchedMessage(exchange, "", message, future));

    if (messageQueue.size() >= batchSize) {
      flush();
    }

    return future;
  }

  /**
   * Adds a message to the batch with routing key
   */
  public synchronized CompletableFuture<Boolean> add(String exchange, String routingKey, String message) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    messageQueue.add(new BatchedMessage(exchange, routingKey, message, future));

    if (messageQueue.size() >= batchSize) {
      flush();
    }

    return future;
  }

  /**
   * Flushes all pending messages immediately
   */
  public synchronized void flush() {
    if (messageQueue.isEmpty()) {
      return;
    }

    List<BatchedMessage> toSend = new ArrayList<>(messageQueue);
    messageQueue.clear();

    logger.info("Flushing batch of " + toSend.size() + " messages");

    // Send all messages
    List<CompletableFuture<Boolean>> futures = new ArrayList<>();
    for (BatchedMessage msg : toSend) {
      CompletableFuture<Boolean> sendFuture = client.publishToExchange(msg.exchange, msg.routingKey, msg.message);

      sendFuture.whenComplete((success, error) -> {
        if (error != null) {
          msg.future.completeExceptionally(error);
        } else {
          msg.future.complete(success);
        }
      });

      futures.add(sendFuture);
    }
  }

  /**
   * Stops the batch publisher and flushes remaining messages
   */
  public void stop() {
    if (started) {
      started = false;
      flush();
      scheduler.shutdown();
      try {
        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
          scheduler.shutdownNow();
        }
      } catch (InterruptedException e) {
        scheduler.shutdownNow();
      }
      logger.info("BatchPublisher stopped");
    }
  }

  /**
   * Gets the current queue size
   */
  public synchronized int getQueueSize() {
    return messageQueue.size();
  }

  private static class BatchedMessage {
    final String exchange;
    final String routingKey;
    final String message;
    final CompletableFuture<Boolean> future;

    BatchedMessage(String exchange, String routingKey, String message, CompletableFuture<Boolean> future) {
      this.exchange = exchange;
      this.routingKey = routingKey;
      this.message = message;
      this.future = future;
    }
  }
}
