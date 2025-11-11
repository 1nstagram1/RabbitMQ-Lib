package io.hydrodevelopments.celesmq.platform;

import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;

import java.util.logging.Logger;

/**
 * Platform implementation for Sponge
 *
 * Sponge is a community-driven plugin platform that runs on Forge and Fabric,
 * allowing plugins to work alongside mods.
 */
public class SpongePlatform implements Platform {

  private final PluginContainer plugin;
  private final Server server;
  private final Logger logger;

  /**
   * Creates a new Sponge platform adapter
   *
   * @param plugin the plugin container
   * @param server the Sponge server instance
   * @param logger the logger instance
   */
  public SpongePlatform(PluginContainer plugin, Server server, Logger logger) {
    this.plugin = plugin;
    this.server = server;
    this.logger = logger;
  }

  @Override
  public void runAsync(Runnable task) {
    Sponge.asyncScheduler().submit(
      Task.builder()
        .plugin(plugin)
        .execute(task)
        .build()
    );
  }

  @Override
  public void runSync(Runnable task) {
    server.scheduler().submit(
      Task.builder()
        .plugin(plugin)
        .execute(task)
        .build()
    );
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public Platform.PlatformType getType() {
    return Platform.PlatformType.SPONGE;
  }

  @Override
  public String getName() {
    return "Sponge";
  }
}