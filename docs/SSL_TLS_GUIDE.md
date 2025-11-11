# SSL/TLS Configuration Guide

This guide explains how to configure SSL/TLS encryption for RabbitMQ connections.

## Overview

The library supports multiple SSL/TLS configuration modes:
- **Simple SSL**: Basic SSL with system default certificates
- **Custom Trust Store**: SSL with custom CA certificates
- **Mutual TLS (mTLS)**: Client certificate authentication
- **Self-Signed Certificates**: For development/testing (not recommended for production)

## Configuration Options

### Basic Configuration

```java
RabbitMQConfig config = RabbitMQConfig.builder()
    .host("rabbitmq.example.com")
    .port(5671)  // SSL port (default RabbitMQ SSL port)
    .username("user")
    .password("pass")
    .virtualHost("/")
    .connectionTimeout(30000)
    .networkRecoveryInterval(10000)
    .automaticRecoveryEnabled(true)
    // Enable SSL/TLS
    .useSsl(true)
    .sslProtocol("TLSv1.3")  // Optional: defaults to TLSv1.3
    .build();
```

### SSL Configuration Methods

| Method | Description | Default |
|--------|-------------|---------|
| `useSsl(boolean)` | Enable/disable SSL/TLS | `false` |
| `sslProtocol(String)` | SSL protocol version | `"TLSv1.3"` |
| `trustStorePath(String)` | Path to trust store file | `null` (system default) |
| `trustStorePassword(String)` | Trust store password | `null` |
| `keyStorePath(String)` | Path to key store for mTLS | `null` |
| `keyStorePassword(String)` | Key store password | `null` |
| `validateServerCertificate(boolean)` | Validate server certificates | `true` |

## Usage Examples

### Example 1: Simple SSL (Production)

Uses system default CA certificates to validate the server.

```java
RabbitMQConfig config = RabbitMQConfig.builder()
    .host("rabbitmq.example.com")
    .port(5671)
    .username("user")
    .password("pass")
    .virtualHost("/")
    .connectionTimeout(30000)
    .networkRecoveryInterval(10000)
    .automaticRecoveryEnabled(true)
    .useSsl(true)
    .build();
```

### Example 2: Custom CA Certificate

When your RabbitMQ server uses a custom CA certificate.

```java
RabbitMQConfig config = RabbitMQConfig.builder()
    .host("rabbitmq.internal.com")
    .port(5671)
    .username("user")
    .password("pass")
    .virtualHost("/")
    .connectionTimeout(30000)
    .networkRecoveryInterval(10000)
    .automaticRecoveryEnabled(true)
    .useSsl(true)
    .sslProtocol("TLSv1.3")
    .trustStorePath("/path/to/truststore.jks")
    .trustStorePassword("truststorePassword")
    .build();
```

### Example 3: Mutual TLS (mTLS)

Client certificate authentication for enhanced security.

```java
RabbitMQConfig config = RabbitMQConfig.builder()
    .host("secure-rabbitmq.example.com")
    .port(5671)
    .username("user")
    .password("pass")
    .virtualHost("/")
    .connectionTimeout(30000)
    .networkRecoveryInterval(10000)
    .automaticRecoveryEnabled(true)
    .useSsl(true)
    .sslProtocol("TLSv1.3")
    .trustStorePath("/path/to/truststore.jks")
    .trustStorePassword("truststorePassword")
    .keyStorePath("/path/to/keystore.jks")
    .keyStorePassword("keystorePassword")
    .build();
```

### Example 4: Self-Signed Certificate (Development Only)

**⚠️ WARNING: Only for development/testing! Never use in production!**

```java
RabbitMQConfig config = RabbitMQConfig.builder()
    .host("localhost")
    .port(5671)
    .username("guest")
    .password("guest")
    .virtualHost("/")
    .connectionTimeout(30000)
    .networkRecoveryInterval(10000)
    .automaticRecoveryEnabled(true)
    .useSsl(true)
    .validateServerCertificate(false)  // ⚠️ Disables certificate validation
    .build();
```

## Supported SSL/TLS Protocols

- `TLSv1.3` (recommended, default)
- `TLSv1.2`
- `TLS` (negotiates highest available)

## Certificate Store Formats

The library supports standard Java KeyStore formats:
- **JKS** (Java KeyStore) - most common
- **PKCS12** (.p12, .pfx)
- **JCEKS**

## Creating Certificate Stores

### Creating a Trust Store (for server certificate validation)

```bash
# Import CA certificate into trust store
keytool -import -alias rabbitmq-ca -file ca-certificate.pem \
    -keystore truststore.jks -storepass truststorePassword
```

### Creating a Key Store (for client certificate / mTLS)

```bash
# Convert PEM to PKCS12
openssl pkcs12 -export -in client-cert.pem -inkey client-key.pem \
    -out keystore.p12 -name client -passout pass:keystorePassword

# Convert PKCS12 to JKS (optional)
keytool -importkeystore -srckeystore keystore.p12 -srcstoretype PKCS12 \
    -destkeystore keystore.jks -deststoretype JKS \
    -srcstorepass keystorePassword -deststorepass keystorePassword
```

## RabbitMQ Server Configuration

Your RabbitMQ server must be configured for SSL/TLS. Example `rabbitmq.conf`:

```ini
# SSL listener
listeners.ssl.default = 5671

# SSL certificate files
ssl_options.cacertfile = /path/to/ca_certificate.pem
ssl_options.certfile   = /path/to/server_certificate.pem
ssl_options.keyfile    = /path/to/server_key.pem

# Verify peer (for mTLS)
ssl_options.verify = verify_peer
ssl_options.fail_if_no_peer_cert = true

# SSL protocol versions
ssl_options.versions.1 = tlsv1.3
ssl_options.versions.2 = tlsv1.2
```

## Platform-Specific Examples

### Spigot/Paper Plugin

```java
public class MyPlugin extends JavaPlugin {
    private RabbitMQClient rabbitMQ;

    @Override
    public void onEnable() {
        RabbitMQConfig config = RabbitMQConfig.builder()
            .host("rabbitmq.myserver.com")
            .port(5671)
            .username("minecraft")
            .password(getConfig().getString("rabbitmq.password"))
            .virtualHost("/minecraft")
            .connectionTimeout(30000)
            .networkRecoveryInterval(10000)
            .automaticRecoveryEnabled(true)
            .useSsl(true)
            .trustStorePath(getDataFolder() + "/truststore.jks")
            .trustStorePassword(getConfig().getString("ssl.truststore.password"))
            .build();

        Platform platform = new SpigotPlatform(this);
        rabbitMQ = new RabbitMQClient(platform, config);
        rabbitMQ.connect();
    }
}
```

## Troubleshooting

### Connection Fails with SSL Handshake Error

**Problem**: `javax.net.ssl.SSLHandshakeException`

**Solutions**:
- Verify the RabbitMQ server certificate is valid
- Check that the CA certificate is in your trust store
- Ensure trust store path and password are correct
- Verify SSL protocol version matches server configuration

### Certificate Not Trusted

**Problem**: `sun.security.validator.ValidatorException: PKIX path building failed`

**Solution**: Import the CA certificate into your trust store:
```bash
keytool -import -alias rabbitmq-ca -file ca-cert.pem -keystore truststore.jks
```

### Client Certificate Not Sent (mTLS)

**Problem**: Server requires client certificate but none is sent

**Solution**: Verify key store configuration:
```java
.keyStorePath("/path/to/keystore.jks")
.keyStorePassword("password")
```

### Protocol Version Mismatch

**Problem**: SSL protocol version not supported

**Solution**: Match client and server protocols:
```java
.sslProtocol("TLSv1.2")  // Use version supported by your server
```

## Security Best Practices

1. **Always Use SSL in Production**: Never send credentials over unencrypted connections
2. **Use Strong Protocols**: Prefer TLSv1.3, minimum TLSv1.2
3. **Validate Certificates**: Keep `validateServerCertificate(true)` in production
4. **Protect Private Keys**: Secure key store files with proper file permissions
5. **Use Environment Variables**: Don't hardcode passwords in source code
6. **Rotate Certificates**: Implement certificate rotation before expiry
7. **Consider mTLS**: For highly sensitive environments, use mutual TLS
8. **Monitor Connections**: Log SSL handshake failures for security auditing

## Performance Considerations

- SSL/TLS adds computational overhead (typically 5-10% for encryption)
- TLSv1.3 is faster than TLSv1.2 (fewer round trips)
- Reused connections benefit from SSL session caching
- Consider hardware acceleration for high-throughput scenarios

## See Also

- [RabbitMQ SSL Documentation](https://www.rabbitmq.com/ssl.html)
- [Java SSL/TLS Documentation](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html)
- [CONFIG_EXAMPLES.md](CONFIG_EXAMPLES.md) - General configuration examples