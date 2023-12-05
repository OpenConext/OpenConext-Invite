package access;

import access.config.HashGenerator;
import access.manage.EntityType;
import access.model.*;
import access.repository.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Set;

public record Seed(InvitationRepository invitationRepository,
                   RemoteProvisionedGroupRepository remoteProvisionedGroupRepository,
                   RemoteProvisionedUserRepository remoteProvisionedUserRepository,
                   RoleRepository roleRepository,
                   ApplicationRepository applicationRepository,
                   UserRepository userRepository,
                   UserRoleRepository userRoleRepository,
                   APITokenRepository apiTokenRepository) {


}
