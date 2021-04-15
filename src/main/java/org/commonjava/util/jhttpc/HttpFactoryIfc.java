package org.commonjava.util.jhttpc;

import org.apache.http.Header;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.util.jhttpc.lifecycle.ShutdownEnabled;
import org.commonjava.util.jhttpc.model.SiteConfig;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * Interface extracted from {@link HttpFactory} in order to allow wrappers, such as those that add tracing.
 */
public interface HttpFactoryIfc
                extends Closeable, ShutdownEnabled
{
    CloseableHttpClient createClient() throws JHttpCException;

    CloseableHttpClient createClient( SiteConfig location ) throws JHttpCException;

    CloseableHttpClient createClient( SiteConfig location, List<Header> defaultHeaders ) throws JHttpCException;

    HttpClientContext createContext() throws JHttpCException;

    HttpClientContext createContext( SiteConfig location ) throws JHttpCException;

    void close() throws IOException;

    @Override
    boolean isShutdown();

    @Override
    boolean shutdownNow();

    @Override
    boolean shutdownGracefully( long timeoutMillis ) throws InterruptedException;
}
