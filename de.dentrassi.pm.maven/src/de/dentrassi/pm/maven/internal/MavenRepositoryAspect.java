/*******************************************************************************
 * Copyright (c) 2015 Jens Reimann.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jens Reimann - initial API and implementation
 *******************************************************************************/
package de.dentrassi.pm.maven.internal;

import de.dentrassi.pm.aspect.ChannelAspect;
import de.dentrassi.pm.aspect.aggregate.ChannelAggregator;

public class MavenRepositoryAspect implements ChannelAspect
{
    @Override
    public String getId ()
    {
        return MavenRepositoryAspectFactory.ID;
    }

    @Override
    public ChannelAggregator getChannelAggregator ()
    {
        return new MavenRepositoryChannelAggregator ();
    }
}