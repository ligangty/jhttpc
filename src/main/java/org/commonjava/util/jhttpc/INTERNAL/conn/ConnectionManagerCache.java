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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by jdcasey on 11/3/15.
 */
public class ConnectionManagerCache
{
    private static final long EXPIRATION_SECONDS = 30;
    private static final long EXPIRATION_MILLIS = TimeUnit.MILLISECONDS.convert( EXPIRATION_SECONDS, TimeUnit.SECONDS );

    private final Map<SiteConnectionConfig, ConnectionManagerTracker> cache = new HashMap<SiteConnectionConfig, ConnectionManagerTracker>();

    private final Timer timer = new Timer("jhttpc-connection-manager-cache", true);

    public ConnectionManagerCache()
    {
        timer.scheduleAtFixedRate( new ExpirationSweeper(this), EXPIRATION_MILLIS, EXPIRATION_MILLIS );
    }

    public synchronized void expireTrackersOlderThan( long duration, TimeUnit unit )
    {
        long expiration = System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert( duration, unit );

        for( SiteConnectionConfig config: new HashSet<SiteConnectionConfig>(cache.keySet()) )
        {
            ConnectionManagerTracker tracker = cache.get( config );
            if ( tracker != null && tracker.getLastRetrieval() < expiration )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.info( "Detaching connection tracker from cache: {}", tracker );
                cache.remove( config );
                tracker.detach();
            }
        }
    }

    public synchronized ConnectionManagerTracker getTrackerFor( SiteConnectionConfig config )
            throws JHttpCException
    {
        ConnectionManagerTracker tracker = cache.get( config );
        if ( tracker == null )
        {
            tracker = new ConnectionManagerTracker( config );
            cache.put( config, tracker );
        }

        return tracker.retrieved();
    }

    static final class ExpirationSweeper
            extends TimerTask
    {

        private ConnectionManagerCache cache;

        public ExpirationSweeper( ConnectionManagerCache cache )
        {
            this.cache = cache;
        }

        @Override
        public void run()
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.info( "Sweeping for old connection trackers." );
            cache.expireTrackersOlderThan( EXPIRATION_SECONDS, TimeUnit.SECONDS );
        }
    }
}
