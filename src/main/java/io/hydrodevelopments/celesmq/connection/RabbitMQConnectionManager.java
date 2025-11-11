package io.hydrodevelopments.celesmq.connection;

import com.rabbitmq.client.*;
import io.hydrodevelopments.celesmq.config.RabbitMQConfig;
import io.hydrodevelopments.celesmq.platform.Platform;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages RabbitMQ connections with automatic recovery and reconnection logic
 */
public class RabbitMQConnectionManager {

  private final Platform platform;
  private final RabbitMQConfig config;
  private final Logger logger;
  private Connection connection;
  private Channel channel;
  private boolean isShuttingDown = false;

  public RabbitMQConnectionManager(Platform platform, RabbitMQConfig config) {
    this.platform = platform;
    this.config = config;
    this.logger = platform.getLogger();
  }

  /**
   * Establishes connection to RabbitMQ server
   *
   * @return true if connection successful, false otherwise
   */
  public boolean connect() {
    try {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost(config.getHost());
      factory.setPort(config.getPort());
      factory.setUsername(config.getUsername());
      factory.setPassword(config.getPassword());
      factory.setVirtualHost(config.getVirtualHost());
      factory.setConnectionTimeout(config.getConnectionTimeout());
      factory.setAutomaticRecoveryEnabled(config.isAutomaticRecoveryEnabled());
      factory.setNetworkRecoveryInterval(config.getNetworkRecoveryInterval());

      // Configure SSL/TLS if enabled
      if (config.isUseSsl()) {
        configureSSL(factory);
      }

      // Add connection recovery listeners
      factory.setRecoveryDelayHandler(recoveryAttempts -> config.getNetworkRecoveryInterval());

      connection = factory.newConnection();
      channel = connection.createChannel();

      // Add connection listeners
      connection.addShutdownListener(cause -> {
        if (!isShuttingDown && !cause.isInitiatedByApplication()) {
          logger.log(Level.WARNING, "RabbitMQ connection lost: " + cause.getMessage());
        }
      });

      ((Recoverable) connection).addRecoveryListener(new RecoveryListener() {
        @Override public void handleRecovery(Recoverable recoverable) {
          logger.info("RabbitMQ connection recovered successfully");
        }

        @Override public void handleRecoveryStarted(Recoverable recoverable) {
          logger.info("RabbitMQ connection recovery started");
        }
      });

      logger.info("Successfully connected to RabbitMQ server at " + config.getHost() + ":" + config.getPort());
      return true;

    } catch (IOException | TimeoutException e) {
      logger.log(Level.SEVERE, "Failed to connect to RabbitMQ server", e);
      return false;
    }
  }

  /**
   * Gets the current channel, creating one if necessary
   *
   * @return Channel instance
   *
   * @throws IOException if channel creation fails
   */
  public Channel getChannel() throws IOException {
    if (channel == null || !channel.isOpen()) {
      if (connection != null && connection.isOpen()) {
        channel = connection.createChannel();
      } else {
        throw new IOException("Connection is not established");
      }
    }
    return channel;
  }

  /**
   * Creates a new channel
   *
   * @return new Channel instance
   *
   * @throws IOException if channel creation fails
   */
  public Channel createChannel() throws IOException {
    if (connection == null || !connection.isOpen()) {
      throw new IOException("Connection is not established");
    }
    return connection.createChannel();
  }

  /**
   * Checks if connection is active
   *
   * @return true if connected, false otherwise
   */
  public boolean isConnected() {
    return connection != null && connection.isOpen();
  }

  /**
   * Closes the connection and channel gracefully
   */
  public void disconnect() {
    isShuttingDown = true;

    try {
      if (channel != null && channel.isOpen()) {
        channel.close();
        logger.info("RabbitMQ channel closed");
      }
    } catch (IOException | TimeoutException e) {
      logger.log(Level.WARNING, "Error closing RabbitMQ channel", e);
    }

    try {
      if (connection != null && connection.isOpen()) {
        connection.close();
        logger.info("RabbitMQ connection closed");
      }
    } catch (IOException e) {
      logger.log(Level.WARNING, "Error closing RabbitMQ connection", e);
    }
  }

  /**
   * Configures SSL/TLS for the connection factory
   *
   * @param factory the ConnectionFactory to configure
   */
  private void configureSSL(ConnectionFactory factory) {
    try {
      // Setup default SSL context if no custom stores are provided
      if (config.getTrustStorePath() == null && config.getKeyStorePath() == null) {
        if (config.isValidateServerCertificate()) {
          // Use default SSL with server certificate validation
          factory.useSslProtocol(config.getSslProtocol());
          logger.info("SSL/TLS enabled with protocol: " + config.getSslProtocol());
        } else {
          // Disable certificate validation
          SSLContext sslContext = SSLContext.getInstance(config.getSslProtocol());
          sslContext.init(null, new javax.net.ssl.TrustManager[]{new javax.net.ssl.X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
          }
          }, null);
          factory.useSslProtocol(sslContext);
          logger.warning("SSL/TLS enabled WITHOUT server certificate validation - NOT RECOMMENDED FOR PRODUCTION!");
        }
        return;
      }

      // Advanced SSL setup with custom trust store and/or client certificates
      SSLContext sslContext = SSLContext.getInstance(config.getSslProtocol());

      // Configure trust manager if trust store is provided
      TrustManagerFactory tmf = null;
      if (config.getTrustStorePath() != null) {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream trustStoreStream = new FileInputStream(config.getTrustStorePath())) {
          trustStore.load(trustStoreStream,
            config.getTrustStorePassword() != null ? config.getTrustStorePassword().toCharArray() : null);
        }
        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        logger.info("SSL/TLS: Loaded custom trust store from: " + config.getTrustStorePath());
      }

      KeyManagerFactory kmf = null;
      if (config.getKeyStorePath() != null) {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream keyStoreStream = new FileInputStream(config.getKeyStorePath())) {
          keyStore.load(keyStoreStream,
            config.getKeyStorePassword() != null ? config.getKeyStorePassword().toCharArray() : null);
        }
        kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, config.getKeyStorePassword() != null ? config.getKeyStorePassword().toCharArray() : null);
        logger.info("SSL/TLS: Loaded client key store from: " + config.getKeyStorePath() + " (mTLS enabled)");
      }

      // Initialize SSL context
      sslContext.init(kmf != null ? kmf.getKeyManagers() : null, tmf != null ? tmf.getTrustManagers() : null, null);

      factory.useSslProtocol(sslContext);
      logger.info("SSL/TLS enabled with protocol: " + config.getSslProtocol());

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to configure SSL/TLS", e);
      throw new RuntimeException("SSL/TLS configuration failed", e);
    }
  }

  /**
   * Gets the platform instance
   *
   * @return Platform instance
   */
  public Platform getPlatform() {
    return platform;
  }

  /**
   * Gets the configuration
   *
   * @return RabbitMQConfig instance
   */
  public RabbitMQConfig getConfig() {
    return config;
  }
}