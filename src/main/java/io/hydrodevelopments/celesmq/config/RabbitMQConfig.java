package io.hydrodevelopments.celesmq.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for RabbitMQ connection settings
 */
public class RabbitMQConfig {

  private final String host;
  private final int port;
  private final String username;
  private final String password;
  private final String virtualHost;
  private final int connectionTimeout;
  private final int networkRecoveryInterval;
  private final boolean automaticRecoveryEnabled;
  private final boolean useSsl;
  private final String sslProtocol;
  private final String trustStorePath;
  private final String trustStorePassword;
  private final String keyStorePath;
  private final String keyStorePassword;
  private final boolean validateServerCertificate;
  private final String consumerName;
  private final Map<String, String> channels;
  private final boolean autoSubscribe;

  private RabbitMQConfig(Builder builder) {
    this.host = builder.host;
    this.port = builder.port;
    this.username = builder.username;
    this.password = builder.password;
    this.virtualHost = builder.virtualHost;
    this.connectionTimeout = builder.connectionTimeout;
    this.networkRecoveryInterval = builder.networkRecoveryInterval;
    this.automaticRecoveryEnabled = builder.automaticRecoveryEnabled;
    this.useSsl = builder.useSsl;
    this.sslProtocol = builder.sslProtocol;
    this.trustStorePath = builder.trustStorePath;
    this.trustStorePassword = builder.trustStorePassword;
    this.keyStorePath = builder.keyStorePath;
    this.keyStorePassword = builder.keyStorePassword;
    this.validateServerCertificate = builder.validateServerCertificate;
    this.consumerName = builder.consumerName;
    this.channels = new HashMap<>(builder.channels);
    this.autoSubscribe = builder.autoSubscribe;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getVirtualHost() {
    return virtualHost;
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public int getNetworkRecoveryInterval() {
    return networkRecoveryInterval;
  }

  public boolean isAutomaticRecoveryEnabled() {
    return automaticRecoveryEnabled;
  }

  public String getConsumerName() {
    return consumerName;
  }

  public Map<String, String> getChannels() {
    return new HashMap<>(channels);
  }

  public boolean isAutoSubscribe() {
    return autoSubscribe;
  }

  public boolean isUseSsl() {
    return useSsl;
  }

  public String getSslProtocol() {
    return sslProtocol;
  }

  public String getTrustStorePath() {
    return trustStorePath;
  }

  public String getTrustStorePassword() {
    return trustStorePassword;
  }

  public String getKeyStorePath() {
    return keyStorePath;
  }

  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  public boolean isValidateServerCertificate() {
    return validateServerCertificate;
  }

  /**
   * Builder class for RabbitMQConfig All fields must be explicitly configured - no defaults
   */
  public static class Builder {
    private String host;
    private int port;
    private String username;
    private String password;
    private String virtualHost;
    private int connectionTimeout;
    private int networkRecoveryInterval;
    private boolean automaticRecoveryEnabled;
    private String consumerName; // Optional - will be auto-generated if null
    private Map<String, String> channels = new HashMap<>();
    private boolean autoSubscribe;

    // SSL/TLS configuration (optional)
    private boolean useSsl = false;
    private String sslProtocol = "TLSv1.3";
    private String trustStorePath;
    private String trustStorePassword;
    private String keyStorePath;
    private String keyStorePassword;
    private boolean validateServerCertificate = true;

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder password(String password) {
      this.password = password;
      return this;
    }

    public Builder virtualHost(String virtualHost) {
      this.virtualHost = virtualHost;
      return this;
    }

    public Builder connectionTimeout(int connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
      return this;
    }

    public Builder networkRecoveryInterval(int networkRecoveryInterval) {
      this.networkRecoveryInterval = networkRecoveryInterval;
      return this;
    }

    public Builder automaticRecoveryEnabled(boolean automaticRecoveryEnabled) {
      this.automaticRecoveryEnabled = automaticRecoveryEnabled;
      return this;
    }

    /**
     * Sets the consumer name (queue name for receiving messages) If not set, a random name will be generated
     */
    public Builder consumerName(String consumerName) {
      this.consumerName = consumerName;
      return this;
    }

    /**
     * Adds a channel (name -> exchange mapping)
     */
    public Builder addChannel(String channelName, String exchangeName) {
      this.channels.put(channelName, exchangeName);
      return this;
    }

    /**
     * Sets multiple channels at once
     */
    public Builder channels(Map<String, String> channels) {
      this.channels.putAll(channels);
      return this;
    }

    /**
     * Sets whether to automatically subscribe to configured channels Default is true
     */
    public Builder autoSubscribe(boolean autoSubscribe) {
      this.autoSubscribe = autoSubscribe;
      return this;
    }

    /**
     * Enables SSL/TLS encryption for the RabbitMQ connection Default is false (unencrypted)
     */
    public Builder useSsl(boolean useSsl) {
      this.useSsl = useSsl;
      return this;
    }

    /**
     * Sets the SSL/TLS protocol version Default is "TLSv1.3" Common values: "TLSv1.2", "TLSv1.3", "TLS"
     */
    public Builder sslProtocol(String sslProtocol) {
      this.sslProtocol = sslProtocol;
      return this;
    }

    /**
     * Sets the path to the trust store file for SSL/TLS Used to verify server certificates Optional - if not set,
     * system default trust store is used
     */
    public Builder trustStorePath(String trustStorePath) {
      this.trustStorePath = trustStorePath;
      return this;
    }

    /**
     * Sets the password for the trust store
     */
    public Builder trustStorePassword(String trustStorePassword) {
      this.trustStorePassword = trustStorePassword;
      return this;
    }

    /**
     * Sets the path to the key store file for client certificate authentication (mTLS) Optional - only needed for
     * mutual TLS authentication
     */
    public Builder keyStorePath(String keyStorePath) {
      this.keyStorePath = keyStorePath;
      return this;
    }

    /**
     * Sets the password for the key store
     */
    public Builder keyStorePassword(String keyStorePassword) {
      this.keyStorePassword = keyStorePassword;
      return this;
    }

    /**
     * Sets whether to validate server certificates Default is true (recommended for production) Set to false only for
     * testing with self-signed certificates
     */
    public Builder validateServerCertificate(boolean validateServerCertificate) {
      this.validateServerCertificate = validateServerCertificate;
      return this;
    }

    public RabbitMQConfig build() {
      // Validate required fields
      if (host == null || host.isEmpty()) {
        throw new IllegalStateException("Host must be configured");
      }
      if (port <= 0) {
        throw new IllegalStateException("Port must be configured");
      }
      if (username == null || username.isEmpty()) {
        throw new IllegalStateException("Username must be configured");
      }
      if (password == null) {
        throw new IllegalStateException("Password must be configured");
      }
      if (virtualHost == null || virtualHost.isEmpty()) {
        throw new IllegalStateException("Virtual host must be configured");
      }
      if (connectionTimeout <= 0) {
        throw new IllegalStateException("Connection timeout must be configured");
      }
      if (networkRecoveryInterval <= 0) {
        throw new IllegalStateException("Network recovery interval must be configured");
      }

      return new RabbitMQConfig(this);
    }
  }
}