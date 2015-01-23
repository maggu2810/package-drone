/*******************************************************************************
 * Copyright (c) 2014, 2015 Jens Reimann.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jens Reimann - initial API and implementation
 *******************************************************************************/
package de.dentrassi.pm.storage.web.internal;

import java.util.LinkedList;
import java.util.List;

import de.dentrassi.osgi.web.LinkTarget;
import de.dentrassi.pm.storage.web.InterfaceExtender;
import de.dentrassi.pm.storage.web.menu.MenuEntry;

public class AboutMenuExtender implements InterfaceExtender
{
    private final List<MenuEntry> entries = new LinkedList<> ();

    public AboutMenuExtender ()
    {
        this.entries.add ( new MenuEntry ( null, 0, "About", Integer.MAX_VALUE, new LinkTarget ( "https://github.com/ctron/package-drone/wiki" ), null, null, true ) );
    }

    @Override
    public List<MenuEntry> getMainMenuEntries ()
    {
        return this.entries;
    }
}