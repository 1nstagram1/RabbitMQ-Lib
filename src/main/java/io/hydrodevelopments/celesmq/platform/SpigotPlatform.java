package io.hydrodevelopments.celesmq.platform;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

/**
 * Platform implementation for Spigot/Paper servers
 */
public class SpigotPlatform implements Platform {

  private final JavaPlugin plugin;

  public SpigotPlatform(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  @Override public Logger getLogger() {
    return plugin.getLogger();
  }

  @Override public void runSync(Runnable task) {
    if (Bukkit.isPrimaryThread()) {
      task.run();
    } else {
      Bukkit.getScheduler().runTask(plugin, task);
    }
  }

  @Override public void runAsync(Runnable task) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
  }

  @Override public PlatformType getType() {
    return PlatformType.SPIGOT;
  }

  @Override public String getName() {
    return "Spigot/Paper";
  }

  /**
   * Gets the underlying JavaPlugin instance
   *
   * @return JavaPlugin instance
   */
  public JavaPlugin getPlugin() {
    return plugin;
  }
}
