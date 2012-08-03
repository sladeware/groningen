/* Copyright 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.arbeitspferde.groningen.security;
/**
 * This interface allows one to apply additional vendor-specific security policies to elements
 * in the server.
 */
public interface VendorSecurityManager {
  /**
   * Apply a given permission to a path.
   *
   * @param permission The allowed permission.
   * @param path The path to have its access changed.
   * @param clazz The context in which the permission is granted.
   */
  public void applyPermissionToPathForClass(final PathPermission permission, final String path,
      final Class<?> clazz);

  /**
   * Permissions that apply to paths.
   */
  public static enum PathPermission {
    EXECUTE_FILESYSTEM_ENTITY,
    DELETE_FILESYSTEM_ENTITY,
    READ_FILESYSTEM_ENTITY,
    WRITE_FILESYSTEM_ENTITY
  }
}
