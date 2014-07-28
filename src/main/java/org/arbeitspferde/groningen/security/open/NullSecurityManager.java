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

package org.arbeitspferde.groningen.security.open;

import com.google.inject.Singleton;

import org.arbeitspferde.groningen.security.VendorSecurityManager;

import java.util.logging.Logger;

/**
 * A no-op {@link VendorSecurityManager} since most folks probably won't do anything special
 * with this.
 */
@Singleton
public class NullSecurityManager implements VendorSecurityManager {
  private static final Logger log = Logger.getLogger(NullSecurityManager.class.getCanonicalName());

  @Override
  public void applyPermissionToPathForClass(final PathPermission permission, final String path,
      final Class<?> clazz) {
    log.info(String.format(
        "Pretending to apply permission %s to path %s for class %s.", permission, path, clazz));
  }
}
