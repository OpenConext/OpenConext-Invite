import {Badge, Spinner} from "@surfnet/curve-react";
import {WarningCircleIcon as AlertLogo} from "@phosphor-icons/react";
import "./RolesUnknownInManage.scss";
import {useAppStore} from "../stores/AppStore";
import React, {useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {useNavigate} from "react-router";
import {AUTHORITIES, isUserAllowed} from "../utils/UserRole";
import {rolesUnknownInManage} from "../api";
import {stopEvent} from "../utils/Utils";
import {badgeVariantForUserRole} from "../utils/Authority";
import {deriveApplicationAttributes} from "../utils/Manage";

export const RolesUnknownInManage = () => {
    const navigate = useNavigate();
    const user = useAppStore(state => state.user);

    const [loading, setLoading] = useState(true);
    const [roles, setRoles] = useState([]);

    useEffect(() => {
        if (isUserAllowed(AUTHORITIES.SUPER_USER, user)) {
            rolesUnknownInManage()
                .then(res => {
                    res.forEach(role =>
                        deriveApplicationAttributes(role, I18n.locale, I18n.t("roles.multiple"), I18n.t("forms.and"))
                    );
                    setRoles(res);
                    setLoading(false);
                })
        } else {
            navigate("/404")
        }
    }, [user]);// eslint-disable-line react-hooks/exhaustive-deps


    const openRole = (e, role) => {
        const path = `/roles/${role.id}`
        if (e.metaKey || e.ctrlKey) {
            window.open(path, '_blank');
        } else {
            stopEvent(e);
            navigate(path);
        }
    };

    const columns = [
        {
            nonSortable: true,
            key: "logo",
            header: "",
            mapper: () => <div className="role-icon unknown-in-manage"><AlertLogo/></div>
        },
        {
            key: "applicationName",
            header: I18n.t("roles.applicationName"),
            mapper: () => <span className="unknown-in-manage">{I18n.t("roles.unknownInManage")}</span>
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
            mapper: role => <Badge variant={badgeVariantForUserRole(role.authority)}>{role.isUserRole ? I18n.t(`access.${role.authority}`) :
                                      I18n.t("roles.noMember")}</Badge>
        },
        {
            key: "userRoleCount",
            header: I18n.t("roles.userRoleCount"),
            mapper: role => role.userRoleCount
        }

    ];

    if (loading) {
        return <div className="loading-container"><Spinner className="size-8"/></div>
    }

    return (
        <div className="mod-unknown-roles">
            <Entities
                entities={roles}
                modelName="unknownRoles"
                showNew={false}
                defaultSort="name"
                columns={columns}
                searchAttributes={["name", "description", "applicationName"]}
                customNoEntities={I18n.t("unknownRoles.noRoles")}
                loading={false}
                inputFocus={true}
                hideTitle={false}
                rowLinkMapper={openRole}
                rowClassNameResolver={entity => (entity.applications || []).length > 1 ? "multi-role" : ""}/>
        </div>
    );

}
