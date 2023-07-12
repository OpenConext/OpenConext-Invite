import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "./UserRoles.scss";
import {Chip, ChipType, Tooltip} from "@surfnet/sds";
import {Entities} from "../components/Entities";
import {ReactComponent as UserIcon} from "@surfnet/sds/icons/functional-icons/id-2.svg";
import "./Users.scss";
import {useAppStore} from "../stores/AppStore";
import {dateFromEpoch, futureDate} from "../utils/Date";
import {useNavigate} from "react-router-dom";
import {chipTypeForUserRole} from "../utils/Authority";
import {allowedToRenewUserRole} from "../utils/UserRole";
import {DateField} from "../components/DateField";
import ConfirmationDialog from "../components/ConfirmationDialog";
import {updateUserRoleEndData} from "../api";


export const UserRoles = ({role, userRoles}) => {
    const navigate = useNavigate();
    const {user, setFlash} = useAppStore(state => state);
    const [confirmation, setConfirmation] = useState({});
    const [confirmationOpen, setConfirmationOpen] = useState(false);

    useEffect(() => {
            userRoles.forEach(userRole => {
                userRole.name = userRole.userInfo.name;
                userRole.email = userRole.userInfo.email;
                userRole.schacHomeOrganization = userRole.userInfo.schacHomeOrganization;
            })
        },
        [userRoles])


    const doUpdateEndDate = (userRole, newEndDate, showConfirmation) => {
        if (showConfirmation) {
            setConfirmation({
                cancel: () => setConfirmationOpen(false),
                action: () => doUpdateEndDate(userRole, newEndDate, false),
                question: I18n.t("userRoles.updateConfirmation", {roleName: userRole.role.name, userName: userRole.userInfo.name}),
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

    const displayEndDate = userRole => {
        if (allowedToRenewUserRole(user, userRole) && userRole.endDate) {
            return (
                <div className={"date-field-container"}>
                    <DateField
                        minDate={futureDate(1)}
                        value={new Date(userRole.endDate * 1000)}
                        onChange={date => doUpdateEndDate(userRole, date, true)}
                        allowNull={false}
                        showYearDropdown={true}
                    />
                </div>
            );
        }
        return dateFromEpoch(userRole.endDate)
    }

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
                  newLabel={I18n.t("invitations.new")}
                  showNew={true}
                  newEntityFunc={() => navigate("/invitation/new", {state: role.id})}
                  customNoEntities={I18n.t(`userRoles.noResults`)}
                  loading={false}
                  hideTitle={true}
                  searchAttributes={["name", "email", "schacHomeOrganization"]}
                  inputFocus={true}>
        </Entities>
    </div>)

}
