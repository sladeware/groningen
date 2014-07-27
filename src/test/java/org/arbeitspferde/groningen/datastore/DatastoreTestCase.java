package org.arbeitspferde.groningen.datastore;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

import junit.framework.TestCase;

import org.arbeitspferde.groningen.Datastore;
import org.arbeitspferde.groningen.Datastore.DatastoreException;
import org.arbeitspferde.groningen.PipelineId;
import org.arbeitspferde.groningen.PipelineState;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Implementation-independent test for {@link Datastore} implementations. In order to test
 * particular Datastore, this class must be subclassed and createDatastore/destroyDatastore
 * methods must be implemented.
 */
public abstract class DatastoreTestCase extends TestCase {
  private Datastore dataStore;

  private PipelineState createValidPipelineState(PipelineId pipelineId, String inputLogName) 
      throws InvalidConfigurationException {
    GroningenConfig config = new ProtoBufConfig(
        ProgramConfiguration.newBuilder()
            .setParamBlock(GroningenParams.newBuilder().setInputLogName(inputLogName).build())
            .setUser("bigtester")
            .addCluster(ClusterConfig.newBuilder()
                .setCluster("xx")
                .addSubjectGroup(SubjectGroupConfig.newBuilder()
                    .setSubjectGroupName("bigTestingGroup")
                    .setExpSettingsFilesDir("/some/path")
                    .build())
                .build())
            .build());
    ExperimentDb experimentDb = new ExperimentDb();

    final JvmFlagSet.Builder jvmFlagSetBuilder = JvmFlagSet.builder();
    jvmFlagSetBuilder.withValue(JvmFlag.USE_SERIAL_GC, 1)
      .withValue(JvmFlag.HEAP_SIZE, 100L)
      .withValue(JvmFlag.MAX_NEW_SIZE, 50L);
    final JvmFlagSet jvmFlagSet = jvmFlagSetBuilder.build();
    
    SubjectStateBridge s1 = experimentDb.makeSubject();
    s1.storeCommandLine(jvmFlagSet);
    SubjectStateBridge s2 = experimentDb.makeSubject();
    s2.storeCommandLine(jvmFlagSet);
    experimentDb.makeExperiment(ImmutableList.of(s1.getIdOfObject(), s2.getIdOfObject()));
    return new PipelineState(pipelineId, config, experimentDb);    
  }
  
  private PipelineState createValidPipelineState(PipelineId pipelineId) 
      throws InvalidConfigurationException {
    return createValidPipelineState(pipelineId, "stdin");
  }
  
  private PipelineState createValidPipelineState() 
      throws InvalidConfigurationException {
    return createValidPipelineState(new PipelineId("localId"));
  }
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    dataStore = createDatastore();
  }
  
  @Override
  protected void tearDown() throws Exception {
    if (dataStore != null) {
      destroyDatastore(dataStore);
    }
    super.tearDown();
  }
  
  protected abstract Datastore createDatastore();
  protected abstract void destroyDatastore(Datastore dataStore);
  
  public void testListPipelineIdsWorksCorrectly() throws InvalidConfigurationException,
      DatastoreException {
    dataStore.createPipeline(createValidPipelineState(new PipelineId("ah")), false);
    dataStore.createPipeline(createValidPipelineState(new PipelineId("oh")), false);

    List<PipelineId> pipelineIds = dataStore.listPipelinesIds();
    Collections.sort(pipelineIds,
        new Comparator<PipelineId>() {
      @Override
      public int compare(PipelineId o1, PipelineId o2) {
        return o1.toString().compareTo(o2.toString());
      }      
    });

    assertEquals(new PipelineId("ah"), pipelineIds.get(0));
    assertEquals(new PipelineId("oh"), pipelineIds.get(1));
  }
  
  public void testCreatePipelineWorksCorrectly() throws InvalidConfigurationException,
      DatastoreException {
    PipelineState state = createValidPipelineState();
    
    dataStore.createPipeline(state, false);

    List<PipelineId> ids = dataStore.listPipelinesIds();
    assertEquals(1, ids.size());
    assertEquals(state.pipelineId(), ids.get(0));
  }
  
  public void testWritePipelinesWorksCorrectly() throws InvalidConfigurationException,
      DatastoreException {
    PipelineId id = new PipelineId("some");
    
    dataStore.createPipeline(createValidPipelineState(id, "stdin"), false);    
    List<PipelineState> resultStates1 = dataStore.getPipelines(Lists.newArrayList(id));
    assertEquals(1, resultStates1.size());
    PipelineState resultState1 = resultStates1.get(0);
    
    dataStore.writePipelines(Lists.newArrayList(createValidPipelineState(id, "stdin2")));
    List<PipelineState> resultStates2 = dataStore.getPipelines(Lists.newArrayList(id));
    assertEquals(1, resultStates2.size());
    PipelineState resultState2 = resultStates2.get(0);
    
    dataStore.writePipelines(Lists.newArrayList(createValidPipelineState(id, "stdin")));
    List<PipelineState> resultStates3 = dataStore.getPipelines(Lists.newArrayList(id));
    assertEquals(1, resultStates3.size());
    PipelineState resultState3 = resultStates3.get(0);
    
    assertEquals(resultState1.pipelineId(), resultState2.pipelineId());
    assertEquals(resultState2.pipelineId(), resultState3.pipelineId());
    assertFalse(
        resultState1.config().getProtoConfig().equals(resultState2.config().getProtoConfig()));
    assertTrue(
        resultState1.config().getProtoConfig().equals(resultState3.config().getProtoConfig()));
  }
  
  public void testReadPipelinesWorksCorrectly() throws InvalidConfigurationException,
      DatastoreException {
    PipelineState state = createValidPipelineState();
    
    dataStore.createPipeline(state, false);
    List<PipelineState> resultStates =
        dataStore.getPipelines(Lists.newArrayList(state.pipelineId()));
    assertEquals(1, resultStates.size());
    
    PipelineState resultState = resultStates.get(0);
    
    assertEquals(state.pipelineId(), resultState.pipelineId());
    
    long[] subjectIds =
        Longs.toArray(state.experimentDb().getLastExperiment().getSubjectIds());
    Arrays.sort(subjectIds);
    
    long[] resultSubjectIds =
        Longs.toArray(resultState.experimentDb().getLastExperiment().getSubjectIds());
    Arrays.sort(resultSubjectIds);
    assertEquals(subjectIds.length, resultSubjectIds.length);
    for (int i = 0; i < subjectIds.length; ++i) {
      assertEquals(subjectIds[i], resultSubjectIds[i]);
    }
  }
  
  public void testDeletePipelinesWorksCorrectly() throws InvalidConfigurationException,
      DatastoreException {
    PipelineState state = createValidPipelineState();    
    dataStore.createPipeline(state, false);

    List<PipelineId> ids = dataStore.listPipelinesIds();
    assertEquals(1, ids.size());
    assertEquals(state.pipelineId(), ids.get(0));

    dataStore.deletePipelines(Lists.newArrayList(state.pipelineId()));
    
    ids = dataStore.listPipelinesIds();
    assertTrue(ids.isEmpty());
  }
}

