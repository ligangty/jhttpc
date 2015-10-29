package org.commonjava.util.jhttpc.model;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Created by jdcasey on 10/28/15.
 */
public interface SiteConfig
{
    // TODO: too low?
    int DEFAULT_REQUEST_TIMEOUT_SECONDS = 10;

    int DEFAULT_PROXY_PORT = 8080;

    String getId();

    String getUri();

    String getHost()
            throws MalformedURLException;

    int getPort()
            throws MalformedURLException;

    String getUser();

    String getProxyHost();

    int getProxyPort();

    String getProxyUser();

    String getKeyCertPem()
            throws IOException;

    String getServerCertPem()
            throws IOException;

    SiteTrustType getTrustType();

    int getRequestTimeoutSeconds();

    String setAttribute( String key, String value );

    String getAttribute( String key );

    void removeAttribute( String key );
}
