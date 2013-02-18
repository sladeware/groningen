package org.arbeitspferde.groningen;

import junit.framework.TestCase;

import org.arbeitspferde.groningen.Datastore.DatastoreException;
import org.arbeitspferde.groningen.config.ConfigManager;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;
import org.easymock.EasyMock;

/**
 * Test for {@link PipelineRestorer}.
 */
public class PipelineRestorerTest extends TestCase {
  private static final int SHARD_INDEX = 1;
  
  private PipelineManager pipelineManagerMock;
  private Datastore dataStoreMock;
  private PipelineIdGenerator pipelineIdGeneratorMock;
  private PipelineRestorer pipelineRestorer;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    dataStoreMock = EasyMock.createMock(Datastore.class);
    pipelineManagerMock = EasyMock.createMock(PipelineManager.class);
    pipelineIdGeneratorMock = EasyMock.createMock(PipelineIdGenerator.class);
    
    pipelineRestorer = new PipelineRestorer(SHARD_INDEX, dataStoreMock, pipelineManagerMock,
        pipelineIdGeneratorMock);
  }
  
  public void testCorrectlyFiltersOutOtherShards() throws DatastoreException {
    EasyMock.expect(dataStoreMock.listPipelinesIds()).andReturn(new PipelineId[] {
       new PipelineId("local0"),
       new PipelineId("local1"),
       new PipelineId("local2")
    });
    
    EasyMock.expect(pipelineIdGeneratorMock.shardIndexForPipelineId(
        EasyMock.eq(new PipelineId("local0")))).andReturn(0);
    EasyMock.expect(pipelineIdGeneratorMock.shardIndexForPipelineId(
        EasyMock.eq(new PipelineId("local1")))).andReturn(1);
    EasyMock.expect(pipelineIdGeneratorMock.shardIndexForPipelineId(
        EasyMock.eq(new PipelineId("local2")))).andReturn(2);

    PipelineState pipelineState = new PipelineState(new PipelineId("local1"),
        EasyMock.createNiceMock(GroningenConfig.class),
        EasyMock.createNiceMock(ExperimentDb.class));
    EasyMock.expect(dataStoreMock.getPipelines(
          EasyMock.aryEq(new PipelineId[] { new PipelineId("local1") })))
        .andReturn(new PipelineState[] { pipelineState });
    
    EasyMock.expect(pipelineManagerMock.restorePipeline(EasyMock.eq(pipelineState),
        EasyMock.anyObject(ConfigManager.class), EasyMock.anyBoolean())).andReturn(
            new PipelineId("local1"));
    
    EasyMock.replay(pipelineManagerMock, dataStoreMock, pipelineIdGeneratorMock);   
    pipelineRestorer.restorePipelines();
    EasyMock.verify(pipelineManagerMock, dataStoreMock, pipelineIdGeneratorMock);
  }
}
