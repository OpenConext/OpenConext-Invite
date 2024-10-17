package access.api;

import access.repository.ApplicationRepository;
import access.repository.ApplicationUsageRepository;

public interface ApplicationResource {

    ApplicationRepository getApplicationRepository();

    ApplicationUsageRepository getApplicationUsageRepository();

}
