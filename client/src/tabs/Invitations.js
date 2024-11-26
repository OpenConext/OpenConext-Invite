import React, {useEffect, useRef, useState} from "react";
import I18n from "../locale/I18n";
import "./Invitations.scss";
import {Button, ButtonSize, ButtonType, Checkbox, Chip, Loader, Tooltip} from "@surfnet/sds";
import {Entities} from "../components/Entities";
import "./Users.scss";
import {shortDateFromEpoch} from "../utils/Date";

import {chipTypeForUserRole, invitationExpiry} from "../utils/Authority";
import {useNavigate} from "react-router-dom";
import {allInvitations, deleteInvitation, invitationsByRoleId, resendInvitation} from "../api";
import ConfirmationDialog from "../components/ConfirmationDialog";
import {useAppStore} from "../stores/AppStore";
import {isEmpty, pseudoGuid} from "../utils/Utils";
import {allowedToDeleteInvitation, AUTHORITIES, INVITATION_STATUS, isUserAllowed} from "../utils/UserRole";
import {UnitHeader} from "../components/UnitHeader";
import Select from "react-select";

const allValue = "all";
const mineValue = "mine";

export const Invitations = ({
                                role,
                                preloadedInvitations,
                                standAlone = false,
                                systemView = false,
                                history = false,
                                pending = true
                            }) => {
    const navigate = useNavigate();
    const {user, setFlash} = useAppStore(state => state);
    const invitations = useRef();
    const [selectedInvitations, setSelectedInvitations] = useState({});
    const [allSelected, setAllSelected] = useState(false);
    const [resultAfterSearch, setResultAfterSearch] = useState([])
    const [loading, setLoading] = useState(true);
    const [confirmation, setConfirmation] = useState({});
    const [confirmationOpen, setConfirmationOpen] = useState(false);
    const [filterOptions, setFilterOptions] = useState([]);
    const [filterValue, setFilterValue] = useState(null);

    useEffect(() => {
            const promise = systemView ? allInvitations() : (isEmpty(role) ? Promise.resolve(preloadedInvitations) : invitationsByRoleId(role.id));
            if (history) {
                useAppStore.setState({
                    breadcrumbPath: [
                        {path: "/inviter", value: I18n.t("tabs.home")},
                        {value: I18n.t("tabs.invitations")}
                    ]
                });
            }
            promise.then(res => {
                res.forEach(invitation => {
                    invitation.intendedRoles = invitation.roles
                        .sort((r1, r2) => r1.role.name.localeCompare(r2.role.name))
                        .map(role => role.role.name).join(", ");
                    const now = new Date();
                    invitation.status = new Date(invitation.expiryDate * 1000) < now ? INVITATION_STATUS.EXPIRED : invitation.status;
                });
                setSelectedInvitations(res
                    .reduce((acc, invitation) => {
                        acc[invitation.id] = {
                            selected: false,
                            ref: invitation,
                            allowed: allowedToDeleteInvitation(user, invitation)
                        };
                        return acc;
                    }, {}));
                invitations.current = res;
                const newFilterOptions = [{
                    label: I18n.t("invitations.statuses.all", {nbr: res.length}),
                    value: allValue
                }];
                const statusOptions = res.reduce((acc, invitation) => {
                    const option = acc.find(opt => opt.status === invitation.status);
                    if (option) {
                        ++option.nbr;
                    } else {
                        acc.push({status: invitation.status, nbr: 1})
                    }
                    return acc;
                }, []).map(option => ({
                    label: `${I18n.t("invitations.statuses." + option.status.toLowerCase())} (${option.nbr})`,
                    value: option.status
                })).concat({
                    label: `${I18n.t("invitations.statuses.mine")} (${res.filter(inv => inv.inviter.email === user.email).length})`,
                    value: mineValue
                }).sort((o1, o2) => o1.label.localeCompare(o2.label));

                setFilterOptions(newFilterOptions.concat(statusOptions));
                setFilterValue(newFilterOptions[0]);

                setResultAfterSearch(res);
                //we need to avoid flickerings
                setTimeout(() => setLoading(false), 75);
            })
        },
        [invitations, user]) // eslint-disable-line react-hooks/exhaustive-deps

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
                {pending &&
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
                </div>}
            </div>);
    }

    const filter = () => {
        return (
            <div className="invitations-filter">
                <Select
                    className={"invitations-filter-select"}
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

    const getActions = () => {
        const actions = [];
        actions.push({
            buttonType: ButtonType.Primary,
            name: I18n.t("inviter.sendInvite"),
            perform: () => {
                navigate(`/invitation/new`)
            }
        });
        return actions;
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
            key: "intendedAuthority",
            header: I18n.t("users.authority"),
            mapper: invitation => <Chip type={chipTypeForUserRole(invitation.intendedAuthority)}
                                        label={I18n.t(`access.${invitation.intendedAuthority}`)}/>
        },
        {
            key: "intendedRoles",
            header: I18n.t("invitations.intendedRoles"),
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
            key: "createdAt",
            header: I18n.t("invitations.createdAt"),
            mapper: invitation => shortDateFromEpoch(invitation.createdAt)
        },
        {
            key: pending ? "expiryDate" : "acceptedAt",
            header: I18n.t(pending ? "invitations.expiryDate" : "invitations.acceptedAt"),
            mapper: invitation => pending ? invitationExpiry(invitation) : shortDateFromEpoch(invitation.acceptedAt)
        }];
    const filteredInvitations = filterValue.value === allValue ? invitations.current :
        invitations.current.filter(invitation => invitation.status === filterValue.value ||
            (filterValue.value === mineValue && invitation.inviter.email === user.email)
        );
    const countInvitations = filteredInvitations.length;
    const hasEntities = countInvitations > 0;
    let title = " ";

    if (hasEntities) {
        title = I18n.t(`invitations.${standAlone ? "found" : "foundWithStatus"}`, {
            count: countInvitations,
            status: pending ? I18n.t("invitations.pending") : I18n.t("invitations.accepted").toLowerCase(),
            plural: I18n.t(`invitations.${countInvitations === 1 ? "singleInvitation" : "multipleInvitations"}`)
        })
    }

    return (<div className="mod-invitations">
        {confirmationOpen && <ConfirmationDialog isOpen={confirmationOpen}
                                                 cancel={confirmation.cancel}
                                                 confirm={confirmation.action}
                                                 confirmationTxt={confirmation.confirmationTxt}
                                                 question={confirmation.question}/>}
        {history && <UnitHeader obj={{name: I18n.t("inviter.history")}} actions={getActions()}/>}
        <Entities entities={filteredInvitations}
                  modelName="invitations"
                  defaultSort="email"
                  columns={columns}
                  title={title}
                  newLabel={I18n.t("invitations.newInvite")}
                  showNew={!!role && (isUserAllowed(AUTHORITIES.MANAGER, user) || standAlone) && !role.unknownInManage}
                  newEntityFunc={role ? () => navigate("/invitation/new", {state: role.id}) : null}
                  customNoEntities={I18n.t(`invitations.noResults`)}
                  loading={loading}
                  hideTitle={loading}
                  filters={filter(filterOptions, filterValue)}
                  actions={actionButtons()}
                  searchCallback={searchCallback}
                  searchAttributes={["name", "email", "schacHomeOrganization", "inviter__email", "inviter__name"]}
                  inputFocus={true}>
        </Entities>
    </div>)

}
