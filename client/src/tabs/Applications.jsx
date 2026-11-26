import "./Applications.scss";
import React, {useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {applicationsFromManage} from "../api";
import {stopEvent} from "../utils/Utils";
import {AUTHORITIES, isUserAllowed} from "../utils/UserRole";
import {useAppStore} from "../stores/AppStore";
import {useNavigate} from "react-router";
import {mergeProvidersProvisioningsRoles} from "../utils/Manage";
import {isEmpty} from "../utils/Utils";

const localizedValue = (obj, attribute) => I18n.locale === "en" ?
    obj[`${attribute}:en`] || obj[`${attribute}:nl`] : obj[`${attribute}:nl`] || obj[`${attribute}:en`];

/*
 * Convert the applicationMaps of a role - no extra server round-trip needed - to the structure used in this table
 */
const applicationsFromRole = role => (role.applicationMaps || [])
    .filter(app => !isEmpty(app))
    .map(app => ({
        id: app.id,
        logo: app.logo,
        name: app.unknown ? I18n.t("roles.unknownInManage") : localizedValue(app, "name"),
        type: app.type,
        organization: localizedValue(app, "OrganizationName"),
        url: app.url,
        unknown: app.unknown,
    }));

/*
 * Show all manage applications, or - if a role is passed - only the applications of that role
 */
const Applications = ({role}) => {
    const user = useAppStore(state => state.user);
    const navigate = useNavigate();

    const [searching, setSearching] = useState(!role);
    const [manageApplications, setApplications] = useState([]);
    const applications = role ? applicationsFromRole(role) : manageApplications;

    const canOpenApplication = isUserAllowed(AUTHORITIES.APPLICATION_MANAGER, user);

    useEffect(() => {
            if (role) {
                return;
            }
            if (!isUserAllowed(AUTHORITIES.APPLICATION_MANAGER, user)) {
                navigate("/404");
                return;
            }
            applicationsFromManage()
                .then(res => {
                    const mergedApps = mergeProvidersProvisioningsRoles(
                        res.providers, res.provisionings);
                    setApplications(mergedApps);
                    setSearching(false);
                })
        },
        [role]) // eslint-disable-line react-hooks/exhaustive-deps

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
            mapper: application => (canOpenApplication && !application.unknown) ?
                <a onClick={e => openApplication(e, application)} href="/#">{application.name}</a> :
                <span className={application.unknown ? "unknown-in-manage" : ""}>{application.name}</span>
        },
        {
            key: "organization",
            header: I18n.t("applications.organization"),
            mapper: application => application.organization
        },
        ...(role ? [] : [{
            key: "roleCount",
            header: I18n.t("applications.roles"),
            mapper: application => application.roleCount
        },
        {
            key: "provisionings",
            header: I18n.t("applications.provisionings"),
            mapper: application => <ul>{application.provisionings.map((prov, index) => <li key={index}>
                {`${prov.name} (${prov.provisioningType})`}
            </li>)}</ul>
        }]),
    ]

    return (
        <div className={"mod-applications"}>
            <Entities entities={applications}
                      modelName="applications"
                      defaultSort="name"
                      columns={columns}
                      busy={searching}
                      hideHeading={true}
                      hideTitle={searching}
                      title={I18n.t("applications.applicationFound", {nbr: applications.length})}
                      customNoEntities={I18n.t(`applications.noResults`)}
                      searchAttributes={["name", "organization"]}
                      inputFocus={!role}>
            </Entities>
        </div>
    );

}

export default Applications;