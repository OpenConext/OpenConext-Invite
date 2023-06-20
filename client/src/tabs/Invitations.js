import React from "react";
import I18n from "../locale/I18n";
import "./Invitations.scss";
import {Chip, Tooltip} from "@surfnet/sds";
import {Entities} from "../components/Entities";
import {ReactComponent as UserIcon} from "@surfnet/sds/icons/functional-icons/id-2.svg";
import "./Users.scss";
import {dateFromEpoch} from "../utils/Date";

import {chipTypeForUserRole} from "../utils/Authority";
import {useNavigate} from "react-router-dom";


export const Invitations = ({role, invitations}) => {
    const navigate = useNavigate();
    const columns = [
        {
            nonSortable: true,
            key: "icon",
            header: "",
            mapper: invitation => <div className="member-icon">
                <Tooltip standalone={true}
                         children={<UserIcon/>}
                         tip={I18n.t("tooltips.invitationIcon",
                             {
                                 email: invitation.email,
                                 createdAt: dateFromEpoch(invitation.createdAt),
                                 expiryDate: dateFromEpoch(invitation.expiryDate)
                             })}/>
            </div>
        },
        {
            key: "email",
            header: I18n.t("users.email"),
            mapper: invitation => <span>{invitation.email}</span>
        },
        {
            key: "authority",
            header: I18n.t("roles.authority"),
            mapper: invitation => <Chip type={chipTypeForUserRole(invitation.intendedAuthority)}
                                        label={I18n.t(`access.${invitation.intendedAuthority}`)}/>
        },
        {
            key: "status",
            header: I18n.t("invitations.status"),
            mapper: invitation => <span>{I18n.t(`invitations.${invitation.status.toLowerCase()}`) }</span>
        },
        {
            key: "enforceEmailEquality",
            header: I18n.t("invitations.enforceEmailEquality"),
            mapper: invitation => I18n.t(`forms.${invitation.enforceEmailEquality ? "yes" : "no"}`)
        },
        {
            key: "eduIDOnly",
            header: I18n.t("invitations.eduIDOnly"),
            mapper: invitation => I18n.t(`forms.${invitation.eduIDOnly ? "yes" : "no"}`)
        },
        {
            key: "endDate",
            header: I18n.t("roles.endDate"),
            mapper: invitation => dateFromEpoch(invitation.endDate)
        }];

    const countInvitations = invitations.length;
    const hasEntities = countInvitations > 0;
    let title = "";

    if (hasEntities) {
        title = I18n.t(`invitations.found`, {
            count: countInvitations,
            plural: I18n.t(`invitations.${countInvitations === 1 ? "singleInvitation" : "multipleInvitations"}`)
        })
    }
    return (<div className="mod-invitations">
        <Entities entities={invitations}
                  modelName="invitations"
                  defaultSort="name"
                  columns={columns}
                  title={title}
                  newLabel={I18n.t("invitations.new")}
                  showNew={true}
                  newEntityFunc={() => navigate("/invitation/new", {state:role.id})}
                  hideTitle={true}
                  customNoEntities={I18n.t(`invitations.noResults`)}
                  loading={false}
                  searchAttributes={["name", "email", "schacHomeOrganization"]}
                  inputFocus={true}>
        </Entities>
    </div>)

}
