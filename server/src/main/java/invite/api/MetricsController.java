package invite.api;

import invite.model.Authority;
import invite.model.Status;
import invite.repository.*;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

@RestController
@Hidden
public class MetricsController {

    public MetricsController(UserRepository userRepository,
                             RoleRepository roleRepository,
                             InvitationRepository invitationRepository,
                             UserRoleRepository userRoleRepository,
                             ApplicationRepository applicationRepository,
                             MeterRegistry meterRegistry) {

        Gauge.builder("total_number_of_users", () ->
                        userRepository.count())
                .description("Total number of Users")
                .register(meterRegistry);

        Gauge.builder("total_number_of_access_roles", () ->
                        roleRepository.count())
                .description("Total number of Access Roles")
                .register(meterRegistry);

        Stream.of(Authority.values())
                .filter(authority -> !authority.equals(Authority.SUPER_USER) && !authority.equals(Authority.INSTITUTION_ADMIN))
                .forEach(authority -> Gauge
                        .builder("total_number_of_" + authority.name().toLowerCase() + "_users",
                                () -> userRoleRepository.countByAuthority(authority))
                        .description("Total number of " + authority.name().toLowerCase() + "-users")
                        .register(meterRegistry));

        Gauge
                .builder("total_number_of_institution_admins",
                        () -> userRepository.countBySuperUserTrue())
                .description("Total number of Institution Admins")
                .register(meterRegistry);

        Gauge
                .builder("total_number_of_super_users",
                        () -> userRepository.countBySuperUserTrue())
                .description("Total number of Super users")
                .register(meterRegistry);

        Gauge.builder("total_number_of_applications",
                        () -> applicationRepository.count())
                .description("Total number of applications")
                .register(meterRegistry);

        Gauge.builder("total_number_of_pending_invitations",
                        () -> invitationRepository.countByStatus(Status.OPEN))
                .description("Total number of pending invitations")
                .register(meterRegistry);

    }

}
