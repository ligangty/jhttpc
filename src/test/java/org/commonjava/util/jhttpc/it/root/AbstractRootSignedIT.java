/**
 * Copyright (C) 2015 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.util.jhttpc.it.root;

import org.commonjava.util.jhttpc.it.AbstractIT;

/**
 * Created by jdcasey on 10/30/15.
 */
public abstract class AbstractRootSignedIT
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
        return new String[]{SITE_CERT_PATH};
//        return new String[]{SSL_CONFIG_BASE + "/root.crt", SITE_CERT_PATH};
    }

}
