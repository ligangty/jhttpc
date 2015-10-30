package org.commonjava.util.jhttpc.it;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.test.http.expect.ExpectationServer;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.util.SSLUtils;
import org.commonjava.util.jhttpc.util.UrlUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.KeyStore;
import java.util.Enumeration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 10/28/15.
 */
public abstract class AbstractIT
{

    private static final String NON_SSL_HOST = "docker.containers.%s.ports.80/tcp.host";

    private static final String NON_SSL_PORT = "docker.containers.%s.ports.80/tcp.port";

    private static final String SSL_HOST = "docker.containers.%s.ports.443/tcp.host";

    private static final String SSL_PORT = "docker.containers.%s.ports.443/tcp.port";

    private static final String NON_SSL_URL_FORMAT = "http://%s:%s";

    private static final String SSL_URL_FORMAT = "https://%s:%s";

    private static final String CONTENT_MGMT_PATH = "/cgi-bin/content.py/";

    protected static final String SSL_CONFIG_BASE ="/ssl-config";

    protected static final String SITE_CERT_PATH = SSL_CONFIG_BASE + "/site.crt";

    @Rule
    public TestName name = new TestName();

    protected HttpFactory factory;

    protected PasswordManager passwordManager;

    protected abstract String getContainerId();

    protected abstract String[] getCertificatePaths();

    @Before
    public void setup()
            throws Exception
    {
        passwordManager = new MemoryPasswordManager();
        factory = new HttpFactory( passwordManager );
    }

    @After
    public void teardown()
            throws Exception
    {
        factory.close();
    }

    @Test
    public void simpleSingleGet_NoSSL()
            throws Exception
    {
        String path = name.getMethodName() + "/path/to/test";
        String content = "This is a test.";

        putContent( path, content );

        try (CloseableHttpClient client = factory.createClient())
        {
            String result = client.execute( new HttpGet( formatUrl( path ) ), new ResponseHandler<String>()
            {
                @Override
                public String handleResponse( HttpResponse response )
                        throws ClientProtocolException, IOException
                {
                    assertThat( response.getStatusLine().getStatusCode(), equalTo( 200 ) );
                    return IOUtils.toString( response.getEntity().getContent() );
                }
            } );

            assertThat( result, equalTo( content ) );
        }
    }

    @Test
    public void retrieveSiteCertificatePems()
            throws Exception
    {
        String[] paths = getCertificatePaths();

        try (CloseableHttpClient client = factory.createClient())
        {
            for ( String path : paths )
            {
                String result = client.execute( new HttpGet( formatUrl( path ) ), new ResponseHandler<String>()
                {
                    @Override
                    public String handleResponse( HttpResponse response )
                            throws ClientProtocolException, IOException
                    {
                        assertThat( response.getStatusLine().getStatusCode(), equalTo( 200 ) );
                        return IOUtils.toString( response.getEntity().getContent() );
                    }
                } );

                System.out.println( result );
                assertThat( result, notNullValue() );
            }
        }
    }

    @Test
    public void decodeSiteCertificatePems()
            throws Exception
    {
        String[] paths = getCertificatePaths();
        StringBuilder pem = new StringBuilder();

        try (CloseableHttpClient client = factory.createClient())
        {
            for ( String path : paths )
            {
                String result = client.execute( new HttpGet( formatUrl( path ) ), new ResponseHandler<String>()
                {
                    @Override
                    public String handleResponse( HttpResponse response )
                            throws ClientProtocolException, IOException
                    {
                        assertThat( response.getStatusLine().getStatusCode(), equalTo( 200 ) );
                        return IOUtils.toString( response.getEntity().getContent() );
                    }
                } );

                System.out.println( result );
                assertThat( result, notNullValue() );
                pem.append( result ).append( "\n" );
            }

            KeyStore store = SSLUtils.readCerts( pem.toString(), "somehost" );
            Enumeration<String> aliases = store.aliases();
            while ( aliases.hasMoreElements() )
            {
                System.out.println( aliases.nextElement() );
            }
        }
    }

    protected synchronized String formatUrl( String... path )
            throws Exception
    {
        String host = System.getProperty( String.format( NON_SSL_HOST, getContainerId() ) );
        String port = System.getProperty( String.format( NON_SSL_PORT, getContainerId() ) );

        if ( StringUtils.isEmpty( host ) || StringUtils.isEmpty( port ) )
        {
            Assert.fail( "Non-SSL host/port properties are missing for container: " + getContainerId()
                                 + ". Did you forget to configure the docker-maven-plugin?" );
        }

        return UrlUtils.buildUrl( String.format( NON_SSL_URL_FORMAT, host, port ), path );
    }

    protected synchronized String formatSSLUrl( String... path )
            throws Exception
    {
        String host = System.getProperty( String.format( SSL_HOST, getContainerId() ) );
        String port = System.getProperty( String.format( SSL_PORT, getContainerId() ) );

        if ( StringUtils.isEmpty( host ) || StringUtils.isEmpty( port ) )
        {
            Assert.fail( "SSL host/port properties are missing for container: " + getContainerId()
                                 + ". Did you forget to configure the docker-maven-plugin?" );
        }

        return UrlUtils.buildUrl( String.format( SSL_URL_FORMAT, host, port ), path );
    }

    protected void deleteContent( String path )
            throws Exception
    {
        String url = formatUrl( CONTENT_MGMT_PATH, path );
        HttpDelete put = new HttpDelete( url );

        try (CloseableHttpClient client = factory.createClient())
        {
            CloseableHttpResponse response = client.execute( put );
            int code = response.getStatusLine().getStatusCode();
            if ( code != 404 && code != 204 )
            {
                String extra = "";
                if ( response.getEntity() != null )
                {
                    String body = IOUtils.toString( response.getEntity().getContent() );
                    extra = "\nBody:\n\n" + body;
                }

                Assert.fail( "Failed to delete content from: " + path + ".\nURL: " + url + "\nStatus: "
                                     + response.getStatusLine() + extra );
            }
        }
    }

    protected void putContent( String path, String content )
            throws Exception
    {
        String url = formatUrl( CONTENT_MGMT_PATH, path );
        HttpPut put = new HttpPut( url );
        put.setEntity( new StringEntity( content ) );

        try (CloseableHttpClient client = factory.createClient())
        {
            CloseableHttpResponse response = client.execute( put );
            int code = response.getStatusLine().getStatusCode();
            if ( code != 200 && code != 201 )
            {
                String extra = "";
                if ( response.getEntity() != null )
                {
                    String body = IOUtils.toString( response.getEntity().getContent() );
                    extra = "\nBody:\n\n" + body;
                }

                Assert.fail(
                        "Failed to put content to: " + path + ".\nURL: " + url + "\nStatus: " + response.getStatusLine()
                                + extra );
            }
        }
    }

}
