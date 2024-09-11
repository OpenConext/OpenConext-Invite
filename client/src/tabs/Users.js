import React, {useEffect, useMemo, useState} from "react";
import I18n from "../locale/I18n";
import "../components/Entities.scss";
import {Chip, ChipType, Loader, Tooltip} from "@surfnet/sds";
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
import {AUTHORITIES, highestAuthority, isUserAllowed} from "../utils/UserRole";
import SearchSvg from "../icons/undraw_people_search_re_5rre.svg";
import {chipTypeForUserRole} from "../utils/Authority";
import Select from "react-select";

const allValue = "all";

export const Users = () => {

    const {user: currentUser, startImpersonation, setFlash} = useAppStore(state => state);

    const [searching, setSearching] = useState(false);
    const [users, setUsers] = useState([]);
    const [moreToShow, setMoreToShow] = useState(false);
    const [initial, setInitial] = useState(true);
    const [noResults, setNoResults] = useState(false);
    const [filterOptions, setFilterOptions] = useState([]);
    const [filterValue, setFilterValue] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        return () => {
            delayedAutocomplete.cancel();
        };
    });

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
            delayedAutocomplete(query);
        }
        if (isEmpty(query)) {
            setSearching(false);
            setMoreToShow(false);
            setNoResults(false);
            setUsers([]);
            initFilterValues([]);
        }
    };

    const delayedAutocomplete = useMemo(() => debounce(query => {
        setSearching(true);
        searchUsers(query)
            .then(results => {
                setInitial(false);
                results.forEach(user => user.highestAuthority = highestAuthority(user, false));
                setUsers(results);
                initFilterValues(results);
                setMoreToShow(results.length === 15 && query !== "owl");
                setNoResults(results.length === 0);
                setSearching(false);
            });
    }, 500), []);

    const moreResultsAvailable = () => {
        return (
            <div className="more-results-available">
                <span>{I18n.t("users.moreResults")}</span>
            </div>)
    }

    const initFilterValues = users => {
        const newFilterOptions = [{
            label: I18n.t("invitations.statuses.all", {nbr: users.length}),
            value: allValue
        }];
        const reducedRoles = users.reduce((acc, user) => {
            const option = acc.find(opt => opt.value === user.highestAuthority);
            if (option) {
                ++option.nbr;
            } else {
                acc.push({value: user.highestAuthority, nbr: 1, name: I18n.t(`access.${user.highestAuthority}`)});
            }
            return acc;
        }, []);
        const authorityOptions = reducedRoles.map(option => ({
            label: `${option.name} (${option.nbr})`,
            value: option.value
        })).sort((o1, o2) => o1.label.localeCompare(o2.label));
        setFilterOptions(newFilterOptions.concat(authorityOptions));
        setFilterValue(newFilterOptions[0]);
    }

    const filter = () => {
        return (
            <div className="users-filter">
                <Select
                    className={"users-filter-select"}
                    value={filterValue}
                    classNamePrefix={"filter-select"}
                    onChange={option => setFilterValue(option)}
                    options={filterOptions}
                    isSearchable={false}
                    isClearable={false}
                />
            </div>
        );
    }

    const filteredUsers = () => {
        return !filterValue || filterValue.value === allValue ? users :
            users.filter(user => user.highestAuthority === filterValue.value);
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
            mapper: user => <Chip type={chipTypeForUserRole(user.highestAuthority)}
                                  label={I18n.t(`access.${user.highestAuthority}`)}/>
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
                : <Chip type={ChipType.Main_400} label={I18n.t("forms.you")}/>
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
        {moreToShow && moreResultsAvailable()}
        <Entities entities={filteredUsers(users)}
                  modelName="users"
                  defaultSort="name"
                  columns={columns}
                  title={title}
                  newLabel={currentUser.superUser ? I18n.t("invitations.newInvite") : null}
                  showNew={isUserAllowed(AUTHORITIES.SUPER_USER, currentUser)}
                  newEntityFunc={() => navigate(`/invitation/new?maintainer=true`)}
                  hideTitle={!hasEntities || noResults}
                  filters={(hasEntities && !noResults) && filter(filterOptions, filterValue)}
                  customNoEntities={I18n.t(`users.noResults`)}
                  loading={searching}
                  searchAttributes={["name", "email", "schacHomeOrganization"]}
                  inputFocus={true}
                  customSearch={search}
                  rowLinkMapper={openUser}
                  busy={searching}>
            {initial && <div className={"image-container"}>
                <img src={SearchSvg} alt="search"/>
            </div>}
        </Entities>
    </div>)

}
