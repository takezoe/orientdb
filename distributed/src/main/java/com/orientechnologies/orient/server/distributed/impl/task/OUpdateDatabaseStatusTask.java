/*
 *
 *  *  Copyright 2010-2016 OrientDB LTD (http://orientdb.com)
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *  * For more information: http://orientdb.com
 *
 */
package com.orientechnologies.orient.server.distributed.impl.task;

import com.orientechnologies.orient.core.command.OCommandDistributedReplicateRequest;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.storage.impl.local.paginated.wal.OLogSequenceNumber;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.distributed.ODistributedRequestId;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager;
import com.orientechnologies.orient.server.distributed.ORemoteTaskFactory;
import com.orientechnologies.orient.server.distributed.task.OAbstractRemoteTask;
import com.orientechnologies.orient.server.hazelcast.OHazelcastDistributedMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Task to update the database status across all the servers.
 *
 * @author Luca Garulli (l.garulli--at---orientdb.com)
 */
public class OUpdateDatabaseStatusTask extends OAbstractRemoteTask {
  public static final int FACTORYID = 25;

  private String             databaseName;
  private String             status;
  private OLogSequenceNumber lsn;

  public OUpdateDatabaseStatusTask() {
  }

  public OUpdateDatabaseStatusTask(final String databaseName, final String status, final OLogSequenceNumber lsn) {
    this.databaseName = databaseName;
    this.status = status;
    this.lsn = lsn;
  }

  @Override
  public Object execute(final ODistributedRequestId msgId, final OServer iServer, ODistributedServerManager iManager,
      final ODatabaseDocumentInternal database) throws Exception {

    iManager.getMessageService().getDatabase(databaseName).getSyncConfiguration().setLastLSN(getNodeSource(), lsn, false);

    return true;
  }

  @Override
  public OCommandDistributedReplicateRequest.QUORUM_TYPE getQuorumType() {
    return OCommandDistributedReplicateRequest.QUORUM_TYPE.ALL;
  }

  @Override
  public boolean isUsingDatabase() {
    return false;
  }

  @Override
  public RESULT_STRATEGY getResultStrategy() {
    return RESULT_STRATEGY.UNION;
  }

  @Override
  public void toStream(final DataOutput out) throws IOException {
    out.writeUTF(databaseName);
    out.writeUTF(status);
    out.writeLong(lsn.getSegment());
    out.writeLong(lsn.getPosition());
  }

  @Override
  public void fromStream(final DataInput in, final ORemoteTaskFactory factory) throws IOException {
    databaseName = in.readUTF();
    status = in.readUTF();
    long seg = in.readLong();
    long pos = in.readLong();
    lsn = new OLogSequenceNumber(seg, pos);
  }

  @Override
  public boolean isNodeOnlineRequired() {
    return false;
  }

  @Override
  public long getDistributedTimeout() {
    return OGlobalConfiguration.DISTRIBUTED_HEARTBEAT_TIMEOUT.getValueAsLong();
  }

  @Override
  public String getName() {
    return "upd_db_status";
  }

  @Override
  public int getFactoryId() {
    return FACTORYID;
  }

  @Override
  public String toString() {
    return getName();
  }
}
