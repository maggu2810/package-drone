/*******************************************************************************
 * Copyright (c) 2014, 2015 IBH SYSTEMS GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBH SYSTEMS GmbH - initial API and implementation
 *******************************************************************************/
package de.dentrassi.pm.common;

/**
 * A meta data key
 * <p>
 * A meta data key consists of a namespace and a key part. The namespace is in
 * most cases the ID of the channel aspect factory.
 * </p>
 * <p>
 * Meta keys with the same namespace and key are equal. The natural sort order
 * is first by namespace and then by key. Both compared as strings.
 * </p>
 */
public class MetaKey implements Comparable<MetaKey>
{
    private final String namespace;

    private final String key;

    public MetaKey ( final String namespace, final String key )
    {
        this.namespace = namespace;
        this.key = key;
    }

    public String getKey ()
    {
        return this.key;
    }

    public String getNamespace ()
    {
        return this.namespace;
    }

    @Override
    public int hashCode ()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( this.key == null ? 0 : this.key.hashCode () );
        result = prime * result + ( this.namespace == null ? 0 : this.namespace.hashCode () );
        return result;
    }

    @Override
    public boolean equals ( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( ! ( obj instanceof MetaKey ) )
        {
            return false;
        }
        final MetaKey other = (MetaKey)obj;
        if ( this.key == null )
        {
            if ( other.key != null )
            {
                return false;
            }
        }
        else if ( !this.key.equals ( other.key ) )
        {
            return false;
        }
        if ( this.namespace == null )
        {
            if ( other.namespace != null )
            {
                return false;
            }
        }
        else if ( !this.namespace.equals ( other.namespace ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString ()
    {
        return "[" + this.namespace + ":" + this.key + "]";
    }

    @Override
    public int compareTo ( final MetaKey o )
    {
        int rc;

        rc = this.namespace.compareTo ( o.namespace );
        if ( rc != 0 )
        {
            return rc;
        }

        return this.key.compareTo ( o.key );
    }
}
