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

package org.arbeitspferde.groningen.common;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Objects;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Map.Entry;

/**
 * A simple implementation of {@link BlockScope}.
 */
public class SimpleScope implements BlockScope {
  static class Memento {
    private final Map<Key<?>, Object> values;

    private Memento(SimpleScope scopeToCapture) {
      values = scopeToCapture.isInScope() ? Maps.newHashMap(scopeToCapture.values.get()) : null;
    }

    @Override
    public boolean equals(Object that) {
      if (!(that instanceof Memento)) {
        return false;
      }
      return Objects.equal(this.values, ((Memento) that).values);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(values);
    }
  }

  private static final Provider<Object> SEEDED_KEY_PROVIDER =
      new Provider<Object>() {
        @Override
        public Object get() {
          throw new IllegalStateException("If you got here then it means that" +
              " your code asked for scoped object which should have been" +
              " explicitly seeded in this scope by calling" +
              " SimpleScope.seed(), but was not.");
        }
      };
  // Dummy value to associate with an Object in the backing Map
  private static final Object PRESENT = new Object();
  private static final ThreadLocal<Map<SimpleScope, Object>> ACTIVE_SCOPES =
      new ThreadLocal<Map<SimpleScope, Object>>() {
        @Override protected Map<SimpleScope, Object> initialValue() {
          return new MapMaker().weakKeys().makeMap();
        }
      };

  private final ThreadLocal<Map<Key<?>, Object>> values = new ThreadLocal<>();

  static Map<SimpleScope, SimpleScope.Memento> captureActiveSimpleScopes() {
    Map<SimpleScope, SimpleScope.Memento> simpleScopeMap = Maps.newHashMap();
    for (SimpleScope simpleScope : ACTIVE_SCOPES.get().keySet()) {
      simpleScopeMap.put(simpleScope, new Memento(simpleScope));
    }
    return simpleScopeMap;
  }

  static void replaceActiveSimpleScopes(Map<SimpleScope, SimpleScope.Memento> capturedScopes) {
    SimpleScope.ACTIVE_SCOPES.get().clear();
    for (Entry<SimpleScope, SimpleScope.Memento> simpleScopeMapEntry : capturedScopes.entrySet()) {
      SimpleScope simpleScope = simpleScopeMapEntry.getKey();
      SimpleScope.Memento memento = simpleScopeMapEntry.getValue();
      if (memento.values == null) {
        simpleScope.values.remove();
      } else {
        simpleScope.values.set(memento.values);
        ACTIVE_SCOPES.get().put(simpleScope, PRESENT);
      }
    }
  }

  @Override
  public void enter() {
    checkState(!isInScope(), "A scoping block is already in progress");
    values.set(Maps.<Key<?>, Object>newHashMap());
    ACTIVE_SCOPES.get().put(this, PRESENT);
  }

  @Override
  public void exit() {
    checkState(isInScope(), "No scoping block in progress");
    values.remove();
    ACTIVE_SCOPES.get().remove(this);
  }

  @Override
  public boolean isInScope() {
    return (values.get() != null);
  }

  @Override
  public <T> void seed(Key<T> key, @Nullable T value) {
    Map<Key<?>, Object> scopedObjects = getScopedObjectMap(key);
    checkState(!scopedObjects.containsKey(key), "A value for the key %s was " +
        "already seeded in this scope. Old value: %s New value: %s", key,
        scopedObjects.get(key), value);
    scopedObjects.put(key, value);
  }

  @Override
  public <T> void seed(Class<T> clazz, @Nullable T value) {
    seed(Key.get(clazz), value);
  }

  @Override
  public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
    return new Provider<T>() {
      @Override
      public T get() {
        Map<Key<?>, Object> scopedObjects = getScopedObjectMap(key);

        @SuppressWarnings("unchecked")
        T current = (T) scopedObjects.get(key);
        if (current == null && !scopedObjects.containsKey(key)) {
          current = unscoped.get();
          scopedObjects.put(key, current);
        }
        return current;
      }
    };
  }

  private <T> Map<Key<?>, Object> getScopedObjectMap(Key<T> key) {
    Map<Key<?>, Object> scopedObjects = values.get();
    if (scopedObjects == null) {
      throw new OutOfScopeException("Cannot access " + key
          + " outside of a scoping block");
    }
    return scopedObjects;
  }

  /**
   * Returns a provider that always throws exception complaining that the object
   * in question must be seeded before it can be injected.
   *
   * @return typed provider
   */
  @SuppressWarnings({"unchecked"})
  public static final <T> Provider<T> seededKeyProvider() {
    return (Provider<T>) SEEDED_KEY_PROVIDER;
  }

}
