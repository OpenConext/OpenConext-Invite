package invite.api;


import invite.model.Role;

@FunctionalInterface
public interface RoleValidator {

    void validate(Role role);
}
