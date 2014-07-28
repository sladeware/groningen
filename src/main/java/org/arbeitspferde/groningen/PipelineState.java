package org.arbeitspferde.groningen;

import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;

import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.ProtoBufConfig;
import org.arbeitspferde.groningen.exceptions.InvalidConfigurationException;
import org.arbeitspferde.groningen.experimentdb.CommandLine;
import org.arbeitspferde.groningen.experimentdb.Experiment;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlag;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlagSet;
import org.arbeitspferde.groningen.proto.ExperimentDbProtos;
import org.arbeitspferde.groningen.proto.ExperimentDbProtos.Subject;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Object representing PipelineState. Mainly used in {@link Datastore}.
 */
public class PipelineState {
  private PipelineId pipelineId;
  private GroningenConfig config;
  private ExperimentDb experimentDb;

  public PipelineState(PipelineId pipelineId, GroningenConfig config, ExperimentDb experimentDb) {
    this.pipelineId = pipelineId;
    this.config = config;
    this.experimentDb = experimentDb;
  }

  public PipelineState(byte[] bytes) {
    org.arbeitspferde.groningen.proto.ExperimentDbProtos.PipelineState stateProto;
    try {
      stateProto =
          org.arbeitspferde.groningen.proto.ExperimentDbProtos.PipelineState.parseFrom(bytes);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }

    pipelineId = new PipelineId(stateProto.getId().getId());
    try {
      config = new ProtoBufConfig(stateProto.getConfiguration());
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    experimentDb = new ExperimentDb();

    final List<Long> subjectIds = Lists.newArrayList();
    for (Subject subjectProto : stateProto.getSubjectsList()) {
      final SubjectStateBridge bridge = experimentDb.makeSubject(subjectProto.getId());
      subjectIds.add(subjectProto.getId());

      /* TODO(team): Migrate the stink that this switch statement is into a EnumMap or have
       *             JvmFlagSet translate the mapping itself.
       */

      final JvmFlagSet.Builder builder = JvmFlagSet.builder();

      final ExperimentDbProtos.CommandLine cl = subjectProto.getCommandLine();
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
    }

    experimentDb.makeExperiment(subjectIds);
  }

  public PipelineId pipelineId() {
    return pipelineId;
  }

  public GroningenConfig config() {
    return config;
  }

  public ExperimentDb experimentDb() {
    return experimentDb;
  }

  public byte[] toBytes() {
    org.arbeitspferde.groningen.proto.ExperimentDbProtos.PipelineId.Builder idProtoBuilder =
        org.arbeitspferde.groningen.proto.ExperimentDbProtos.PipelineId.newBuilder();
    idProtoBuilder.setId(pipelineId.id());
    org.arbeitspferde.groningen.proto.ExperimentDbProtos.PipelineId idProto =
        idProtoBuilder.build();

    ProgramConfiguration configurationProto = config.getProtoConfig();

    List<ExperimentDbProtos.Subject> subjectProtos = new ArrayList<>();
    Experiment lastExperiment = experimentDb.getLastExperiment();

    if (lastExperiment != null) {
      // Then write out all the subjects in the experiment
      final JvmFlag[] arguments = JvmFlag.values();
      for (final SubjectStateBridge subject : lastExperiment.getSubjects()) {
        final ExperimentDbProtos.Subject.Builder subjectBuilder =
            ExperimentDbProtos.Subject.newBuilder();
        subjectBuilder.setId(subject.getIdOfObject());
        subjectBuilder.setIsDefault(subject.getAssociatedSubject().isDefault());
        // Copy the command line
        final CommandLine commandLine = subject.getCommandLine();
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
        subjectBuilder.setCommandLine(commandLineBuilder);
        subjectProtos.add(subjectBuilder.build());
      }
    }

    org.arbeitspferde.groningen.proto.ExperimentDbProtos.PipelineState.Builder stateProtoBuilder =
        org.arbeitspferde.groningen.proto.ExperimentDbProtos.PipelineState.newBuilder();
    stateProtoBuilder.setId(idProto);
    stateProtoBuilder.setConfiguration(configurationProto);
    stateProtoBuilder.addAllSubjects(subjectProtos);
    return stateProtoBuilder.build().toByteArray();
  }
}
