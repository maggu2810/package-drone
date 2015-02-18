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
package de.dentrassi.pm.storage.web.channel;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.http.HttpServletRequest;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import de.dentrassi.osgi.web.Controller;
import de.dentrassi.osgi.web.LinkTarget;
import de.dentrassi.osgi.web.ModelAndView;
import de.dentrassi.osgi.web.RequestMapping;
import de.dentrassi.osgi.web.ViewResolver;
import de.dentrassi.osgi.web.controller.ControllerInterceptor;
import de.dentrassi.osgi.web.controller.binding.PathVariable;
import de.dentrassi.pm.common.web.CommonController;
import de.dentrassi.pm.common.web.InterfaceExtender;
import de.dentrassi.pm.common.web.Modifier;
import de.dentrassi.pm.common.web.menu.MenuEntry;
import de.dentrassi.pm.importer.Importer;
import de.dentrassi.pm.importer.ImporterDescription;
import de.dentrassi.pm.importer.web.ImportDescriptor;
import de.dentrassi.pm.sec.web.controller.HttpContraintControllerInterceptor;
import de.dentrassi.pm.sec.web.controller.Secured;
import de.dentrassi.pm.sec.web.controller.SecuredControllerInterceptor;
import de.dentrassi.pm.storage.Channel;
import de.dentrassi.pm.storage.service.StorageService;

@Secured
@Controller
@ViewResolver ( "/WEB-INF/views/imp/%s.jsp" )
@ControllerInterceptor ( SecuredControllerInterceptor.class )
@HttpConstraint ( rolesAllowed = "MANAGER" )
@ControllerInterceptor ( HttpContraintControllerInterceptor.class )
public class ImportController implements InterfaceExtender
{
    private StorageService service;

    public static class ImporterDescriptionLabelComparator implements Comparator<ImporterDescription>
    {
        @Override
        public int compare ( final ImporterDescription o1, final ImporterDescription o2 )
        {
            return o1.getLabel ().compareTo ( o2.getLabel () );
        }
    }

    private static ImporterDescriptionLabelComparator NAME_COMPARATOR = new ImporterDescriptionLabelComparator ();

    private static class Entry
    {
        private final BundleContext context;

        private final Importer service;

        private final ServiceReference<Importer> reference;

        private final ImporterDescription description;

        public Entry ( final BundleContext context, final ServiceReference<Importer> reference )
        {
            this.context = context;
            this.reference = reference;
            this.service = context.getService ( reference );
            this.description = this.service.getDescription ();
        }

        public void dispose ()
        {
            this.context.ungetService ( this.reference );
        }

        public Importer getService ()
        {
            return this.service;
        }

        public ImporterDescription getDescription ()
        {
            return this.description;
        }
    }

    private ServiceTracker<Importer, Entry> tracker;

    private final ServiceTrackerCustomizer<Importer, Entry> customizer = new ServiceTrackerCustomizer<Importer, Entry> () {

        @Override
        public void removedService ( final ServiceReference<Importer> reference, final Entry service )
        {
            service.dispose ();
        }

        @Override
        public void modifiedService ( final ServiceReference<Importer> reference, final Entry service )
        {
        }

        @Override
        public Entry addingService ( final ServiceReference<Importer> reference )
        {
            return new Entry ( FrameworkUtil.getBundle ( ImportController.class ).getBundleContext (), reference );
        }
    };

    public void setService ( final StorageService service )
    {
        this.service = service;
    }

    public void start ()
    {
        this.tracker = new ServiceTracker<> ( FrameworkUtil.getBundle ( ImportController.class ).getBundleContext (), Importer.class, this.customizer );
        this.tracker.open ();
    }

    public void stop ()
    {
        this.tracker.close ();
    }

    @RequestMapping ( value = "/channel/{channelId}/import" )
    public ModelAndView index ( @PathVariable ( "channelId" ) final String channelId )
    {
        final Channel channel = this.service.getChannel ( channelId );
        if ( channel == null )
        {
            return CommonController.createNotFound ( "channel", channelId );
        }

        final Map<String, Object> model = new HashMap<> ();
        model.put ( "channel", channel );
        model.put ( "descriptions", getDescriptions () );

        final ImportDescriptor desc = new ImportDescriptor ();

        desc.setId ( channel.getId () );
        desc.setType ( "channel" );
        model.put ( "token", desc.toBase64 () );

        return new ModelAndView ( "index", model );
    }

    public ImporterDescription[] getDescriptions ()
    {
        final ImporterDescription[] result = this.tracker.getTracked ().values ().stream ().map ( Entry::getDescription ).toArray ( ImporterDescription[]::new );
        Arrays.sort ( result, NAME_COMPARATOR );
        return result;
    }

    @Override
    public List<MenuEntry> getActions ( final HttpServletRequest request, final Object object )
    {
        if ( ! ( object instanceof Channel ) )
        {
            return null;
        }
        if ( !request.isUserInRole ( "MANAGER" ) )
        {
            return null;
        }

        final List<MenuEntry> result = new LinkedList<> ();

        final Channel channel = (Channel)object;

        final Map<String, String> model = new HashMap<> ( 1 );
        model.put ( "channelId", channel.getId () );

        result.add ( new MenuEntry ( "Import", 600, LinkTarget.createFromController ( ImportController.class, "index" ).expand ( model ), Modifier.DEFAULT, "import" ) );

        return result;
    }
}
