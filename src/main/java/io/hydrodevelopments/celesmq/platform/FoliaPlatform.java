package io.hydrodevelopments.celesmq.platform;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Platform implementation for Folia
 * <p>
 * Folia is Paper's experimental multithreaded fork using regional multithreading. This platform implementation uses
 * Folia's global region scheduler for thread-safe RabbitMQ operations.
 * <p>
 * Note: Folia requires all operations to be scheduled on appropriate regions. RabbitMQ operations are inherently async
 * and thread-safe, so we use the global region for scheduling.
 */
public class FoliaPlatform implements Platform {

  private final Plugin plugin;
  private final Logger logger;

  /**
   * Creates a new Folia platform adapter
   *
   * @param plugin the Bukkit plugin instance
   */
  public FoliaPlatform(Plugin plugin) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
  }

  @Override public void runAsync(Runnable task) {
    // Folia's async scheduler for non-world-specific tasks
    try {
      // Try Folia's async scheduler first
      Class<?> asyncScheduler = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
      Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
      asyncScheduler.getMethod("runNow", Plugin.class, Runnable.class).invoke(scheduler, plugin, (Runnable) task::run);
    } catch (Exception e) {
      // Fallback to standard async if Folia's async scheduler is unavailable
      Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }
  }

  @Override public void runSync(Runnable task) {
    // Use Folia's global region scheduler
    try {
      // Try Folia's global region scheduler
      Class<?> globalRegionScheduler =
        Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
      Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
      globalRegionScheduler.getMethod("run", Plugin.class, Runnable.class)
        .invoke(scheduler, plugin, (Runnable) task::run);
    } catch (Exception e) {
      // Fallback to standard sync if Folia's global region scheduler is unavailable
      Bukkit.getScheduler().runTask(plugin, task);
    }
  }

  @Override public Logger getLogger() {
    return logger;
  }

  @Override public Platform.PlatformType getType() {
    return Platform.PlatformType.FOLIA;
  }

  @Override public String getName() {
    return "Folia";
  }
}