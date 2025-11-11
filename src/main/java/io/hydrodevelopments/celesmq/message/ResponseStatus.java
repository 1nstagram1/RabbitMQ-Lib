package io.hydrodevelopments.celesmq.message;

import java.util.Objects;

/**
 * Response status for MessageResponse
 * <p>
 * This class provides built-in status constants and allows developers to create custom statuses for their specific use
 * cases.
 * <p>
 * Example usage:
 * <pre>
 * // Built-in statuses
 * ResponseStatus status = ResponseStatus.SUCCESS;
 *
 * // Custom statuses
 * ResponseStatus rateLimited = ResponseStatus.custom("RATE_LIMITED");
 * ResponseStatus maintenance = ResponseStatus.custom("MAINTENANCE_MODE");
 * </pre>
 */
public final class ResponseStatus {
  private final String name;

  private ResponseStatus(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Status name cannot be null or empty");
    }
    this.name = name.toUpperCase();
  }

  // Built-in response statuses

  /**
   * Indicates the request completed successfully
   */
  public static final ResponseStatus SUCCESS = new ResponseStatus("SUCCESS");

  /**
   * Indicates an error occurred during processing
   */
  public static final ResponseStatus ERROR = new ResponseStatus("ERROR");

  /**
   * Indicates the request timed out waiting for a response
   */
  public static final ResponseStatus TIMEOUT = new ResponseStatus("TIMEOUT");

  /**
   * Indicates the requested resource was not found
   */
  public static final ResponseStatus NOT_FOUND = new ResponseStatus("NOT_FOUND");

  /**
   * Indicates the request was invalid or malformed
   */
  public static final ResponseStatus INVALID = new ResponseStatus("INVALID");

  /**
   * Indicates the request is still pending
   */
  public static final ResponseStatus PENDING = new ResponseStatus("PENDING");

  /**
   * Creates a custom response status
   * <p>
   * This allows developers to define their own status codes for application-specific scenarios.
   *
   * @param name the name of the custom status (case-insensitive)
   *
   * @return a new ResponseStatus with the given name
   *
   * @throws IllegalArgumentException if name is null or empty
   * @example <pre>
   *   ResponseStatus rateLimited = ResponseStatus.custom("RATE_LIMITED");
   *   ResponseStatus authRequired = ResponseStatus.custom("AUTH_REQUIRED");
   *   ResponseStatus maintenanceMode = ResponseStatus.custom("MAINTENANCE_MODE");
   *   </pre>
   */
  public static ResponseStatus custom(String name) {
    return new ResponseStatus(name);
  }

  /**
   * Parses a status name and returns the corresponding ResponseStatus
   * <p>
   * This method first checks if the name matches any built-in status, otherwise creates a custom status with the given
   * name.
   *
   * @param name the status name to parse (case-insensitive)
   *
   * @return the matching built-in status or a new custom status
   *
   * @throws IllegalArgumentException if name is null or empty
   */
  public static ResponseStatus fromString(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Status name cannot be null or empty");
    }

    String upperName = name.toUpperCase();

    // Check built-in statuses
    if (upperName.equals("SUCCESS")) {
      return SUCCESS;
    }
    if (upperName.equals("ERROR")) {
      return ERROR;
    }
    if (upperName.equals("TIMEOUT")) {
      return TIMEOUT;
    }
    if (upperName.equals("NOT_FOUND")) {
      return NOT_FOUND;
    }
    if (upperName.equals("INVALID")) {
      return INVALID;
    }
    if (upperName.equals("PENDING")) {
      return PENDING;
    }

    // Create custom status for unknown names
    return custom(name);
  }

  /**
   * Gets the name of this status
   *
   * @return the status name (always uppercase)
   */
  public String getName() {
    return name;
  }

  /**
   * Checks if this status represents a successful response
   *
   * @return true if this is the SUCCESS status
   */
  public boolean isSuccess() {
    return this.equals(SUCCESS);
  }

  /**
   * Checks if this status represents an error
   *
   * @return true if this is the ERROR status
   */
  public boolean isError() {
    return this.equals(ERROR);
  }

  /**
   * Checks if this status represents a timeout
   *
   * @return true if this is the TIMEOUT status
   */
  public boolean isTimeout() {
    return this.equals(TIMEOUT);
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResponseStatus that = (ResponseStatus) o;
    return name.equals(that.name);
  }

  @Override public int hashCode() {
    return Objects.hash(name);
  }

  @Override public String toString() {
    return name;
  }
}
