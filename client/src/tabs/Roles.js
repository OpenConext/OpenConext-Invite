import "./Roles.scss";
import {useAppStore} from "../stores/AppStore";
import React, {useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {Button, ButtonSize, Chip, Loader, Tooltip} from "@surfnet/sds";
import {useNavigate} from "react-router-dom";
import {AUTHORITIES, highestAuthority, isUserAllowed, markAndFilterRoles} from "../utils/UserRole";
import {rolesByApplication, searchRoles} from "../api";
import {distinctValues, isEmpty, stopEvent} from "../utils/Utils";
import debounce from "lodash.debounce";
import {chipTypeForUserRole} from "../utils/Authority";
import {ReactComponent as VoidImage} from "../icons/undraw_void_-3-ggu.svg";
import Select from "react-select";
import {ReactComponent as AlertLogo} from "@surfnet/sds/icons/functional-icons/alert-circle.svg";

const allValue = "all";

export const Roles = () => {
    const user = useAppStore(state => state.user);
    const {roleSearchRequired} = useAppStore(state => state.config);
    const navigate = useNavigate();

    const [loading, setLoading] = useState(true);
    const [searching, setSearching] = useState(false);
    const [roles, setRoles] = useState([]);
    const [moreToShow, setMoreToShow] = useState(false);
    const [noResults, setNoResults] = useState(false);// eslint-disable-line no-unused-vars
    const [filterOptions, setFilterOptions] = useState([]);
    const [filterValue, setFilterValue] = useState(null);

    useEffect(() => {
        if (isUserAllowed(AUTHORITIES.INSTITUTION_ADMIN, user)) {
            if (roleSearchRequired) {
                setLoading(false);
            } else {
                rolesByApplication()
                    .then(res => {
                        const newRoles = markAndFilterRoles(
                            user,
                            distinctValues(res, "id"),
                            I18n.locale,
                            I18n.t("roles.multiple"),
                            I18n.t("forms.and"));
                        setRoles(newRoles);
                        initFilterValues(newRoles);
                        setLoading(false);
                    })
            }
        } else {
            const newRoles = markAndFilterRoles(user, [], I18n.locale, I18n.t("roles.multiple"), I18n.t("forms.and"));
            setRoles(newRoles);
            initFilterValues(newRoles);
            setLoading(false);
        }
    }, [user]);// eslint-disable-line react-hooks/exhaustive-deps

    const initFilterValues = res => {
        const userRoles = res.filter(role => !(role.isUserRole && role.authority === "GUEST"));
        const newFilterOptions = [{
            label: I18n.t("invitations.statuses.all", {nbr: userRoles.length}),
            value: allValue
        }];
        const reducedRoles = userRoles.reduce((acc, role) => {
            const manageIdentifiers = role.applicationMaps.map(m => m.id);
            const option = acc.find(opt => manageIdentifiers.includes(opt.manageId));
            if (option) {
                ++option.nbr;
            } else {
                role.applicationMaps
                    .forEach(app => acc.push({manageId: app.id, nbr: 1, name: app[`name:${I18n.locale}`] || app["name:en"]}))
            }
            return acc;
        }, []);
        const appOptions = reducedRoles.map(option => ({
            label: `${option.name} (${option.nbr})`,
            value: option.manageId
        })).sort((o1, o2) => o1.label.localeCompare(o2.label));

        setFilterOptions(newFilterOptions.concat(appOptions));
        setFilterValue(newFilterOptions[0]);

    }

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

    const search = query => {
        if (!isEmpty(query) && query.trim().length > 2) {
            setSearching(true);
            delayedAutocomplete(query);
        }
        if (isEmpty(query)) {
            setSearching(false);
            setMoreToShow(false);
            setNoResults(false);
            setRoles([]);
        }
    };

    const delayedAutocomplete = debounce(query => {
        searchRoles(query)
            .then(res => {
                setRoles(markAndFilterRoles(user, res, I18n.locale, I18n.t("roles.multiple"), I18n.t("forms.and")));
                setMoreToShow(res.length === 15);
                setNoResults(res.length === 0);
                setSearching(false);
            });
    }, 500);

    const moreResultsAvailable = () => {
        return (
            <div className="more-results-available">
                <span>{I18n.t("users.moreResults")}</span>
            </div>)
    }

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
    const filter = () => {
        return (
            <div className="roles-filter">
                <Select
                    className={"roles-filter-select"}
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

    const columns = [
        {
            nonSortable: true,
            key: "logo",

            header: "",
            mapper: role => role.unknownInManage ? <div className="role-icon unknown-in-manage"><AlertLogo/></div> : <div className="role-icon">
                    {typeof role.logo === "string" ? <img src={role.logo} alt="logo"/> : role.logo}
                </div>
        },
        {
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
    if (isInstitutionAdmin && !isEmpty(user.institution) && roles.length === 0) {
        return (
            <div className={"mod-roles"}>
                {noRolesInstitutionAdmin()}
            </div>
        )
    }
    const filteredRoles = filterValue.value === allValue ? roles :
        roles.filter(role => role.applicationMaps.map(m => m.id).includes(filterValue.value));

    return (
        <div className={"mod-roles"}>
            {moreToShow && moreResultsAvailable()}
            <Entities
                entities={isSuperUser ? filteredRoles : filteredRoles.filter(role => !(role.isUserRole && role.authority === "GUEST"))}
                modelName="roles"
                showNew={isManager}
                newLabel={I18n.t("roles.new")}
                newEntityPath={"/role/new"}
                defaultSort="name"
                columns={columns}
                searchAttributes={["name", "description", "applicationName"]}
                customNoEntities={I18n.t(`roles.noResults`)}
                loading={false}
                inputFocus={true}
                hideTitle={false}
                filters={filter(filterOptions, filterValue)}
                customSearch={roleSearchRequired && isSuperUser ? search : null}
                rowLinkMapper={isUserAllowed(AUTHORITIES.INVITER, user) ? openRole : null}
                rowClassNameResolver={entity => (entity.applications || []).length > 1 ? "multi-role" : ""}
                busy={searching}/>
        </div>
    );

}
