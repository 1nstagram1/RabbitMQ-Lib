package io.hydrodevelopments.celesmq.metrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Tracks metrics and statistics for RabbitMQ operations Useful for monitoring and debugging
 */
public class RabbitMQMetrics {

  private final LongAdder messagesSent = new LongAdder();
  private final LongAdder messagesReceived = new LongAdder();
  private final LongAdder messagesProcessed = new LongAdder();
  private final LongAdder messagesFailed = new LongAdder();
  private final LongAdder bytesReceived = new LongAdder();
  private final LongAdder bytesSent = new LongAdder();

  private final AtomicLong totalProcessingTime = new AtomicLong(0);
  private final AtomicLong peakProcessingTime = new AtomicLong(0);
  private final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());

  /**
   * Records a sent message
   */
  public void recordSent(int bytes) {
    messagesSent.increment();
    bytesSent.add(bytes);
  }

  /**
   * Records a received message
   */
  public void recordReceived(int bytes) {
    messagesReceived.increment();
    bytesReceived.add(bytes);
  }

  /**
   * Records a processed message with processing time
   */
  public void recordProcessed(long processingTimeMs) {
    messagesProcessed.increment();
    totalProcessingTime.addAndGet(processingTimeMs);

    // Update peak time
    long current;
    do {
      current = peakProcessingTime.get();
      if (processingTimeMs <= current) {
        break;
      }
    } while (!peakProcessingTime.compareAndSet(current, processingTimeMs));
  }

  /**
   * Records a failed message
   */
  public void recordFailed() {
    messagesFailed.increment();
  }

  /**
   * Gets total messages sent
   */
  public long getMessagesSent() {
    return messagesSent.sum();
  }

  /**
   * Gets total messages received
   */
  public long getMessagesReceived() {
    return messagesReceived.sum();
  }

  /**
   * Gets total messages processed
   */
  public long getMessagesProcessed() {
    return messagesProcessed.sum();
  }

  /**
   * Gets total messages failed
   */
  public long getMessagesFailed() {
    return messagesFailed.sum();
  }

  /**
   * Gets total bytes sent
   */
  public long getBytesSent() {
    return bytesSent.sum();
  }

  /**
   * Gets total bytes received
   */
  public long getBytesReceived() {
    return bytesReceived.sum();
  }

  /**
   * Gets average processing time in milliseconds
   */
  public double getAverageProcessingTime() {
    long processed = messagesProcessed.sum();
    if (processed == 0) {
      return 0;
    }
    return (double) totalProcessingTime.get() / processed;
  }

  /**
   * Gets peak processing time in milliseconds
   */
  public long getPeakProcessingTime() {
    return peakProcessingTime.get();
  }

  /**
   * Gets messages per second rate
   */
  public double getMessagesPerSecond() {
    long elapsed = (System.currentTimeMillis() - startTime.get()) / 1000;
    if (elapsed == 0) {
      return 0;
    }
    return (double) messagesReceived.sum() / elapsed;
  }

  /**
   * Gets throughput in bytes per second
   */
  public double getThroughputBytesPerSecond() {
    long elapsed = (System.currentTimeMillis() - startTime.get()) / 1000;
    if (elapsed == 0) {
      return 0;
    }
    return (double) bytesReceived.sum() / elapsed;
  }

  /**
   * Gets success rate (0.0 to 1.0)
   */
  public double getSuccessRate() {
    long total = messagesProcessed.sum() + messagesFailed.sum();
    if (total == 0) {
      return 1.0;
    }
    return (double) messagesProcessed.sum() / total;
  }

  /**
   * Gets uptime in milliseconds
   */
  public long getUptimeMs() {
    return System.currentTimeMillis() - startTime.get();
  }

  /**
   * Resets all metrics
   */
  public void reset() {
    messagesSent.reset();
    messagesReceived.reset();
    messagesProcessed.reset();
    messagesFailed.reset();
    bytesReceived.reset();
    bytesSent.reset();
    totalProcessingTime.set(0);
    peakProcessingTime.set(0);
    startTime.set(System.currentTimeMillis());
  }

  /**
   * Gets a formatted summary of metrics
   */
  public String getSummary() {
    return String.format("RabbitMQ Metrics:\n" +
        "  Messages Sent: %d\n" +
        "  Messages Received: %d\n" +
        "  Messages Processed: %d\n" +
        "  Messages Failed: %d\n" +
        "  Success Rate: %.2f%%\n" +
        "  Average Processing Time: %.2f ms\n" +
        "  Peak Processing Time: %d ms\n" +
        "  Messages/Second: %.2f\n" +
        "  Throughput: %.2f KB/s\n" +
        "  Uptime: %d seconds",
      getMessagesSent(),
      getMessagesReceived(),
      getMessagesProcessed(),
      getMessagesFailed(),
      getSuccessRate() * 100,
      getAverageProcessingTime(),
      getPeakProcessingTime(),
      getMessagesPerSecond(),
      getThroughputBytesPerSecond() / 1024,
      getUptimeMs() / 1000);
  }
}
