import "./Applications.scss";
import React, {useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {allApplications, rolesByApplication} from "../api";
import {stopEvent} from "../utils/Utils";
import {AUTHORITIES, isUserAllowed} from "../utils/UserRole";
import {useAppStore} from "../stores/AppStore";
import {useNavigate} from "react-router-dom";
import {mergeProvidersProvisioningsRoles} from "../utils/Manage";

/*
 * Show all manage applications
 */
const Applications = () => {
    const user = useAppStore(state => state.user);
    const navigate = useNavigate();

    const [searching, setSearching] = useState(true);
    const [applications, setApplications] = useState([]);

    useEffect(() => {
            if (!isUserAllowed(AUTHORITIES.INSTITUTION_ADMIN, user)) {
                navigate("/404");
                return;
            }
            allApplications()
                .then(res => {
                    const mergedApps = mergeProvidersProvisioningsRoles(
                        res.providers, res.provisionings);
                    setApplications(mergedApps);
                    setSearching(false);
                })
        },
        []) // eslint-disable-line react-hooks/exhaustive-deps

    const openApplication = (e, application) => {
        const path = `/applications/${application.id}`
        if (e.metaKey || e.ctrlKey) {
            window.open(path, '_blank');
        } else {
            stopEvent(e);
            navigate(path);
        }
    };

    const columns = [
        {
            key: "logo",
            header: "",
            nonSortable: true,
            mapper: application => <div className="role-icon">
                {typeof application.logo === "string" ? <img src={application.logo} alt="logo"/> : application.logo}
            </div>
        },
        {
            key: "name",
            header: I18n.t("applications.name"),
            mapper: application =>
                <a onClick={e => openApplication(e, application)} href="/#">{application.name}</a>
        },
        {
            key: "organization",
            header: I18n.t("applications.organization"),
            mapper: application => application.organization
        },
        {
            key: "roles",
            header: I18n.t("applications.roles"),
            mapper: application => application.roleCount
        },
        {
            key: "provisionings",
            header: I18n.t("applications.provisionings"),
            mapper: application => <ul>{application.provisionings.map((prov, index) => <li key={index}>
                {`${prov.name} (${prov.provisioningType})`}
            </li>)}</ul>
        },
    ]

    return (
        <div className={"mod-applications"}>
            <Entities entities={applications}
                      modelName="applications"
                      defaultSort="name"
                      columns={columns}
                      busy={searching}
                      hideTitle={searching}
                      title={I18n.t("applications.applicationFound", {nbr: applications.length})}
                      customNoEntities={I18n.t(`applications.noResults`)}
                      searchAttributes={["name", "organization"]}
                      inputFocus={true}>
            </Entities>
        </div>
    );

}

export default Applications;