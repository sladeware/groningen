package org.arbeitspferde.groningen.http;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;

import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;


/**
 * Guice ServletModule to configure Groningen Servlets.
 */
public class GroningenServletModule extends ServletModule {

  @Override
  protected void configureServlets() {
    bind(Pipelines.class).in(Singleton.class);
    bind(GroningenResourceServlet.class).in(Singleton.class);
    serve("/groningen/*").with(GuiceContainer.class, ImmutableMap.of(
        PackagesResourceConfig.PROPERTY_PACKAGES,
        Pipelines.class.getPackage().getName()
        ));
    serve("/*").with(GroningenResourceServlet.class);
  }

}
