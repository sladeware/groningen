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

package org.arbeitspferde.groningen.http;

import com.google.gson.Gson;
import com.google.inject.Inject;

import org.arbeitspferde.groningen.Pipeline;
import org.arbeitspferde.groningen.PipelineId;
import org.arbeitspferde.groningen.PipelineManager;
import org.arbeitspferde.groningen.common.EvaluatedSubject;
import org.arbeitspferde.groningen.config.GroningenConfig.ClusterConfig;
import org.arbeitspferde.groningen.config.GroningenConfig.SubjectGroupConfig;
import org.arbeitspferde.groningen.display.DisplayMediator;
import org.arbeitspferde.groningen.display.DisplayableObject;
import org.arbeitspferde.groningen.experimentdb.PauseTime;
import org.arbeitspferde.groningen.experimentdb.ResourceMetric;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.proto.Params.GroningenParamsOrBuilder;
import org.arbeitspferde.groningen.subject.Subject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * RESTful Service, serves data for the dashboard.
 */
@Path("/pipelines")
@Produces(MediaType.APPLICATION_JSON)
public class Pipelines {

  private final Gson gson;
  private final PipelineManager pipelineManager;

  // TODO(sanragsood): This is configurable via params.proto; switch once the code is moved.
  private static final int NUM_EXPERIMENT_SCORES = 5;
  private static final DateFormat df = new SimpleDateFormat("d MMM yyyy hh:mm a z");

  @Inject
  public Pipelines(Gson gson, PipelineManager pipelineManager) {
    this.gson = gson;
    this.pipelineManager = pipelineManager;
  }

  // TODO(sanragsood): These methods assume the base yet prominent use case where the experiment is
  // run on a single job in a single cluster. This needs to be fixed.
  private static String extractJobName(Pipeline pipeline) {
    for (ClusterConfig clusterConfig : pipeline.getConfig().getClusterConfigs()) {
      for (SubjectGroupConfig subjectGroupConfig : clusterConfig.getSubjectGroupConfigs()) {
        return subjectGroupConfig.getName();
      }
    }
    return "";
  }

  private static String extractUserName(Pipeline pipeline) {
    for (ClusterConfig clusterConfig : pipeline.getConfig().getClusterConfigs()) {
      for (SubjectGroupConfig subjectGroupConfig : clusterConfig.getSubjectGroupConfigs()) {
        return subjectGroupConfig.getUser();
      }
    }
    return "";
  }

  @GET
  public String getAllPipelines() {
    ArrayList<PipelineInfo> pipelineGroup = new ArrayList<PipelineInfo>();
    Map<PipelineId, Pipeline> pipelineMap = pipelineManager.getAllPipelines();
    for (Pipeline pipeline : pipelineMap.values()) {
      pipelineGroup.add(new PipelineInfo(pipeline.id().toString(), extractJobName(pipeline),
          extractUserName(pipeline)));
    }
    return this.gson.toJson(pipelineGroup);
  }

  private static ExperimentInfo[] extractExperimentInfo(
      EvaluatedSubject[] subjects, long cumulativeExperimentIdSum) {
    ArrayList<ExperimentInfo> experimentScores = new ArrayList<ExperimentInfo>();
    int count = 1;
    for (EvaluatedSubject subject : subjects) {
      experimentScores.add(new ExperimentInfo(
          subject.getExperimentId(), count,
          subject.isDefault() ? "DEFAULT SETTINGS" :
            subject.getBridge().getCommandLine().toArgumentString(),
          df.format(subject.getTimeStamp().getMillis())));
      if (count++ >= NUM_EXPERIMENT_SCORES) {
        break;
      }
    }
    return experimentScores.toArray(new ExperimentInfo[0]);
  }

  @GET
  @Path("/{pipelineIds}")
  public String getPipeline(@PathParam("pipelineIds") String pipelineIds) {
    String[] pipelineIdList = pipelineIds.split(",");
    ArrayList<DetailedPipelineInfo> pipelines = new ArrayList<DetailedPipelineInfo>();
    for (String pipelineId : pipelineIdList) {
      if (!pipelineId.isEmpty()) {
        Pipeline pipeline = pipelineManager.findPipelineById(new PipelineId(pipelineId));
        DetailedPipelineInfo pipelineInfo = new DetailedPipelineInfo(
            pipelineId, extractJobName(pipeline), extractUserName(pipeline));
        // TODO(sanragsood): Possibly rename DisplayMediator?
        DisplayMediator infoProvider = pipeline.getDisplayableInformationProvider();
        // Warnings
        pipelineInfo.warnings = infoProvider.getWarnings();
        // Status
        ArrayList<StatusData> status = new ArrayList<StatusData>();
        for (DisplayableObject statusObj : infoProvider.getMonitoredObjects()) {
          status.add(new StatusData(statusObj.getInfoString(), statusObj.getObject().toString()));
        }
        pipelineInfo.status = status.toArray(new StatusData[0]);
        // Best Experiment Scores
        EvaluatedSubject[] alltimeSubjects = infoProvider.getAlltimeExperimentSubjects();
        long cumulativeExperimentIdSum = infoProvider.getCumulativeExperimentIdSum();
        pipelineInfo.bestExperimentScores = extractExperimentInfo(
            alltimeSubjects, cumulativeExperimentIdSum);
        // Experiment History
        EvaluatedSubject[] allSubjects = infoProvider.getAllExperimentSubjects();
        ArrayList<HistoricalData> historyData = new ArrayList<HistoricalData>();
        for (EvaluatedSubject subject : allSubjects) {
          historyData.add(new HistoricalData(
              subject.getExperimentId(), subject.getFitness(),
              subject.isDefault() ? "DEFAULT SETTINGS" :
                subject.getBridge().getCommandLine().toArgumentString()));
        }
        pipelineInfo.history = historyData.toArray(new HistoricalData[0]);
        pipelines.add(pipelineInfo);
      }
    }
    return this.gson.toJson(pipelines);
  }

  @GET
  @Path("/{pipelineId}/csv")
  @Produces("text/csv")
  public Response getPipelineDataCsv(@PathParam("pipelineId") String pipelineId) {
    StringBuilder sb = new StringBuilder();
    sb.append("Timestamp,TaskID,Job,User,ExperimentIteration,JVMParamString," +
              "FitnessScore,LatencyWeight,LatencyScore,ThroughputWeight," +
              "ThroughputScore,FootprintWeight,FootprintScore\n");
    Pipeline pipeline = pipelineManager.findPipelineById(new PipelineId(pipelineId));
    if (pipeline != null) {
      DisplayMediator infoProvider = pipeline.getDisplayableInformationProvider();
      EvaluatedSubject[] allSubjects = infoProvider.getAllExperimentSubjects();
      GroningenParamsOrBuilder params = pipeline.getConfig().getParamBlock();
      for (EvaluatedSubject subject : allSubjects) {
        SubjectStateBridge subjectBridge = subject.getBridge();
        sb.append('"')
            .append(df.format(subject.getTimeStamp().getMillis()))            // Timestamp
            .append('"')
            .append(',')
            .append(subject.getSubjectGroupIndex())                           // TaskID
            .append(',')
            .append(subject.getSubjectGroupName())                            // Job
            .append(',')
            .append(subject.getUserName())                                    // User
            .append(',')
            .append(subject.getExperimentId())                                // ExperimentIteration
            .append(',')
            .append('"')
            .append(subject.isDefault() ? "DEFAULT SETTINGS" :
                subjectBridge.getCommandLine().toArgumentString())            // JVMParamString
            .append('"')
            .append(',')
            .append(subject.getFitness())                                     // FitnessScore
            .append(',')
            .append(params.getLatencyWeight())                                // LatencyWeight
            .append(',')
            .append(subjectBridge.getPauseTime().computeScore(
                PauseTime.ScoreType.LATENCY))                                 // LatencyScore
            .append(',')
            .append(params.getThroughputWeight())                             // ThroughputWeight
            .append(',')
            .append(subjectBridge.getPauseTime().computeScore(
                PauseTime.ScoreType.THROUGHPUT))                              // ThroughputScore
            .append(',')
            .append(params.getMemoryWeight())                                 // FootprintWeight
            .append(',')
            .append(subjectBridge.getResourceMetric().computeScore(
                ResourceMetric.ScoreType.MEMORY))                             // FootprintScore
            .append('\n');
      }
    }
    return Response
        .ok(sb.toString())
        .header("Content-Disposition", "attachment; filename=" + pipelineId + ".csv")
        .build();
  }

  @GET
  @Path("/{pipelineId}/config")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getPipelineConfig(@PathParam("pipelineId") String pipelineId) {
    String config = "";
    Pipeline pipeline = this.pipelineManager.findPipelineById(new PipelineId(pipelineId));
    if (pipeline != null) {
      config = pipeline.getConfig().getProtoConfig().toString();
    }
    return Response
        .ok(config)
        .header("Content-Disposition", "attachment; filename=" + pipelineId + ".config")
        .build();
  }
}
