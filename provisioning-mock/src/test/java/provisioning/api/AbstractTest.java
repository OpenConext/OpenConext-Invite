package provisioning.api;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import provisioning.repository.ProvisioningRepository;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected ProvisioningRepository provisioningRepository;

    @BeforeEach
    protected void beforeEach() {
        RestAssured.port = port;
        provisioningRepository.deleteAllInBatch();
    }


}
