import {Button, Card, CardContent} from "@surfnet/curve-react";
import {TrashIcon, UserCircleIcon} from "@phosphor-icons/react";
import {SearchField} from "./SearchField";
import React, {useEffect, useRef, useState} from "react";
import "./User.scss";
import {dateFromEpoch} from "../utils/Date";
import {AUTHORITIES, highestAuthority} from "../utils/UserRole";
import I18n from "../locale/I18n";
import Logo from "./Logo";
import {isEmpty} from "../utils/Utils";
import {deriveRemoteApplicationAttributes} from "../utils/Manage";
import {MoreLessText} from "./MoreLessText";
import {RoleCard} from "./RoleCard";
import DOMPurify from "dompurify";
import ConfirmationDialog from "./ConfirmationDialog";
import {deleteUser} from "../api";
import {useNavigate} from "react-router";
import {useAppStore} from "../stores/AppStore";

export const User = ({user, other, config, currentUser, otherInstitutionAdmins}) => {
    const navigate = useNavigate();
    const {setFlash} = useAppStore(state => state);
    const searchRef = useRef();
    const [query, setQuery] = useState("");
    const [queryApplication, setQueryApplication] = useState("");
    const [queryUserApplication, setQueryUserApplication] = useState("");
    const [confirmation, setConfirmation] = useState({});
    const [confirmationOpen, setConfirmationOpen] = useState(false);

    if (user.institutionAdmin) {
        (user.applications || []).forEach(application => deriveRemoteApplicationAttributes(application, I18n.locale));
    }
    if (!isEmpty(user.userApplications)) {
        user.userApplications.forEach(application => deriveRemoteApplicationAttributes(application.applicationMap, I18n.locale));
    }

    useEffect(() => {
        if (searchRef && searchRef.current) {
            searchRef.current.focus();
        }
    }, [searchRef, user])

    const attribute = (index, name, isDate = false) => {
        const attr = user[name];
        return renderAttribute(index, I18n.t(`users.${name}`), attr ? (isDate ? dateFromEpoch(attr) : attr) : "-");
    }

    const renderAttribute = (index, label, value) => (
        <div className="attribute" key={index}>
            <span className="attribute-label">{label}</span>
            <span className="attribute-value">{value}</span>
        </div>
    );

    const renderSearch = (value, valueSetter, placeholder, valueReference) => {
        return (
            <div className={`search standalone`}>
                <SearchField inputRef={valueReference}
                             onChange={e => valueSetter(e.target.value)}
                             value={value}
                             placeholder={placeholder}/>
            </div>
        )
    };

    const filterUserRole = userRole => {
        if (isEmpty(query)) {
            return true;
        }
        const queryLower = query.toLowerCase();
        const role = userRole.role;
        return role.name.toLowerCase().indexOf(queryLower) > -1 ||
            role.description.toLowerCase().indexOf(queryLower) > -1
    };

    const filterApplication = (application, queryString) => {
        if (isEmpty(queryString)) {
            return true;
        }
        const queryApplicationLower = queryString.toLowerCase();

        return (application.organizationName || "").toLowerCase().indexOf(queryApplicationLower) > -1 ||
            (application.name || "").toLowerCase().indexOf(queryApplicationLower) > -1
    };

    const renderApplication = (application, index) => {
        const logo = application.logo;
        const children =
            <div key={index} className={"user-role"}>
                <Logo src={logo} alt={"provider"} className={"provider"}/>
                <section className={"user-role-info"}>
                    <h3>{application.name}</h3>
                    <MoreLessText txt={application.organizationName}/>
                </section>
            </div>;
        return (
            <Card key={index}><CardContent>{children}</CardContent></Card>
        );
    }

    const doDeleteUser = confirmation => {
        if (confirmation) {
            setConfirmation({
                cancel: () => setConfirmationOpen(false),
                action: () => doDeleteUser(false),
                warning: true,
                question: I18n.t("users.deleteConfirmation", {name: user.name}),
            });
            setConfirmationOpen(true);
        } else {
            deleteUser(user.id).then(() => {
                setConfirmationOpen(false);
                setConfirmation({});
                navigate("/home/users");
                setFlash(I18n.t("users.deleteFlash", {name: user.name}));
            })
        }
    }

    user.highestAuthority = I18n.t(`access.${highestAuthority(user, false)}`);
    const attributes = [["name"], ["sub"], ["eduPersonPrincipalName"], ["schacHomeOrganization"], ["email"], ["highestAuthority"],
        ["lastActivity", true]];
    const filteredUserRoles = user.userRoles
        .filter(filterUserRole)
        .filter(role => role.authority !== AUTHORITIES.GUEST || currentUser.superUser);
    const filteredApplications = (user.applications || []).filter(app => filterApplication(app, queryApplication));
    const filteredUserApplications = (user.userApplications || [])
        .map(app => app.applicationMap).filter(app => filterApplication(app, queryUserApplication));

    const hasRoles = !isEmpty(user.userRoles.filter(role => role.authority !== AUTHORITIES.GUEST || currentUser.superUser))

    return (
        <section className={"user"}>
            {confirmationOpen && <ConfirmationDialog isOpen={confirmationOpen}
                                                     cancel={confirmation.cancel}
                                                     confirm={confirmation.action}
                                                     question={confirmation.question}/>}
            <div className="user-header">
                <div className="user-identity">
                    <UserCircleIcon className="user-icon" size={48}/>
                    <div className="user-name-email">
                        <h1>{user.name}</h1>
                        <p>{user.email}</p>
                    </div>
                </div>
                {(currentUser.superUser && other && currentUser.id !== user.id) &&
                    <Button variant="outline" onClick={() => doDeleteUser(true)}>
                        <TrashIcon/>{I18n.t("users.deleteUser")}
                    </Button>
                }
            </div>
            <div className="attributes">
                {attributes.map((attr, index) => attribute(index, attr[0], attr[1]))}
                {(currentUser.superUser && user.crmContactId) &&
                    renderAttribute("crmContactId", I18n.t("users.crmContactId"), user.crmContactId)}
                {attribute(attributes.length, "organizationGUID")}
                {(currentUser.superUser && user.crmOrganisation) &&
                    renderAttribute("crmOrganisation", I18n.t("users.crmOrganisation"),
                        `${user.crmOrganisation.crmOrganisationId} (${user.crmOrganisation.crmOrganisationAbbrevation})`)}
            </div>
            {user.institutionAdmin && <div>
                <p className="label">{isEmpty(otherInstitutionAdmins) ? I18n.t("users.onlyInstitutionAdmins") :
                    I18n.t("users.institutionAdmins",)}</p>
                {!isEmpty(otherInstitutionAdmins) && <ul className="admins">
                    {otherInstitutionAdmins.map((admin, index) => <li key={index}>
                        <span>{`${admin.name} - ${admin.email}`}</span>
                    </li>)}
                </ul>}
            </div>}
            <h3 className={"title span-row "}>{I18n.t("users.roles")}</h3>
            {(highestAuthority(user, false) === AUTHORITIES.GUEST && !other) &&
                <p className={"span-row"}
                   dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("users.guestRoleOnly", {welcomeUrl: config.welcomeUrl}))}}/>}
            {(!hasRoles && !isEmpty(user.userApplications)) &&
                <p className={"span-row "}>{I18n.t(`users.noRolesInfoApplicationManager${other ? "Other" : ""}`, {name: user.name})}</p>}
            {(!hasRoles && user.superUser) &&
                <p className={"span-row "}>{I18n.t("users.noRolesInfo")}</p>}
            {(!hasRoles && user.institutionAdmin) &&
                <p className={"span-row "}>{I18n.t(`users.noRolesInstitutionAdmin${other ? "Other" : ""}`, {name: user.name})}</p>}
            {(hasRoles) &&
                <>
                    {filteredUserRoles.length > 3 &&
                        <div className="roles-search span-row">
                            {renderSearch(query, setQuery, I18n.t(`roles.searchPlaceHolder`), searchRef)}
                        </div>}
                    <div className="role-cards span-row">
                        {filteredUserRoles.map((userRole, index) =>
                            <RoleCard key={index} userRole={userRole}/>)}
                    </div>
                    {(filteredUserRoles.length === 0 && hasRoles) &&
                        <p>{I18n.t(`users.noRolesFound`)}</p>}
                </>}
            {(!isEmpty(user.applications) && user.institutionAdmin) &&
                <>
                    <div className="roles-search span-row">
                        <p>
                            {I18n.t(`users.${other ? "applicationsInfoOther" : "applicationsInfo"}`, {name: user.name})}
                        </p>
                        {renderSearch(queryApplication, setQueryApplication, I18n.t(`users.applicationsSearchPlaceHolder`))}
                    </div>
                    {filteredApplications
                        .map((application, index) => renderApplication(application, index))}
                    {filteredApplications.length === 0 &&
                        <p>{I18n.t(`users.noApplicationsFound`)}</p>}
                </>}
            {!isEmpty(user.userApplications) &&
                <>
                    <h3 className={"title span-row "}>{I18n.t("users.applications")}</h3>
                    <div className="roles-search span-row">
                        <p>
                            {I18n.t(`users.${other ? "roleCreatorInfoOther" : "roleCreatorInfo"}`, {name: user.name})}
                        </p>
                        {renderSearch(queryUserApplication, setQueryUserApplication, I18n.t(`users.applicationsSearchPlaceHolder`))}
                    </div>
                    {filteredUserApplications
                        .map((application, index) => renderApplication(application, index))}
                    {filteredUserApplications.length === 0 &&
                        <p>{I18n.t(`users.noApplicationsFound`)}</p>}
                </>}
        </section>
    );
}