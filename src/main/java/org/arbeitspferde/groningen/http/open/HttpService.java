package org.arbeitspferde.groningen.http.open;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceFilter;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.DispatcherType;

import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service to manage the lifecycle of HTTP Server.
 */
@Singleton
public final class HttpService extends AbstractIdleService {

  private static final Logger log = Logger.getLogger(HttpService.class.getCanonicalName());

  private final GuiceFilter guiceFilter;
  private final ServletContextHandler contextHandler;
  private final Server server;

  @Inject
  public HttpService(GuiceFilter guiceFilter,
      ServletContextHandler contextHandler,
      Provider<Server> serverProvider) {
    this.guiceFilter = guiceFilter;
    this.contextHandler = contextHandler;
    this.server = serverProvider.get();
  }

  @Override
  protected void startUp() throws Exception {
    contextHandler.addFilter(new FilterHolder(guiceFilter), "/*",
        EnumSet.allOf(DispatcherType.class));
    contextHandler.addServlet(DefaultServlet.class.getCanonicalName(), "/");
    server.setHandler(contextHandler);
    server.start();
    log.log(Level.INFO, "Started HTTP Server at port {0}",
        ((ServerConnector) server.getConnectors()[0]).getLocalPort());
  }

  @Override
  protected void shutDown() throws Exception {
    log.log(Level.INFO, "Shutting down HTTP server.");
    server.stop();
  }
}
