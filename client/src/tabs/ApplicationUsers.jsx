import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "../components/Entities.scss";
import {Loader, Tooltip} from "@surfnet/sds";
import {Entities} from "../components/Entities";
import {searchUsersByApplication} from "../api";
import UserIcon from "@surfnet/sds/icons/functional-icons/id-2.svg";
import "./ApplicationUsers.scss";
import {isEmpty, stopEvent} from "../utils/Utils";
import debounce from "lodash.debounce";
import {dateFromEpoch, shortDateFromEpoch} from "../utils/Date";
import {useNavigate} from "react-router-dom";
import {defaultPagination, pageCount} from "../utils/Pagination";
import {AUTHORITIES, isUserAllowed} from "../utils/UserRole";
import {useAppStore} from "../stores/AppStore";


export const ApplicationUsers = () => {

    const {user: currentUser} = useAppStore(state => state);

    const [paginationQueryParams, setPaginationQueryParams] = useState(defaultPagination());
    const [searching, setSearching] = useState(true);
    const [users, setUsers] = useState([]);
    const [totalElements, setTotalElements] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        searchUsersByApplication(paginationQueryParams)
            .then(page => {
                const results = page.content;
                results.forEach(user => user.endDate = user.roleSummaries
                    .reduce((acc, rs) => Math.min(rs.endDate || Number.MAX_VALUE, acc), Number.MAX_VALUE));
                results.forEach(user => user.roleSummaries
                    .sort((r1, r2) => (r1.endDate || Number.MAX_VALUE) - (r2.endDate || Number.MAX_VALUE)));
                setUsers(results);
                setSearching(false);
                setTotalElements(page.totalElements);
            });
    }, [paginationQueryParams]);

    const gotoRole = (e, roleId) => {
        stopEvent(e);
        navigate(`/roles/${roleId}`)
    }

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
                <Tooltip standalone={true}
                         children={<UserIcon/>}
                         tip={I18n.t("tooltips.userIcon",
                             {
                                 name: user.name,
                                 createdAt: dateFromEpoch(user.createdAt),
                                 lastActivity: dateFromEpoch(user.lastActivity)
                             })}/>
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
            mapper: user => <span>{user.schacHomeOrganization}</span>
        },
        {
            key: "endDate",
            header: I18n.t("users.roles"),
            toolTip: I18n.t("users.roleExpiryTooltip"),
            mapper: user => <ul key={user.id}>
                {user.roleSummaries.map((role, index) => <li key={`${user.id}_${index}`}>
                    <span className="role">
                        <a href="/#"
                           onClick={e => gotoRole(e, role.id)}>{role.roleName}</a>
                    </span>
                    <span className="authority">
                        <span className="inline">{`${I18n.t("users.authority")}: `}</span>
                        {I18n.t(`access.${role.authority}`)}
                    </span>
                    <span className="end-date">{role.endDate ?
                        <span className="inline">{I18n.t("roles.endDate")}<em>: {shortDateFromEpoch(role.endDate)}</em></span> :
                        <em> {I18n.t("roles.noEndDate")}</em>}
                    </span>
                </li>)}
            </ul>
        },
    ];

    return (
        <div className="mod-application-users">
            {searching && <Loader/>}
            <Entities entities={users}
                      modelName="users"
                      defaultSort="name"
                      columns={columns}
                      inputFocus={true}
                      totalElements={totalElements}
                      customSearch={search}
                      newLabel={I18n.t("invitations.newInvite")}
                      showNew={isUserAllowed(AUTHORITIES.INSTITUTION_ADMIN, currentUser)}
                      newEntityFunc={() => navigate(`/invitation/new?institution=true`)}
                      hideTitle={searching}
                      busy={searching}/>
        </div>
    );

}
