import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "./UserRoles.scss";
import {Button, ButtonSize, ButtonType, Checkbox, Chip, ChipType, Loader, Tooltip} from "@surfnet/sds";
import {Entities} from "../components/Entities";
import {ReactComponent as AlarmBell} from "../icons/alarm_bell.svg";
import "./Users.scss";
import {useAppStore} from "../stores/AppStore";
import {dateFromEpoch, futureDate, shortDateFromEpoch} from "../utils/Date";
import {useNavigate} from "react-router-dom";
import {chipTypeForUserRole} from "../utils/Authority";
import {allowedToRenewUserRole, AUTHORITIES, highestAuthority, isUserAllowed} from "../utils/UserRole";
import ConfirmationDialog from "../components/ConfirmationDialog";
import {deleteUserRole, updateUserRoleEndData} from "../api";
import {isEmpty, pseudoGuid} from "../utils/Utils";
import {MinimalDateField} from "../components/MinimalDateField";

const oneMonthMillis = 1000 * 60 * 60 * 24 * 30;

export const UserRoles = ({role, guests, userRoles}) => {
    const navigate = useNavigate();
    const {user, setFlash, config} = useAppStore(state => state);

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
                        allowed: allowedToRenewUserRole(user, userRole, true, guests)
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
                confirmationTxt: I18n.t("confirmationDialog.confirm"),
                confirmationHeader: I18n.t("confirmationDialog.title")
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

    const willUpdateCurrentUser = () => {
        return Object.entries(selectedUserRoles)
            .filter(entry => (entry[1].selected) && entry[1].allowed && entry[1]?.ref?.userInfo?.id === user.id)
            .map(entry => parseInt(entry[0]))
            .some(id => resultAfterSearch.some(res => res.id === id));
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
            const deleteCurrentUserRole = willUpdateCurrentUser();
            Promise.all(identifiers.map(identifier => deleteUserRole(identifier, guests )))
                .then(() => {
                    setConfirmationOpen(false);
                    setFlash(I18n.t("userRoles.deleteFlash"));
                    if (deleteCurrentUserRole) {
                        useAppStore.setState(() => ({reload: true}));
                        navigate("/home", {replace: true});
                    } else {
                        const path = encodeURIComponent(window.location.pathname);
                        navigate(`/refresh-route/${path}`, {replace: true});
                    }
                }).catch(handleError);
        }
    };

    const handleError = e => {
        setLoading(false);
        e.response.json().then(j => {
            const reference = j.reference || 999;
            setConfirmation({
                cancel: null,
                action: () => setConfirmationOpen(false),
                warning: false,
                error: true,
                question: I18n.t("forms.error", {reference: reference}),
                confirmationTxt: I18n.t("forms.ok"),
                confirmationHeader: I18n.t("confirmationDialog.error")
            });
            setConfirmationOpen(true);
        })
    }

    const displayExpiryWarning = userRole => {
        const endDateTime = userRole.endDate;
        if (endDateTime == null) {
            return null;
        }
        const now = new Date();
        const endDate = new Date(endDateTime * 1000);
        if (now > endDate) {
            //This will be handled by the MinimalDateField
            return null;
        }
        //show waring indication if endDate is within one month
        if ((now.getTime() + oneMonthMillis) > endDate.getTime()) {
            return (
                <Tooltip standalone={true}
                         children={<div className={"alarm-bell"}><AlarmBell/></div>}
                         tip={I18n.t("tooltips.expiredUserRole",
                             {
                                 date: dateFromEpoch(userRole.endDate)
                             })}/>

            );
        }
        return null;
    }

    const displayEndDate = userRole => {
        const allowed = allowedToRenewUserRole(user, userRole, false);
        if (allowed && userRole.authority !== AUTHORITIES.GUEST) {
            return (
                <MinimalDateField
                        minDate={futureDate(1)}
                        value={userRole.endDate}
                        onChange={date => doUpdateEndDate(userRole, date, true)}
                        pastDatesAllowed={config.pastDateAllowed}
                        allowNull={true}
                        showYearDropdown={true}
                    />
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
                                         txt={I18n.t("userRoles.delete")}/>
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
            key: "expiry-warning",
            nonSortable: true,
            header: "",
            mapper: userRole => displayExpiryWarning(userRole)
        },
        {
            key: "endDate",
            header: I18n.t("roles.endDate"),
            toolTip: I18n.t("tooltips.roleExpiryDateTooltip"),
            mapper: userRole => displayEndDate(userRole)
        },
        {
            key: "createdAt",
            header: I18n.t("userRoles.createdAt"),
            mapper: userRole => shortDateFromEpoch(userRole.createdAt)
        },];

    return (<div className="mod-user-roles">
        {confirmationOpen && <ConfirmationDialog isOpen={confirmationOpen}
                                                 cancel={confirmation.cancel}
                                                 confirm={confirmation.action}
                                                 confirmationTxt={confirmation.confirmationTxt}
                                                 confirmationHeader={confirmation.confirmationHeader}
                                                 isError={confirmation.error}
                                                 question={confirmation.question}/>}

        <Entities entities={userRoles}
                  modelName="userRoles"
                  defaultSort="name"
                  columns={columns}
                  newLabel={I18n.t(guests ? "invitations.newGuest" : isUserAllowed(AUTHORITIES.INSTITUTION_ADMIN, user) ? "invitations.new" : "invitations.newInvitation")}
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
