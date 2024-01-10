import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "./UserRoles.scss";
import {Button, ButtonSize, ButtonType, Checkbox, Chip, ChipType, Loader, Tooltip} from "@surfnet/sds";
import {Entities} from "../components/Entities";
import "./Users.scss";
import {useAppStore} from "../stores/AppStore";
import {dateFromEpoch, futureDate, shortDateFromEpoch} from "../utils/Date";
import {useNavigate} from "react-router-dom";
import {chipTypeForUserRole} from "../utils/Authority";
import {allowedToRenewUserRole, AUTHORITIES, highestAuthority, isUserAllowed} from "../utils/UserRole";
import {DateField} from "../components/DateField";
import ConfirmationDialog from "../components/ConfirmationDialog";
import {deleteUserRole, updateUserRoleEndData} from "../api";
import {isEmpty, pseudoGuid} from "../utils/Utils";


export const UserRoles = ({role, guests, userRoles}) => {
    const navigate = useNavigate();
    const {user, setFlash} = useAppStore(state => state);

    const [selectedUserRoles, setSelectedUserRoles] = useState({});
    const [allSelected, setAllSelected] = useState(false);
    const [resultAfterSearch, setResultAfterSearch] = useState(userRoles);
    const [confirmation, setConfirmation] = useState({});
    const [confirmationOpen, setConfirmationOpen] = useState(false);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
            userRoles.forEach(userRole => {
                userRole.name = userRole.userInfo.name;
                userRole.email = userRole.userInfo.email;
                userRole.schacHomeOrganization = userRole.userInfo.schacHomeOrganization;
            });
            setSelectedUserRoles(userRoles
                .reduce((acc, userRole) => {
                    acc[userRole.id] = {
                        selected: false,
                        ref: userRole,
                        allowed: allowedToRenewUserRole(user, userRole)
                    };
                    return acc;
                }, {}));
            setLoading(false);
        },
        [userRoles, user])

    if (loading) {
        return <Loader/>
    }

    const showCheckAllHeader = () => {
        return Object.entries(selectedUserRoles)
            .filter(entry => entry[1].allowed)
            .length > 0;
    }

    const doUpdateEndDate = (userRole, newEndDate, showConfirmation) => {
        if (showConfirmation) {
            setConfirmation({
                cancel: () => setConfirmationOpen(false),
                action: () => doUpdateEndDate(userRole, newEndDate, false),
                question: I18n.t(`userRoles.${isEmpty(newEndDate) ? "updateConfirmationRemoveEndDate" : "updateConfirmation"}`, {
                    roleName: userRole.role.name,
                    userName: userRole.userInfo.name
                }),
                confirmationTxt: I18n.t("confirmationDialog.confirm")
            });
            setConfirmationOpen(true);
        } else {
            updateUserRoleEndData(userRole.id, newEndDate)
                .then(() => {
                    setConfirmationOpen(false);
                    setFlash(I18n.t("userRoles.updateFlash", {roleName: userRole.role.name}));
                    const path = encodeURIComponent(window.location.pathname);
                    navigate(`/refresh-route/${path}`, {replace: true});
                })
        }
    };

    const onCheck = userRole => e => {
        const checked = e.target.checked;
        const newSelectedUserRoles = {...selectedUserRoles}
        newSelectedUserRoles[userRole.id].selected = checked;
        setSelectedUserRoles(newSelectedUserRoles);
        if (!checked) {
            setAllSelected(false);
        }
    }

    const selectAll = e => {
        const checked = e.target.checked;
        setAllSelected(checked);
        const newSelectedUserRoles = {...selectedUserRoles}
        Object.values(newSelectedUserRoles).forEach(inv => inv.selected = checked);
        setSelectedUserRoles(newSelectedUserRoles);
    }

    const userRoleIdentifiers = () => {
        return Object.entries(selectedUserRoles)
            .filter(entry => (entry[1].selected) && entry[1].allowed)
            .map(entry => parseInt(entry[0]))
            .filter(id => resultAfterSearch.some(res => res.id === id));
    }

    const doDeleteUserRoles = showConfirmation => {
        if (showConfirmation) {
            setConfirmation({
                cancel: () => setConfirmationOpen(false),
                action: () => doDeleteUserRoles(false),
                question: I18n.t("userRoles.deleteConfirmation"),
                confirmationTxt: I18n.t("confirmationDialog.confirm")
            });
            setConfirmationOpen(true);
        } else {
            const identifiers = userRoleIdentifiers();
            Promise.all(identifiers.map(identifier => deleteUserRole(identifier)))
                .then(() => {
                    setConfirmationOpen(false);
                    setFlash(I18n.t("userRoles.deleteFlash"));
                    const path = encodeURIComponent(window.location.pathname);
                    navigate(`/refresh-route/${path}`, {replace: true});
                })
        }
    };

    const displayEndDate = userRole => {
        if (allowedToRenewUserRole(user, userRole)) {
            return (
                <div className={"date-field-container"}>
                    {!userRole.endDate &&
                        <span className="no-end-date">
                        {I18n.t("roles.noEndDate")}
                    </span>}
                    <DateField
                        minDate={futureDate(1)}
                        value={userRole.endDate ? new Date(userRole.endDate * 1000) : null}
                        onChange={date => doUpdateEndDate(userRole, date, true)}
                        allowNull={true}
                        showYearDropdown={true}
                    />
                </div>
            );
        }
        return dateFromEpoch(userRole.endDate)
    }

    const searchCallback = afterSearch => {
        setResultAfterSearch(afterSearch);
    }

    const actionButtons = () => {
        if (isEmpty(userRoleIdentifiers())) {
            return null;
        }
        return (
            <div className="admin-actions">
                <div>
                    <Tooltip standalone={true}
                             anchorId={"remove-members"}
                             tip={I18n.t("tooltips.removeUserRole")}
                             children={
                                 <Button onClick={() => doDeleteUserRoles(true)}
                                         size={ButtonSize.Small}
                                         type={ButtonType.Secondary}
                                         txt={I18n.t("invitations.delete")}/>
                             }/>
                </div>
            </div>);
    }
    const hideCheckbox = highestAuthority(user) === AUTHORITIES.INVITER && !guests;
    const columns = [
        {
            nonSortable: true,
            key: "check",
            header: (!hideCheckbox && showCheckAllHeader()) ?
                <Checkbox value={allSelected}
                          name={"allSelected"}
                          onChange={selectAll}/> : null,
            mapper: userRole => <div className="check">
                {selectedUserRoles[userRole.id].allowed ? <Checkbox name={pseudoGuid()}
                                                                    onChange={onCheck(userRole)}
                                                                    value={selectedUserRoles[userRole.id].selected}/> :
                    (hideCheckbox ? null : <Tooltip tip={I18n.t("userRoles.notAllowed")}/>)}
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
            mapper: userRole => <Chip type={chipTypeForUserRole(guests ? AUTHORITIES.GUEST: userRole.authority)}
                                      label={I18n.t(`access.${guests ? AUTHORITIES.GUEST: userRole.authority}`)}/>
        },
        {
            key: "createdAt",
            header: I18n.t("userRoles.createdAt"),
            mapper: userRole => shortDateFromEpoch(userRole.createdAt)
        },
        {
            key: "endDate",
            header: I18n.t("roles.endDate"),
            toolTip: I18n.t("tooltips.roleExpiryDateTooltip"),
            mapper: userRole => displayEndDate(userRole)
        }];

    return (<div className="mod-user-roles">
        {confirmationOpen && <ConfirmationDialog isOpen={confirmationOpen}
                                                 cancel={confirmation.cancel}
                                                 confirm={confirmation.action}
                                                 confirmationTxt={confirmation.confirmationTxt}
                                                 isWarning={confirmation.warning}
                                                 isError={confirmation.error}
                                                 question={confirmation.question}/>}

        <Entities entities={userRoles}
                  modelName="userRoles"
                  defaultSort="name"
                  columns={columns}
                  newLabel={I18n.t(guests ? "invitations.newGuest" : "invitations.new")}
                  showNew={isUserAllowed(AUTHORITIES.MANAGER, user) && !role.unknownInManage}
                  newEntityFunc={() => navigate(`/invitation/new?maintainer=${guests === false}`, {state: role.id})}
                  customNoEntities={I18n.t(`userRoles.noResults`)}
                  loading={false}
                  title={I18n.t(guests ? "userRoles.guestRoles" : "userRoles.managerRoles", {count: userRoles.length})}
                  actions={actionButtons()}
                  searchCallback={searchCallback}
                  searchAttributes={["name", "email", "schacHomeOrganization"]}
                  inputFocus={true}>
        </Entities>
    </div>)

}
