/*******************************************************************************
 * Copyright (c) 2014 Jens Reimann.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jens Reimann - initial API and implementation
 *******************************************************************************/
package de.dentrassi.pm.p2;

import de.dentrassi.pm.aspect.common.osgi.BundleInformation;

public class InstallableUnit
{
    private String id;

    public void setId ( final String id )
    {
        this.id = id;
    }

    public String getId ()
    {
        return this.id;
    }

    public static InstallableUnit fromBundle ( final BundleInformation bundle )
    {
        final InstallableUnit result = new InstallableUnit ();

        result.setId ( bundle.getId () );

        return result;
    }
}
