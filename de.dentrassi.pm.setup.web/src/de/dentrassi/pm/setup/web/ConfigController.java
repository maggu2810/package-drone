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
package de.dentrassi.pm.setup.web;

import java.lang.reflect.Method;
import java.sql.Driver;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.eclipse.scada.utils.ExceptionHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.jdbc.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dentrassi.osgi.web.Controller;
import de.dentrassi.osgi.web.LinkTarget;
import de.dentrassi.osgi.web.ModelAndView;
import de.dentrassi.osgi.web.RequestMapping;
import de.dentrassi.osgi.web.RequestMethod;
import de.dentrassi.osgi.web.ViewResolver;
import de.dentrassi.osgi.web.controller.ControllerInterceptor;
import de.dentrassi.osgi.web.controller.binding.BindingResult;
import de.dentrassi.osgi.web.controller.binding.ExceptionError;
import de.dentrassi.osgi.web.controller.binding.MessageBindingError;
import de.dentrassi.osgi.web.controller.form.FormData;
import de.dentrassi.osgi.web.controller.validator.ControllerValidator;
import de.dentrassi.osgi.web.controller.validator.ValidationContext;
import de.dentrassi.pm.common.web.CommonController;
import de.dentrassi.pm.common.web.InterfaceExtender;
import de.dentrassi.pm.common.web.Modifier;
import de.dentrassi.pm.common.web.menu.MenuEntry;
import de.dentrassi.pm.database.Configurator;
import de.dentrassi.pm.database.DatabaseConnectionData;
import de.dentrassi.pm.database.DatabaseSetup;
import de.dentrassi.pm.database.JdbcHelper;
import de.dentrassi.pm.mail.service.MailService;
import de.dentrassi.pm.sec.web.controller.HttpConstraints;
import de.dentrassi.pm.sec.web.controller.HttpContraintControllerInterceptor;
import de.dentrassi.pm.sec.web.controller.Secured;
import de.dentrassi.pm.sec.web.controller.SecuredControllerInterceptor;
import de.dentrassi.pm.setup.web.internal.Activator;

@Controller
@RequestMapping ( value = "/config" )
@ViewResolver ( "/WEB-INF/views/%s.jsp" )
@Secured
@ControllerInterceptor ( SecuredControllerInterceptor.class )
@HttpConstraint ( rolesAllowed = "ADMIN" )
@ControllerInterceptor ( HttpContraintControllerInterceptor.class )
public class ConfigController implements InterfaceExtender
{
    private final static Logger logger = LoggerFactory.getLogger ( ConfigController.class );

    private final static Method METHOD_MAIN = LinkTarget.getControllerMethod ( ConfigController.class, "main" );

    private EventAdmin eventAdmin;

    public void setEventAdmin ( final EventAdmin eventAdmin )
    {
        this.eventAdmin = eventAdmin;
    }

    @Override
    public List<MenuEntry> getMainMenuEntries ( final HttpServletRequest request )
    {
        final List<MenuEntry> result = new LinkedList<> ();

        if ( HttpConstraints.isCallAllowed ( METHOD_MAIN, request ) )
        {
            result.add ( new MenuEntry ( "Administration", 10_000, "Database Setup", 100, LinkTarget.createFromController ( METHOD_MAIN ), Modifier.DEFAULT, null, false ) );
        }

        return result;
    }

    @RequestMapping
    public ModelAndView main ()
    {
        final Map<String, Object> model = new HashMap<> ();

        try ( Configurator cfg = Configurator.create () )
        {
            final DatabaseConnectionData command = cfg.getDatabaseSettings ();

            if ( command != null )
            {
                try ( DatabaseSetup db = new DatabaseSetup ( command ) )
                {
                    fillData ( db, model );
                }
            }
            else
            {
                fillData ( null, model );
            }

            model.put ( "command", command );
        }

        return new ModelAndView ( "/config/index", model );
    }

    @RequestMapping ( value = "/testConnection", method = RequestMethod.POST )
    public ModelAndView testConnection ( @Valid @FormData ( "command" ) final DatabaseConnectionData data, final BindingResult result)
    {
        if ( result.hasErrors () )
        {
            return testMessageResult ( "warning", "Invalid configuration!", "The configuration has validation errors. Please fix them first!" );
        }

        Exception testResult;

        try ( DatabaseSetup setup = new DatabaseSetup ( data ) )
        {
            testResult = setup.testConnection ();
        }

        if ( testResult != null )
        {
            return CommonController.createError ( "Database connection test", "Connection test failed", testResult, true );
        }

        return testMessageResult ( "success", "Success!", "The connection test was successful!" );
    }

    private ModelAndView testMessageResult ( final String type, final String shortMessage, final String message )
    {
        final Map<String, Object> model = new HashMap<> ( 2 );

        model.put ( "type", type );
        model.put ( "shortMessage", shortMessage );
        model.put ( "message", message );

        return new ModelAndView ( "config/testMessage", model );
    }

    private void fillData ( final DatabaseSetup setup, final Map<String, Object> model )
    {
        model.put ( "configured", Boolean.FALSE );

        if ( setup != null )
        {
            Exception testResult = null;
            try
            {
                testResult = setup.testConnection ();

                model.put ( "databaseSchemaVersion", setup.getSchemaVersion () );
                model.put ( "currentVersion", setup.getCurrentVersion () );
                model.put ( "configured", setup.isConfigured () );

                if ( testResult != null )
                {
                    final Throwable root = ExceptionHelper.getRootCause ( testResult );
                    model.put ( "testResultMessage", ExceptionHelper.extractMessage ( root ) );
                    model.put ( "testStackTrace", ExceptionHelper.formatted ( root ) );
                }
            }
            catch ( final Exception e )
            {
            }
        }

        model.put ( "storageServicePresent", Activator.getTracker ().getService () != null );
        model.put ( "jdbcDrivers", JdbcHelper.getJdbcDrivers () );
    }

    @ControllerValidator ( formDataClass = DatabaseConnectionData.class )
    public void validateDatabase ( final DatabaseConnectionData data, final ValidationContext context )
    {
        final String driverName = data.getJdbcDriver ();
        if ( driverName == null || driverName.isEmpty () )
        {
            return;
        }

        if ( data.getUrl () != null && !data.getUrl ().isEmpty () )
        {
            doWithDriver ( driverName, ( driver ) -> {
                try
                {
                    if ( !driver.acceptsURL ( data.getUrl () ) )
                    {
                        context.error ( "url", new MessageBindingError ( "The JDBC driver does not accept the connection URL" ) );
                    }
                }
                catch ( final Exception e )
                {
                    logger.warn ( "Failed to setup", e );
                    context.error ( "url", new ExceptionError ( e ) );
                }
            } );
        }
    }

    private void doWithDriver ( final String driverName, final Consumer<Driver> driverConsumer )
    {
        doWithFactory ( driverName, ( factory ) -> {
            try
            {
                final Driver driver = factory.createDriver ( null );
                driverConsumer.accept ( driver );
            }
            catch ( final Exception e )
            {
                logger.warn ( "Failed to create driver", e );
            }
        } );
    }

    /**
     * Call the consumer with the data source factory
     * <p>
     * Note: The consumer will not be called if the factory was not found
     * </p>
     *
     * @param driverName
     *            the jdbc driver name
     * @param factoryConsumer
     *            the consumer
     */
    private void doWithFactory ( final String driverName, final Consumer<DataSourceFactory> factoryConsumer )
    {
        final BundleContext ctx = FrameworkUtil.getBundle ( ConfigController.class ).getBundleContext ();
        try
        {
            final Collection<ServiceReference<DataSourceFactory>> refs = ctx.getServiceReferences ( DataSourceFactory.class, String.format ( "(osgi.jdbc.driver.class=%s)", driverName ) );
            if ( refs == null || refs.isEmpty () )
            {
                return;
            }

            final ServiceReference<DataSourceFactory> ref = refs.iterator ().next ();
            final DataSourceFactory service = ctx.getService ( ref );
            try
            {
                factoryConsumer.accept ( service );
            }
            finally
            {
                ctx.ungetService ( ref );
            }
        }
        catch ( final InvalidSyntaxException e )
        {
        }
    }

    @RequestMapping ( method = RequestMethod.POST )
    public ModelAndView setup ( @Valid @FormData ( "command" ) final DatabaseConnectionData data, final BindingResult result)
    {
        final Map<String, Object> model = new HashMap<> ();
        model.put ( "command", data );

        if ( !result.hasErrors () )
        {
            // store

            try ( Configurator cfg = Configurator.create () )
            {
                cfg.setDatabaseSettings ( data );
            }

            // now wait until the configuration was performed in the background

            try
            {
                Activator.getTracker ().waitForService ( 5000 );
            }
            catch ( final InterruptedException e )
            {
            }
        }

        Exception testConnection = null;
        boolean needUpgrade = false;
        if ( data != null )
        {
            try ( DatabaseSetup db = new DatabaseSetup ( data ) )
            {
                needUpgrade = db.isNeedUpgrade ();
                testConnection = db.testConnection ();
                fillData ( db, model );
            }
        }
        else
        {
            fillData ( null, model );
        }

        if ( needUpgrade || isMailServicePresent () || result.hasErrors () || testConnection != null )
        {
            // either we still have something to do here, or we are fully set up
            return new ModelAndView ( "/config/index", model );
        }
        else
        {
            return new ModelAndView ( "redirect:/setup" );
        }
    }

    protected boolean isMailServicePresent ()
    {
        return FrameworkUtil.getBundle ( ConfigController.class ).getBundleContext ().getServiceReference ( MailService.class ) != null;
    }

    @RequestMapping ( value = "/databaseUpgrade", method = RequestMethod.POST )
    public ModelAndView upgrade ()
    {
        final Map<String, Object> model = new HashMap<> ();

        try
        {
            try ( Configurator cfg = Configurator.create () )
            {
                final DatabaseConnectionData data = cfg.getDatabaseSettings ();
                try ( DatabaseSetup setup = new DatabaseSetup ( data ) )
                {
                    setup.performUpgrade ();
                    fillData ( setup, model );
                }
            }

            model.put ( "mailServicePresent", isMailServicePresent () );

            this.eventAdmin.postEvent ( new Event ( "drone/database/schemaUpgrade", Collections.emptyMap () ) );

            return new ModelAndView ( "/config/upgrade", model );
        }
        catch ( final Throwable e )
        {
            return CommonController.createError ( "Database schema", "Upgrade failed", e );
        }
    }
}
