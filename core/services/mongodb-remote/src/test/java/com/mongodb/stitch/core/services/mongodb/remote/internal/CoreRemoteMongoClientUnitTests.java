/*
 * Copyright 2018-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.stitch.core.services.mongodb.remote.internal;

import static com.mongodb.stitch.core.services.mongodb.remote.internal.TestUtils.getClient;
import static org.junit.Assert.assertEquals;

import com.mongodb.stitch.core.services.mongodb.remote.sync.internal.CoreRemoteClientFactory;
import com.mongodb.stitch.server.services.mongodb.local.internal.ServerEmbeddedMongoClientFactory;

import org.junit.After;
import org.junit.Test;

public class CoreRemoteMongoClientUnitTests {

  @After
  public void teardown() {
    CoreRemoteClientFactory.close();
    ServerEmbeddedMongoClientFactory.getInstance().close();
  }

  @Test
  public void testGetDatabase() {
    final CoreRemoteMongoClient client = getClient();
    final CoreRemoteMongoDatabase db1 = client.getDatabase("dbName1");
    assertEquals("dbName1", db1.getName());

    final CoreRemoteMongoDatabase db2 = client.getDatabase("dbName2");
    assertEquals("dbName2", db2.getName());
  }
}
