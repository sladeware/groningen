package org.arbeitspferde.groningen.historydatastore;

import junit.framework.TestCase;

import org.arbeitspferde.groningen.HistoryDatastore;
import org.arbeitspferde.groningen.HistoryDatastore.HistoryDatastoreException;
import org.arbeitspferde.groningen.PipelineHistoryState;
import org.arbeitspferde.groningen.PipelineId;
import org.arbeitspferde.groningen.common.EvaluatedSubject;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.config.ProtoBufConfig;
import org.arbeitspferde.groningen.exceptions.InvalidConfigurationException;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.arbeitspferde.groningen.experimentdb.SubjectStateBridge;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlag;
import org.arbeitspferde.groningen.experimentdb.jvmflags.JvmFlagSet;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration.ClusterConfig;
import org.arbeitspferde.groningen.proto.GroningenConfigProto.ProgramConfiguration.ClusterConfig.SubjectGroupConfig;
import org.arbeitspferde.groningen.proto.Params.GroningenParams;
import org.arbeitspferde.groningen.utility.PinnedClock;
import org.joda.time.Instant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Base class for {@link HistoryDatastore} implementations tests.
 */
public abstract class HistoryDatastoreTestCase extends TestCase {
  HistoryDatastore historyDatastore;
  
  private PipelineHistoryState createValidHistoryPipelineState(PipelineId pipelineId,
      Instant historyStateTimestamp, Instant evaluatedSubjectsTimestamps[], 
      JvmFlagSet[] jvmFlagSets, double[] fitnessScores) 
      throws InvalidConfigurationException {
    assertEquals(jvmFlagSets.length, fitnessScores.length);
    assertEquals(jvmFlagSets.length, evaluatedSubjectsTimestamps.length);
    
    GroningenConfig config = new ProtoBufConfig(
        ProgramConfiguration.newBuilder()
        .setParamBlock(GroningenParams.newBuilder().setInputLogName("stdin").build())
        .setUser("bigtester")
        .addCluster(ClusterConfig.newBuilder()
            .setCluster("xx")
            .addSubjectGroup(SubjectGroupConfig.newBuilder().
                setSubjectGroupName("bigTestingGroup").
                setExpSettingsFilesDir("/some/path").
                build())
            .build())
        .build());
    ExperimentDb experimentDb = new ExperimentDb();

    List<EvaluatedSubject> evaluatedSubjects = new ArrayList<>();
    for (int i = 0; i < jvmFlagSets.length; ++i) {
      SubjectStateBridge ssb = experimentDb.makeSubject();
      ssb.storeCommandLine(jvmFlagSets[i]);
      
      PinnedClock clock = new PinnedClock(evaluatedSubjectsTimestamps[i].getMillis());
      EvaluatedSubject es = new EvaluatedSubject(clock, ssb, fitnessScores[i]);
      es.setClusterName("cluster");
      es.setSubjectGroupIndex(i);
      es.setSubjectGroupName("subjectGroup");
      es.setUserName("user");
      
      evaluatedSubjects.add(es);
    }
    return new PipelineHistoryState(pipelineId, config, historyStateTimestamp,
        evaluatedSubjects.toArray(new EvaluatedSubject[] {}), /* experimentId */ 42);    
  }
  
  private PipelineHistoryState createValidHistoryPipelineState(PipelineId pipelineId)
      throws InvalidConfigurationException {
    final JvmFlagSet.Builder jvmFlagSetBuilder = JvmFlagSet.builder();
    jvmFlagSetBuilder.withValue(JvmFlag.USE_SERIAL_GC, 1)
      .withValue(JvmFlag.HEAP_SIZE, 100L)
      .withValue(JvmFlag.MAX_NEW_SIZE, 50L);
    final JvmFlagSet jvmFlagSet = jvmFlagSetBuilder.build();
    
    Instant timestamp = Instant.now();
    
    return createValidHistoryPipelineState(pipelineId, timestamp, 
        new Instant[] {timestamp, timestamp}, new JvmFlagSet[] { jvmFlagSet, jvmFlagSet },
        new double[] {10.0, 20.0});
  }
  
  private PipelineHistoryState createValidHistoryPipelineState(PipelineId pipelineId, 
      Instant pivotTime, long heapSize1, long heapSize2, long maxNewSize1, long maxNewSize2, 
      double fitnessScore1, double fitnessScore2) 
          throws InvalidConfigurationException {
    final JvmFlagSet.Builder jvmFlagSetBuilder1 = JvmFlagSet.builder();
    jvmFlagSetBuilder1.withValue(JvmFlag.USE_SERIAL_GC, 1)
      .withValue(JvmFlag.HEAP_SIZE, heapSize1)
      .withValue(JvmFlag.MAX_NEW_SIZE, maxNewSize1);
    final JvmFlagSet jvmFlagSet1 = jvmFlagSetBuilder1.build();

    final JvmFlagSet.Builder jvmFlagSetBuilder2 = JvmFlagSet.builder();
    jvmFlagSetBuilder2.withValue(JvmFlag.USE_SERIAL_GC, 1)
      .withValue(JvmFlag.HEAP_SIZE, heapSize2)
      .withValue(JvmFlag.MAX_NEW_SIZE, maxNewSize2);
    final JvmFlagSet jvmFlagSet2 = jvmFlagSetBuilder2.build();
    
    return createValidHistoryPipelineState(pipelineId,
        pivotTime, new Instant[] { pivotTime.plus(1), pivotTime.plus(2) },
        new JvmFlagSet[] { jvmFlagSet1, jvmFlagSet2 }, 
        new double[] { fitnessScore1, fitnessScore2 });
  }
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    historyDatastore = createHistoryDatastore();
  }
  
  @Override
  protected void tearDown() throws Exception {
    if (historyDatastore != null) {
      destroyHistoryDatastore(historyDatastore);
    }
    super.tearDown();
  }
  
  protected abstract HistoryDatastore createHistoryDatastore();
  protected abstract void destroyHistoryDatastore(HistoryDatastore dataStore);
  
  public void testListPipelinesIdsWorksCorrectly() 
      throws HistoryDatastoreException, InvalidConfigurationException {
    historyDatastore.writeState(createValidHistoryPipelineState(new PipelineId("state1")));
    historyDatastore.writeState(createValidHistoryPipelineState(new PipelineId("state2")));
    
    List<PipelineId> pipelineIds = historyDatastore.listPipelinesIds();
    Collections.sort(pipelineIds,
        new Comparator<PipelineId>() {
      @Override
      public int compare(PipelineId o1, PipelineId o2) {
        return o1.toString().compareTo(o2.toString());
      }      
    });
  }
  
  public void testGetAllStatesForPipelineIdWorksCorrectly() 
      throws HistoryDatastoreException, InvalidConfigurationException {
    Instant pivotTime = Instant.now();
    PipelineHistoryState state1a = createValidHistoryPipelineState(new PipelineId("state1"),
        pivotTime, 100, 200, 300, 400, 10.0, 20.0);
    PipelineHistoryState state1b = createValidHistoryPipelineState(new PipelineId("state1"),
        pivotTime.plus(1000), 1, 2, 3, 4, 5.0, 6.0);
    
    historyDatastore.writeState(state1a);
    historyDatastore.writeState(state1b);
    historyDatastore.writeState(createValidHistoryPipelineState(new PipelineId("state2")));
    
    List<PipelineHistoryState> states = 
        historyDatastore.getStatesForPipelineId(new PipelineId("state1"));
    assertEquals(2, states.size());
    
    assertEquals(state1a.pipelineId(), states.get(0).pipelineId());
    assertEquals(state1b.pipelineId(), states.get(1).pipelineId());
    
    assertEquals(state1a.config().getProtoConfig(), states.get(0).config().getProtoConfig());
    assertEquals(state1b.config().getProtoConfig(), states.get(1).config().getProtoConfig());

    assertEquals(state1a.endTimestamp(), states.get(0).endTimestamp());
    assertEquals(state1b.endTimestamp(), states.get(1).endTimestamp());

    assertEquals(state1a.evaluatedSubjects().length, states.get(0).evaluatedSubjects().length);
    assertEquals(state1b.evaluatedSubjects().length, states.get(1).evaluatedSubjects().length);
    
    assertEquals(state1a.evaluatedSubjects()[0].getBridge().getCommandLine(),
        states.get(0).evaluatedSubjects()[0].getBridge().getCommandLine());
    assertEquals(state1b.evaluatedSubjects()[1].getBridge().getCommandLine(),
        states.get(1).evaluatedSubjects()[1].getBridge().getCommandLine());
    
    assertEquals(state1a.evaluatedSubjects()[0].getFitness(), 
        states.get(0).evaluatedSubjects()[0].getFitness(), 1e-7);
    assertEquals(state1b.evaluatedSubjects()[0].getFitness(), 
        states.get(1).evaluatedSubjects()[0].getFitness(), 1e-7);
    assertEquals(state1a.evaluatedSubjects()[1].getFitness(), 
        states.get(0).evaluatedSubjects()[1].getFitness(), 1e-7);
    assertEquals(state1b.evaluatedSubjects()[1].getFitness(), 
        states.get(1).evaluatedSubjects()[1].getFitness(), 1e-7);
    
    assertEquals(state1a.evaluatedSubjects()[0].getClusterName(),
        states.get(0).evaluatedSubjects()[0].getClusterName());
    assertEquals(state1b.evaluatedSubjects()[1].getClusterName(),
        states.get(1).evaluatedSubjects()[1].getClusterName());

    assertEquals(state1a.evaluatedSubjects()[0].getSubjectGroupName(),
        states.get(0).evaluatedSubjects()[0].getSubjectGroupName());
    assertEquals(state1b.evaluatedSubjects()[1].getSubjectGroupName(),
        states.get(1).evaluatedSubjects()[1].getSubjectGroupName());

    assertEquals(state1a.evaluatedSubjects()[0].getUserName(),
        states.get(0).evaluatedSubjects()[0].getUserName());
    assertEquals(state1b.evaluatedSubjects()[1].getUserName(),
        states.get(1).evaluatedSubjects()[1].getUserName());
    
    assertEquals(state1a.evaluatedSubjects()[0].getSubjectGroupIndex(),
        states.get(0).evaluatedSubjects()[0].getSubjectGroupIndex());
    assertEquals(state1b.evaluatedSubjects()[1].getSubjectGroupIndex(),
        states.get(1).evaluatedSubjects()[1].getSubjectGroupIndex());

    assertEquals(state1a.evaluatedSubjects()[0].getTimeStamp(),
        states.get(0).evaluatedSubjects()[0].getTimeStamp());
    assertEquals(state1b.evaluatedSubjects()[1].getTimeStamp(),
        states.get(1).evaluatedSubjects()[1].getTimeStamp());
}
}
