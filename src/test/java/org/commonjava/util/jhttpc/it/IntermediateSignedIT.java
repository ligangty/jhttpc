package org.commonjava.util.jhttpc.it;

/**
 * Created by jdcasey on 10/30/15.
 */
public class IntermediateSignedIT
        extends AbstractIT
{
    @Override
    protected String getContainerId()
    {
        return "intersigned";
    }

    @Override
    protected String[] getCertificatePaths()
    {
        return new String[]{SSL_CONFIG_BASE + "/root.crt", SSL_CONFIG_BASE + "/web.crt", SITE_CERT_PATH};
    }

}
