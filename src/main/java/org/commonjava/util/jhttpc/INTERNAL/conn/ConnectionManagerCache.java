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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.commonjava.util.jhttpc.JHttpCException;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by jdcasey on 11/3/15.
 */
public class ConnectionManagerCache
{
    private final Cache<SiteConnectionConfig, ConnectionManagerTracker> cache;

    private final CacheLoader<SiteConnectionConfig, ConnectionManagerTracker> loader;

    public ConnectionManagerCache()
    {
        this.loader = new ConnectionManagerLoader();
        this.cache = CacheBuilder.newBuilder()
                                 .expireAfterAccess( 30, TimeUnit.SECONDS )
                                 .removalListener( new ClosingRemovalListener() )
                                 .build( loader );
    }

    public void expireTrackersOlderThan( long duration, TimeUnit unit )
    {
        long expiration = System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert( duration, unit );

        Iterator<ConnectionManagerTracker> iter = cache.asMap().values().iterator();
        while( iter.hasNext() )
        {
            ConnectionManagerTracker tracker = iter.next();
            if ( tracker.getLastRetrieval() < expiration )
            {
                iter.remove();
            }
        }
    }

    public ConnectionManagerTracker getTrackerFor( SiteConnectionConfig config )
            throws JHttpCException
    {
        try
        {
            ConnectionManagerTracker tracker = cache.get( config, newCallable( config ) );
            return tracker.retrieved();
        }
        catch ( ExecutionException e )
        {
            throw new JHttpCException( "Failed to retrieve connection manager for: %s. Reason: %s", e, config,
                                       e.getMessage() );
        }
    }

    private Callable<ConnectionManagerTracker> newCallable( SiteConnectionConfig config )
    {
        return new LoaderCall( config, loader );
    }

    static final class LoaderCall
            implements Callable<ConnectionManagerTracker>
    {
        private final SiteConnectionConfig config;

        private final CacheLoader<SiteConnectionConfig, ConnectionManagerTracker> loader;

        LoaderCall( SiteConnectionConfig config, CacheLoader<SiteConnectionConfig, ConnectionManagerTracker> loader )
        {
            this.config = config;
            this.loader = loader;
        }

        public ConnectionManagerTracker call()
                throws Exception
        {
            return loader.load( config );
        }
    }

    static final class ClosingRemovalListener
            implements RemovalListener<SiteConnectionConfig, ConnectionManagerTracker>
    {
        @Override
        public void onRemoval( RemovalNotification<SiteConnectionConfig, ConnectionManagerTracker> notification )
        {
            notification.getValue().detach();
        }
    }

    static final class ConnectionManagerLoader
            extends CacheLoader<SiteConnectionConfig, ConnectionManagerTracker>
    {
        @Override
        public ConnectionManagerTracker load( SiteConnectionConfig key )
                throws Exception
        {
            return new ConnectionManagerTracker(key);
        }
    }
}
