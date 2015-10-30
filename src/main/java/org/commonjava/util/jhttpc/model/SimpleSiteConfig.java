package org.commonjava.util.jhttpc.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jdcasey on 10/28/15.
 */
public class SimpleSiteConfig
    implements SiteConfig
{

    private String id;

    private String uri;

    private String user;

    private String proxyHost;

    private Integer proxyPort;

    private String proxyUser;

    private SiteTrustType trustType;

    private String keyCertPem;

    private String serverCertPem;

    private Map<String, String> attributes;

    private Integer requestTimeoutSeconds;

    public SimpleSiteConfig(){}

    public SimpleSiteConfig( String id, String uri )
    {
        this.id = id;
        this.uri = uri;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public String getProxyHost()
    {
        return proxyHost;
    }

    @Override
    public int getProxyPort()
    {
        return proxyPort == null ? DEFAULT_PROXY_PORT : proxyPort;
    }

    @Override
    public String getUser()
    {
        return user;
    }

    @Override
    public String getHost()
            throws MalformedURLException
    {
        return new URL( getUri() ).getHost();
    }

    @Override
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

    @Override
    public String getProxyUser()
    {
        return proxyUser;
    }

    @Override
    public String getKeyCertPem()
    {
        return keyCertPem;
    }

    @Override
    public String getUri()
    {
        return uri;
    }

    @Override
    public String getServerCertPem()
    {
        return serverCertPem;
    }

    @Override
    public int getRequestTimeoutSeconds()
    {
        return requestTimeoutSeconds == null ? DEFAULT_REQUEST_TIMEOUT_SECONDS : requestTimeoutSeconds;
    }

    @Override
    public synchronized String setAttribute( String key, String value )
    {
        if ( attributes == null )
        {
            attributes = new HashMap<>();
        }

        return attributes.put( key, value );
    }

    @Override
    public String getAttribute( String key )
    {
        return attributes == null ? null : attributes.get( key );
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public void setUri( String uri )
    {
        this.uri = uri;
    }

    public void setUser( String user )
    {
        this.user = user;
    }

    public void setProxyHost( String proxyHost )
    {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort( Integer proxyPort )
    {
        this.proxyPort = proxyPort;
    }

    public void setProxyUser( String proxyUser )
    {
        this.proxyUser = proxyUser;
    }

    public void setTrustType( SiteTrustType trustType )
    {
        this.trustType = trustType;
    }

    public void setKeyCertPem( String keyCertPem )
    {
        this.keyCertPem = keyCertPem;
    }

    public void setServerCertPem( String serverCertPem )
    {
        this.serverCertPem = serverCertPem;
    }

    public void setAttributes( Map<String, String> attributes )
    {
        this.attributes = attributes;
    }

    public void setRequestTimeoutSeconds( Integer requestTimeoutSeconds )
    {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    @Override
    public SiteTrustType getTrustType()
    {
        return trustType == null ? SiteTrustType.DEFAULT : trustType;
    }

    @Override
    public void removeAttribute( String key )
    {
        if ( attributes != null )
        {
            attributes.remove( key );
        }
    }
}
