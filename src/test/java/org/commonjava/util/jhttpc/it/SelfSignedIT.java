package org.commonjava.util.jhttpc.it;

import org.commonjava.util.jhttpc.model.SimpleSiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteTrustType;

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

    @Override
    protected SimpleSiteConfig getSiteConfig()
            throws Exception
    {
        SimpleSiteConfig config = super.getSiteConfig();
        config.setTrustType( SiteTrustType.TRUST_SELF_SIGNED );

        return config;
    }

}
