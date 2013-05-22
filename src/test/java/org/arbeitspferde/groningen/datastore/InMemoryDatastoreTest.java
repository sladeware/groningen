package org.arbeitspferde.groningen.datastore;

import org.arbeitspferde.groningen.Datastore;

/**
 * Test for {@link InMemoryDatastore}.
 */
public class InMemoryDatastoreTest extends DatastoreTestBase {

  @Override
  protected Datastore createDatastore() {
    return new InMemoryDatastore();
  }

  @Override
  protected void destroyDatastore(Datastore dataStore) {
  }

}

