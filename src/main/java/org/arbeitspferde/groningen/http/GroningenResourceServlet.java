package org.arbeitspferde.groningen.http;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A simple HTTP Servlet to serve static Groningen resources.
 */
public class GroningenResourceServlet extends HttpServlet {

  private static final String DEFAULT_RESOURCE = "/index.html";
  private static final String BASE_PATH = "/org/arbeitspferde/groningen/ui";

  public GroningenResourceServlet() {
  }

  private static String getContentType(String resourcePath) {
    if (resourcePath.endsWith(".css")) {
      return "text/css";
    } else if (resourcePath.endsWith(".js")) {
      return "application/x-javascript";
    } else {
      return "text/html";
    }
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String requestedPath = request.getRequestURI();
    if (Strings.isNullOrEmpty(requestedPath) || requestedPath.equals("/")) {
      requestedPath = DEFAULT_RESOURCE;
    }
    String completePath = BASE_PATH + requestedPath;
    InputStream istream = getClass().getResourceAsStream(completePath);
    if (istream == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.setHeader("X-Google-Requested-Path", completePath);
      return;
    }
    response.setHeader("Content-Type", getContentType(requestedPath));
    OutputStream ostream = response.getOutputStream();
    ByteStreams.copy(istream, ostream);
  }
}
