package io.hydrodevelopments.celesmq.platform;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import java.util.logging.Logger;

/**
 * Platform implementation for BungeeCord proxy servers
 */
public class BungeeCordPlatform implements Platform {

  private final Plugin plugin;
  private final TaskScheduler scheduler;

  public BungeeCordPlatform(Plugin plugin) {
    this.plugin = plugin;
    this.scheduler = plugin.getProxy().getScheduler();
  }

  @Override public Logger getLogger() {
    return plugin.getLogger();
  }

  @Override public void runSync(Runnable task) {
    task.run();
  }

  @Override public void runAsync(Runnable task) {
    scheduler.runAsync(plugin, task);
  }

  @Override public PlatformType getType() {
    return PlatformType.BUNGEECORD;
  }

  @Override public String getName() {
    return "BungeeCord";
  }

  /**
   * Gets the underlying BungeeCord Plugin instance
   *
   * @return Plugin instance
   */
  public Plugin getPlugin() {
    return plugin;
  }

  /**
   * Gets the ProxyServer instance
   *
   * @return ProxyServer instance
   */
  public ProxyServer getProxy() {
    return plugin.getProxy();
  }
}
