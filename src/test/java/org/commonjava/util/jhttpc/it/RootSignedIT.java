package org.commonjava.util.jhttpc.it;

import org.commonjava.util.jhttpc.model.SimpleSiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfig;

/**
 * Created by jdcasey on 10/30/15.
 */
public class RootSignedIT
        extends AbstractIT
{
    @Override
    protected String getContainerId()
    {
        return "rootsigned";
    }

    @Override
    protected String[] getCertificatePaths()
    {
        return new String[]{SSL_CONFIG_BASE + "/root.crt", SITE_CERT_PATH};
    }

}
