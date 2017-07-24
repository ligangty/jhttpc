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
package org.commonjava.util.jhttpc.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jdcasey on 10/28/15.
 */
public final class SiteConfig
{

    // TODO: too low?
    public static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 10;

    public static final int DEFAULT_PROXY_PORT = 8080;

    public static final int DEFAULT_MAX_CONNECTIONS = 4;

    public static final int DEFAULT_CONNECTION_POOL_TIMEOUT_SECONDS = 60;

    private final String id;

    private final String uri;

    private final String user;

    private final String proxyHost;

    private final Integer proxyPort;

    private final String proxyUser;

    private final SiteTrustType trustType;

    private final String keyCertPem;

    private final String serverCertPem;

    private Map<String, Object> attributes;

    private final Integer requestTimeoutSeconds;

    private final Integer maxConnections;

    private final Integer connectionPoolTimeoutSeconds;

    SiteConfig( String id, String uri, String user, String proxyHost, Integer proxyPort, String proxyUser,
                       SiteTrustType trustType, String keyCertPem, String serverCertPem, Integer requestTimeoutSeconds,
                       Integer connectionPoolTimeoutSeconds, Integer maxConnections, Map<String, Object> attributes )
    {
        this.id = id;
        this.uri = uri;
        this.user = user;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.trustType = trustType;
        this.keyCertPem = keyCertPem;
        this.serverCertPem = serverCertPem;
        this.requestTimeoutSeconds = requestTimeoutSeconds;
        this.connectionPoolTimeoutSeconds = connectionPoolTimeoutSeconds;
        this.maxConnections = maxConnections;
        this.attributes = attributes == null ? new HashMap<String, Object>() : attributes;
    }

    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    public String getId()
    {
        return id;
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    public int getProxyPort()
    {
        return proxyPort == null ? DEFAULT_PROXY_PORT : proxyPort;
    }

    public String getUser()
    {
        return user;
    }

    public String getHost()
            throws MalformedURLException
    {
        return new URL( getUri() ).getHost();
    }

    public int getPort()
            throws MalformedURLException
    {
        URL u = new URL( getUri() );
        int port = u.getPort();
        if ( port < 1 )
        {
            port = u.getDefaultPort();
        }

        return port;
    }

    public String getProxyUser()
    {
        return proxyUser;
    }

    public String getKeyCertPem()
    {
        return keyCertPem;
    }

    public String getUri()
    {
        return uri;
    }

    public String getServerCertPem()
    {
        return serverCertPem;
    }

    public int getConnectionPoolTimeoutSeconds()
    {
        return connectionPoolTimeoutSeconds == null ? DEFAULT_CONNECTION_POOL_TIMEOUT_SECONDS : connectionPoolTimeoutSeconds;
    }

    public int getRequestTimeoutSeconds()
    {
        return requestTimeoutSeconds == null ? DEFAULT_REQUEST_TIMEOUT_SECONDS : requestTimeoutSeconds;
    }

    public <T> T getAttribute( String key, Class<T> type )
    {
        Object value = getAttribute( key );
        return value == null ? null : type.cast( value );
    }

    public <T> T getAttribute( String key, Class<T> type, T defaultValue )
    {
        Object value = getAttribute( key );
        return value == null ? defaultValue : type.cast( value );
    }

    public synchronized Object setAttribute( String key, Object value )
    {
        if ( attributes == null )
        {
            attributes = new HashMap<String, Object>();
        }

        return attributes.put( key, value );
    }

    public Object getAttribute( String key )
    {
        return attributes == null ? null : attributes.get( key );
    }

    public int getMaxConnections()
    {
        return maxConnections == null ? DEFAULT_MAX_CONNECTIONS : maxConnections;
    }

    public SiteTrustType getTrustType()
    {
        return trustType == null ? SiteTrustType.DEFAULT : trustType;
    }

    public Object removeAttribute( String key )
    {
        if ( attributes != null )
        {
            return attributes.remove( key );
        }

        return null;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof SiteConfig ) )
        {
            return false;
        }

        SiteConfig that = (SiteConfig) o;

        return !( getId() != null ? !getId().equals( that.getId() ) : that.getId() != null );

    }

    @Override
    public int hashCode()
    {
        return 13 + (getId() != null ? getId().hashCode() : 0);
    }

    @Override
    public String toString()
    {
        return "SiteConfig{" +
                "id='" + id + '\'' +
                '}';
    }
}
