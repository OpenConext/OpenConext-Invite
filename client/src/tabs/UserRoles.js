import React, {useEffect} from "react";
import I18n from "../locale/I18n";
import "./UserRoles.scss";
import {Chip, ChipType, Tooltip} from "@surfnet/sds";
import {Entities} from "../components/Entities";
import {ReactComponent as UserIcon} from "@surfnet/sds/icons/functional-icons/id-2.svg";
import "./Users.scss";
import {useAppStore} from "../stores/AppStore";
import {dateFromEpoch} from "../utils/Date";
import {useNavigate} from "react-router-dom";
import {chipTypeForUserRole} from "../utils/Authority";


export const UserRoles = ({role, userRoles}) => {
    const navigate = useNavigate();
    const {user} = useAppStore(state => state);

    useEffect(() => {
        userRoles.forEach(userRole => {
                userRole.name = userRole.userInfo.name;
                userRole.email = userRole.userInfo.email;
                userRole.schacHomeOrganization = userRole.userInfo.schacHomeOrganization;
            })
        },
        [userRoles])

    const columns = [
        {
            nonSortable: true,
            key: "icon",
            header: "",
            mapper: userRole => <div className="member-icon">
                <Tooltip standalone={true}
                         children={<UserIcon/>}
                         tip={I18n.t("tooltips.userRoleIcon",
                             {
                                 name: userRole.userInfo.name,
                                 createdAt: dateFromEpoch(userRole.createdAt)
                             })}/>
            </div>
        },
        {
            key: "name",
            header: I18n.t("users.name_email"),
            mapper: userRole => (
                <div className="user-name-email">
                    <span className="name">{userRole.userInfo.name}</span>
                    <span className="email">{userRole.userInfo.email}</span>
                </div>)
        },
        {
            key: "me",
            nonSortable: true,
            header: "",
            mapper: userRole => userRole.userInfo.id === user.id ?
                <Chip label={I18n.t("forms.you")} type={ChipType.Status_info}/> : null
        },
        {
            key: "schacHomeOrganisation",
            header: I18n.t("users.schacHomeOrganization"),
            mapper: userRole => <span>{userRole.userInfo.schacHomeOrganization}</span>
        },
        {
            key: "authority",
            header: I18n.t("roles.authority"),
            mapper: userRole => <Chip type={chipTypeForUserRole(userRole.authority)}
                                      label={I18n.t(`access.${userRole.authority}`)}/>
        },
        {
            key: "endDate",
            header: I18n.t("roles.endDate"),
            mapper: userRole => dateFromEpoch(userRole.endDate)
        }];

    return (<div className="mod-user-roles">
        <Entities entities={userRoles}
                  modelName="userRoles"
                  defaultSort="name"
                  columns={columns}
                  newLabel={I18n.t("invitations.new")}
                  showNew={true}
                  newEntityFunc={() => navigate("/invitation/new", {state:role.id})}
                  customNoEntities={I18n.t(`userRoles.noResults`)}
                  loading={false}
                  hideTitle={true}
                  searchAttributes={["name", "email", "schacHomeOrganization"]}
                  inputFocus={true}>
        </Entities>
    </div>)

}
