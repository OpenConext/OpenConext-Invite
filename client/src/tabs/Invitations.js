import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "./Invitations.scss";
import {Button, ButtonSize, ButtonType, Checkbox, Chip, Loader, Tooltip} from "@surfnet/sds";
import {Entities} from "../components/Entities";
import "./Users.scss";
import {shortDateFromEpoch} from "../utils/Date";

import {chipTypeForInvitationStatus, chipTypeForUserRole} from "../utils/Authority";
import {useNavigate} from "react-router-dom";
import {deleteInvitation, resendInvitation} from "../api";
import ConfirmationDialog from "../components/ConfirmationDialog";
import {useAppStore} from "../stores/AppStore";
import {isEmpty, pseudoGuid} from "../utils/Utils";
import {allowedToDeleteInvitation, INVITATION_STATUS} from "../utils/UserRole";


export const Invitations = ({role, invitations}) => {
    const navigate = useNavigate();
    const {user, setFlash} = useAppStore(state => state);

    const [selectedInvitations, setSelectedInvitations] = useState({});
    const [allSelected, setAllSelected] = useState(false);
    const [resultAfterSearch, setResultAfterSearch] = useState(invitations)
    const [loading, setLoading] = useState(true);
    const [confirmation, setConfirmation] = useState({});
    const [confirmationOpen, setConfirmationOpen] = useState(false);

    useEffect(() => {
            invitations.forEach(invitation => {
                invitation.intendedRoles = invitation.roles
                    .sort((r1, r2) => r1.role.name.localeCompare(r2.role.name))
                    .map(role => role.role.name).join(", ");
                const now = new Date();
                invitation.status = new Date(invitation.expiryDate * 1000) < now ? INVITATION_STATUS.EXPIRED : invitation.status;
            });
            setSelectedInvitations(invitations
                .reduce((acc, invitation) => {
                    acc[invitation.id] = {
                        selected: false,
                        ref: invitation,
                        allowed: allowedToDeleteInvitation(user, invitation)
                    };
                    return acc;
                }, {}));
            setLoading(false);
        },
        [invitations, user])

    const onCheck = invitation => e => {
        const checked = e.target.checked;
        const newSelectedInvitations = {...selectedInvitations}
        newSelectedInvitations[invitation.id].selected = checked;
        setSelectedInvitations(newSelectedInvitations);
        if (!checked) {
            setAllSelected(false);
        }
    }

    const selectAll = e => {
        const checked = e.target.checked;
        setAllSelected(checked);
        const newSelectedInvitations = {...selectedInvitations}
        Object.values(newSelectedInvitations).forEach(inv => inv.selected = checked);
        setSelectedInvitations(newSelectedInvitations);
    }

    const invitationIdentifiers = () => {
        return Object.entries(selectedInvitations)
            .filter(entry => (entry[1].selected) && entry[1].allowed)
            .map(entry => parseInt(entry[0]))
            .filter(id => resultAfterSearch.some(res => res.id === id));
    }

    const showCheckAllHeader = () => {
        return Object.entries(selectedInvitations)
            .filter(entry => entry[1].allowed)
            .length > 0;
    }

    const doResendInvitations = showConfirmation => {
        if (showConfirmation) {
            setConfirmation({
                cancel: () => setConfirmationOpen(false),
                action: () => doResendInvitations(false),
                question: I18n.t("invitations.resendConfirmation"),
                confirmationTxt: I18n.t("confirmationDialog.confirm")
            });
            setConfirmationOpen(true);
        } else {
            const identifiers = invitationIdentifiers();
            Promise.all(identifiers.map(identifier => resendInvitation(identifier)))
                .then(() => {
                    setConfirmationOpen(false);
                    setFlash(I18n.t("invitations.resendFlash"));
                    const path = encodeURIComponent(window.location.pathname);
                    navigate(`/refresh-route/${path}`, {replace: true});
                })
        }
    };

    const doDeleteInvitations = showConfirmation => {
        if (showConfirmation) {
            setConfirmation({
                cancel: () => setConfirmationOpen(false),
                action: () => doDeleteInvitations(false),
                question: I18n.t("invitations.deleteConfirmation"),
                confirmationTxt: I18n.t("confirmationDialog.confirm")
            });
            setConfirmationOpen(true);
        } else {
            const identifiers = invitationIdentifiers();
            Promise.all(identifiers.map(identifier => deleteInvitation(identifier)))
                .then(() => {
                    setConfirmationOpen(false);
                    setFlash(I18n.t("invitations.deleteFlash"));
                    const path = encodeURIComponent(window.location.pathname);
                    navigate(`/refresh-route/${path}`, {replace: true});
                })
        }
    };

    if (loading) {
        return <Loader/>
    }

    const searchCallback = afterSearch => {
        setResultAfterSearch(afterSearch);
    }

    const actionButtons = () => {
        if (isEmpty(invitationIdentifiers())) {
            return null;
        }
        return (
            <div className="admin-actions">
                <div>
                    <Tooltip standalone={true}
                             anchorId={"remove-members"}
                             tip={I18n.t("tooltips.removeInvitation")}
                             children={
                                 <Button onClick={() => doDeleteInvitations(true)}
                                         size={ButtonSize.Small}
                                         type={ButtonType.Secondary}
                                         txt={I18n.t("invitations.delete")}/>
                             }/>
                </div>
                <div>
                    <Tooltip standalone={true}
                             anchorId={"remove-members"}
                             tip={I18n.t("tooltips.resendInvitation")}
                             children={
                                 <Button onClick={() => doResendInvitations(true)}
                                         size={ButtonSize.Small}
                                         type={ButtonType.Secondary}
                                         txt={I18n.t("invitations.resend")}/>
                             }/>
                </div>
            </div>);
    }

    const columns = [
        {
            nonSortable: true,
            key: "check",
            header: showCheckAllHeader() ? <Checkbox value={allSelected}
                                                     name={"allSelected"}
                                                     onChange={selectAll}/> : null,
            mapper: invitation => <div className="check">
                {selectedInvitations[invitation.id].allowed ? <Checkbox name={pseudoGuid()}
                                                                        onChange={onCheck(invitation)}
                                                                        value={selectedInvitations[invitation.id].selected}/> :
                    <Tooltip tip={I18n.t("invitations.notAllowed")}/>}
            </div>
        },
        {
            key: "email",
            header: I18n.t("users.email"),
            mapper: invitation => <span>{invitation.email}</span>
        },
        {
            key: "authority",
            header: I18n.t("users.authority"),
            mapper: invitation => <Chip type={chipTypeForUserRole(invitation.intendedAuthority)}
                                        label={I18n.t(`access.${invitation.intendedAuthority}`)}/>
        },
        {
            key: "intendedRoles",
            header: I18n.t("roles.title"),
            mapper: invitation => invitation.intendedRoles
        },
        {
            key: "inviter__name",
            header: I18n.t("invitations.inviter"),
            mapper: invitation => <div className="user-name-email">
                <span className="name">{invitation.inviter.name}</span>
                <span className="email">{invitation.inviter.email}</span>
            </div>
        },
        {
            key: "status",
            header: I18n.t("invitations.status"),
            mapper: invitation => <Chip type={chipTypeForInvitationStatus(invitation)}
                                        label={I18n.t(`invitations.${invitation.status.toLowerCase()}`)}/>
        },
        {
            key: "createdAt",
            header: I18n.t("invitations.createdAt"),
            mapper: invitation => shortDateFromEpoch(invitation.createdAt)
        },
        {
            key: "expiryDate",
            header: I18n.t("invitations.expiryDate"),
            mapper: invitation => shortDateFromEpoch(invitation.expiryDate)
        },
        {
            key: "roleExpiryDate",
            header: I18n.t("invitations.roleExpiryDate"),
            mapper: invitation => shortDateFromEpoch(invitation.roleExpiryDate)
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
        {confirmationOpen && <ConfirmationDialog isOpen={confirmationOpen}
                                                 cancel={confirmation.cancel}
                                                 confirm={confirmation.action}
                                                 confirmationTxt={confirmation.confirmationTxt}
                                                 question={confirmation.question}/>}

        <Entities entities={invitations}
                  modelName="invitations"
                  defaultSort="name"
                  columns={columns}
                  title={title}
                  newLabel={I18n.t("invitations.new")}
                  showNew={true}
                  newEntityFunc={() => navigate("/invitation/new", {state: role.id})}
                  hideTitle={true}
                  customNoEntities={I18n.t(`invitations.noResults`)}
                  loading={false}
                  actions={actionButtons()}
                  searchCallback={searchCallback}
                  searchAttributes={["name", "email", "schacHomeOrganization", "inviter__email", "inviter__name"]}
                  inputFocus={true}>
        </Entities>
    </div>)

}
