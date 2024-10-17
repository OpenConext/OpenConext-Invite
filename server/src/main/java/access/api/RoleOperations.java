package access.api;

import access.exception.InvalidInputException;
import access.internal.InternalInviteController;
import access.model.Application;
import access.model.ApplicationUsage;
import access.model.Role;
import access.repository.ApplicationRepository;
import access.repository.ApplicationUsageRepository;
import access.validation.URLFormatValidator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

public class RoleOperations {

    private final URLFormatValidator urlFormatValidator = new URLFormatValidator();
    private final AppRepositoryResource appRepositoryResource;

    public RoleOperations(AppRepositoryResource appRepositoryResource) {
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
        ApplicationRepository applicationRepository = appRepositoryResource.getApplicationRepository();
        ApplicationUsageRepository applicationUsageRepository = appRepositoryResource.getApplicationUsageRepository();
        //This is the disadvantage of having to save references from Manage
        Set<ApplicationUsage> applicationUsages = role.getApplicationUsages().stream()
                .map(applicationUsageFromClient -> {
                    Application application = applicationUsageFromClient.getApplication();
                    Application applicationFromDB = applicationRepository
                            .findByManageIdAndManageType(application.getManageId(), application.getManageType())
                            .orElseGet(() -> applicationRepository.save(application));
                    ApplicationUsage applicationUsageFromDB = applicationUsageRepository.findByRoleIdAndApplicationManageIdAndApplicationManageType(
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


}
