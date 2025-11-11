package io.hydrodevelopments.celesmq.platform;

import com.velocitypowered.api.proxy.ProxyServer;

import java.util.logging.Logger;

/**
 * Platform implementation for Velocity proxy servers
 */
public class VelocityPlatform implements Platform {

  private final Object plugin;
  private final ProxyServer proxyServer;
  private final Logger logger;

  /**
   * Creates a new VelocityPlatform instance
   *
   * @param plugin      the plugin instance (must be annotated with @Plugin)
   * @param proxyServer the ProxyServer instance
   * @param logger      the logger instance
   */
  public VelocityPlatform(Object plugin, ProxyServer proxyServer, Logger logger) {
    this.plugin = plugin;
    this.proxyServer = proxyServer;
    this.logger = logger;
  }

  @Override public Logger getLogger() {
    return logger;
  }

  @Override public void runSync(Runnable task) {
    task.run();
  }

  @Override public void runAsync(Runnable task) {
    proxyServer.getScheduler().buildTask(plugin, task).schedule();
  }

  @Override public PlatformType getType() {
    return PlatformType.VELOCITY;
  }

  @Override public String getName() {
    return "Velocity";
  }

  /**
   * Gets the underlying plugin instance
   *
   * @return plugin instance
   */
  public Object getPlugin() {
    return plugin;
  }

  /**
   * Gets the ProxyServer instance
   *
   * @return ProxyServer instance
   */
  public ProxyServer getProxyServer() {
    return proxyServer;
  }
}
