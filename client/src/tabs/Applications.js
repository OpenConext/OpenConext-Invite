import "./Applications.scss";
import React, {useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {Loader} from "@surfnet/sds";
import {applications, rolesByApplication} from "../api";
import {isEmpty, splitListSemantically, stopEvent} from "../utils/Utils";
import {AUTHORITIES, isUserAllowed} from "../utils/UserRole";
import {useAppStore} from "../stores/AppStore";
import {useNavigate} from "react-router-dom";
import {providerInfo} from "../utils/Manage";

/*
 * Show all roles with the manage information and a link to role detail for super admin
 */
const Applications = () => {
    const user = useAppStore(state => state.user);
    const navigate = useNavigate();

    const [loading, setLoading] = useState(false);
    const [roles, setRoles] = useState([]);

    useEffect(() => {
            Promise.all([applications(), rolesByApplication()])
                .then(res => {
                    const providers = res[0].providers;
                    const provisionings = res[0].provisionings;
                    res[1].forEach(role => {
                        role.logo = providerLogoById(role.manageId, providers);
                        role.provider = providerById(role.manageId, providers);
                        role.provisioning = provisioningsByProviderId(role.manageId, provisionings);
                    })
                    setRoles(res[1]);
                    setLoading(false);
                })
        },
        [])

    if (loading) {
        return <Loader/>
    }

    const openRole = (e, role) => {
        const id = role.isUserRole ? role.role.id : role.id;
        const path = `/roles/${id}`
        if (e.metaKey || e.ctrlKey) {
            window.open(path, '_blank');
        } else {
            stopEvent(e);
            navigate(path);
        }
    };

    const providerById = (manageId, allProviders) => {
        const provider = allProviders.find(provider => provider.id === manageId) || providerInfo(null);
        const organisation = provider["OrganizationName:en"];
        const organisationValue = isEmpty(organisation) ? "" : ` (${organisation})`;
        return `${provider["name:en"]}${organisationValue}`;
    }

    const providerLogoById = (manageId, allProviders) => {
        const provider = allProviders.find(provider => provider.id === manageId) || providerInfo(null);
        return provider.logo;
    }

    const provisioningsByProviderId = (manageId, allProvisionings) => {
        const providers = allProvisionings
            .filter(provisioning => provisioning.applications.some(app => app.id === manageId))
            .map(provider => `${provider["name:en"]} (${provider.provisioning_type})`);
        return splitListSemantically(providers, I18n.t("forms.and"));
    }

    const columns = [
        {
            key: "logo",
            header: "",
            nonSortable: true,
            mapper: role => <img src={role.logo} alt="logo"/>
        },
        {
            key: "name",
            header: I18n.t("roles.name"),
            mapper: role => role.name
        },
        {
            key: "provider",
            header: I18n.t("roles.manageMetaData"),
            mapper: role => role.provider
        },
        {
            key: "provisioning",
            header: I18n.t("roles.provisioning"),
            mapper: role => role.provisioning
        },

    ]

    return (
        <div className={"mod-applications"}>
            <Entities entities={roles}
                      modelName="applications"
                      defaultSort="name"
                      columns={columns}
                      hideTitle={true}
                      customNoEntities={I18n.t(`roles.noResults`)}
                      searchAttributes={["name", "provider", "provisioning"]}
                      rowLinkMapper={isUserAllowed(AUTHORITIES.INVITER, user) ? openRole : null}
                      inputFocus={true}>
            </Entities>

        </div>
    );

}

export default Applications;