import "./Applications.scss";
import React, {useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {Loader} from "@surfnet/sds";
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

    const [loading, setLoading] = useState(false);
    const [applications, setApplications] = useState([]);

    useEffect(() => {
            if (!isUserAllowed(AUTHORITIES.INSTITUTION_ADMIN, user)) {
                navigate("/404");
                return;
            }
            Promise.all([allApplications(), rolesByApplication(true)])
                .then(res => {
                    const mergedApps = mergeProvidersProvisioningsRoles(res[0].providers, res[0].provisionings, res[1]);
                    setApplications(mergedApps);
                    setLoading(false);
                })
        },
        []) // eslint-disable-line react-hooks/exhaustive-deps

    if (loading) {
        return <Loader/>
    }

    const openRole = (e, role) => {
        const path = `/roles/${role.id}`
        if (e.metaKey || e.ctrlKey) {
            window.open(path, '_blank');
        } else {
            stopEvent(e);
            navigate(path);
        }
    };

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
            key: "type",
            header: I18n.t("applications.type"),
            mapper: application => I18n.t(`applications.types.${application.type}`)
        },
        {
            key: "organization",
            header: I18n.t("applications.organization"),
            mapper: application => application.organization
        },
        {
            key: "roles",
            header: I18n.t("applications.roles"),
            mapper: application => <ul>{application.roles.map((role, index) => <li key={index}>
                <a href="/" onClick={e => openRole(e, role)}>{role.name}</a>
            </li>)}</ul>
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
                      title={I18n.t("applications.applicationFound", {nbr: applications.length})}
                      customNoEntities={I18n.t(`applications.noResults`)}
                      searchAttributes={["name", "organization"]}
                      inputFocus={true}>
            </Entities>
        </div>
    );

}

export default Applications;