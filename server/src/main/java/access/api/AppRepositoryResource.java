package access.api;

import access.repository.ApplicationRepository;
import access.repository.ApplicationUsageRepository;

public interface AppRepositoryResource {

    ApplicationRepository getApplicationRepository();

    ApplicationUsageRepository getApplicationUsageRepository();

}
