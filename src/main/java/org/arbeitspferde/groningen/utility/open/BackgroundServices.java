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

package org.arbeitspferde.groningen.utility.open;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * {@link BackgroundServices} encompasses services or facilities related to Groningen that are
 * required upon the start of the service through its lifetime—i.e., daemon ones.
 *
 * It works in conjunction with the Guice {@link Multibinder}, which passes this a {@link Set} of
 * {@link Service} instances whose lifecycles this class will manage.
 */
@Singleton
public class BackgroundServices implements Service {
  private static final Logger log = Logger.getLogger(BackgroundServices.class.getCanonicalName());

  private final Set<Service> services;

  private State state = State.NEW;

  @Inject
  public BackgroundServices(final Set<Service> services) {
    this.services = services;
  }

  @Override
  public ListenableFuture<State> start() {
    log.info("Starting dummy background services...");
    state = State.RUNNING;

    return future();
  }

  @Override
  public State startAndWait() {
    log.info("Starting dummy background services...");

    state = State.RUNNING;

    return state;
  }

  @Override
  public boolean isRunning() {
    return State.RUNNING.equals(state);
  }

  @Override
  public State state() {
    return state;
  }

  @Override
  public ListenableFuture<State> stop() {
    log.info("Stopping dummy background services...");

    state = State.TERMINATED;

    return future();
  }

  @Override
  public State stopAndWait() {
    log.info("Stopping dummy background services...");

    state = State.TERMINATED;

    return state;
  }

  private ListenableFuture<State> future() {
    return new ListenableFuture<State>() {
      @Override
      public void addListener(final Runnable listener, final Executor executor) {
        log.info(String.format(
            "Not registering %s listener in dummy BackgroundServices.", listener));
      }

      @Override
      public boolean cancel(final boolean mayInterruptIfRunning) {
        log.info("Cancelling dummy BackgroundServices.");
        return true;
      }

      @Override
      public boolean isCancelled() {
        return true;
      }

      @Override
      public boolean isDone() {
        return true;
      }

      @Override
      public State get() {
        return state;
      }

      @Override
      public State get(final long timeout, final TimeUnit unit) {
        return state;
      }
    };
  }

  @Override
  public void addListener(final Listener listener, final Executor executor) {
    log.info(String.format(
        "Not registering %s listener in dummy BackgroundServices.", listener));
  }

  public Throwable failureCause() {
    log.info("Not returning a failure cause.");
    return null;
  }
}
