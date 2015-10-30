package org.commonjava.util.jhttpc.it;

/**
 * Created by jdcasey on 10/30/15.
 */
public class SelfSignedIT
        extends AbstractIT
{
    @Override
    protected String getContainerId()
    {
        return "selfsigned";
    }

    @Override
    protected String[] getCertificatePaths()
    {
        return new String[]{SITE_CERT_PATH};
    }

}
