package io.hydrodevelopments.celesmq.platform;

import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.TaskSchedule;

import java.util.logging.Logger;

/**
 * Platform implementation for Minestom
 *
 * Minestom is a modern, from-scratch Minecraft server implementation
 * with a focus on performance and extensibility.
 */
public class MinestomPlatform implements Platform {

  private final Logger logger;

  /**
   * Creates a new Minestom platform adapter
   */
  public MinestomPlatform() {
    this.logger = Logger.getLogger("CelesMQ");
  }

  /**
   * Creates a new Minestom platform adapter with custom logger
   *
   * @param logger custom logger instance
   */
  public MinestomPlatform(Logger logger) {
    this.logger = logger;
  }

  @Override
  public void runAsync(Runnable task) {
    MinecraftServer.getSchedulerManager()
      .scheduleTask(task, TaskSchedule.immediate(), TaskSchedule.stop());
  }

  @Override
  public void runSync(Runnable task) {
    MinecraftServer.getSchedulerManager()
      .scheduleTask(task, TaskSchedule.tick(1), TaskSchedule.stop());
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public Platform.PlatformType getType() {
    return Platform.PlatformType.MINESTOM;
  }

  @Override
  public String getName() {
    return "Minestom";
  }
}