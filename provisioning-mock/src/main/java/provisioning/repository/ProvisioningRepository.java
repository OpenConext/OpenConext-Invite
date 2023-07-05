package provisioning.repository;

import access.provision.ProvisioningType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import provisioning.model.Provisioning;

import java.util.List;

@Repository
public interface ProvisioningRepository extends JpaRepository<Provisioning, Long> {

    List<Provisioning> findProvisioningByProvisioningType(ProvisioningType provisioningType);
}
