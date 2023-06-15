import React, {useState} from "react";
import I18n from "../locale/I18n";
import "../components/Entities.scss";
import {Loader, Tooltip} from "@surfnet/sds";
import {Entities} from "../components/Entities";
import {searchUsers} from "../api";
import {ReactComponent as UserIcon} from "@surfnet/sds/icons/functional-icons/id-2.svg";
import "./Users.scss";
import {isEmpty, stopEvent} from "../utils/Utils";
import debounce from "lodash.debounce";
import {ReactComponent as ImpersonateIcon} from "@surfnet/sds/icons/illustrative-icons/presentation-amphitheater.svg";
import {useNavigate} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import {dateFromEpoch} from "../utils/Date";
import {highestAuthority} from "../utils/UserRole";


export const Users = () => {

    const {user: currentUser, startImpersonation, setFlash} = useAppStore(state => state);

    const [searching, setSearching] = useState(false);
    const [users, setUsers] = useState([]);
    const [moreToShow, setMoreToShow] = useState(false);
    const [noResults, setNoResults] = useState(false);
    const navigate = useNavigate();

    const openUser = (e, user) => {
        const path = `/profile/${user.id}`
        if (e.metaKey || e.ctrlKey) {
            window.open(path, '_blank');
        } else {
            stopEvent(e);
            navigate(path);
        }
    };

    const search = query => {
        if (!isEmpty(query) && query.trim().length > 2) {
            setSearching(true);
            delayedAutocomplete(query);
        }
        if (isEmpty(query)) {
            setSearching(false);
            setMoreToShow(false);
            setNoResults(false);
            setUsers([]);
        }
    };

    const delayedAutocomplete = debounce(query => {
        searchUsers(query)
            .then(results => {
                results.forEach(user => user.highestAuthority = highestAuthority(user));
                setUsers(results);
                setMoreToShow(results.length === 15);
                setNoResults(results.length === 0);
                setSearching(false);
            });
    }, 500);

    const moreResultsAvailable = () => {
        return (
            <div className="more-results-available">
                <span>{I18n.t("users.moreResults")}</span>
            </div>)
    }

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
            key: "highestAuthority",
            header: I18n.t("users.highestAuthority"),
            mapper: user => <span>{user.highestAuthority}</span>
        },
        {
            key: "sub",
            header: I18n.t("users.sub"),
            mapper: user => user.sub
        }];
    const showImpersonation = currentUser && currentUser.superUser;

    const impersonate = user => {
        startImpersonation(user);
        setFlash(I18n.t("impersonate.flash.startedImpersonation", {name: user.name}));
        const path = encodeURIComponent(window.location.pathname);
        navigate(`/refresh-route/${path}`, {replace: true});
    }

    if (showImpersonation) {
        columns.push({
            nonSortable: true,
            key: "icon",
            hasLink: true,
            header: "",
            mapper: user => (currentUser.id !== user.id) ?
                <Tooltip standalone={true}
                         children={<ImpersonateIcon className="impersonate"
                                                    onClick={() => impersonate(user)}/>}
                         tip={I18n.t("tooltips.impersonateIcon",
                             {
                                 name: user.name
                             })}/>
                : null
        })
    }
    const countUsers = users.length;
    const hasEntities = countUsers > 0;
    let title = "";

    if (hasEntities) {
        title = I18n.t(`users.found`, {
            count: countUsers,
            plural: I18n.t(`users.${countUsers === 1 ? "singleUser" : "multipleUsers"}`)
        })
    }
    return (<div className="mod-users">
        {searching && <Loader/>}

        <Entities entities={users}
                  modelName="users"
                  defaultSort="name"
                  filters={moreToShow && moreResultsAvailable()}
                  columns={columns}
                  title={title}
                  hideTitle={!hasEntities || noResults}
                  customNoEntities={I18n.t(`users.noResults`)}
                  loading={false}
                  inputFocus={true}
                  customSearch={search}
                  rowLinkMapper={openUser}
                  busy={searching}>
        </Entities>
    </div>)

}
