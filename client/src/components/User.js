import React, {useEffect, useRef, useState} from "react";
import "./User.scss";
import InputField from "./InputField";
import {dateFromEpoch} from "../utils/Date";
import {highestAuthority} from "../utils/UserRole";
import I18n from "../locale/I18n";
import Logo from "./Logo";
import {Card, CardType} from "@surfnet/sds";
import {isEmpty} from "../utils/Utils";
import {deriveRemoteApplicationAttributes} from "../utils/Manage";
import {ReactComponent as SearchIcon} from "@surfnet/sds/icons/functional-icons/search.svg";
import {MoreLessText} from "./MoreLessText";
import {RoleCard} from "./RoleCard";

export const User = ({user, other}) => {
    const searchRef = useRef();

    const [query, setQuery] = useState("");
    const [queryApplication, setQueryApplication] = useState("");

    if (user.institutionAdmin) {
        (user.applications || []).forEach(application => deriveRemoteApplicationAttributes(application, I18n.locale));
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

    const renderUserRole = (userRole, index) => {
        const role = userRole.role;
        return <RoleCard role={role} index={index} key={index}/>
    }

    const filterApplication = application => {
        if (isEmpty(queryApplication)) {
            return true;
        }
        const queryApplicationLower = queryApplication.toLowerCase();

        return application.organizationName.toLowerCase().indexOf(queryApplicationLower) > -1 ||
            application.name.toLowerCase().indexOf(queryApplicationLower) > -1
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
            <Card cardType={CardType.Big} children={children}/>
        );
    }

    user.highestAuthority = I18n.t(`access.${highestAuthority(user)}`);
    const attributes = [["name"], ["sub"], ["eduPersonPrincipalName"], ["schacHomeOrganization"], ["email"], ["highestAuthority"],
        ["lastActivity", true]];
    const filteredUserRoles = user.userRoles.filter(filterUserRole);
    const filteredApplications = (user.applications || []).filter(filterApplication);
    return (
        <section className={"user"}>
            {attributes.map((attr, index) => attribute(index, attr[0], attr[1]))}

            <h3 className={"title span-row "}>{I18n.t("users.roles")}</h3>
            {(isEmpty(user.userRoles) && user.superUser) &&
                <p className={"span-row "}>{I18n.t("users.noRolesInfo")}</p>}
            {(isEmpty(user.userRoles) && user.institutionAdmin) &&
                <p className={"span-row "}>{I18n.t("users.noRolesInstitutionAdmin")}</p>}
            {!isEmpty(user.userRoles) &&
                <>
                    <div className="roles-search span-row">
                        <p>
                            {I18n.t(`users.${other ? "rolesInfoOther" : "rolesInfo"}`, {name: user.name})}
                        </p>
                        {renderSearch(query, setQuery, I18n.t(`roles.searchPlaceHolder`, searchRef))}
                    </div>
                    {filteredUserRoles
                        .map((userRole, index) => renderUserRole(userRole, index))}
                    {filteredUserRoles.length === 0 &&
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
        </section>
    );
}