package org.commonjava.util.jhttpc.INTERNAL.util;

import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.Map;

/**
 * Created by jdcasey on 11/6/15.
 */
public class MonolithicKeyStrategy
        implements PrivateKeyStrategy
{
    @Override
    public String chooseAlias( Map<String, PrivateKeyDetails> aliases, Socket socket )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info(
                "Returning hard-coded alias 'key' to coordinate with key/cert read from SiteConfig for socket: {}\n"
                        + "List of available aliases: {}", socket.getInetAddress(), aliases );
        return "key";
    }
}
