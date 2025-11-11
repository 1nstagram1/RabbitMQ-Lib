# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2025-12-11

### Added
- **New Platform Support**: Added support for 5 additional platforms
    - `MinestomPlatform` - Modern from-scratch Minecraft server (1.21+)
    - `SpongePlatform` - Forge/Fabric plugin platform (Sponge 11.0.0+)
    - `FoliaPlatform` - Paper's experimental multithreaded fork
    - `NukkitPlatform` - Bedrock Edition servers (PowerNukkitX)
- **Extensible ResponseStatus**: Converted `ResponseStatus` from enum to class-based pattern
    - Developers can now create custom status codes using `ResponseStatus.custom("STATUS_NAME")`
    - Built-in statuses: SUCCESS, ERROR, TIMEOUT, NOT_FOUND, INVALID, PENDING
    - Backward compatible with existing code
- RabbitMQ client with connection management
- Message publishing and consuming
- Request-response pattern support
- Event bus system for cross-server events
- Batch publishing capabilities
- Platform abstraction layer
- Comprehensive documentation and examples
- **SSL/TLS Support**: Full encryption support with configurable options
  - Simple SSL with system certificates
  - Custom trust stores for CA certificates
  - Mutual TLS (mTLS) with client certificates
  - Self-signed certificate support for development
  - Configurable SSL protocols (TLSv1.2, TLSv1.3)

### Changed
- Updated all documentation to mention all 7+ supported platforms

### Documentation
- Updated API_GUIDE.md with all new platforms and ResponseStatus documentation
- Updated README.md with new platform requirements
- Added SSL/TLS guide with examples of how to configure SSL options
- Added CONFIG_EXAMPLES.md with various configuration scenarios
- Added ADVANCED_FEATURES.md with RabbitMQ and event bus examples
