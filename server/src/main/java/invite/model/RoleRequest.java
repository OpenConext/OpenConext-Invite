package invite.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import invite.provision.scim.GroupURN;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RoleRequest implements Serializable{

    private Long id;

    @NotNull
    @NotBlank
    private String name;

    private String description;

    private Integer defaultExpiryDays;

    private boolean enforceEmailEquality;

    private boolean eduIDOnly;

    private boolean blockExpiryDate;

    private boolean overrideSettingsAllowed;

    private String organizationGUID;

    private String inviterDisplayName;

    private Set<ApplicationUsage> applicationUsages = new HashSet<>();

}
