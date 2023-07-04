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
        roleExpiryDate: futureDate(365),
        invites: [],
        intendedAuthority: AUTHORITIES.GUEST
    });
    const {user, setFlash} = useAppStore(state => state);
    const [loading, setLoading] = useState(true);
    const [initial, setInitial] = useState(true);
    const required = ["intendedAuthority", "invites"];

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
                setInvitation({...invitation, roleExpiryDate: futureDate(initialRole.defaultExpiryDays)})
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
                    navigate(-1);
                });
        }
    }

    const isValid = () => {
        return required.every(attr => !isEmpty(invitation[attr])) && (!isEmpty(selectedRoles) || invitation.intendedAuthority === AUTHORITIES.SUPER_USER);
    }

    const addEmails = emails => {
        const newEmails = [...new Set(invitation.invites.concat(emails))]
        setInvitation({...invitation, invites: newEmails});
    }

    const removeMail = mail => {
        setInvitation({...invitation, invites: invitation.invites.filter(email => mail !== email)});
    }

    const defaultRoleExpiryDate = newRoles => {
        const allDefaultExpiryDays = (newRoles || [])
            .filter(role => role.defaultExpiryDays)
            .map(role => role.defaultExpiryDays)
            .sort();
        if (invitation.intendedAuthority === AUTHORITIES.GUEST) {
            return futureDate(isEmpty(allDefaultExpiryDays) ? 365 : allDefaultExpiryDays[0]);
        }
        return invitation.roleExpiryDate;
    }

    const rolesChanged = selectedOptions => {
        if (selectedOptions === null) {
            setSelectedRoles([])
        } else {
            const newSelectedOptions = Array.isArray(selectedOptions) ? [...selectedOptions] : [selectedOptions];
            setSelectedRoles(newSelectedOptions);
            setInvitation({...invitation, roleExpiryDate: defaultRoleExpiryDate(newSelectedOptions)});
        }
    }

    const authorityChanged = option => {
        setInvitation({
            ...invitation,
            intendedAuthority: option.value,
            roleExpiryDate: defaultRoleExpiryDate(selectedRoles)
        });

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

                <SelectField
                    value={authorityOptions.find(option => option.value === invitation.intendedAuthority)
                        || authorityOptions[authorityOptions.length - 1]}
                    options={authorityOptions}
                    name={I18n.t("invitations.intendedAuthority")}
                    searchable={false}
                    disabled={authorityOptions.length === 1}
                    onChange={authorityChanged}
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
                           showYearDropdown={true}
                           allowNull={invitation.intendedAuthority !== AUTHORITIES.GUEST}
                           minDate={futureDate(1, invitation.expiryDate)}
                           name={I18n.t("invitations.roleExpiryDate")}
                           toolTip={I18n.t("tooltips.roleExpiryDateTooltip")}/>

                <Checkbox name={I18n.t("invitations.enforceEmailEquality")}
                          value={invitation.enforceEmailEquality || false}
                          info={I18n.t("invitations.enforceEmailEquality")}
                          onChange={e => setInvitation({...invitation, enforceEmailEquality: e.target.checked})}
                          tooltip={I18n.t("tooltips.enforceEmailEqualityTooltip")}
                />

                <Checkbox name={I18n.t("invitations.eduIDOnly")}
                          value={invitation.eduIDOnly || false}
                          info={I18n.t("invitations.eduIDOnly")}
                          onChange={e => setInvitation({...invitation, eduIDOnly: e.target.checked})}
                          tooltip={I18n.t("tooltips.eduIDOnlyTooltip")}
                />

                <InputField value={invitation.message}
                            onChange={e => setInvitation({...invitation, message: e.target.value})}
                            placeholder={I18n.t("invitations.messagePlaceholder")}
                            name={I18n.t("invitations.message")}
                            large={true}
                            multiline={true}/>

                <DateField value={invitation.expiryDate}
                           onChange={e => setInvitation({...invitation, expiryDate: e})}
                           showYearDropdown={true}
                           minDate={futureDate(1)}
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