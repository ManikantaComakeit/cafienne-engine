package org.cafienne.tenant;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.cmmn.actorapi.command.platform.NewUserInformation;
import org.cafienne.cmmn.actorapi.command.platform.PlatformUpdate;
import org.cafienne.system.CaseSystem;
import org.cafienne.tenant.actorapi.command.TenantCommand;
import org.cafienne.tenant.actorapi.event.TenantAppliedPlatformUpdate;
import org.cafienne.tenant.actorapi.event.TenantEvent;
import org.cafienne.tenant.actorapi.event.TenantModified;
import org.cafienne.tenant.actorapi.event.deprecated.DeprecatedTenantUserEvent;
import org.cafienne.tenant.actorapi.event.platform.TenantCreated;
import org.cafienne.tenant.actorapi.event.platform.TenantDisabled;
import org.cafienne.tenant.actorapi.event.platform.TenantEnabled;
import org.cafienne.tenant.actorapi.event.user.TenantUserAdded;
import org.cafienne.tenant.actorapi.event.user.TenantUserChanged;
import org.cafienne.tenant.actorapi.event.user.TenantUserRemoved;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
public class TenantActor extends ModelActor {
    private final static Logger logger = LoggerFactory.getLogger(TenantActor.class);

    private TenantCreated creationEvent;
    private final Map<String, TenantUser> users = new HashMap<>();
    private boolean disabled = false; // TODO: we can add some behavior behind this...

    public TenantActor(CaseSystem caseSystem) {
        super(caseSystem);
    }

    @Override
    protected boolean supportsCommand(Object msg) {
        return msg instanceof TenantCommand;
    }

    @Override
    protected boolean supportsEvent(Object msg) {
        return msg instanceof TenantEvent;
    }

    @Override
    public String getDescription() {
        return "Tenant[" + getId() + "]";
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    public boolean exists() {
        return this.creationEvent != null;
    }

    public void createInstance(List<TenantUser> newUsers) {
        addEvent(new TenantCreated(this));
        replaceInstance(newUsers);
    }

    public void replaceInstance(List<TenantUser> newUsers) {
        // Remove users that no longer exist
        users.keySet().stream().filter(userId -> newUsers.stream().noneMatch(newUser -> newUser.id().equals(userId))).collect(Collectors.toList()).forEach(this::removeUser);
        // Update existing and add new users
        newUsers.forEach(this::setUser);
    }

    public void removeUser(String userId) {
        TenantUser user = users.get(userId);
        if (user != null) {
            addEvent(new TenantUserRemoved(this, user));
        }
    }

    public void setUser(TenantUser newUserInfo) {
        TenantUser existingUser = users.get(newUserInfo.id());
        if (existingUser == null) {
            addEvent(new TenantUserAdded(this, newUserInfo));
        } else {
            if (existingUser.differs(newUserInfo)) {
                Set<String> removedRoles = existingUser.getRoles().stream().filter(role -> !newUserInfo.roles().contains(role)).collect(Collectors.toSet());
                addEvent(new TenantUserChanged(this, newUserInfo, removedRoles));
            }
        }
    }

    public void updateState(TenantAppliedPlatformUpdate event) {
        Map<String, NewUserInformation> updatedUsers = new HashMap<>();
        event.newUserInformation.info().foreach(userInfo -> {
            TenantUser user = users.remove(userInfo.existingUserId());
            if (user != null) {
                users.put(userInfo.newUserId(), userInfo.copyTo(user));
                updatedUsers.put(userInfo.existingUserId(), userInfo);
            } else {
                // Ouch. How can that be? Well ... if same user id is updated multiple times within this event.
                // We'll ignore those updates for now.
                NewUserInformation previouslyUpdated = updatedUsers.get(userInfo.existingUserId());
                if (previouslyUpdated != null) {
                    logger.warn("Not updating user id " + userInfo.existingUserId() + " to " + userInfo.newUserId() + ", because a user with this id has just now been updated to " + previouslyUpdated.newUserId());
                } else {
                    logger.warn("Not updating user id " + userInfo.existingUserId() + " to " + userInfo.newUserId() + ", because a user with this id is not found in the tenant.");
                }
            }
            return userInfo;
        });
    }

    public void updateState(TenantCreated tenantCreated) {
        this.setEngineVersion(tenantCreated.engineVersion);
        this.creationEvent = tenantCreated;
    }

    public void updateState(TenantUserAdded event) {
        users.put(event.memberId, event.member);
    }

    public void updateState(TenantUserChanged event) {
        users.put(event.memberId, event.member);
    }

    public void updateState(TenantUserRemoved event) {
        users.remove(event.memberId);
    }

    public void disable() {
        if (!disabled) {
            addEvent(new TenantDisabled(this));
        }
    }

    public void enable() {
        if (disabled) {
            addEvent(new TenantEnabled(this));
        }
    }

    public void updateState(TenantDisabled event) {
        this.disabled = true;
    }

    public void updateState(TenantEnabled event) {
        this.disabled = false;
    }

    public TenantUser getUser(String userId) {
        return users.get(userId);
    }

    public boolean isOwner(String userId) {
        TenantUser user = getUser(userId);
        return user != null && user.isOwner();
    }

    public boolean isOwner(TenantUser user) {
        return isOwner(user.id());
    }

    public List<String> getOwnerList() {
        return users.values().stream().filter(TenantUser::isOwner).filter(TenantUser::enabled).map(TenantUser::id).collect(Collectors.toList());
    }

    public void updateState(DeprecatedTenantUserEvent event) {
        TenantUser.handleDeprecatedEvent(users, event);
    }

    public void updateState(TenantModified event) {
        setLastModified(event.lastModified());
    }

    public void updatePlatformInformation(PlatformUpdate newUserInformation) {
        addEvent(new TenantAppliedPlatformUpdate(this, newUserInformation));
    }
}
