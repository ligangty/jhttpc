/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.util.jhttpc;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.commonjava.util.jhttpc.auth.PasswordKey;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordType;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteTrustType;
import org.commonjava.util.jhttpc.util.CertEnumerator;
import org.commonjava.util.jhttpc.util.CloseBlockingConnectionManager;
import org.commonjava.util.jhttpc.util.SSLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

public class HttpFactory
    implements Closeable
{
    public static final int DEFAULT_MAX_CONNECTIONS = 200;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final PasswordManager passwords;

    private int maxConnections;

    private final CloseBlockingConnectionManager connectionManager;

    public HttpFactory( final PasswordManager passwords )
    {
        this( passwords, DEFAULT_MAX_CONNECTIONS );
    }

    public HttpFactory( final PasswordManager passwords, final int maxConnections )
    {
        this.passwords = passwords;
        this.maxConnections = maxConnections;
        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal( maxConnections );
        connectionManager = new CloseBlockingConnectionManager( cm );
    }

    public HttpFactory( final PasswordManager passwordManager, final HttpClientConnectionManager connectionManager )
    {
        passwords = passwordManager;
        this.connectionManager = new CloseBlockingConnectionManager( connectionManager );
    }

    public int getMaxConnections()
    {
        return maxConnections;
    }

    public PasswordManager getPasswordManager()
    {
        return passwords;
    }

    public CloseableHttpClient createClient()
        throws IOException
    {
        return createClient( null );
    }

    public CloseableHttpClient createClient( final SiteConfig location )
        throws IOException
    {
        final HttpClientBuilder builder = HttpClients.custom()
                                                     .setConnectionManager( connectionManager );

        if ( location != null )
        {
            final LayeredConnectionSocketFactory sslFac = createSSLSocketFactory( location );
            if ( sslFac != null )
            {
//                HostnameVerifier verifier = new SSLHostnameVerifierImpl( );
//                builder.setSSLHostnameVerifier( verifier );
                builder.setSSLSocketFactory( sslFac );
            }

            if ( location.getProxyHost() != null )
            {
                final HttpRoutePlanner planner =
                    new DefaultProxyRoutePlanner( new HttpHost( location.getProxyHost(), getProxyPort( location ) ) );
                builder.setRoutePlanner( planner );
            }

            final int timeout = 1000 * location.getRequestTimeoutSeconds();
            builder.setDefaultRequestConfig( RequestConfig.custom()
                                                          .setConnectionRequestTimeout( timeout )
                                                          .setSocketTimeout( timeout )
                                                          .setConnectTimeout( timeout )
                                                          .build() );
        }


        return builder.build();
    }

    private int getProxyPort( final SiteConfig location )
    {
        int port = location.getProxyPort();
        if ( port < 1 )
        {
            port = -1;
        }

        return port;
    }

    public HttpClientContext createContext()
            throws MalformedURLException
    {
        return createContext( null );
    }

    public HttpClientContext createContext( final SiteConfig location )
            throws MalformedURLException
    {
        final HttpClientContext ctx = HttpClientContext.create();

        if ( location != null )
        {
            final CredentialsProvider creds = new BasicCredentialsProvider();
            final AuthScope as = new AuthScope( location.getHost(), location.getPort() );

            if ( location.getUser() != null )
            {
                final String password =
                    passwords.lookup( new PasswordKey( location, PasswordType.USER ) );
                creds.setCredentials( as, new UsernamePasswordCredentials( location.getUser(), password ) );
            }

            if ( location.getProxyHost() != null && location.getProxyUser() != null )
            {
                final String password =
                    passwords.lookup( new PasswordKey( location, PasswordType.PROXY ) );
                creds.setCredentials( new AuthScope( location.getProxyHost(), getProxyPort( location ) ),
                                      new UsernamePasswordCredentials( location.getProxyUser(), password ) );
            }

            ctx.setCredentialsProvider( creds );
        }

        return ctx;
    }

    private SSLConnectionSocketFactory createSSLSocketFactory( final SiteConfig location )
        throws IOException
    {
        KeyStore ks = null;
        KeyStore ts = null;

        final String kcPem = location.getKeyCertPem();
        final String kcPass = passwords.lookup( new PasswordKey( location, PasswordType.KEY ) );
        if ( kcPem != null )
        {
            if ( kcPass == null || kcPass.length() < 1 )
            {
                logger.error( "Invalid configuration. Location: {} cannot have an empty key password!",
                              location.getUri() );
                throw new IOException( "Location: " + location.getUri() + " is misconfigured!" );
            }

            try
            {
                logger.debug( "Reading Client SSL key from:\n\n{}\n\n", kcPem );
                ks = SSLUtils.readKeyAndCert( kcPem, kcPass );

                logger.debug( "Keystore contains the following certificates: {}", new CertEnumerator( ks ) );
            }
            catch ( final CertificateException e )
            {
                logger.error( String.format( "Invalid configuration. Location: %s has an invalid client certificate! Error: %s",
                                             location.getUri(), e.getMessage() ), e );
                throw new IOException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( final KeyStoreException e )
            {
                logger.error( String.format( "Invalid configuration. Cannot initialize keystore for repository: %s. Error: %s",
                                             location.getUri(), e.getMessage() ), e );
                throw new IOException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( final NoSuchAlgorithmException e )
            {
                logger.error( String.format( "Invalid configuration. Cannot initialize keystore for repository: %s. Error: %s",
                                             location.getUri(), e.getMessage() ), e );
                throw new IOException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( final InvalidKeySpecException e )
            {
                logger.error( String.format( "Invalid configuration. Invalid client key for repository: %s. Error: %s",
                                             location.getUri(), e.getMessage() ), e );
                throw new IOException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
        }

        final String sPem = location.getServerCertPem();
        //        logger.debug( "Server certificate PEM:\n{}", sPem );
        if ( sPem != null )
        {
            try
            {
                logger.debug( "Reading Server SSL cert from:\n\n{}\n\n", sPem );
                ts = SSLUtils.readCerts( sPem, location.getHost() );

                //                logger.debug( "Trust store contains the following certificates:\n{}", new CertEnumerator( ts ) );
            }
            catch ( final CertificateException e )
            {
                logger.error( String.format( "Invalid configuration. Location: %s has an invalid server certificate! Error: %s",
                                             location.getUri(), e.getMessage() ), e );
                throw new IOException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( final KeyStoreException e )
            {
                logger.error( String.format( "Invalid configuration. Cannot initialize keystore for repository: %s. Error: %s",
                                             location.getUri(), e.getMessage() ), e );
                throw new IOException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( final NoSuchAlgorithmException e )
            {
                logger.error( String.format( "Invalid configuration. Cannot initialize keystore for repository: %s. Error: %s",
                                             location.getUri(), e.getMessage() ), e );
                throw new IOException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
        }

        if ( ks != null || ts != null )
        {
            try
            {
                SSLContextBuilder sslBuilder = SSLContexts.custom()
                                              .useProtocol( SSLConnectionSocketFactory.TLS );
                if ( ks != null )
                {
                    logger.debug( "Loading key material for SSL context..." );
                    sslBuilder.loadKeyMaterial( ks, kcPass.toCharArray() );
                }

                if ( ts != null )
                {
                    logger.debug( "Loading trust material for SSL context..." );

                    SiteTrustType trustType = location.getTrustType();
                    if ( trustType == null )
                    {
                        trustType = SiteTrustType.DEFAULT;
                    }

                    sslBuilder.loadTrustMaterial( ts, trustType.getTrustStrategy() );
                }

                SSLContext ctx = sslBuilder.build();

                return new SSLConnectionSocketFactory( ctx, new DefaultHostnameVerifier() );
            }
            catch ( final KeyManagementException e )
            {
                logger.error( "Invalid configuration. Cannot initialize SSL socket factory for repository: {}. Error: {}",
                              e, location.getUri(), e.getMessage() );
                throw new IOException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( final UnrecoverableKeyException e )
            {
                logger.error( "Invalid configuration. Cannot initialize SSL socket factory for repository: {}. Error: {}",
                              e, location.getUri(), e.getMessage() );
                throw new IOException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( final NoSuchAlgorithmException e )
            {
                logger.error( "Invalid configuration. Cannot initialize SSL socket factory for repository: {}. Error: {}",
                              e, location.getUri(), e.getMessage() );
                throw new IOException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( final KeyStoreException e )
            {
                logger.error( "Invalid configuration. Cannot initialize SSL socket factory for repository: {}. Error: {}",
                              e, location.getUri(), e.getMessage() );
                throw new IOException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
        }

        return null;
    }

    public void close()
        throws IOException
    {
        connectionManager.reallyShutdown();
    }
}
