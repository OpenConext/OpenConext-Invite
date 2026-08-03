import React, {useCallback, useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "../components/Entities.scss";
import {Chip, ChipType, Loader, Tooltip} from "@surfnet/sds";
import {Entities} from "../components/Entities";
import {applicationManagers, institutionAdmins, removeApplicationManager, removeInstitutionAdmin} from "../api";
import UserIcon from "@surfnet/sds/icons/functional-icons/id-2.svg";
import "./InstitutionAdmins.scss";
import {useNavigate} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import {dateFromEpoch, shortDateFromEpoch} from "../utils/Date";
import {AUTHORITIES} from "../utils/UserRole";
import {chipTypeForUserRole} from "../utils/Authority";
import TrashIcon from "@surfnet/sds/icons/functional-icons/bin.svg";
import ConfirmationDialog from "../components/ConfirmationDialog";
import {isEmpty} from "../utils/Utils";

export const InstitutionAdmins = () => {

    const {user: currentUser, setFlash} = useAppStore(state => state);

    const [confirmation, setConfirmation] = useState({});
    const [confirmationOpen, setConfirmationOpen] = useState(false);
    const [admins, setAdmins] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    const loadAdmins = useCallback(() => {
        Promise.all([applicationManagers(), institutionAdmins(true)])
            .then(res => {
                setAdmins(res[0].concat(res[1]));
            })
            .finally(() => {
                setLoading(false);
            });
    }, []);

    useEffect(() => {
        if (!currentUser.institutionAdmin || !currentUser.organizationGUID) {
            navigate("/home");
            return;
        }
        loadAdmins();
    }, [currentUser.institutionAdmin, currentUser.organizationGUID, loadAdmins, navigate]);

    if (loading) {
        return <Loader/>
    }

    const columns = [
        {
            nonSortable: true,
            key: "icon",
            header: "",
            mapper: user => <div className="member-icon">
                <Tooltip standalone={true}
                         children={<UserIcon/>}
                         tip={I18n.t("tooltips.userIcon",
                             {
                                 name: user.name,
                                 createdAt: dateFromEpoch(user.createdAt, false),
                                 lastActivity: dateFromEpoch(user.lastActivity, false)
                             })}/>
            </div>
        },
        {
            key: "name",
            header: I18n.t("users.name_email"),
            mapper: user => (
                <div className="user-name-email">
                    <span className="name">{user.name}</span>
                    <span className="email">{user.email}</span>
                </div>)
        },
        {
            key: "schac_home_organization",
            header: I18n.t("users.schacHomeOrganization"),
            mapper: user => <span>{user.schac_home_organization}</span>
        },
        {
            key: "authority",
            header: I18n.t("users.highestAuthority"),
            mapper: user => {
                const authority = user.institution_admin ? AUTHORITIES.INSTITUTION_ADMIN : AUTHORITIES.APPLICATION_MANAGER;
                return <Chip type={chipTypeForUserRole(authority)}
                             label={I18n.t(`access.${authority}`)}/>
            }
        },
        {
            key: "createdAt",
            header: I18n.t("users.createdAt"),
            mapper: user => shortDateFromEpoch(user.createdAt, false)
        },
        {
            key: "lastActivity",
            header: I18n.t("users.lastActivity"),
            mapper: user => shortDateFromEpoch(user.lastActivity, false)
        },
        {
            key: "roles",
            header: I18n.t("users.roles"),
            mapper: user => isEmpty(user.roles) ? "-" :
                <ul>{user.roles.split(",").map(role => <li key={role}>{role}</li>)}</ul>
        },
        {
            nonSortable: true,
            key: "actions",
            header: "",
            mapper: user => (currentUser.id !== user.id) ?
                ((user.institution_admin_by_invite || !isEmpty(user.roles)) ? <
                    span onClick={() => doRemoveInstitutionAdmin(user, true)}>
                    <TrashIcon/>
                </span> : null)
                : <Chip type={ChipType.Main_400} label={I18n.t("forms.you")}/>
        }
    ].filter(column => column !== null);

    const doRemoveInstitutionAdmin = (user, showConfirmation) => {
        const isInstitutionAdmin = user.institution_admin;
        if (showConfirmation) {
            setConfirmation({
                cancel: () => setConfirmationOpen(false),
                action: () => doRemoveInstitutionAdmin(user, false),
                question: I18n.t("users.removeInstitutionAdminConfirmation", {
                    userName: user.name
                }),
                confirmationTxt: I18n.t("confirmationDialog.confirm"),
                confirmationHeader: I18n.t("confirmationDialog.title")
            });
            setConfirmationOpen(true);
        } else {
            setLoading(true);
            const promise = isInstitutionAdmin ? removeInstitutionAdmin(user) : removeApplicationManager(user);
            promise
                .then(() => {
                    setConfirmationOpen(false);
                    setFlash(I18n.t("users.removeInstitutionAdminFlash", {userName: user.name}));
                    loadAdmins();
                }).finally(() => {
                setLoading(false);
            })
        }
    };

    return (
        <div className="mod-users">
            {confirmationOpen && <ConfirmationDialog isOpen={confirmationOpen}
                                                     cancel={confirmation.cancel}
                                                     confirm={confirmation.action}
                                                     confirmationTxt={confirmation.confirmationTxt}
                                                     confirmationHeader={confirmation.confirmationHeader}
                                                     isError={confirmation.error}
                                                     question={confirmation.question}/>}
            <Entities entities={admins}
                      modelName="institutionAdmins"
                      defaultSort="name"
                      columns={columns}
                      newLabel={currentUser.superUser ? I18n.t("invitations.newInvite") : null}
                      showNew={false}
                      customNoEntities={I18n.t(`users.noResults`)}
                      searchAttributes={["name", "email", "schacHomeOrganization"]}
                      inputFocus={true}
            />
        </div>
    );

}
