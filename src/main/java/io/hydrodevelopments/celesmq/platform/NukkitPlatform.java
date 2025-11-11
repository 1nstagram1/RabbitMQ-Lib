package io.hydrodevelopments.celesmq.platform;

import cn.nukkit.Server;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.scheduler.AsyncTask;

import java.util.logging.Logger;

/**
 * Platform implementation for NukkitX/PowerNukkitX
 * <p>
 * Nukkit is a Minecraft: Bedrock Edition server software written in Java. This allows cross-platform communication
 * between Java Edition servers and Bedrock Edition servers using RabbitMQ.
 * <p>
 * Compatible with both NukkitX and PowerNukkitX variants.
 */
public class NukkitPlatform implements Platform {

  private final Plugin plugin;
  private final Server server;
  private final Logger logger;

  /**
   * Creates a new Nukkit platform adapter
   *
   * @param plugin the Nukkit plugin instance
   */
  public NukkitPlatform(Plugin plugin) {
    this.plugin = plugin;
    this.server = plugin.getServer();
    this.logger = Logger.getLogger(plugin.getName());
  }

  /**
   * Creates a new Nukkit platform adapter with custom logger
   *
   * @param plugin the Nukkit plugin instance
   * @param logger custom logger instance
   */
  public NukkitPlatform(Plugin plugin, Logger logger) {
    this.plugin = plugin;
    this.server = plugin.getServer();
    this.logger = logger;
  }

  @Override public void runAsync(Runnable task) {
    // PowerNukkitX async task
    server.getScheduler().scheduleAsyncTask(plugin, new AsyncTask() {
      @Override public void onRun() {
        task.run();
      }
    });
  }

  @Override public void runSync(Runnable task) {
    server.getScheduler().scheduleTask(plugin, task);
  }

  @Override public Logger getLogger() {
    return logger;
  }

  @Override public Platform.PlatformType getType() {
    return Platform.PlatformType.NUKKIT;
  }

  @Override public String getName() {
    return "PowerNukkitX";
  }
}