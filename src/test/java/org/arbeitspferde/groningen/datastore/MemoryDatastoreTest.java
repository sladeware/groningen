package org.arbeitspferde.groningen.datastore;

import org.arbeitspferde.groningen.Datastore;

/**
 * Test for {@link MemoryDatastore}.
 */
public class MemoryDatastoreTest extends DatastoreTestBase {

  @Override
  protected Datastore createDatastore() {
    return new MemoryDatastore();
  }

  @Override
  protected void destroyDatastore(Datastore dataStore) {
  }

}
