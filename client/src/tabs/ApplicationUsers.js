import React, {useEffect, useMemo, useState} from "react";
import I18n from "../locale/I18n";
import "../components/Entities.scss";
import {Loader, Tooltip} from "@surfnet/sds";
import {Entities} from "../components/Entities";
import {searchUsersByApplication} from "../api";
import {ReactComponent as UserIcon} from "@surfnet/sds/icons/functional-icons/id-2.svg";
import "./ApplicationUsers.scss";
import {isEmpty, stopEvent} from "../utils/Utils";
import debounce from "lodash.debounce";
import {dateFromEpoch, shortDateFromEpoch} from "../utils/Date";
import SearchSvg from "../icons/undraw_people_search_re_5rre.svg";
import {useNavigate} from "react-router-dom";


export const ApplicationUsers = () => {

    const [searching, setSearching] = useState(false);
    const [users, setUsers] = useState([]);
    const [totalElements, setTotalElements] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        return () => {
            delayedAutocomplete.cancel();
        };
    });

    const gotoRole = (e, roleId) => {
        stopEvent(e);
        navigate(`/roles/${roleId}`)
    }

    const search = query => {
        if (isEmpty(query) || query.trim().length > 2) {
            delayedAutocomplete(query);
        }
    };

    const delayedAutocomplete = useMemo(() => debounce(query => {
        setSearching(true);
        searchUsersByApplication(query)
            .then(page => {
                setInitial(false);
                const results = page.content;
                results.forEach(user => user.endDate = user.roleSummaries
                    .reduce((acc, rs) => Math.min(rs.endDate || Number.MAX_VALUE, acc), Number.MAX_VALUE));
                results.forEach(user => user.roleSummaries
                    .sort((r1, r2) => (r1.endDate || Number.MAX_VALUE) - (r2.endDate || Number.MAX_VALUE)));
                setUsers(results);
                setSearching(false);
            });
    }, 500), []);

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
            key: "schac_home_organisation",
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

    const countUsers = users.length;
    const hasEntities = countUsers > 0;
    let title = "";

    if (hasEntities) {
        title = I18n.t(`users.found`, {
            count: countUsers,
            plural: I18n.t(`users.${countUsers === 1 ? "singleUser" : "multipleUsers"}`)
        })
    }
    return (<div className="mod-application-users">
        {searching && <Loader/>}

        <Entities entities={users}
                  modelName="users"
                  defaultSort="name"
                  columns={columns}
                  title={title}
                  totalElements={totalElements}
                  inputFocus={true}
                  customSearch={search}
                  busy={searching}/>
    </div>)

}
