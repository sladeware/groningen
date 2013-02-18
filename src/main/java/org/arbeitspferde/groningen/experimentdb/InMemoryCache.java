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

package org.arbeitspferde.groningen.experimentdb;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

import org.arbeitspferde.groningen.config.NamedConfigParam;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Experiment Database In Memory Cache is used to store data locally in the
 * system's main memory. The intention is only data in-use by the Groningen pipeline
 * should be stored in classes inheriting from this one.
 *
 * @param T The actual type of cache entries.
 */
abstract class InMemoryCache<T extends InMemoryCache.Value<T>> {
  @Inject
  @NamedConfigParam("default_in_memory_cache_ttl")
  private int defaultInMemoryCacheTtl = 600000;

  private final AtomicLong insertionCount = new AtomicLong(0);
  private final AtomicLong hitCount = new AtomicLong(0);
  private final AtomicLong missCount = new AtomicLong(0);
  private final Cache<Long, T> cache = CacheBuilder
      .newBuilder()
      .expireAfterAccess(defaultInMemoryCacheTtl, TimeUnit.SECONDS)
      .build();

  public void reset(InMemoryCache<T> anotherCache) {
    cache.invalidateAll();
    cache.putAll(anotherCache.cache.asMap());
  }
  
  /**
   * Register an element in the cache (and return it unchanged).
   */
  T register(T toCache) {
    cache.put(toCache.getIdOfObject(), toCache);
    insertionCount.incrementAndGet();
    return toCache;
  }

  /**
   * Return the cache element with the given id, or null if it doesn't exist
   */
  public T lookup(long id) {
    checkArgument(id > 0, "IDs should be positive.");

    final T result = cache.getIfPresent(id);

    if (null == result) {
      missCount.incrementAndGet();
    } else {
      hitCount.incrementAndGet();
    }

    return result;
  }

  abstract static class Value<T extends Value> {
    private final long idOfObject;

    /** Returns the id of the subject owning the cached data */
    public long getIdOfObject() {
      return idOfObject;
    }

    protected Value(long id) {
      checkArgument(id > 0, "IDs should be positive.");
      this.idOfObject = id;
    }
  }

  public long getCacheSize() {
    return cache.size();
  }

  public long getInsertionCount() {
    return insertionCount.get();
  }

  public long getHitCount() {
    return hitCount.get();
  }

  public long getMissCount() {
    return missCount.get();
  }
}
