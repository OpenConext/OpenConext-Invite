import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "./MineInvitations.scss";
import {Chip} from "@surfnet/sds";
import {Entities} from "../components/Entities";
import "./Users.scss";
import {shortDateFromEpoch} from "../utils/Date";

import {chipTypeForUserRole, invitationExpiry} from "../utils/Authority";
import {invitationsMine} from "../api";
import {useAppStore} from "../stores/AppStore";
import {INVITATION_STATUS} from "../utils/UserRole";


export const MineInvitations = () => {

    const {user} = useAppStore(state => state);
    const [invitations, setInvitations] = useState([]);

    useEffect(() => {
            invitationsMine()
                .then(res => {
                    res.forEach(invitation => {
                        invitation.intendedRoles = (invitation.roles || [])
                            .sort((r1, r2) => r1.role.name.localeCompare(r2.role.name))
                            .map(role => role.role.name).join(", ");
                        const now = new Date();
                        invitation.status = new Date(invitation.expiryDate * 1000) < now ? INVITATION_STATUS.EXPIRED : invitation.status;
                    });
                    setInvitations(res);
                })
        },
        [user])


    const columns = [
        {
            key: "email",
            header: I18n.t("users.email"),
            mapper: invitation => <span>{invitation.email}</span>
        },
        {
            key: "intended_authority",
            header: I18n.t("users.authority"),
            mapper: invitation => <Chip type={chipTypeForUserRole(invitation.intendedAuthority)}
                                        label={I18n.t(`access.${invitation.intendedAuthority}`)}/>
        },
        {
            key: "intendedRoles",
            nonSortable: true,
            header: I18n.t("invitations.intendedRoles"),
            mapper: invitation => invitation.intendedRoles
        },
        {
            key: "name",
            header: I18n.t("invitations.inviter"),
            mapper: invitation => <div className="user-name-email">
                    <span className="name">{invitation.inviter.name}</span>
                    <span className="email">{invitation.inviter.email}</span>
            </div>
        },
        {
            key: "created_at",
            header: I18n.t("invitations.createdAt"),
            mapper: invitation => shortDateFromEpoch(invitation.createdAt, false)
        },
        {
            key: "expiry_date",
            header: I18n.t("invitations.expiryDate"),
            mapper: invitation => invitationExpiry(invitation)
        }
    ]

    return (<div className="mod-mine-invitations">
        <Entities entities={invitations}
                  modelName="invitations"
                  defaultSort="email"
                  title={I18n.t("invitations.mine")}
                  columns={columns}
                  customNoEntities={I18n.t(`invitations.noResults`)}
                  showNew={false}
                  loading={false}
                  searchAttributes={["intendedRoles", "email", "intendedAuthority", "inviter__email", "inviter__name"]}
                  inputFocus={true}
        >
        </Entities>
    </div>)

}
