import "./RolesUnknownInManage.scss";
import {useAppStore} from "../stores/AppStore";
import React, {useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {Chip, Loader} from "@surfnet/sds";
import {useNavigate} from "react-router-dom";
import {AUTHORITIES, isUserAllowed} from "../utils/UserRole";
import {rolesUnknownInManage} from "../api";
import {stopEvent} from "../utils/Utils";
import {chipTypeForUserRole} from "../utils/Authority";
import {ReactComponent as AlertLogo} from "@surfnet/sds/icons/functional-icons/alert-circle.svg";
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
                    deriveApplicationAttributes(res, I18n.locale, I18n.t("roles.multiple"), I18n.t("forms.and"))
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
            mapper: role => <span className="unknown-in-manage">{I18n.t("roles.unknownInManage")}</span>
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

    return (
        <div className="mod-unknown-roles">
            <Entities
                entities={roles}
                modelName="unknownRoles"
                showNew={false}
                defaultSort="name"
                columns={columns}
                searchAttributes={["name", "description", "applicationName"]}
                customNoEntities={I18n.t(`system.noRoles`)}
                loading={false}
                inputFocus={true}
                hideTitle={false}
                rowLinkMapper={openRole}
                rowClassNameResolver={entity => (entity.applications || []).length > 1 ? "multi-role" : ""}/>
        </div>
    );

}
