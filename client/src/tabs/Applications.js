import "./Applications.scss";
import React, {useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {Loader} from "@surfnet/sds";
import {applications, rolesByApplication} from "../api";
import {splitListSemantically} from "../utils/Utils";

/*
 * Show all roles with the manage information and a link to role detail for super admin
 */
const Applications = () => {
    const [loading, setLoading] = useState(false);
    const [providers, setProviders] = useState([]);
    const [provisionings, setProvisionings] = useState([]);
    const [roles, setRoles] = useState([]);

    useEffect(() => {
            Promise.all([applications(), rolesByApplication()])
                .then(res => {
                    setProviders(res[0].providers);
                    setProvisionings(res[0].provisionings);
                    setRoles(res[1]);
                    setLoading(false);
                })
        },
        [])

    if (loading) {
        return <Loader/>
    }

    const providerById = manageId => {
        const provider = providers.find(provider => provider.id === manageId);
        const metaData = provider.data.metaDataFields;
        return `${metaData["name:en"]} (${metaData["OrganizationName:en"]})`;
    }

    const providerLogoById = manageId => {
        const provider = providers.find(provider => provider.id === manageId);
        return provider.data.metaDataFields["logo:0:url"];
    }

    const provisioningsByProviderId = manageId => {
        const provs = provisionings.filter(provisioning => provisioning.data.applications.some(app => app.id === manageId))
            .map(prov => {
                const metaData = prov.data.metaDataFields;
                return `${metaData["name:en"]} (${metaData.provisioning_type})`
            });
        return splitListSemantically(provs, I18n.t("forms.and"));
    }

    const columns = [
        {
            key: "logo",
            header: "",
            sortable: false,
            mapper: role => <img src={providerLogoById(role.manageId)} alt="logo"/>
        },
        {
            key: "name",
            header: I18n.t("roles.name"),
            mapper: role => <span>{role.name}</span>
        },
        {
            key: "provider",
            sortable: false,
            header: I18n.t("roles.manageMetaData"),
            mapper: role => <span>{providerById(role.manageId)}</span>
        },
        {
            key: "provisioning",
            sortable: false,
            header: I18n.t("roles.provisioning"),
            mapper: role => <span>{provisioningsByProviderId(role.manageId)}</span>
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
                      searchAttributes={["name"]}
                      inputFocus={true}>
            </Entities>

        </div>
    );

}

export default Applications;