import React, {useEffect, useState} from "react";
import {useLocation, useNavigate} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import I18n from "../locale/I18n";
import {allowedAuthoritiesForInvitation, AUTHORITIES, isUserAllowed, markAndFilterRoles} from "../utils/UserRole";
import {ReactComponent as UserIcon} from "@surfnet/sds/icons/functional-icons/id-2.svg";
import {newInvitation, rolesByApplication} from "../api";
import {Button, ButtonType, Checkbox, Loader} from "@surfnet/sds";
import "./InvitationForm.scss";
import {UnitHeader} from "../components/UnitHeader";
import InputField from "../components/InputField";
import {isEmpty} from "../utils/Utils";
import ErrorIndicator from "../components/ErrorIndicator";
import SelectField from "../components/SelectField";
import ConfirmationDialog from "../components/ConfirmationDialog";
import {DateField} from "../components/DateField";
import EmailField from "../components/EmailField";
import {futureDate} from "../utils/Date";

export const InvitationForm = () => {
    const location = useLocation();
    const navigate = useNavigate();
    // const nameRef = useRef();

    const [roles, setRoles] = useState([]);
    const [selectedRoles, setSelectedRoles] = useState([]);
    const [invitation, setInvitation] = useState({
        expiryDate: futureDate(30),
        invites: [],
        intendedAuthority: AUTHORITIES.GUEST
    });
    const {user, setFlash} = useAppStore(state => state);
    const [loading, setLoading] = useState(true);
    const [initial, setInitial] = useState(true);
    const required = ["intendedAuthority", "invites"];
    const [confirmation, setConfirmation] = useState({});
    const [confirmationOpen, setConfirmationOpen] = useState(false);

    useEffect(() => {
        if (!isUserAllowed(AUTHORITIES.INVITER, user)) {
            navigate("/404");
            return;
        }
        if (isUserAllowed(AUTHORITIES.MANAGER, user)) {
            rolesByApplication()
                .then(res => {
                    const markedRoles = markAndFilterRoles(user, res);
                    return setRoles(markedRoles);
                })
            setLoading(false);
        } else {
            setRoles(markAndFilterRoles(user, []))
        }
        const breadcrumbPath = [
            {path: "/home", value: I18n.t("tabs.home")},
            {path: "/home/roles", value: I18n.t("tabs.roles")},
        ];
        useAppStore.setState({breadcrumbPath: breadcrumbPath});
        setLoading(false);
    }, [user]);// eslint-disable-line react-hooks/exhaustive-deps

    useEffect(() => {
        if (location.state) {
            // See markAndFilterRoles - we are mixing up userRoles and roles
            const initialRole = roles.find(role => role.value === location.state);
            if (initialRole) {
                setSelectedRoles([initialRole])
            }
        }
    }, [roles, location.state])// eslint-disable-line react-hooks/exhaustive-deps

    const submit = () => {
        setInitial(false);
        if (isValid()) {
            const invitationRequest = {...invitation, roleIdentifiers: selectedRoles.map(role => role.value)}
            newInvitation(invitationRequest)
                .then(() => {
                    setFlash(I18n.t("invitations.createFlash"));
                    navigate("/home/roles");
                }).catch(handleError);
        }
    }

    const handleError = e => {
        e.response.json().then(j => {
            const reference = j.reference;
            setConfirmation({
                cancel: null,
                action: () => setConfirmationOpen(false),
                warning: false,
                error: true,
                question: I18n.t("forms.error", {reference: reference}),
                confirmationTxt: I18n.t("forms.ok")
            });
            setConfirmationOpen(true);
        })
    }

    const isValid = () => {
        return required.every(attr => !isEmpty(invitation[attr])) && !isEmpty(selectedRoles);
    }

    const addEmails = emails => {
        setInvitation({...invitation, invites: emails});
    }

    const removeMail = mail => {
        setInvitation({...invitation, invites: invitation.invites.filter(email => mail !== email)});
    }

    const rolesChanged = selectedOptions => {
        if (selectedOptions === null) {
            setSelectedRoles([])
        } else {
            const newSelectedOptions = Array.isArray(selectedOptions) ? [...selectedOptions] : [selectedOptions];
            setSelectedRoles(newSelectedOptions);
            const allDefaultExpiryDays = newSelectedOptions
                .filter(option => option.defaultExpiryDays)
                .map(option => option.defaultExpiryDays)
                .sort();
            if (!isEmpty(allDefaultExpiryDays) && !invitation.roleExpiryDate) {
                setInvitation({...invitation, roleExpiryDate: futureDate(allDefaultExpiryDays[0])});
            }
        }
    }

    const renderForm = () => {
        const disabledSubmit = !initial && !isValid();
        const authorityOptions = allowedAuthoritiesForInvitation(user, selectedRoles)
            .map(authorityOption => ({value: authorityOption, label: I18n.t(`access.${authorityOption}`)}));
        return (
            <>
                <EmailField
                    name={I18n.t("invitations.invitees")}
                    addEmails={addEmails}
                    emails={invitation.invites}
                    isAdmin={false}
                    pinnedEmails={[]}
                    removeMail={removeMail}
                    error={!initial && isEmpty(invitation.invites)}/>

                {(!initial && isEmpty(invitation.invites)) &&
                    <ErrorIndicator msg={I18n.t("invitations.requiredEmail")}/>}

                <Checkbox name={I18n.t("invitations.enforceEmailEquality")}
                          value={invitation.enforceEmailEquality || false}
                          info={I18n.t("invitations.enforceEmailEquality")}
                          onChange={e => setInvitation({...invitation, enforceEmailEquality: e.target.checked})}
                          tooltip={I18n.t("tooltips.enforceEmailEqualityTooltip")}
                />

                <SelectField
                    value={authorityOptions.find(option => option.value === invitation.intendedAuthority)
                        || authorityOptions[authorityOptions.length - 1]}
                    options={authorityOptions}
                    name={I18n.t("invitations.intendedAuthority")}
                    searchable={false}
                    onChange={option => setInvitation({...invitation, intendedAuthority: option.value})}
                    toolTip={I18n.t("tooltips.intendedAuthorityTooltip")}
                    clearable={false}
                />

                <SelectField value={selectedRoles}
                             options={roles.filter(role => !selectedRoles.find(r => r.value === role.value))}
                             name={I18n.t("invitations.roles")}
                             toolTip={I18n.t("tooltips.rolesTooltip")}
                             isMulti={true}
                             error={!initial && isEmpty(selectedRoles)}
                             searchable={true}
                             placeholder={I18n.t("invitations.rolesPlaceHolder")}
                             onChange={rolesChanged}/>
                {(!initial && isEmpty(selectedRoles)) &&
                    <ErrorIndicator msg={I18n.t("invitations.requiredRole")}/>}

                <DateField value={invitation.roleExpiryDate}
                           onChange={e => setInvitation({...invitation, roleExpiryDate: e})}
                           allowNull={true}
                           showYearDropdown={true}
                           name={I18n.t("invitations.expiryDateRole")}
                           toolTip={I18n.t("tooltips.expiryDateRoleTooltip")}/>

                <InputField value={invitation.message}
                            onChange={e => setInvitation({...invitation, message: e.target.value})}
                            placeholder={I18n.t("invitations.messagePlaceholder")}
                            name={I18n.t("invitations.message")}
                            large={true}
                            multiline={true}/>

                <DateField value={invitation.expiryDate}
                           onChange={e => setInvitation({...invitation, expiryDate: e})}
                           allowNull={false}
                           showYearDropdown={true}
                           maxDate={futureDate(30)}
                           name={I18n.t("invitations.expiryDate")}
                           toolTip={I18n.t("tooltips.expiryDateTooltip")}/>

                <section className="actions">
                    <Button type={ButtonType.Secondary}
                            txt={I18n.t("forms.cancel")}
                            onClick={() => navigate(-1)}/>
                    <Button disabled={disabledSubmit}
                            txt={I18n.t("invitations.invite")}
                            onClick={submit}/>
                </section>


            </>
        );
    }

    if (loading) {
        return <Loader/>
    }
    return (
        <div className={"mod-invitation-form"}>
            {confirmationOpen && <ConfirmationDialog isOpen={confirmationOpen}
                                                     cancel={confirmation.cancel}
                                                     confirm={confirmation.action}
                                                     confirmationTxt={confirmation.confirmationTxt}
                                                     isWarning={confirmation.warning}
                                                     isError={confirmation.error}
                                                     question={confirmation.question}/>}

            <UnitHeader
                obj={({
                    name: I18n.t(`invitations.new`),
                    svg: UserIcon,
                    style: "small"
                })}/>
            <div className={"invitation-form"}>
                {renderForm()}
            </div>
        </div>
    );
}