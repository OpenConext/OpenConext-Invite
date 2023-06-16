import "./Roles.scss";
import {useAppStore} from "../stores/AppStore";
import React, {useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {Chip, Loader, Tooltip} from "@surfnet/sds";
import {useNavigate} from "react-router-dom";
import {AUTHORITIES, chipTypeForUserRole, isUserAllowed} from "../utils/UserRole";
import {rolesByApplication, searchRoles} from "../api";
import {isEmpty, stopEvent} from "../utils/Utils";
import debounce from "lodash.debounce";
import {dateFromEpoch} from "../utils/Date";
import {ReactComponent as RoleIcon} from "@surfnet/sds/icons/illustrative-icons/hierarchy.svg";

export const Roles = () => {
    const user = useAppStore(state => state.user);
    const {roleSearchRequired} = useAppStore(state => state.config);
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [searching, setSearching] = useState(false);
    const [roles, setRoles] = useState([]);
    const [moreToShow, setMoreToShow] = useState(false);
    const [noResults, setNoResults] = useState(false);// eslint-disable-line no-unused-vars

    const markAndFilterRoles = allRoles => {
        const userRoles = user.userRoles;
        userRoles.forEach(userRole => {
            userRole.isUserRole = true;
            userRole.name = userRole.role.name;
            userRole.description = userRole.role.description;
        })
        allRoles.forEach(role => {
            role.isUserRole = false;
        });
        return allRoles
            .filter(role => userRoles.every(userRole => userRole.role.id !== role.id))
            .concat(userRoles);
    }

    useEffect(() => {
        if (isUserAllowed(AUTHORITIES.MANAGER, user)) {
            if (!roleSearchRequired) {
                rolesByApplication()
                    .then(res => setRoles(markAndFilterRoles(res)))
                setLoading(false);
            }
        } else {
            setRoles(markAndFilterRoles([]))
        }
        setLoading(false);
    }, [user, roleSearchRequired]);// eslint-disable-line react-hooks/exhaustive-deps

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
                setRoles(markAndFilterRoles(res));
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
    const columns = [
        {
            nonSortable: true,
            key: "icon",
            header: "",
            mapper: role => <div className="role-icon">
                <Tooltip standalone={true}
                         children={<RoleIcon/>}
                         tip={I18n.t("tooltips.roleIcon",
                             {
                                 name: role.name,
                                 createdAt: dateFromEpoch(role.isUserRole ? role.createdAt : role.auditable.createdAt)
                             })}/>
            </div>
        },
        {
            key: "name",
            header: I18n.t("roles.name"),
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
            mapper: role => role.isUserRole ?
                <Chip type={chipTypeForUserRole(role)} label={I18n.t(`access.${role.authority}`)}/> :
                <span>{I18n.t("roles.noMember")}</span>
        },
        {
            key: "endDate",
            header: I18n.t("roles.endDate"),
            mapper: role => role.isUserRole ? dateFromEpoch(role.endDate) : I18n.t("forms.notApplicable")
        },
        {
            key: "landingPage",
            header: I18n.t("roles.landingPage"),
            mapper: role => <span>{role.isUserRole ? role.role.landingPage : role.landingPage}</span>
        }

    ];

    if (loading) {
        return <Loader/>
    }

    const isSuperUser = isUserAllowed(AUTHORITIES.SUPER_USER, user);
    const isManager = isUserAllowed(AUTHORITIES.MANAGER, user);

    return (
        <div className={"mod-roles"}>
            <Entities entities={roles}
                      modelName="roles"
                      showNew={isManager}
                      newLabel={I18n.t("roles.new")}
                      newEntityPath={"/role/new"}
                      defaultSort="name"
                      columns={columns}
                      searchAttributes={["name", "description"]}
                      customNoEntities={I18n.t(`roles.noResults`)}
                      loading={false}
                      inputFocus={true}
                      hideTitle={true}
                      filters={moreToShow && moreResultsAvailable()}
                      customSearch={roleSearchRequired && isSuperUser ? search : null}
                      rowLinkMapper={isUserAllowed(AUTHORITIES.INVITER, user) ? openRole : null}
                      busy={searching}/>
        </div>
    );

}
