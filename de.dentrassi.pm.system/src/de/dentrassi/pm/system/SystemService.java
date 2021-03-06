/*******************************************************************************
 * Copyright (c) 2015 IBH SYSTEMS GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBH SYSTEMS GmbH - initial API and implementation
 *******************************************************************************/
package de.dentrassi.pm.system;

/**
 * Service for system information
 */
public interface SystemService
{
    /**
     * Get the default site prefix (e.g. <code>http://localhost:8080</code> )
     *
     * @return the default site prefix, may return <code>null</code> when the
     *         hostname cannot be determined
     */
    public String getDefaultSitePrefix ();
}
