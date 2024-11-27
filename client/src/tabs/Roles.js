import "./Roles.scss";
import {useAppStore} from "../stores/AppStore";
import React, {useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {Button, ButtonSize, Chip, Loader, Tooltip} from "@surfnet/sds";
import {useNavigate} from "react-router-dom";
import {AUTHORITIES, highestAuthority, isUserAllowed, markAndFilterRoles} from "../utils/UserRole";
import {rolesByApplication} from "../api";
import {isEmpty, stopEvent} from "../utils/Utils";
import debounce from "lodash.debounce";
import {chipTypeForUserRole} from "../utils/Authority";
import {ReactComponent as VoidImage} from "../icons/undraw_void_-3-ggu.svg";
import {ReactComponent as AlertLogo} from "@surfnet/sds/icons/functional-icons/alert-circle.svg";
import DOMPurify from "dompurify";
import {defaultPagination, pageCount} from "../utils/Pagination";

export const Roles = () => {
    const {user, config} = useAppStore(state => state);
    const navigate = useNavigate();

    const [loading, setLoading] = useState(false);
    const [searching, setSearching] = useState(true);
    const [roles, setRoles] = useState([]);
    const [paginationQueryParams, setPaginationQueryParams] = useState(defaultPagination());
    const [totalElements, setTotalElements] = useState(0);

    useEffect(() => {
        if (isUserAllowed(AUTHORITIES.INSTITUTION_ADMIN, user)) {
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
                    //we need to avoid flickerings
                    setTimeout(() => setSearching(false), 75);
                    setLoading(false);
                })
        } else {
            const newRoles = markAndFilterRoles(
                user,
                [],
                I18n.locale,
                I18n.t("roles.multiple"),
                I18n.t("forms.and"),
                "name",
                false);
            setRoles(newRoles);
            setSearching(false);
            setLoading(false);
        }
    }, [user, paginationQueryParams]);// eslint-disable-line react-hooks/exhaustive-deps

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
        if (isEmpty(query) || query.trim().length > 2) {
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
                <Button txt={I18n.t("institutionAdmin.create")}
                        size={ButtonSize.Full}
                        onClick={() => navigate("/role/new")}/>
            </div>
        );
    }

    const columns = [
        {
            nonSortable: true,
            key: "logo",

            header: "",
            mapper: role => role.unknownInManage ? <div className="role-icon unknown-in-manage"><AlertLogo/></div> :
                <div className="role-icon">
                    {typeof role.logo === "string" ? <img src={role.logo} alt="logo"/> : role.logo}
                </div>
        },
        {
            nonSortable: true,
            key: "applicationName",
            header: I18n.t("roles.applicationName"),
            mapper: role => role.unknownInManage ?
                <span className="unknown-in-manage">{I18n.t("roles.unknownInManage")}
                    <Tooltip tip={I18n.t("roles.unknownInManageToolTip")}
                             standalone={true}
                             clickable={true}/>
                </span>
                :
                <span>{role.applicationName}</span>
        },
        {
            key: "name",
            header: I18n.t("roles.accessRole"),
            mapper: role => <span>{role.name}</span>
        },
        {
            key: "description",
            header: I18n.t("roles.description"),
            mapper: role => <span className={"cut-of-lines"}>{role.description}</span>
        },
        {
            nonSortable: true,
            key: "authority",
            header: I18n.t("roles.authority"),
            mapper: role => <Chip type={chipTypeForUserRole(role.authority)}
                                  label={role.isUserRole ? I18n.t(`access.${role.authority}`) :
                                      I18n.t("roles.noMember")}/>
        },
        {
            key: "userRoleCount",
            header: I18n.t("roles.userRoleCount"),
            mapper: role => role.userRoleCount
        }

    ];

    if (loading) {
        return <Loader/>
    }

    const isSuperUser = isUserAllowed(AUTHORITIES.SUPER_USER, user);
    const isManager = isUserAllowed(AUTHORITIES.INSTITUTION_ADMIN, user);
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
            {!isGuest && <Entities
                entities={isSuperUser ? roles : roles.filter(role => !(role.isUserRole && role.authority === "GUEST"))}
                modelName="roles"
                showNew={isManager}
                newLabel={I18n.t("roles.new")}
                newEntityPath={"/role/new"}
                defaultSort="name"
                columns={columns}
                searchAttributes={["name", "description", "applicationName"]}
                customNoEntities={I18n.t(`roles.noResults`)}
                loading={false}
                inputFocus={!searching}
                hideTitle={searching}
                customSearch={user.superUser ? search : null}
                totalElements={user.superUser ? totalElements : null}
                rowLinkMapper={isUserAllowed(AUTHORITIES.INVITER, user) ? openRole : null}
                rowClassNameResolver={entity => (entity.applications || []).length > 1 ? "multi-role" : ""}
                busy={searching}
            />}
        </div>
    );

}
