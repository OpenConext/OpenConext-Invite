import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "../components/Entities.scss";
import {Loader, Tooltip} from "@surfnet/sds";
import {Entities} from "../components/Entities";
import {searchUsers} from "../api";
import {ReactComponent as UserIcon} from "@surfnet/sds/icons/functional-icons/id-2.svg";
import {ReactComponent as InviteIcon} from "@surfnet/sds/icons/functional-icons/id-2.svg";
import "./Users.scss";
import {UserColumn} from "../components/UserColumn";
import {isEmpty, stopEvent} from "../utils/Utils";
import debounce from "lodash.debounce";
import {ReactComponent as ImpersonateIcon} from "@surfnet/sds/icons/illustrative-icons/presentation-amphitheater.svg";
import {useNavigate} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";


export const Users = () => {

    const {user: currentUser, startImpersonation, setFlash} = useAppStore(state => state);

    const [searching, setSearching] = useState(false);
    const [users, setUsers] = useState([]);
    const [moreToShow, setMoreToShow] = useState(false);
    const [noResults, setNoResults] = useState(false);
    const navigate = useNavigate();

    const openUser = user => e => {
        const path = `/users/${user.id}`
        if (e.metaKey || e.ctrlKey) {
            window.open(path, '_blank');
        } else {
            stopEvent(e);
            navigate(path)
        }
    };

    const search = query => {
        if (!isEmpty(query) && query.trim().length > 2) {
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
        setSearching(true);
        searchUsers(query)
            .then(results => {
                setSearching(false);
                setMoreToShow(results.length === 15);
                setNoResults(results.length === 0);
                setUsers(results);
            });
    }, 750);

    const moreResultsAvailable = () => {
        return (
            <div className="more-results-available">
                <span>{I18n.t("models.allUsers.moreResults")}</span>
            </div>)
    }

    const columns = [
        {
            nonSortable: true,
            key: "icon",
            header: "",
            mapper: () => <div className="member-icon">
                <Tooltip standalone={true} children={<UserIcon/>}
                         tip={I18n.t("tooltips.user")}/>
            </div>
        },
        {
            key: "name",
            header: I18n.t("users.name_email"),
            mapper: user => !user.isUser ?  <UserColumn entity={user}
                                                        currentUser={currentUser}/>:
                <UserColumn entity={{user: user}} currentUser={currentUser}/>
        },
        {
            key: "schac_home_organisation",
            header: I18n.t("users.institute"),
            mapper: user => <span>TODO</span>
        },
        {
            key: "sub",
            header: I18n.t("users.sub"),
            mapper: user => user.isUser ? user.uid : "-"
        }];
    const showImpersonation = currentUser.superUser;

    function impersonate(user) {
        startImpersonation(user);
        setFlash(I18n.t("impersonate.flash.startedImpersonation"));
        navigate("/home")
    }

    if (showImpersonation) {
        columns.push({
            nonSortable: true,
            key: "icon",
            hasLink: true,
            header: "",
            mapper: user => (currentUser.id !== user.id) ?
                <ImpersonateIcon className="impersonate"
                                 onClick={() => impersonate(user)}/> : null
        })
    }
    const countUsers = users.length;
    const hasEntities = countUsers > 0;
    let title = "";

    if (hasEntities) {
        title = I18n.t(`users.found`, {
            count: countUsers,
            plural: I18n.t(`models.allUsers.${countUsers === 1 ? "singleUser" : "multipleUsers"}`)
        })
    }
    return (<div className="mod-users">
        {searching && <Loader/>}

        <Entities entities={users}
                  modelName="allUsers"
                  defaultSort="name"
                  filters={moreToShow && moreResultsAvailable()}
                  columns={columns}
                  title={title}
                  hideTitle={!hasEntities || noResults}
                  customNoEntities={I18n.t(`users.noResults`)}
                  loading={false}
                  inputFocus={true}
                  customSearch={search}
                  rowLinkMapper={() => openUser}/>
    </div>)

}
