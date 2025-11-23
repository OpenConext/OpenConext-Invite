import React, {useEffect, useMemo, useState} from "react";
import {useLocation, useNavigate} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import I18n from "../locale/I18n";
import CloseIcon from "@surfnet/sds/icons/functional-icons/close.svg";
import {
    allowedAuthoritiesForInvitation,
    AUTHORITIES,
    highestAuthority,
    isUserAllowed,
    markAndFilterRoles
} from "../utils/UserRole";
import UserIcon from "@surfnet/sds/icons/functional-icons/id-2.svg";
import UpIcon from "@surfnet/sds/icons/functional-icons/arrow-up-2.svg";
import DownIcon from "@surfnet/sds/icons/functional-icons/arrow-down-2.svg";
import {
    eduidIdentityProvider,
    newInvitation,
    organizationGUIDValidation,
    requestedAuthnContextValues,
    rolesByApplication
} from "../api";
import {Button, ButtonType, Loader, Tooltip} from "@surfnet/sds";
import "./InvitationForm.scss";
import {UnitHeader} from "../components/UnitHeader";
import InputField from "../components/InputField";
import {isEmpty, splitListSemantically, stopEvent} from "../utils/Utils";
import ErrorIndicator from "../components/ErrorIndicator";
import SelectField from "../components/SelectField";
import {DateField} from "../components/DateField";
import EmailField from "../components/EmailField";
import {displayExpiryDate, futureDate} from "../utils/Date";
import SwitchField from "../components/SwitchField";
import {InvitationRoleCard} from "../components/InvitationRoleCard";
import DOMPurify from "dompurify";


export const InviterContainer = ({isInviter, children}) => {
    return isInviter ?
        <div className="inviter-wrapper">{children}</div> : <>{children}</>

}

const requestedAuthnContextOptions = Object.entries(I18n.translations[I18n.locale].requestedAuthnContext)
    .map(arr => ({value: arr[0], label: arr[1]}));

export const InvitationForm = () => {
    const location = useLocation();
    const navigate = useNavigate();

    const languageOptions = ["en", "nl"].map(lang => ({label: I18n.t(`languages.${lang}`), value: lang}))
    const {user, setFlash, config} = useAppStore(state => state);

    const [guest, setGuest] = useState(false);
    const [roles, setRoles] = useState([]);
    const [selectedRoles, setSelectedRoles] = useState([]);
    const [originalRoleId, setOriginalRoleId] = useState(null);
    const [invitation, setInvitation] = useState({
        expiryDate: futureDate(30),
        roleExpiryDate: futureDate(366),
        invites: [],
        intendedAuthority: AUTHORITIES.GUEST
    });

    const [validOrganizationGUID, setValidOrganizationGUID] = useState(true);
    const [organizationGUIDIdentityProvider, setOrganizationGUIDIdentityProvider] = useState(null);

    const [displayAdvancedSettings, setDisplayAdvancedSettings] = useState(false);
    const [loading, setLoading] = useState(true);
    const [customExpiryDate, setCustomExpiryDate] = useState(false);
    const [customRoleExpiryDate, setCustomRoleExpiryDate] = useState(false);
    const [initial, setInitial] = useState(true);
    const [eduIDIdP, setEduIDIdP] = useState(null);
    const [acrValues, setACRValues] = useState({});
    const [language, setLanguage] = useState(I18n.locale === "en" ? languageOptions[0] : languageOptions[1]);
    const required = ["intendedAuthority", "invites"];

    const isInviter = highestAuthority(user) === AUTHORITIES.INVITER;

    useEffect(() => {
        if (!isUserAllowed(AUTHORITIES.INVITER, user)) {
            navigate("/404");
            return;
        }
        if (isUserAllowed(AUTHORITIES.INSTITUTION_ADMIN, user)) {
            rolesByApplication(true)
                .then(page => {
                    const markedRoles = markAndFilterRoles(user, page.content, I18n.locale,
                        I18n.t("roles.multiple"), I18n.t("forms.and"), "name", false);
                    setInitialRole(markedRoles);
                    setRoles(markedRoles);
                    setLoading(false);
                })
        } else {
            const markedRoles = markAndFilterRoles(user, [], I18n.locale,
                I18n.t("roles.multiple"), I18n.t("forms.and"), "name", false);
            setInitialRole(markedRoles);
            setRoles(markedRoles)
            setLoading(false);
        }
        const breadcrumbPath = isInviter ? [] : [
            {path: "/home", value: I18n.t("tabs.home")},
            {path: "/home/roles", value: I18n.t("tabs.roles")},
        ];
        useAppStore.setState({breadcrumbPath: breadcrumbPath});
    }, [user]);// eslint-disable-line react-hooks/exhaustive-deps

    const acrWarning = useMemo(() => {
            if (isEmpty(invitation.requestedAuthnContext) || isEmpty(eduIDIdP) || isEmpty(selectedRoles)) {
                return null;
            }
            //Filter out the roles that are linked to applications that are not present in the mfaEntities of the eduIDIdp
            //or where the MFA level does not equal the requestedAuthnContext. If the requestedAuthnContext === TransparentAuthnContext,
            //then we skip the warning
            const mfaEntities = eduIDIdP.mfaEntities;
            const acrValue = acrValues[invitation.requestedAuthnContext];
            const missingEntities = selectedRoles.reduce((acc, role) => {
                const missingMfaApps = role.applicationMaps
                    .filter(app => {
                        const mfa = mfaEntities.find(mfa => mfa.name === app.entityid);
                        return isEmpty(mfa) || (mfa.level !== acrValue && mfa.level !== acrValues.TransparentAuthnContext);
                    });
                if (!isEmpty(missingMfaApps)) {
                    acc.applications = acc.applications.concat(missingMfaApps.map(app => app[`name:${I18n.locale}`] || app["name:en"]));
                    acc.roles.push(role.name)
                }
                return acc;
            }, {applications: [], roles: []})
            if (isEmpty(missingEntities.roles)) {
                return null;
            }
            const roleNames = splitListSemantically(missingEntities.roles, I18n.t("forms.and"));
            const applicationNames = splitListSemantically(missingEntities.applications, I18n.t("forms.and"));
            return DOMPurify.sanitize(I18n.t("invitations.requestedAuthnContextWarning",
                {roles: roleNames, applications: applicationNames}));

        },
        [invitation.requestedAuthnContext, eduIDIdP, selectedRoles, acrValues])

    const setInitialRole = markedRoles => {
        const urlSearchParams = new URLSearchParams(window.location.search);
        const isGuest = urlSearchParams.get("maintainer") !== "true";
        setGuest(isGuest)
        const initialRole = markedRoles.find(role => role.value === location.state);
        if (initialRole) {
            // See markAndFilterRoles - we are mixing up userRoles and roles
            const defaultExpiryDays = initialRole.isUserRole ? initialRole.role.defaultExpiryDays : initialRole.defaultExpiryDays;
            setSelectedRoles([initialRole])
            setInvitation({
                ...invitation,
                intendedAuthority: isGuest ? AUTHORITIES.GUEST : AUTHORITIES.INVITER,
                enforceEmailEquality: initialRole.enforceEmailEquality,
                eduIDOnly: initialRole.eduIDOnly,
                roleExpiryDate: futureDate(defaultExpiryDays)
            })
            setOriginalRoleId(initialRole.isUserRole ? initialRole.role.id : initialRole.id);
        } else {
            let defaultExpiryDays = 366;
            if (markedRoles.length === 1) {
                const role = markedRoles[0]
                defaultExpiryDays = role.isUserRole ? role.role.defaultExpiryDays : role.defaultExpiryDays;
                setSelectedRoles(markedRoles);
            }
            setInvitation({
                ...invitation,
                intendedAuthority: isGuest ? AUTHORITIES.GUEST : AUTHORITIES.INVITER,
                roleExpiryDate: futureDate(defaultExpiryDays)
            })
        }
    }

    const toggleDisplayAdvancedSettings = e => {
        stopEvent(e);
        setDisplayAdvancedSettings(!displayAdvancedSettings);
    }

    const submit = () => {
        setInitial(false);
        if (isValid()) {
            const invitationRequest = {
                ...invitation,
                roleIdentifiers: selectedRoles.map(role => role.value),
                language: language.value
            };
            setLoading(true);
            newInvitation(invitationRequest)
                .then(() => {
                    setLoading(false);
                    setFlash(I18n.t("invitations.createFlash"));
                    if (originalRoleId) {
                        navigate(`/roles/${originalRoleId}/invitations`);
                    } else if (!isEmpty(invitationRequest.roleIdentifiers)) {
                        navigate(`/roles/${invitationRequest.roleIdentifiers[0]}/invitations`);
                    } else {
                        navigate(-1);
                    }
                });
        }
    }

    const isValid = () => {
        return required.every(attr => !isEmpty(invitation[attr])) &&
            (!isEmpty(selectedRoles) || [AUTHORITIES.SUPER_USER, AUTHORITIES.INSTITUTION_ADMIN].includes(invitation.intendedAuthority))
            && (invitation.intendedAuthority !== AUTHORITIES.INSTITUTION_ADMIN || !user.superUser || validOrganizationGUID)
            && !(user.superUser && invitation.intendedAuthority === AUTHORITIES.INSTITUTION_ADMIN && isEmpty(invitation.organizationGUID))
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

    const validateOrganizationGUID = e => {
        const organizationGUID = e.target.value;
        if (!isEmpty(organizationGUID)) {
            organizationGUIDValidation(organizationGUID)
                .then(idp => {
                    setOrganizationGUIDIdentityProvider(idp);
                    setValidOrganizationGUID(true);
                })
                .catch(() => {
                    setOrganizationGUIDIdentityProvider(null);
                    setValidOrganizationGUID(false);
                })
        }
    }

    const eduIDOnlyChanged = val => {
        const requestedAuthnContext = val ? invitation.requestedAuthnContext : null;
        setInvitation({...invitation, eduIDOnly: val, requestedAuthnContext: requestedAuthnContext})
    }

    const requestedAuthnContextChanged = option => {
        setInvitation({...invitation, requestedAuthnContext: option ? option.value : null});
        if (option && isEmpty(eduIDIdP)) {
            Promise.all([eduidIdentityProvider(), requestedAuthnContextValues()]).then(res => {
                setEduIDIdP(res[0]);
                setACRValues(res[1])
            });
        }
    }

    const rolesChanged = selectedOptions => {
        if (selectedOptions === null) {
            setSelectedRoles([])
            setInvitation({...invitation, enforceEmailEquality: false, eduIDOnly: false})
        } else {
            const allowedAuthorities = allowedAuthoritiesForInvitation(user, selectedOptions);
            let intendedAuthority = invitation.intendedAuthority;
            //If the chosen authority is no longer allowed, then change it
            if (!allowedAuthorities.includes(invitation.intendedAuthority)) {
                intendedAuthority = allowedAuthorities[0];
            }
            const newSelectedOptions = Array.isArray(selectedOptions) ? [...selectedOptions] : [selectedOptions];
            setSelectedRoles(newSelectedOptions);
            const overrideSettingsAllowed = selectedRoles.every(role => role.overrideSettingsAllowed);
            let enforceEmailEquality = invitation.enforceEmailEquality;
            let eduIDOnly = invitation.eduIDOnly;
            if (!overrideSettingsAllowed) {
                enforceEmailEquality = newSelectedOptions.some(role => role.enforceEmailEquality) || enforceEmailEquality;
                eduIDOnly = newSelectedOptions.some(role => role.eduIDOnly) || eduIDOnly;
            }
            setInvitation({
                ...invitation,
                intendedAuthority: intendedAuthority,
                enforceEmailEquality: enforceEmailEquality,
                eduIDOnly: eduIDOnly,
                roleExpiryDate: defaultRoleExpiryDate(newSelectedOptions)
            })
        }
    }

    const authorityChanged = option => {
        setInvitation({
            ...invitation,
            intendedAuthority: option.value,
            roleExpiryDate: defaultRoleExpiryDate(selectedRoles)
        });
    }

    const renderUserRole = (role, index, invitationSelected, invitationSelectCallback) => {
        const applicationMaps = role.isUserRole ? role.role.applicationMaps : role.applicationMaps;
        return (
            <InvitationRoleCard role={role}
                                index={index}
                                applicationMaps={applicationMaps}
                                key={index}
                                invitationSelected={invitationSelected}
                                invitationSelectCallback={invitationSelectCallback}
            />
        )
    }

    const renderFormElements = authorityOptions => {
        const skipRoles = [AUTHORITIES.SUPER_USER, AUTHORITIES.INSTITUTION_ADMIN].includes(invitation.intendedAuthority);
        return (
            <>
                <EmailField
                    name={I18n.t(isInviter ? "invitations.inviterRole.to" : "invitations.invitees")}
                    addEmails={addEmails}
                    emails={invitation.invites}
                    isAdmin={false}
                    pinnedEmails={[]}
                    removeMail={removeMail}
                    required={true}
                    maxEmails={null} //TODO is there a internal placeholder identifier? then one.
                    // See https://github.com/OpenConext/OpenConext-Invite/issues/540
                    maxEmailsMessage={"TODO : localize Not allowed to add more then one email with external identifier"}
                    error={!initial && isEmpty(invitation.invites)}/>

                {(!initial && isEmpty(invitation.invites)) &&
                    <ErrorIndicator msg={I18n.t("invitations.requiredEmail")}/>}
                {(authorityOptions.length > 1 || (!isInviter && authorityOptions.length === 1)) &&
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
                    />}

                {(user.superUser && AUTHORITIES.INSTITUTION_ADMIN === invitation.intendedAuthority) &&
                    <InputField
                        name={I18n.t("roles.organizationGUID")}
                        value={invitation.organizationGUID}
                        onChange={e => {
                            setInvitation({
                                ...invitation,
                                organizationGUID: e.target.value
                            });
                            setValidOrganizationGUID(true);
                            setOrganizationGUIDIdentityProvider(null);
                        }}
                        onBlur={validateOrganizationGUID}
                        toolTip={I18n.t("tooltips.organizationGUID")}
                    />}
                {!validOrganizationGUID &&
                    <ErrorIndicator msg={I18n.t("forms.invalid", {
                        value: invitation.organizationGUID,
                        attribute: I18n.t("roles.organizationGUID").toLowerCase()
                    })}/>}
                {!isEmpty(organizationGUIDIdentityProvider) &&
                    <p className="info">{I18n.t("roles.identityProvider", {name: organizationGUIDIdentityProvider["name:en"]})}</p>}
                {(!initial && isEmpty(invitation.organizationGUID) &&
                        invitation.intendedAuthority === AUTHORITIES.INSTITUTION_ADMIN && user.superUser) &&
                    <ErrorIndicator msg={I18n.t("invitations.requiredOrganizationGUID")}/>}

                {(!isInviter && !skipRoles) && <>
                    <SelectField value={selectedRoles}
                                 options={roles.filter(role => !selectedRoles.find(r => r.value === role.value))}
                                 name={I18n.t("invitations.roles")}
                                 toolTip={I18n.t("tooltips.rolesTooltip")}
                                 isMulti={true}
                                 required={true}
                                 error={!initial && isEmpty(selectedRoles)}
                                 searchable={true}
                                 placeholder={I18n.t("invitations.rolesPlaceHolder")}
                                 onChange={rolesChanged}/>
                    {(!initial && isEmpty(selectedRoles) &&
                            !skipRoles) &&
                        <ErrorIndicator msg={I18n.t("invitations.requiredRole")}/>}
                </>}


                <InputField name={I18n.t(isInviter ? "invitations.inviterRole.message" : "invitations.message")}
                            value={invitation.message}
                            onChange={e => setInvitation({...invitation, message: e.target.value})}
                            placeholder={I18n.t("invitations.messagePlaceholder")}
                            small={true}
                            cols={1}
                            multiline={true}/>

                <SelectField
                    value={language}
                    options={languageOptions}
                    name={I18n.t("languages.language")}
                    searchable={false}
                    onChange={val => setLanguage(val)}
                    toolTip={I18n.t("languages.languageTooltip")}
                    clearable={false}
                />
            </>
        );
    }

    const renderForm = () => {
        const disabledSubmit = !initial && !isValid();
        const authorityOptions = allowedAuthoritiesForInvitation(user, selectedRoles)
            .map(authority => ({value: authority, label: I18n.t(`access.${authority}`)}));
        const overrideSettingsAllowed = selectedRoles.every(role => role.overrideSettingsAllowed);
        const skipRoles = [AUTHORITIES.SUPER_USER, AUTHORITIES.INSTITUTION_ADMIN].includes(invitation.intendedAuthority)
        return (
            <>
                {isInviter &&
                    <div className="card-containers">
                        <span className={"label"}>
                            {I18n.t("invitations.inviterRole.roles")}
                            <Tooltip tip={I18n.t("tooltips.rolesTooltip")}/>
                        </span>
                        {roles.map((role, index) => renderUserRole(role, index, selectedRoles.some(r => r.value === role.value),
                            (e, value) => {
                                const checked = e.target.checked;
                                const roleSelected = roles.find(r => r.value === value);
                                const newSelectedRoles = checked ? selectedRoles.concat(roleSelected) : selectedRoles.filter(r => r.value !== roleSelected.value);
                                rolesChanged(newSelectedRoles);
                            }))
                        }
                        {(!initial && isEmpty(selectedRoles)) &&
                            <ErrorIndicator msg={I18n.t("invitations.requiredRole")} adjustMargin={true}/>
                        }
                    </div>}
                <InviterContainer isInviter={isInviter}>
                    {renderFormElements(authorityOptions)}
                </InviterContainer>

                <InviterContainer isInviter={isInviter}>
                    {!displayAdvancedSettings &&
                        <a className={`advanced-settings ${isInviter ? "inviter" : ""}`} href="/#"
                           onClick={e => toggleDisplayAdvancedSettings(e)}>
                            {I18n.t("roles.showAdvancedSettings")}
                            <DownIcon/>
                        </a>
                    }

                    {displayAdvancedSettings &&
                        <div className="advanced-settings-container">
                            <a className={`advanced-settings ${isInviter ? "inviter" : ""}`} href="/#"
                               onClick={e => toggleDisplayAdvancedSettings(e)}>
                                {I18n.t("roles.hideAdvancedSettings")}
                                <UpIcon/>
                            </a>

                            {overrideSettingsAllowed &&
                                <SwitchField name={"enforceEmailEquality"}
                                             value={invitation.enforceEmailEquality || false}
                                             onChange={val => setInvitation({...invitation, enforceEmailEquality: val})}
                                             label={I18n.t("invitations.enforceEmailEquality")}
                                             info={I18n.t("tooltips.enforceEmailEqualityTooltip")}
                                />}

                            {overrideSettingsAllowed &&
                                <SwitchField name={"eduIDOnly"}
                                             value={invitation.eduIDOnly || false}
                                             onChange={eduIDOnlyChanged}
                                             label={I18n.t("invitations.eduIDOnly")}
                                             info={I18n.t("tooltips.eduIDOnlyTooltip")}
                                             last={invitation.eduIDOnly}
                                />}

                            {(overrideSettingsAllowed && invitation.eduIDOnly) &&
                                <SelectField
                                    value={requestedAuthnContextOptions.find(option => option.value === invitation.requestedAuthnContext)}
                                    options={requestedAuthnContextOptions}
                                    className={"requested-authn-context"}
                                    name={I18n.t("invitations.requestedAuthnContext")}
                                    toolTip={I18n.t("tooltips.requestedAuthnContextTooltip")}
                                    placeholder={I18n.t("invitations.requestedAuthnContextPlaceHolder")}
                                    clearable={true}
                                    onChange={requestedAuthnContextChanged}
                                >
                                    {acrWarning &&
                                        <p className="warning" dangerouslySetInnerHTML={{__html: acrWarning}}/>}
                                </SelectField>}

                            {(invitation.intendedAuthority !== AUTHORITIES.GUEST && !isInviter &&
                                    !skipRoles) &&
                                <SwitchField name={"guestRoleIncluded"}
                                             value={invitation.guestRoleIncluded || false}
                                             onChange={val => setInvitation({...invitation, guestRoleIncluded: val})}
                                             label={I18n.t("invitations.guestRoleIncluded")}
                                             info={I18n.t("tooltips.guestRoleIncludedTooltip")}
                                />

                            }
                            {(overrideSettingsAllowed && !skipRoles) &&
                                <SwitchField name={"roleExpiryDate"}
                                             value={customRoleExpiryDate}
                                             onChange={() => {
                                                 setCustomRoleExpiryDate(!customRoleExpiryDate);
                                                 setInvitation({
                                                     ...invitation,
                                                     roleExpiryDate: defaultRoleExpiryDate(selectedRoles)
                                                 })
                                             }}
                                             label={I18n.t("invitations.roleExpiryDateQuestion")}
                                             info={I18n.t("invitations.roleExpiryDateInfo", {
                                                 expiry: displayExpiryDate(invitation.roleExpiryDate)
                                             })}
                                />
                            }
                            {customRoleExpiryDate &&
                                <DateField value={invitation.roleExpiryDate}
                                           onChange={e => setInvitation({...invitation, roleExpiryDate: e})}
                                           showYearDropdown={true}
                                           disabled={selectedRoles.some(role => !role.overrideSettingsAllowed)}
                                           pastDatesAllowed={config.pastDateAllowed}
                                           allowNull={overrideSettingsAllowed && invitation.intendedAuthority !== AUTHORITIES.GUEST}
                                           minDate={futureDate(1, invitation.expiryDate)}
                                           name={I18n.t("invitations.roleExpiryDate")}
                                           toolTip={I18n.t("tooltips.roleExpiryDateTooltip")}/>}

                            <SwitchField name={"expiryDate"}
                                         value={customExpiryDate}
                                         onChange={() => {
                                             setCustomExpiryDate(!customExpiryDate);
                                             setInvitation({...invitation, expiryDate: futureDate(30)})
                                         }}
                                         label={I18n.t("invitations.expiryDateQuestion")}
                                         info={I18n.t("invitations.expiryDateInfo")}
                                         last={true}
                            />

                            {customExpiryDate &&
                                <DateField value={invitation.expiryDate}
                                           onChange={e => setInvitation({...invitation, expiryDate: e})}
                                           showYearDropdown={true}
                                           pastDatesAllowed={config.pastDateAllowed}
                                           minDate={futureDate(1)}
                                           maxDate={futureDate(365)}
                                           name={I18n.t("invitations.expiryDate")}
                                           toolTip={I18n.t("tooltips.expiryDateTooltip")}/>}

                        </div>}

                </InviterContainer>
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
        <div className={`mod-invitation-form ${isInviter ? "inviter" : ""}`}>
            {!isInviter && <UnitHeader
                obj={({
                    name: I18n.t(`invitations.${guest ? "newGuest" : "new"}`),
                    svg: UserIcon,
                    style: "small"
                })}/>}
            <div className={`invitation-form ${isInviter ? "inviter" : ""}`}>
                {isInviter && <div className="invite-header">
                    <h3>{I18n.t("invitations.inviterRole.title")}</h3>
                    <span onClick={() => navigate("/")}>
                        <CloseIcon/>
                    </span>
                </div>}
                {renderForm()}
            </div>
        </div>
    );
}