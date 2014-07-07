package org.kie.workbench.backend;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.jbpm.kie.services.api.IdentityProvider;
import org.jbpm.services.cdi.RequestScopedBackupIdentityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.security.Identity;
import org.uberfire.security.Role;

@SessionScoped
public class UberFireIdentityProvider implements IdentityProvider, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(UberFireIdentityProvider.class);

    @Inject
    private BeanManager beanManager;

    @Inject
    private Identity identity;

    @Inject
    private HttpServletRequest request;

    @Override
    public String getName() {
        return getIdentityName();
    }

    @Override
    public List<String> getRoles() {


        return getIdentityRoles();
    }

    @Override
    public boolean hasRole(String role) {

        List<String> roles = getRoles();
        if (roles.contains(role)) {
            return true;
        }

        if (request != null) {
            return request.isUserInRole(role);
        }
        return false;
    }

    /**
     * This method returns the identity of the user who initiated the command.
     * @return The identity
     */
    protected String getIdentityName() {
        String name = "unknown";
        try {
            name = identity.getName();
            logger.debug( "Used original identity provider with user: {}", name);

        } catch (ContextNotActiveException e) {
            if (request != null && request.getUserPrincipal() != null) {
                name = request.getUserPrincipal().getName();
            } else {
                try {
                    RequestScopedBackupIdentityProvider provider = getBackupIdentityProvider();
                    // if the beanManager field has NOT been set, then provider == null
                    if( provider != null ) {
                        name = provider.getName();
                        logger.debug( "Used debug identity provider with user: {}", name);
                    }
                } catch (ContextNotActiveException ex) {

                    name = "unknown";
                }
            }
        }

        return name;
    }

    /**
     * This method returns the identity of the user who initiated the command.
     * @return The identity
     */
    protected List<String> getIdentityRoles() {
        List<String> roleNames = new ArrayList<String>();
        try {

            List<Role> ufRoles = identity.getRoles();
            for (Role role : ufRoles) {
                roleNames.add(role.getName());
            }
            logger.debug( "Used original identity provider with roles: {}", roleNames);
        } catch (ContextNotActiveException e) {
            throw new IllegalStateException("Unable to get roles due to missing context");
        }

        return roleNames;
    }

    /**
     * Sets the {@link BeanManager} field.
     * </p>
     * This field is necessary in order to retrieve a {@link RequestScopedBackupIdentityProvider} bean from the CDI context.
     * A {@link RequestScopedBackupIdentityProvider} bean is necessary when the a command is issued to the a {@link RuntimeEngine}
     * in a context or scope where HTTP is *not* used. The normal {@link IdentityProvider} bean is only available if HTTP is being
     * used, because it relies on HTTP authorization mechanisms in order to get the user (See the UberfireIdentityProvider class).
     *
     * @param beanManager A {@link BeanManager} instance
     */
    public void setBeanManager(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    /**
     * This retrieves a {@link RequestScopedBackupIdentityProvider} bean from the CDI (request scoped) context.
     * @return a {@link RequestScopedBackupIdentityProvider} instance
     */
    protected RequestScopedBackupIdentityProvider getBackupIdentityProvider() {
        Class<?> type = RequestScopedBackupIdentityProvider.class;
        logger.debug("Retrieving {} bean", type.getSimpleName() );
        if( beanManager != null ) {
            final Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
            if (bean == null) {
                return null;
            }
            CreationalContext<?> cc = beanManager.createCreationalContext(null);
            return (RequestScopedBackupIdentityProvider) beanManager.getReference(bean, type, cc);
        } else {
            return null;
        }
    }

}

