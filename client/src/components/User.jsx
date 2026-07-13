import React, {useEffect, useRef, useState} from "react";
import "./User.scss";
import InputField from "./InputField";
import {dateFromEpoch} from "../utils/Date";
import {AUTHORITIES, highestAuthority} from "../utils/UserRole";
import I18n from "../locale/I18n";
import Logo from "./Logo";
import {Button, ButtonType, Card, CardType} from "@surfnet/sds";
import {isEmpty} from "../utils/Utils";
import {deriveRemoteApplicationAttributes, reduceApplicationFromUserRoles} from "../utils/Manage";
import SearchIcon from "@surfnet/sds/icons/functional-icons/search.svg";
import {MoreLessText} from "./MoreLessText";
import {RoleCard} from "./RoleCard";
import DOMPurify from "dompurify";
import ConfirmationDialog from "./ConfirmationDialog";
import {deleteUser} from "../api";
import {useNavigate} from "react-router-dom";
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
        return (
            <InputField noInput={true}
                        key={index}
                        disabled={true}
                        value={attr ? (isDate ? dateFromEpoch(attr) : attr) : "-"}
                        name={I18n.t(`users.${name}`)}/>
        )
    }

    const renderSearch = (value, valueSetter, placeholder, valueReference) => {
        return (
            <div className={`search standalone`}>
                <div className={"sds--text-field sds--text-field--has-icon"}>
                    <div className="sds--text-field--shape">
                        <div className="sds--text-field--input-and-icon">
                            <input className={"sds--text-field--input"}
                                   type="search"
                                   onChange={e => valueSetter(e.target.value)}
                                   value={value}
                                   ref={valueReference}
                                   placeholder={placeholder}/>
                            <span className="sds--text-field--icon">
                                    <SearchIcon/>
                                </span>
                        </div>
                    </div>
                </div>
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

    const renderRoleCard = (application, index) => {
        return (
            <RoleCard
                application={application}
                index={index}/>
        )
    }

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
            <Card key={index} cardType={CardType.Big} children={children}/>
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
        ["lastActivity", true], ["organizationGUID"]];
    const filteredUserRoles = user.userRoles
        .filter(filterUserRole)
        .filter(role => role.authority !== AUTHORITIES.GUEST || currentUser.superUser);
    const filteredApplications = (user.applications || []).filter(app => filterApplication(app, queryApplication));
    const filteredUserApplications = (user.userApplications || [])
        .map(app => app.applicationMap).filter(app => filterApplication(app, queryUserApplication));
    const allCardApplications = reduceApplicationFromUserRoles(filteredUserRoles, I18n.locale);

    const hasRoles = !isEmpty(user.userRoles.filter(role => role.authority !== AUTHORITIES.GUEST || currentUser.superUser))

    return (
        <section className={"user"}>
            {confirmationOpen && <ConfirmationDialog isOpen={confirmationOpen}
                                                     cancel={confirmation.cancel}
                                                     confirm={confirmation.action}
                                                     question={confirmation.question}/>}
            {attributes.map((attr, index) => attribute(index, attr[0], attr[1]))}
            {(currentUser.superUser && other && currentUser.id !== user.id) &&
                <div className="span-row">
                    <Button type={ButtonType.Delete}
                            onClick={() => doDeleteUser(true)}/>
                </div>
            }
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
                    <div className="roles-search span-row">
                        <p>
                            {I18n.t(`users.${other ? "rolesInfoOther" : "rolesInfo"}`, {name: user.name})}
                        </p>
                        {renderSearch(query, setQuery, I18n.t(`roles.searchPlaceHolder`), searchRef)}
                    </div>
                    {allCardApplications
                        .map((application, index) => renderRoleCard(application, index))}
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
                            {I18n.t(`users.${other ? "applicationsInfoOther" : "applicationsInfo"}`, {name: user.name})}
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