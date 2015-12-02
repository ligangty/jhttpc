/**
 * Copyright (C) 2015 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.util.jhttpc.INTERNAL.conn;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jdcasey on 11/3/15.
 */
public class ConnectionManagerTracker
{
    private SiteConnectionConfig config;
    private CloseBlockingConnectionManager manager;
    private int users = 0;
    private boolean detached;
    private long lastRetrieval;

    public ConnectionManagerTracker( SiteConnectionConfig config )
    {
        this.config = config;
    }

    public synchronized CloseBlockingConnectionManager acquire()
    {
        if ( manager == null )
        {
            PoolingHttpClientConnectionManager poolingMgr = new PoolingHttpClientConnectionManager(config.getSocketFactoryRegistry());
            poolingMgr.setMaxTotal( config.getMaxConnections() );

            manager = new CloseBlockingConnectionManager( config, poolingMgr );
        }

        users++;
        return manager;
    }

    public synchronized void release()
    {
        users--;
        tryShutdown();
    }

    private void tryShutdown()
    {
        if ( detached && users < 1 )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.debug( "Shutdown connection manager: {}", this );
            manager.reallyShutdown();
        }
    }

    public long getLastRetrieval()
    {
        return lastRetrieval;
    }

    public ConnectionManagerTracker retrieved()
    {
        lastRetrieval = System.currentTimeMillis();
        return this;
    }

    public void detach()
    {
        this.detached = true;
        tryShutdown();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        ConnectionManagerTracker that = (ConnectionManagerTracker) o;

        return config.equals( that.config );

    }

    @Override
    public int hashCode()
    {
        return config.hashCode();
    }

    @Override
    public String toString()
    {
        return "ConnectionManagerTracker{" +
                "config=" + config +
                ", manager=" + manager + ", instance=" + super.hashCode() + '}';
    }
}
