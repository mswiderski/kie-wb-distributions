/*
 * Copyright 2012 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.kie.workbench.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay;
import com.google.gwt.user.client.ui.TextBox;
import org.jboss.errai.bus.client.api.RemoteCallback;
import org.jboss.errai.ioc.client.api.AfterInitialization;
import org.jboss.errai.ioc.client.api.Caller;
import org.jboss.errai.ioc.client.api.EntryPoint;
import org.jboss.errai.ioc.client.container.IOCBeanDef;
import org.jboss.errai.ioc.client.container.IOCBeanManager;
import org.kie.workbench.client.resources.i18n.Constants;
import org.kie.workbench.common.services.shared.config.AppConfigService;
import org.kie.workbench.common.services.shared.config.ApplicationPreferences;
import org.kie.workbench.common.widgets.client.resources.RoundedCornersResource;
import org.uberfire.client.mvp.AbstractWorkbenchPerspectiveActivity;
import org.uberfire.client.mvp.ActivityManager;
import org.uberfire.client.mvp.Command;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.workbench.widgets.menu.MenuFactory;
import org.uberfire.client.workbench.widgets.menu.MenuItem;
import org.uberfire.client.workbench.widgets.menu.MenuPosition;
import org.uberfire.client.workbench.widgets.menu.MenuSearchItem;
import org.uberfire.client.workbench.widgets.menu.Menus;
import org.uberfire.client.workbench.widgets.menu.WorkbenchMenuBarPresenter;
import org.uberfire.security.Identity;
import org.uberfire.security.Role;
import org.uberfire.shared.mvp.impl.DefaultPlaceRequest;

import static org.uberfire.client.workbench.widgets.menu.MenuFactory.*;

/**
 *
 */
@EntryPoint
public class KieWorkbenchEntryPoint {

    private Constants constants = GWT.create(Constants.class);
    
    @Inject
    private PlaceManager placeManager;

    @Inject
    private WorkbenchMenuBarPresenter menubar;

    @Inject
    private ActivityManager activityManager;

    @Inject
    private IOCBeanManager iocManager;

    @Inject
    public Identity identity;

    @Inject
    private Caller<AppConfigService> appConfigService;

    private SuggestBox actionText;
    private TextBox textSuggestBox;
    DefaultSuggestionDisplay suggestionDisplay;
    Map<String, String> actions = new HashMap<String, String>();

    
    
    @AfterInitialization
    public void startApp() {
        loadPreferences();
        loadStyles();
        setupMenu();
        hideLoadingPopup();
    }

    private void loadPreferences() {
        appConfigService.call( new RemoteCallback<Map<String, String>>() {
            @Override
            public void callback( final Map<String, String> response ) {
                ApplicationPreferences.setUp(response);
            }
        } ).loadPreferences();
    }

    private void loadStyles() {
        //Ensure CSS has been loaded
        //ShowcaseResources.INSTANCE.showcaseCss().ensureInjected();
        RoundedCornersResource.INSTANCE.roundCornersCss().ensureInjected();
    }

    private void setupMenu() {

        final AbstractWorkbenchPerspectiveActivity defaultPerspective = getDefaultPerspectiveActivity();

        final Menus menus = MenuFactory.newTopLevelMenu(constants.Home()).respondsWith(new Command() {
                    @Override
                    public void execute() {
                        if ( defaultPerspective != null ) {
                            placeManager.goTo( new DefaultPlaceRequest( defaultPerspective.getIdentifier() ) );
                        } else {
                            Window.alert( "Default perspective not found." );
                        }
                    }
                } )
                .endMenu()
                .newTopLevelMenu( constants.Views() )
                .withItems( getPerspectives() )
                .endMenu()
                .newTopLevelMenu( constants.BPM() )
                .withItems( getBPMViews() )
                .endMenu()
                .newTopLevelMenu( constants.LogOut() )
                .respondsWith( new Command() {
                    @Override
                    public void execute() {
                        redirect( GWT.getModuleBaseURL() + "uf_logout" );
                    }
                } )
                .endMenu()
                .newTopLevelMenu( "Find" )
                .position( MenuPosition.RIGHT )
                .respondsWith( new Command() {
                    @Override
                    public void execute() {
                        placeManager.goTo( "FindForm" );
                    }
                } )
                .endMenu()
                .newSearchItem( constants.Search() )
                .position( MenuPosition.RIGHT )
                .respondsWith( new MenuSearchItem.SearchCommand() {
                    @Override
                    public void execute( final String term ) {
                        placeManager.goTo( new DefaultPlaceRequest( "FullTextSearchForm" ).addParameter( "term", term ) );
                    }
                } )
                .endMenu()
                .newTopLevelMenu( identity.getName() )
                    .position( MenuPosition.RIGHT )
                    .withItems( getRoles() )
                .endMenu()
                .build();

        menubar.aggregateWorkbenchMenus( menus );
    }

     private List<? extends MenuItem> getRoles() {
        final List<MenuItem> result = new ArrayList<MenuItem>( identity.getRoles().size() );
        for ( final Role role : identity.getRoles() ) {
            result.add( MenuFactory.newSimpleItem( role.getName() ).endMenu().build().getItems().get( 0 ) );
        }

        return result;
    }
    
     private List<? extends MenuItem> getAuthoringViews() {
        final List<MenuItem> result = new ArrayList<MenuItem>(1);

        result.add(MenuFactory.newSimpleItem(constants.Process_Authoring()).respondsWith(new Command() {
            @Override
            public void execute() {
                placeManager.goTo(new DefaultPlaceRequest("Authoring"));
            }
        }).endMenu().build().getItems().get(0));

        return result;
    }

    private List<? extends MenuItem> getProcessMGMTViews() {
        final List<MenuItem> result = new ArrayList<MenuItem>(2);

        result.add(MenuFactory.newSimpleItem(constants.Process_Definitions()).respondsWith(new Command() {
            @Override
            public void execute() {
                placeManager.goTo(new DefaultPlaceRequest("Process Definitions"));
            }
        }).endMenu().build().getItems().get(0));

        result.add(MenuFactory.newSimpleItem(constants.Process_Instances()).respondsWith(new Command() {
            @Override
            public void execute() {
                placeManager.goTo(new DefaultPlaceRequest("Process Instances"));
            }
        }).endMenu().build().getItems().get(0));

        return result;
    }

    private List<? extends MenuItem> getDeploymentViews() {
        final List<MenuItem> result = new ArrayList<MenuItem>(1);

        result.add(MenuFactory.newSimpleItem(constants.Deployments()).respondsWith(new Command() {
            @Override
            public void execute() {
                placeManager.goTo(new DefaultPlaceRequest("Deployments"));
            }
        }).endMenu().build().getItems().get(0));

        return result;
    }

    private List<? extends MenuItem> getWorkViews() {
        final List<MenuItem> result = new ArrayList<MenuItem>(2);

        result.add(MenuFactory.newSimpleItem(constants.Tasks_Calendar_View()).respondsWith(new Command() {
            @Override
            public void execute() {
                placeManager.goTo(new DefaultPlaceRequest("Tasks"));
            }
        }).endMenu().build().getItems().get(0));

        result.add(MenuFactory.newSimpleItem(constants.Tasks_Grid_View()).respondsWith(new Command() {
            @Override
            public void execute() {
                placeManager.goTo(new DefaultPlaceRequest("Grid Tasks List"));
            }
        }).endMenu().build().getItems().get(0));

        return result;
    }

    private List<? extends MenuItem> getBAMViews() {
        final List<MenuItem> result = new ArrayList<MenuItem>(1);
        result.add(MenuFactory.newSimpleItem(constants.Process_Dashboard()).respondsWith(new Command() {
            @Override
            public void execute() {
                Window.open("http://localhost:8080/dashbuilder/workspace/jbpm-dashboard", "_blank", "");
            }
        }).endMenu().build().getItems().get(0));

        return result;
    }
     
    private List<? extends MenuItem> getBPMViews() {
        final List<MenuItem> result = new ArrayList<MenuItem>( );
        result.addAll( getAuthoringViews());
        result.addAll( getDeploymentViews());
        result.addAll( getWorkViews());
        result.addAll( getProcessMGMTViews());
        result.addAll( getBAMViews());
        return result;
    }
    
    private AbstractWorkbenchPerspectiveActivity getDefaultPerspectiveActivity() {
        AbstractWorkbenchPerspectiveActivity defaultPerspective = null;
        final Collection<IOCBeanDef<AbstractWorkbenchPerspectiveActivity>> perspectives = iocManager.lookupBeans( AbstractWorkbenchPerspectiveActivity.class );
        final Iterator<IOCBeanDef<AbstractWorkbenchPerspectiveActivity>> perspectivesIterator = perspectives.iterator();
        outer_loop:
        while ( perspectivesIterator.hasNext() ) {
            final IOCBeanDef<AbstractWorkbenchPerspectiveActivity> perspective = perspectivesIterator.next();
            final AbstractWorkbenchPerspectiveActivity instance = perspective.getInstance();
            if ( instance.isDefault() ) {
                defaultPerspective = instance;
                break outer_loop;
            } else {
                iocManager.destroyBean( instance );
            }
        }
        return defaultPerspective;
    }

    private List<MenuItem> getPerspectives() {
        final List<MenuItem> perspectives = new ArrayList<MenuItem>();
        for ( final AbstractWorkbenchPerspectiveActivity perspective : getPerspectiveActivities() ) {
            final String name = perspective.getPerspective().getName();
            final Command cmd = new Command() {

                @Override
                public void execute() {
                    placeManager.goTo( new DefaultPlaceRequest( perspective.getIdentifier() ) );
                }

            };
            final MenuItem item = newSimpleItem( name ).respondsWith( cmd ).endMenu().build().getItems().get( 0 );
            perspectives.add( item );
        }

        return perspectives;
    }

    private List<AbstractWorkbenchPerspectiveActivity> getPerspectiveActivities() {

        //Get Perspective Providers
        final Set<AbstractWorkbenchPerspectiveActivity> activities = activityManager.getActivities( AbstractWorkbenchPerspectiveActivity.class );

        //Sort Perspective Providers so they're always in the same sequence!
        List<AbstractWorkbenchPerspectiveActivity> sortedActivities = new ArrayList<AbstractWorkbenchPerspectiveActivity>( activities );
        Collections.sort( sortedActivities,
                          new Comparator<AbstractWorkbenchPerspectiveActivity>() {
                              @Override
                              public int compare( AbstractWorkbenchPerspectiveActivity o1,
                                                  AbstractWorkbenchPerspectiveActivity o2 ) {
                                  return o1.getPerspective().getName().compareTo( o2.getPerspective().getName() );
                              }
                          } );

        return sortedActivities;
    }
    //Fade out the "Loading application" pop-up

    private void hideLoadingPopup() {
        final Element e = RootPanel.get( "loading" ).getElement();

        new Animation() {
            @Override
            protected void onUpdate( double progress ) {
                e.getStyle().setOpacity( 1.0 - progress );
            }

            @Override
            protected void onComplete() {
                e.getStyle().setVisibility( Style.Visibility.HIDDEN );
            }
        }.run( 500 );
    }

    public static native void redirect( String url )/*-{
        $wnd.location = url;
    }-*/;

}
