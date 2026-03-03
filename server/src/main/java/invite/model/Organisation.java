package invite.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "organisations")
@NoArgsConstructor
@Getter
@Setter
public class Organisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "crm_organisation_id")
    private String crmOrganisationId;

    @Column(name = "crm_organisation_name")
    private String crmOrganisationName;

    @Column(name = "crm_organisation_abbrevation")
    private String crmOrganisationAbbrevation;

}
