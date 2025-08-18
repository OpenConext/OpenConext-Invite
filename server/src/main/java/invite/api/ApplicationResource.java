package invite.api;

import invite.repository.ApplicationRepository;
import invite.repository.ApplicationUsageRepository;

public interface ApplicationResource {

    ApplicationRepository getApplicationRepository();

    ApplicationUsageRepository getApplicationUsageRepository();

}
