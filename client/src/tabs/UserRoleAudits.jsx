import "./UserRoleAudits.scss";
import {useAppStore} from "../stores/AppStore";
import React, {useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {useNavigate} from "react-router-dom";
import {AUTHORITIES, isUserAllowed} from "../utils/UserRole";
import {fetchRoles, searchUserRoleAudits} from "../api";
import {isEmpty} from "../utils/Utils";
import debounce from "lodash.debounce";
import {defaultPagination, pageCount} from "../utils/Pagination";
import {dateFromEpoch, shortDateFromEpoch} from "../utils/Date";
import SelectField from "../components/SelectField";
import {UnitHeader} from "../components/UnitHeader";
import Logo from "@surfnet/sds/icons/illustrative-icons/database-hand.svg";

export const UserRoleAudits = () => {
    const {user} = useAppStore(state => state);

    const [searching, setSearching] = useState(isUserAllowed(AUTHORITIES.INSTITUTION_ADMIN, user));
    const [userRoleAudits, setUserRoleAudits] = useState([]);
    const [roles, setRoles] = useState([]);
    const [selectedRole, setSelectedRole] = useState(null);
    const [paginationQueryParams, setPaginationQueryParams] = useState(defaultPagination("userEmail"));
    const [totalElements, setTotalElements] = useState(0);
    const navigate = useNavigate();

    if (!isUserAllowed(AUTHORITIES.INSTITUTION_ADMIN, user)) {
        navigate("/home")
    }

    useEffect(() => {
        fetchRoles()
            .then(res => {
                setRoles(res);
                useAppStore.setState({
                    breadcrumbPath: [
                        {path: "/home", value: I18n.t("tabs.home")},
                        {value: I18n.t("header.links.audit")}
                    ]
                });
            })
    }, [user]);

    useEffect(() => {
        searchUserRoleAudits(paginationQueryParams, isEmpty(selectedRole) ? null : selectedRole.value)
            .then(page => {
                setUserRoleAudits(page.content);
                setTotalElements(page.totalElements);
                setSearching(false);
            })
    }, [paginationQueryParams, selectedRole]);

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

    const filters = () => {
        return (
            <SelectField
                value={selectedRole}
                options={roles.map(role => ({value:role.id, label: role.name}))}
                searchable={true}
                clearable={true}
                placeholder={I18n.t("userRoleAudit.rolePlaceHolder")}
                onChange={option => setSelectedRole(option)}
            />
        )
    }

    const columns = [
        {
            key: "userEmail",
            header: I18n.t("userRoleAudit.email"),
            mapper: userRoleAudit => userRoleAudit.userEmail
        },
        {
            key: "roleName",
            header: I18n.t("userRoleAudit.roleName"),
            mapper: userRoleAudit => userRoleAudit.roleName
        },
        {
            key: "action",
            header: I18n.t("userRoleAudit.action"),
            mapper: userRoleAudit => I18n.t(`userRoleAudit.actions.${userRoleAudit.action}`)
        },
        {
            key: "authority",
            header: I18n.t("userRoleAudit.authority"),
            mapper: userRoleAudit => userRoleAudit.authority
        },
        {
            key: "createdAt",
            header: I18n.t("userRoleAudit.createdAt"),
            mapper: userRoleAudit => shortDateFromEpoch(userRoleAudit.createdAt, true)
        },
        {
            key: "endDate",
            header: I18n.t("userRoleAudit.endDate"),
            mapper: userRoleAudit => shortDateFromEpoch(userRoleAudit.endDate, true)
        },
    ];

    return (
        <div className={"mod-user-role-audits"}>
            <UnitHeader obj={({name: I18n.t("userRoleAudit.header"), svg: Logo, style: "small"})}>
                <p>{I18n.t("userRoleAudit.info")}</p>
            </UnitHeader>
            <Entities
                entities={userRoleAudits}
                modelName="userRoleAudit"
                showNew={false}
                defaultSort="userEmail"
                columns={columns}
                searchAttributes={["userEmail", "roleName"]}
                loading={false}
                filters={filters()}
                inputFocus={!searching}
                hideTitle={searching}
                customSearch={search}
                totalElements={totalElements}
                busy={searching}
            />
        </div>
    );

}
