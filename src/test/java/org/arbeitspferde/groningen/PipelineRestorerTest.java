package org.arbeitspferde.groningen;

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.google.common.collect.Lists;

import org.arbeitspferde.groningen.Datastore.DatastoreException;
import org.arbeitspferde.groningen.config.ConfigManager;
import org.arbeitspferde.groningen.config.GroningenConfig;
import org.arbeitspferde.groningen.experimentdb.ExperimentDb;

import junit.framework.TestCase;

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

    dataStoreMock = createMock(Datastore.class);
    pipelineManagerMock = createMock(PipelineManager.class);
    pipelineIdGeneratorMock = createMock(PipelineIdGenerator.class);

    pipelineRestorer = new PipelineRestorer(SHARD_INDEX, dataStoreMock, pipelineManagerMock,
        pipelineIdGeneratorMock);
  }

  public void testCorrectlyFiltersOutOtherShards() throws DatastoreException {
    expect(dataStoreMock.listPipelinesIds()).andReturn(Lists.newArrayList(
        new PipelineId("local0"), new PipelineId("local1"), new PipelineId("local2")));

    expect(pipelineIdGeneratorMock.shardIndexForPipelineId(
        eq(new PipelineId("local0")))).andReturn(0);
    expect(pipelineIdGeneratorMock.shardIndexForPipelineId(
        eq(new PipelineId("local1")))).andReturn(1);
    expect(pipelineIdGeneratorMock.shardIndexForPipelineId(
        eq(new PipelineId("local2")))).andReturn(2);

    PipelineState pipelineState = new PipelineState(new PipelineId("local1"),
        createNiceMock(GroningenConfig.class), createNiceMock(ExperimentDb.class));
    expect(dataStoreMock.getPipelines(eq(Lists.newArrayList(new PipelineId("local1")))))
        .andReturn(Lists.newArrayList(pipelineState));

    expect(pipelineManagerMock.restorePipeline(eq(pipelineState), anyObject(ConfigManager.class),
        anyBoolean())).andReturn(new PipelineId("local1"));

    replay(pipelineManagerMock, dataStoreMock, pipelineIdGeneratorMock);
    pipelineRestorer.restorePipelines();
    verify(pipelineManagerMock, dataStoreMock, pipelineIdGeneratorMock);
  }
}

