import {Badge, Button, Checkbox} from "@surfnet/curve-react";
import {InfoTooltip} from "../components/InfoTooltip";
import {WarningCircleIcon as AlertLogo} from "@phosphor-icons/react";
import "./Roles.scss";
import {useAppStore} from "../stores/AppStore";
import React, {useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {useNavigate} from "react-router";
import {AUTHORITIES, highestAuthority, isUserAllowed, markAndFilterRoles} from "../utils/UserRole";
import {rolesByApplication} from "../api";
import {isEmpty, stopEvent} from "../utils/Utils";
import debounce from "lodash.debounce";
import VoidImage from "../icons/undraw_void_-3-ggu.svg";
import DOMPurify from "dompurify";
import {defaultPagination, pageCount} from "../utils/Pagination";

export const Roles = () => {
    const {user, config} = useAppStore(state => state);
    const navigate = useNavigate();

    const [searching, setSearching] = useState(isUserAllowed(AUTHORITIES.INSTITUTION_ADMIN, user));
    const [roles, setRoles] = useState([]);
    const [paginationQueryParams, setPaginationQueryParams] = useState(defaultPagination());
    const [totalElements, setTotalElements] = useState(0);

    useEffect(() => {
        if (isUserAllowed(AUTHORITIES.APPLICATION_MANAGER, user)) {
            rolesByApplication(false, paginationQueryParams)
                .then(page => {
                    const newRoles = markAndFilterRoles(
                        user,
                        page.content,
                        I18n.locale,
                        I18n.t("roles.multiple"),
                        I18n.t("forms.and"),
                        paginationQueryParams.sort,
                        paginationQueryParams.sortDirection === "DESC");
                    setRoles(newRoles);
                    setTotalElements(page.totalElements);
                    setSearching(false);
                })
        } else {
            Promise.resolve(markAndFilterRoles(
                user,
                [],
                I18n.locale,
                I18n.t("roles.multiple"),
                I18n.t("forms.and"),
                "name",
                false))
                .then(res => setRoles(res))
        }
    }, [user, paginationQueryParams]);

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

    const search = (query, sorted, reverse, page) => {
        const paginationQueryParamsChanged = sorted !== paginationQueryParams.sort || reverse !== paginationQueryParams.sortDirection ||
            page !== paginationQueryParams.pageNumber;
        if ((!isEmpty(query) && query.trim().length > 2) || paginationQueryParamsChanged) {
            delayedAutocomplete(query, sorted, reverse, page);
        }
    };

    const delayedAutocomplete = debounce((query, sorted, reverse, page) => {
        setSearching(true);
        //this will trigger a new search
        setPaginationQueryParams({
            query: query,
            pageNumber: page,
            pageSize: pageCount,
            sort: sorted,
            sortDirection: reverse ? "DESC" : "ASC"
        })
    }, 375);

    const noRolesInstitutionAdmin = () => {
        const institution = user.institution;
        const name = institution[`name:${I18n.locale}`] || institution["name:en"];
        const logo = institution["logo"]
        return (
            <div className="institution-admin-welcome">
                {logo ? <img src={logo} alt="logo"/> : <VoidImage/>}
                <p>{I18n.t("institutionAdmin.welcome", {name: name})}</p>
                <Button className="w-full" onClick={() => navigate("/role/new")}>{I18n.t("institutionAdmin.create")}</Button>
            </div>
        );
    }

    const showCrm = user.superUser || user.institutionAdmin;

    const maxApplicationBadges = 2;
    const applicationBadges = role => {
        if (role.unknownInManage) {
            return <span className="unknown-in-manage"><AlertLogo/>{I18n.t("roles.unknownInManage")}
                <InfoTooltip tip={I18n.t("roles.unknownInManageToolTip")}/>
            </span>;
        }
        const names = [...new Set((role.applicationMaps || [])
            .filter(app => !isEmpty(app))
            .map(app => app[`name:${I18n.locale}`] || app["name:en"]))];
        return (
            <div className="application-badges">
                {names.slice(0, maxApplicationBadges).map(name => <Badge key={name} variant="outline">{name}</Badge>)}
                {names.length > maxApplicationBadges && <span className="more">+{names.length - maxApplicationBadges}</span>}
            </div>
        );
    }

    const columns = [
        {
            key: "name",
            header: I18n.t("roles.accessRole"),
            mapper: role => <span className="role-name">{role.name}</span>
        },
        {
            key: "description",
            header: I18n.t("roles.description"),
            mapper: role => <span className={"cut-of-lines"}>{role.description}</span>
        },
        {
            nonSortable: true,
            key: "applicationName",
            header: I18n.t("roles.applications"),
            mapper: applicationBadges
        },
        showCrm ?
            {
                key: "crm",
                nonSortable: true,
                header: I18n.t("roles.isCrm"),
                mapper: role => <div className="crm-check"><Checkbox checked={!isEmpty(role.crmRoleId)} disabled={true}/></div>
            } : null,
        {
            key: "userRoleCount",
            header: I18n.t("roles.userRoleCount"),
            mapper: role => role.userRoleCount
        }
    ].filter(tab => tab !== null);

    const isSuperUser = isUserAllowed(AUTHORITIES.SUPER_USER, user);
    const isAllowedToCreateNewRole = isUserAllowed(AUTHORITIES.APPLICATION_MANAGER, user);
    const isInstitutionAdmin = highestAuthority(user) === AUTHORITIES.INSTITUTION_ADMIN;
    const isGuest = highestAuthority(user) === AUTHORITIES.GUEST;
    if (isInstitutionAdmin && !isEmpty(user.institution) && roles.length === 0 && !searching) {
        return (
            <div className={"mod-roles"}>
                {noRolesInstitutionAdmin()}
            </div>
        )
    }

    return (
        <div className={"mod-roles"}>
            {(isGuest && !user.institutionAdmin) && <p className={"guest-only"}
                                                       dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("users.guestRoleOnly", {welcomeUrl: config.welcomeUrl}))}}/>}
            {(isGuest && user.institutionAdmin) && <p className={"guest-only"}
                                                      dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("users.noRolesNoApplicationsInstitutionAdmin"))}}/>}
            {!isGuest &&
                <Entities
                    entities={isSuperUser ? roles : roles.filter(role => !(role.isUserRole && role.authority === "GUEST"))}
                    modelName="roles"
                    showNew={isAllowedToCreateNewRole}
                    newLabel={I18n.t("roles.new")}
                    newEntityPath={"/role/new"}
                    defaultSort="name"
                    columns={columns}
                    searchAttributes={["name", "description", "applicationName"]}
                    customNoEntities={I18n.t(`roles.noResults`)}
                    loading={false}
                    inputFocus={!searching}
                    hideHeading={true}
                    hideTitle={searching}
                    customSearch={user.superUser ? search : null}
                    totalElements={user.superUser ? totalElements : null}
                    rowLinkMapper={isUserAllowed(AUTHORITIES.INVITER, user) ? openRole : null}
                    busy={searching}
                />}
        </div>
    );

}
