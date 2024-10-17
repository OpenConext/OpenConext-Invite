package access.api;


import access.model.Role;

@FunctionalInterface
public interface RoleValidator {

    void validate(Role role);
}
