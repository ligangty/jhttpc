package org.commonjava.util.jhttpc.it;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.test.http.expect.ExpectationServer;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 10/28/15.
 */
public class AbstractIT
{

    private HttpFactory factory;

    private PasswordManager passwordManager;

    @Before
    public void setup()
            throws Exception
    {
        passwordManager = new MemoryPasswordManager();
        factory = new HttpFactory( passwordManager );
    }

    @Test
    public void simpleSingleGet_NoSiteConfig()
            throws Exception
    {
        String path = "/path/to/test";
        String content = "This is a test.";

        HttpFactory factory = new HttpFactory( new MemoryPasswordManager() );
        try(CloseableHttpClient client = factory.createClient())
        {
            String result = client.execute( new HttpGet( server.formatUrl( path ) ), new ResponseHandler<String>()
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

}
