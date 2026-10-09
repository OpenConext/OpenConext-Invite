import {Badge} from "@surfnet/curve-react";
import {InfoTooltip} from "../components/InfoTooltip";
import {IdentificationBadgeIcon as UserIcon, ChalkboardTeacherIcon as ImpersonateIcon} from "@phosphor-icons/react";
import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "../components/Entities.scss";
import {Entities} from "../components/Entities";
import {other, searchUsers} from "../api";
import "./Users.scss";
import {isEmpty, stopEvent} from "../utils/Utils";
import debounce from "lodash.debounce";
import {useNavigate} from "react-router";
import {useAppStore} from "../stores/AppStore";
import {dateFromEpoch, shortDateFromEpoch} from "../utils/Date";
import {AUTHORITIES, isUserAllowed} from "../utils/UserRole";
import {authorityForUserOverview, badgeVariantForUserRole} from "../utils/Authority";
import {defaultPagination, pageCount} from "../utils/Pagination";


export const Users = () => {

    const {user: currentUser, startImpersonation, setFlash} = useAppStore(state => state);

    const [searching, setSearching] = useState(true);
    const [paginationQueryParams, setPaginationQueryParams] = useState(defaultPagination());
    const [totalElements, setTotalElements] = useState(0);
    const [users, setUsers] = useState([]);
    const navigate = useNavigate();

    useEffect(() => {
            searchUsers(paginationQueryParams)
                .then(page => {
                    setUsers(page.content);
                    setTotalElements(page.totalElements);
                    setSearching(false);
                });
        },
        [paginationQueryParams]);

    const openUser = (e, user) => {
        const path = `/profile/${user.id}`
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

    const columns = [
        {
            nonSortable: true,
            key: "icon",
            header: "",
            mapper: user => <div className="member-icon">
                <InfoTooltip tip={I18n.t("tooltips.userIcon",
                             {
                                 name: user.name,
                                 createdAt: dateFromEpoch(user.createdAt, false),
                                 lastActivity: dateFromEpoch(user.lastActivity, false)
                             })}><UserIcon/></InfoTooltip>
            </div>
        },
        {
            key: "name",
            header: I18n.t("users.name_email"),
            mapper: user => (
                <div className="user-name-email">
                    <span className="name">{user.name}</span>
                    <span className="email">{user.email}</span>
                </div>)
        },
        {
            key: "schac_home_organization",
            header: I18n.t("users.schacHomeOrganization"),
            mapper: user => <span>{user.schac_home_organization}</span>
        },
        {
            key: "authority",
            header: I18n.t("users.highestAuthority"),
            mapper: user => {
                const authority = authorityForUserOverview(user);
                return <Badge variant={badgeVariantForUserRole(authority)}>{I18n.t(`access.${authority || "No member"}`)}</Badge>
            }
        },
        {
            key: "createdAt",
            header: I18n.t("users.createdAt"),
            mapper: user => shortDateFromEpoch(user.createdAt, false)
        },
        {
            key: "lastActivity",
            header: I18n.t("users.lastActivity"),
            mapper: user => shortDateFromEpoch(user.lastActivity, false)
        },
    ];
    const showImpersonation = currentUser && currentUser.superUser;

    const impersonate = user => {
        //First, fetch the entire other user otherwise we get undefined errors
        other(user.id).then(otherUser => {
            startImpersonation(otherUser);
            setFlash(I18n.t("impersonate.flash.startedImpersonation", {name: otherUser.name}));
            navigate("/", {replace: true});
        })
    }

    if (showImpersonation) {
        columns.push({
            nonSortable: true,
            key: "icon",
            hasLink: true,
            header: "",
            mapper: user => (currentUser.id !== user.id) ?
                <InfoTooltip tip={I18n.t("tooltips.impersonateIcon",
                             {
                                 name: user.name
                             })}><ImpersonateIcon className="impersonate"
                                                    onClick={() => impersonate(user)}/></InfoTooltip>
                : <Badge variant="default">{I18n.t("forms.you")}</Badge>
        })
    }
    return (
        <div className="mod-users">
            <Entities entities={users}
                      modelName="users"
                      defaultSort="name"
                      columns={columns}
                      newLabel={currentUser.superUser ? I18n.t("invitations.newInvite") : null}
                      showNew={isUserAllowed(AUTHORITIES.SUPER_USER, currentUser)}
                      newEntityFunc={() => navigate(`/invitation/new?maintainer=true`)}
                      hideTitle={searching}
                      customNoEntities={I18n.t(`users.noResults`)}
                      searchAttributes={["name", "email", "schacHomeOrganization"]}
                      customSearch={search}
                      rowLinkMapper={openUser}
                      totalElements={totalElements}
                      inputFocus={!searching}
                      busy={searching}/>
        </div>
    );

}
