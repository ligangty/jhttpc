package org.commonjava.util.jhttpc.lifecycle;

public interface ShutdownEnabled
{
    boolean isShutdown();

    boolean shutdownNow();

    boolean shutdownGracefully( long timeoutMillis )
            throws InterruptedException;
}
