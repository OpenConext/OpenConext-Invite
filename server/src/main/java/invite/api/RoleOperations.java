package invite.api;

import invite.exception.InvalidInputException;
import invite.model.Application;
import invite.model.ApplicationUsage;
import invite.model.Role;
import invite.model.RoleRequest;
import invite.repository.ApplicationRepository;
import invite.repository.ApplicationUsageRepository;
import invite.validation.URLFormatValidator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

public class RoleOperations {

    private final URLFormatValidator urlFormatValidator = new URLFormatValidator();
    private final ApplicationResource appRepositoryResource;

    public RoleOperations(ApplicationResource appRepositoryResource) {
        this.appRepositoryResource = appRepositoryResource;
    }

    public void assertValidRole(Role role) {
        if (CollectionUtils.isEmpty(role.getApplicationUsages())) {
            throw new InvalidInputException("applicationUsages are required");
        }
        role.getApplicationUsages().forEach(applicationUsage -> {
            if (StringUtils.hasText(applicationUsage.getLandingPage()) && !urlFormatValidator.isValid(applicationUsage.getLandingPage())) {
                throw new InvalidInputException("Valid landingPage is required");
            }
        });
    }

    public void syncRoleApplicationUsages(Role role) {
        syncRoleApplicationUsages(role, role.getApplicationUsages());
    }

    //Resolves desiredApplicationUsages against the database without ever staging them onto role.applicationUsages first.
    //When role is an already-managed (attached) entity, staging unresolved/transient ApplicationUsage instances onto it
    //before this method's repository queries run would dirty the persistence context and trigger an auto-flush mid-resolution,
    //causing Hibernate to insert those transient duplicates before the reused/orphaned rows are reconciled -
    //see https://github.com/OpenConext/OpenConext-Invite/issues/764
    public void syncRoleApplicationUsages(Role role, Set<ApplicationUsage> desiredApplicationUsages) {
        ApplicationRepository applicationRepository = appRepositoryResource.getApplicationRepository();
        ApplicationUsageRepository applicationUsageRepository = appRepositoryResource.getApplicationUsageRepository();
        //This is the disadvantage of having to save references from Manage
        Set<ApplicationUsage> applicationUsages = desiredApplicationUsages.stream()
                .map(applicationUsageFromClient -> {
                    Application application = applicationUsageFromClient.getApplication();
                    Application applicationFromDB = applicationRepository
                            .findByManageIdAndManageTypeOrderById(application.getManageId(), application.getManageType())
                            .orElseGet(() -> applicationRepository.save(application));
                    ApplicationUsage applicationUsageFromDB = applicationUsageRepository
                            .findByRoleIdAndApplicationManageIdAndApplicationManageTypeOrderByApplicationId(
                                    role.getId(),
                                    applicationFromDB.getManageId(),
                                    applicationFromDB.getManageType()
                            ).orElseGet(() -> new ApplicationUsage(applicationFromDB, applicationUsageFromClient.getLandingPage()));
                    applicationUsageFromDB.setLandingPage(applicationUsageFromClient.getLandingPage());
                    return applicationUsageFromDB;
                })
                .collect(Collectors.toSet());
        role.setApplicationUsages(applicationUsages);
    }


    public void setDefaultsValidRole(Role role) {
        if (role.getDefaultExpiryDate() == null && (
                role.getDefaultExpiryDays() == null || role.getDefaultExpiryDays() == 0)) {
            role.setDefaultExpiryDays(365);
        }
    }
}
