package org.arbeitspferde.groningen;

import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;

import org.arbeitspferde.groningen.common.EvaluatedSubject;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.ProtoBufConfig;
import org.arbeitspferde.groningen.exceptions.InvalidConfigurationException;
import org.arbeitspferde.groningen.experimentdb.CommandLine;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlag;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlagSet;
import org.arbeitspferde.groningen.proto.ExperimentDbProtos;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration;
import org.arbeitspferde.groningen.utility.PinnedClock;
import org.joda.time.Instant;

import java.util.List;

/**
 * PipelineHistoryState encapsulates pipeline state after each iteration (unlike PipelineState
 * it includes evaluated experiment results).
 * 
 * PipelineHistoryState is intended for HistoryDatastore. Please note that although
 * HistoryDatastore's functionality resembles event evaluation system (see EventLoggerService),
 * it's different from the latter. Unlike EventLoggerService, HistoryDatastore (and consequently,
 * PipelineHistoryState is multiple-pipelines-aware).
 * 
 * Also, although PipelineHistoryState resembles PipelineState, they're kept separate, because
 * their respective datastores, HistoryDatastore and Datastore, were designed to be totally
 * independent of each other. 
 */
public class PipelineHistoryState {
  private final PipelineId pipelineId;
  private final GroningenConfig config;
  private final Instant endTimestamp;
  private final EvaluatedSubject[] evaluatedSubjects;
  private final long experimentId;
  
  public PipelineHistoryState(PipelineId pipelineId, GroningenConfig config, Instant endTimestamp,
      EvaluatedSubject[] evluatedSubjects, long experimentId) {
    this.pipelineId = pipelineId;
    this.config = config;
    this.endTimestamp = endTimestamp;
    this.evaluatedSubjects = evluatedSubjects;
    this.experimentId = experimentId;
  }
  
  public PipelineHistoryState(byte[] bytes) {
    ExperimentDbProtos.PipelineHistoryState stateProto;
    try {
      stateProto = ExperimentDbProtos.PipelineHistoryState.parseFrom(bytes);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
    
    pipelineId = new PipelineId(stateProto.getId().getId());
    try {
      config = new ProtoBufConfig(stateProto.getConfiguration());
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
    
    endTimestamp = new Instant(stateProto.getEndTimestamp());
    
    experimentId = stateProto.getExperimentId();
    
    // TODO(mbushkov): we don't need it here. Refactor to create SubjectStateBridge without it.
    ExperimentDb experimentDb = new ExperimentDb();
    
    List<EvaluatedSubject> evaluatedSubjectsList = Lists.newArrayList();
    for (ExperimentDbProtos.EvaluatedSubject esProto : stateProto.getEvaluatedSubjectsList()) {
      SubjectStateBridge bridge = experimentDb.makeSubject(esProto.getSubject().getId());
      
      final JvmFlagSet.Builder builder = JvmFlagSet.builder();
      final ExperimentDbProtos.CommandLine cl = esProto.getSubject().getCommandLine();
      for (final ExperimentDbProtos.CommandLineArgument arg : cl.getArgumentList()) {
        final int value = Integer.parseInt(arg.getValue());
        final JvmFlag argument = JvmFlag.valueOf(arg.getName());
        switch (argument) {

          case ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR:
            builder.withValue(JvmFlag.ADAPTIVE_SIZE_DECREMENT_SCALE_FACTOR, value);
            break;
          case CMS_EXP_AVG_FACTOR:
            builder.withValue(JvmFlag.CMS_EXP_AVG_FACTOR, value);
            break;
          case CMS_INCREMENTAL_DUTY_CYCLE:
            builder.withValue(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE, value);
            break;
          case CMS_INCREMENTAL_DUTY_CYCLE_MIN:
            builder.withValue(JvmFlag.CMS_INCREMENTAL_DUTY_CYCLE_MIN, value);
            break;
          case CMS_INCREMENTAL_OFFSET:
            builder.withValue(JvmFlag.CMS_INCREMENTAL_OFFSET, value);
            break;
          case CMS_INCREMENTAL_SAFETY_FACTOR:
            builder.withValue(JvmFlag.CMS_INCREMENTAL_SAFETY_FACTOR, value);
            break;
          case CMS_INITIATING_OCCUPANCY_FRACTION:
            builder.withValue(JvmFlag.CMS_INITIATING_OCCUPANCY_FRACTION, value);
            break;
          case GC_TIME_RATIO:
            builder.withValue(JvmFlag.GC_TIME_RATIO, value);
            break;
          case HEAP_SIZE:
            builder.withValue(JvmFlag.HEAP_SIZE, value);
            break;
          case MAX_GC_PAUSE_MILLIS:
            builder.withValue(JvmFlag.MAX_GC_PAUSE_MILLIS, value);
            break;
          case MAX_HEAP_FREE_RATIO:
            builder.withValue(JvmFlag.MAX_HEAP_FREE_RATIO, value);
            break;
          case MIN_HEAP_FREE_RATIO:
            builder.withValue(JvmFlag.MIN_HEAP_FREE_RATIO, value);
            break;
          case NEW_RATIO:
            builder.withValue(JvmFlag.NEW_RATIO, value);
            break;
          case NEW_SIZE:
            builder.withValue(JvmFlag.NEW_SIZE, value);
            break;
          case MAX_NEW_SIZE:
            builder.withValue(JvmFlag.MAX_NEW_SIZE, value);
            break;
          case PARALLEL_GC_THREADS:
            builder.withValue(JvmFlag.PARALLEL_GC_THREADS, value);
            break;
          case SOFT_REF_LRU_POLICY_MS_PER_MB:
            builder.withValue(JvmFlag.SOFT_REF_LRU_POLICY_MS_PER_MB, value);
            break;
          case SURVIVOR_RATIO:
            builder.withValue(JvmFlag.SURVIVOR_RATIO, value);
            break;
          case TENURED_GENERATION_SIZE_INCREMENT:
            builder.withValue(JvmFlag.TENURED_GENERATION_SIZE_INCREMENT, value);
            break;
          case YOUNG_GENERATION_SIZE_INCREMENT:
            builder.withValue(JvmFlag.YOUNG_GENERATION_SIZE_INCREMENT, value);
            break;
          case USE_CONC_MARK_SWEEP_GC:
            builder.withValue(JvmFlag.USE_CONC_MARK_SWEEP_GC, value);
            break;
          case USE_PARALLEL_GC:
            builder.withValue(JvmFlag.USE_PARALLEL_GC, value);
            break;
          case USE_PARALLEL_OLD_GC:
            builder.withValue(JvmFlag.USE_PARALLEL_OLD_GC, value);
            break;
          case USE_SERIAL_GC:
            builder.withValue(JvmFlag.USE_SERIAL_GC, value);
            break;
          case CMS_INCREMENTAL_MODE:
            builder.withValue(JvmFlag.CMS_INCREMENTAL_MODE, value);
            break;
          case CMS_INCREMENTAL_PACING:
            builder.withValue(JvmFlag.CMS_INCREMENTAL_PACING, value);
            break;
          case USE_CMS_INITIATING_OCCUPANCY_ONLY:
            builder.withValue(JvmFlag.USE_CMS_INITIATING_OCCUPANCY_ONLY, value);
            break;
        }
      }
      
      final JvmFlagSet jvmFlagSet = builder.build();
      bridge.storeCommandLine(jvmFlagSet);
      
      EvaluatedSubject evaluatedSubject = new EvaluatedSubject(
          new PinnedClock(esProto.getEndTimestamp()),
          bridge,
          esProto.getFitness(),
          stateProto.getExperimentId());
      evaluatedSubject.setClusterName(esProto.getSubject().getClusterName());
      evaluatedSubject.setSubjectGroupIndex((int) esProto.getSubject().getSubjectGroupIndex());
      evaluatedSubject.setSubjectGroupName(esProto.getSubject().getSubjectGroupName());
      evaluatedSubject.setUserName(esProto.getSubject().getUserName());
      evaluatedSubject.setDefault(esProto.getSubject().getIsDefault());
      evaluatedSubjectsList.add(evaluatedSubject);
    }
    
    evaluatedSubjects = evaluatedSubjectsList.toArray(new EvaluatedSubject[] {});
  }
  
  public byte[] toBytes() {
    ExperimentDbProtos.PipelineId.Builder idProtoBuilder =
        ExperimentDbProtos.PipelineId.newBuilder();
    idProtoBuilder.setId(pipelineId.id());
    ExperimentDbProtos.PipelineId idProto = idProtoBuilder.build();
    
    ProgramConfiguration configurationProto = config.getProtoConfig();
    
    List<ExperimentDbProtos.EvaluatedSubject> evaluatesSubjectsProtos = Lists.newArrayList();
    for (EvaluatedSubject es : evaluatedSubjects) {
      ExperimentDbProtos.EvaluatedSubject.Builder espBuilder =
          ExperimentDbProtos.EvaluatedSubject.newBuilder();
      
      // Build Subject proto
      ExperimentDbProtos.Subject.Builder spBuilder = ExperimentDbProtos.Subject.newBuilder();
      spBuilder.setClusterName(es.getClusterName());
      spBuilder.setSubjectGroupIndex(es.getSubjectGroupIndex());
      spBuilder.setSubjectGroupName(es.getSubjectGroupName());
      spBuilder.setUserName(es.getUserName());
      spBuilder.setIsDefault(es.isDefault());
      spBuilder.setId(es.getBridge().getIdOfObject());
      
      // Copy the command line
      final JvmFlag[] arguments = JvmFlag.values();
      final CommandLine commandLine = es.getBridge().getCommandLine();
      final ExperimentDbProtos.CommandLine.Builder commandLineBuilder =
          ExperimentDbProtos.CommandLine.newBuilder();
      for (final JvmFlag argument : arguments) {
        final long value = commandLine.getValue(argument);
        final ExperimentDbProtos.CommandLineArgument.Builder argumentBuilder =
            ExperimentDbProtos.CommandLineArgument.newBuilder();

        argumentBuilder.setName(argument.name());
        argumentBuilder.setValue(String.valueOf(value));
        commandLineBuilder.addArgument(argumentBuilder);
      }
      spBuilder.setCommandLine(commandLineBuilder);
      
      espBuilder.setSubject(spBuilder.build());
      espBuilder.setEndTimestamp(es.getTimeStamp().getMillis());
      espBuilder.setFitness(es.getFitness());
      
      evaluatesSubjectsProtos.add(espBuilder.build());
    }
    
    ExperimentDbProtos.PipelineHistoryState.Builder stateProtoBuilder =
        ExperimentDbProtos.PipelineHistoryState.newBuilder();
    stateProtoBuilder.setId(idProto);
    stateProtoBuilder.setConfiguration(configurationProto);
    stateProtoBuilder.addAllEvaluatedSubjects(evaluatesSubjectsProtos);
    stateProtoBuilder.setEndTimestamp(endTimestamp.getMillis());
    stateProtoBuilder.setExperimentId(experimentId);
    
    return stateProtoBuilder.build().toByteArray();
  }
  
  public PipelineId pipelineId() {
    return pipelineId;
  }
  
  public GroningenConfig config() {
    return config;
  }
  
  public Instant endTimestamp() {
    return endTimestamp;
  }
  
  public EvaluatedSubject[] evaluatedSubjects() {
    return evaluatedSubjects;
  }
}
