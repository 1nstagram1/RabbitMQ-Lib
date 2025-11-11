package io.hydrodevelopments.celesmq.platform;

import java.util.logging.Logger;

/**
 * Platform abstraction interface for supporting multiple Minecraft server platforms
 * (Spigot/Paper, BungeeCord, Velocity, Minestom, Sponge, Folia, NukkitX/PowerNukkitX)
 */
public interface Platform {

  /**
   * Gets the platform's logger
   * @return Logger instance
   */
  Logger getLogger();

  /**
   * Executes a task synchronously on the main thread
   * For async-first platforms like Velocity, this may execute immediately
   * @param task the task to execute
   */
  void runSync(Runnable task);

  /**
   * Executes a task asynchronously
   * @param task the task to execute
   */
  void runAsync(Runnable task);

  /**
   * Gets the platform type
   * @return PlatformType enum value
   */
  PlatformType getType();

  /**
   * Gets the platform name
   * @return platform name as string
   */
  String getName();

  /**
   * Enum representing different platform types
   */
  enum PlatformType {
    SPIGOT,
    BUNGEECORD,
    VELOCITY,
    MINESTOM,
    SPONGE,
    FOLIA,
    NUKKIT
  }
}