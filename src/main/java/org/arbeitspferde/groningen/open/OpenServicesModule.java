package org.arbeitspferde.groningen.open;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import org.arbeitspferde.groningen.http.open.HttpService;

/**
 * A Guice module for background services.
 */
public class OpenServicesModule extends AbstractModule {

  @Override
  protected void configure() {
    final Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
    serviceBinder.addBinding().to(HttpService.class);
  }

}
