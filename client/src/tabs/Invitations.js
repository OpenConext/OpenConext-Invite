import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "./Invitations.scss";
import {Button, ButtonSize, ButtonType, Checkbox, Chip, Tooltip} from "@surfnet/sds";
import {Entities} from "../components/Entities";
import "./Users.scss";
import {shortDateFromEpoch} from "../utils/Date";

import {chipTypeForUserRole, invitationExpiry} from "../utils/Authority";
import {useNavigate} from "react-router-dom";
import {deleteInvitation, resendInvitation, searchInvitations} from "../api";
import ConfirmationDialog from "../components/ConfirmationDialog";
import {useAppStore} from "../stores/AppStore";
import {isEmpty, pseudoGuid} from "../utils/Utils";
import {allowedToDeleteInvitation, AUTHORITIES, INVITATION_STATUS, isUserAllowed} from "../utils/UserRole";
import {ReactComponent as TrashIcon} from "@surfnet/sds/icons/functional-icons/bin.svg";
import {ReactComponent as ResendIcon} from "@surfnet/sds/icons/functional-icons/go-to-other-website.svg";
import {defaultPagination, pageCount} from "../utils/Pagination";
import debounce from "lodash.debounce";


export const Invitations = ({
                                role,
                                systemView = false
                            }) => {
    const navigate = useNavigate();
    const {user, setFlash} = useAppStore(state => state);
    const [invitations, setInvitations] = useState([]);
    const [selectedInvitations, setSelectedInvitations] = useState({});
    const [allSelected, setAllSelected] = useState(false);
    const [paginationQueryParams, setPaginationQueryParams] = useState(defaultPagination("email"));
    const [totalElements, setTotalElements] = useState(0);
    const [searching, setSearching] = useState(true);
    const [confirmation, setConfirmation] = useState({});
    const [confirmationOpen, setConfirmationOpen] = useState(false);

    useEffect(() => {
            searchInvitations(systemView ? null : role.id, paginationQueryParams)
                .then(page => {
                    const content = page.content;
                    content.forEach(invitation => {
                        invitation.intendedRoles = (invitation.roles || [])
                            .sort((r1, r2) => r1.name.localeCompare(r2.name))
                            .map(role => role.name).join(", ");
                        const now = new Date();
                        invitation.status = new Date(invitation.expiryDate * 1000) < now ? INVITATION_STATUS.EXPIRED : invitation.status;
                        //We don't get the invitation.user_roles.role.applicationUsages from the server anymore due to custom pagination queries
                        (invitation.roles || []).forEach(invitationRole => {
                            invitationRole.role = {
                                id: invitationRole.id,
                                applicationUsages: invitationRole.manageIdentifiers.map(mi => ({application: {manageId: mi}})),
                                user_id: invitationRole.user_id,
                                authority: invitationRole.intended_authority
                            }
                        });

                    });
                    setInvitations(content);
                    setSelectedInvitations(content
                        .reduce((acc, invitation) => {
                            acc[invitation.id] = {
                                selected: false,
                                ref: invitation,
                                allowed: allowedToDeleteInvitation(user, invitation)
                            };
                            return acc;
                        }, {}));
                    setAllSelected(false);
                    setTotalElements(page.totalElements);
                    setSearching(false);
                })
        },
        [user, paginationQueryParams]) // eslint-disable-line react-hooks/exhaustive-deps

    const search = (query, sorted, reverse, page) => {
        if (isEmpty(query) || query.trim().length > 2) {
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
            .map(entry => parseInt(entry[0]));

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
                    setPaginationQueryParams({...paginationQueryParams});
                })
        }
    };

    const doResendInvitationsFromActionLink = (invitation, showConfirmation) => {
        if (showConfirmation) {
            setConfirmation({
                cancel: () => setConfirmationOpen(false),
                action: () => doResendInvitationsFromActionLink(invitation, false),
                question: I18n.t("invitations.resendConfirmationOne"),
                confirmationTxt: I18n.t("confirmationDialog.confirm")
            });
            setConfirmationOpen(true);
        } else {
            resendInvitation(invitation.id)
                .then(() => {
                    setConfirmationOpen(false);
                    setFlash(I18n.t("invitations.resendFlash"));
                })
        }
    }

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
                    setPaginationQueryParams({...paginationQueryParams});
                })
        }
    };

    const doDeleteInvitationsFromActionLink = (invitation, showConfirmation) => {
        if (showConfirmation) {
            setConfirmation({
                cancel: () => setConfirmationOpen(false),
                action: () => doDeleteInvitationsFromActionLink(invitation, false),
                question: I18n.t("invitations.deleteOneConfirmation"),
                confirmationTxt: I18n.t("confirmationDialog.confirm")
            });
            setConfirmationOpen(true);
        } else {
            deleteInvitation(invitation.id)
                .then(() => {
                    setConfirmationOpen(false);
                    setFlash(I18n.t("invitations.deleteFlash"));
                    setPaginationQueryParams({...paginationQueryParams});
                })
        }
    };

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

    const actionIcons = invitation => {
        if (!selectedInvitations[invitation.id].allowed) {
            return null;
        }
        return (
            <div className="admin-icons">
                <div onClick={() => doResendInvitationsFromActionLink(invitation, true)}>
                    <Tooltip standalone={true}
                             anchorId={"remove-members"}
                             tip={I18n.t("tooltips.resendOneInvitation")}
                             children={
                                 <ResendIcon/>
                             }/>
                </div>
                <div onClick={() => doDeleteInvitationsFromActionLink(invitation, true)}>
                    <Tooltip standalone={true}
                             anchorId={"remove-members"}
                             tip={I18n.t("tooltips.removeOneInvitation")}
                             children={
                                 <TrashIcon/>
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
            key: "intended_authority",
            header: I18n.t("users.authority"),
            mapper: invitation => <Chip type={chipTypeForUserRole(invitation.intended_authority)}
                                        label={I18n.t(`access.${invitation.intended_authority}`)}/>
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
                <span className="name">{invitation.name}</span>
                <span className="email">{invitation.inviter_email}</span>
            </div>
        },
        {
            key: "created_at",
            header: I18n.t("invitations.createdAt"),
            mapper: invitation => shortDateFromEpoch(invitation.created_at, false)
        },
        {
            key: "expiry_date",
            header: I18n.t("invitations.expiryDate"),
            mapper: invitation => invitationExpiry(invitation)
        },
        {
            key: "adminIcons",
            nonSortable: true,
            header: "",
            mapper: invitation => actionIcons(invitation)
        }];

    return (<div className="mod-invitations">
        {confirmationOpen && <ConfirmationDialog isOpen={confirmationOpen}
                                                 cancel={confirmation.cancel}
                                                 confirm={confirmation.action}
                                                 confirmationTxt={confirmation.confirmationTxt}
                                                 question={confirmation.question}/>}
        <Entities entities={invitations}
                  modelName="invitations"
                  defaultSort="email"
                  columns={columns}
                  newLabel={I18n.t("invitations.newInvite")}
                  showNew={!!role && isUserAllowed(AUTHORITIES.MANAGER, user) && !role.unknownInManage}
                  newEntityFunc={role ? () => navigate("/invitation/new", {state: role.id}) : null}
                  customNoEntities={I18n.t(`invitations.noResults`)}
                  loading={false}
                  onHover={true}
                  actions={actionButtons()}
                  searchAttributes={["name", "email", "schacHomeOrganization", "inviter__email", "inviter__name"]}
                  customSearch={search}
                  totalElements={totalElements}
                  inputFocus={!searching}
                  hideTitle={searching}
                  busy={searching}
        >
        </Entities>
    </div>)

}
