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

package org.arbeitspferde.groningen.display;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.arbeitspferde.groningen.Pipeline;
import org.arbeitspferde.groningen.PipelineId;
import org.arbeitspferde.groningen.PipelineManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet to display Groningen specific information on the HUD.
 */
@Singleton
public class GroningenServlet extends HttpServlet {
  @VisibleForTesting boolean isInitialized = false;

  private final PipelineManager pipelineManager;
  
  /**
   * Constructor for the GroningenServlet class
   *
   * @param objectToDisplayProvider a {@link Displayable}
   */
  @Inject
  public GroningenServlet(final PipelineManager pipelineManager) {
    this.pipelineManager = pipelineManager;
    isInitialized = true;
  }

  /**
   * Called by the server (via the service method) to allow a servlet to handle
   * a GET request. Calls <code>displayMediator.display()</code>.
   *
   * @param req   an {@link HttpServletRequest} object that
   *                  of the servlet
   * @param res  an {@link HttpServletResponse} object that
   *              contains the response the servlet sends
   *              to the client
   *
   * @exception IOException
   * @exception ServletException
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException {
    res.setContentType("text/html");
    PrintWriter out = res.getWriter();
    out.println("<html>");
    out.println("<head>");
    out.println("<title>Groningen Status</title>");
    out.println("</head>");

    out.println("<body>");

    Map<PipelineId, Pipeline> pipelinesSnapshot = pipelineManager.getAllPipelines();
    for (PipelineId id : pipelinesSnapshot.keySet()) {
      Pipeline pipeline = pipelinesSnapshot.get(id);
      out.println("<p>" + pipeline.getDisplayable().toHtml() + "</p>");
      out.println("<hr/>");
    }

    out.println("</body>");
    out.println("</html>");
  }
}
