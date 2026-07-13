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
    allApplicationsFromManage,
    allIdentityProviders,
    eduidIdentityProvider,
    newInvitation,
    requestedAuthnContextValues,
    rolesByApplication
} from "../api";
import {Button, ButtonType, Tooltip} from "@surfnet/sds";
import "./InvitationForm.scss";
import {UnitHeader} from "../components/UnitHeader";
import InputField from "../components/InputField";
import {isEmpty, splitListSemantically, stopEvent} from "../utils/Utils";
import ErrorIndicator from "../components/ErrorIndicator";
import SelectField from "../components/SelectField";
import {DateField} from "../components/DateField";
import EmailField from "../components/EmailField";
import {deriveExpirationDate, displayExpiryDate, futureDate, longDateFormat} from "../utils/Date";
import SwitchField from "../components/SwitchField";
import {InvitationRoleCard} from "../components/InvitationRoleCard";
import DOMPurify from "dompurify";
import {ExpandableSwitchField} from "../components/ExpandableSwitchField";
import Select from "react-select";
import {providersToOptions} from "../utils/Manage";

const DEFAULT_ROLE_EXPIRY_DAYS = 366;

const removeByOptions = ["after", "on"].map(val => ({value: val, label: I18n.t(`invitations.${val}`)}))

export const InviterContainer = ({isInviter, children}) => {
    return isInviter ?
        <div className="inviter-wrapper">{children}</div> : <>{children}</>

}

export const InvitationForm = () => {
    const location = useLocation();
    const navigate = useNavigate();

    const languageOptions = ["en", "nl"].map(lang => ({label: I18n.t(`languages.${lang}`), value: lang}))
    const {user, setFlash, config} = useAppStore(state => state);

    const [guest, setGuest] = useState(false);
    const [roles, setRoles] = useState([]);
    const [selectedRoles, setSelectedRoles] = useState([]);
    const [applications, setApplications] = useState([]);
    const [selectedApplications, setSelectedApplications] = useState([]);
    const [originalRoleId, setOriginalRoleId] = useState(null);
    const [invitation, setInvitation] = useState({
        expiryDate: futureDate(30),
        roleExpiryDate: null,
        roleExpiryDays: 0,
        invites: [],
        intendedAuthority: AUTHORITIES.GUEST
    });
    const [identityProviders, setIdentityProviders] = useState([]);
    const [organizationGUIDIdentityProvider, setOrganizationGUIDIdentityProvider] = useState({});
    const [displayAdvancedSettings, setDisplayAdvancedSettings] = useState(false);
    const [customExpiryDate, setCustomExpiryDate] = useState(false);
    const [customRoleExpiryDate, setCustomRoleExpiryDate] = useState(false);
    const [initial, setInitial] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [eduIDIdP, setEduIDIdP] = useState(null);
    const [acrValues, setACRValues] = useState({});
    const [language, setLanguage] = useState(I18n.locale === "en" ? languageOptions[0] : languageOptions[1]);
    const required = ["intendedAuthority", "invites"];
    const [removeRoleBy, setRemoveRoleBy] = useState(removeByOptions[1]);
    const [requestedAuthnContextOptions, setRequestedAuthnContextOptions] = useState([]);
    const isInviter = highestAuthority(user) === AUTHORITIES.INVITER;

    useEffect(() => {
        if (!isUserAllowed(AUTHORITIES.INVITER, user)) {
            navigate("/404");
            return;
        }
        if (isUserAllowed(AUTHORITIES.APPLICATION_MANAGER, user)) {
            rolesByApplication(true)
                .then(page => {
                    const markedRoles = markAndFilterRoles(user, page.content, I18n.locale,
                        I18n.t("roles.multiple"), I18n.t("forms.and"), "name", false);
                    setInitialRole(markedRoles);
                    setRoles(markedRoles);
                    if (user.institutionAdmin && !isEmpty(user.applications)) {
                        const options = providersToOptions(user.applications);
                        setApplications(options);
                    }
                })
        } else {
            Promise.resolve().then(() => {
                const markedRoles = markAndFilterRoles(user, [], I18n.locale,
                    I18n.t("roles.multiple"), I18n.t("forms.and"), "name", false);
                setInitialRole(markedRoles);
                setRoles(markedRoles);
            });
        }
        const breadcrumbPath = isInviter ? [] : [
            {path: "/home", value: I18n.t("tabs.home")},
            {path: "/home/roles", value: I18n.t("tabs.roles")},
        ];
        useAppStore.setState({breadcrumbPath: breadcrumbPath});
        //Now fetch all IdentityProviders for the superuser
        if (user.superUser) {
            Promise.all([allApplicationsFromManage(), allIdentityProviders()])
                .then(res => {
                    const options = providersToOptions(res[0]);
                    setApplications(options);
                    const identityProviderOptions = res[1]
                        .filter(idp => !isEmpty(idp.institutionGuid))
                        .map(idp => ({
                            value: idp.id,
                            label: idp["name:en"],
                            institutionGuid: idp.institutionGuid
                        }))
                    setIdentityProviders(identityProviderOptions);
                });
        }
        Promise.all([eduidIdentityProvider(), requestedAuthnContextValues()])
            .then(res => {
                setEduIDIdP(res[0]);
                setACRValues(res[1]);
                const acrContexts = Object.entries(res[1])
                    .map(arr => ({value: arr[1], label: I18n.t(`requestedAuthnContext.${arr[0]}`) }));
                setRequestedAuthnContextOptions(acrContexts);
            });

    }, [user]);// eslint-disable-line react-hooks/exhaustive-deps

    const acrWarning = useMemo(() => {
            if (isEmpty(invitation.requestedAuthnContext) || isEmpty(eduIDIdP) || isEmpty(selectedRoles)) {
                return null;
            }
            //Filter out the roles that are linked to applications that are not present in the mfaEntities of the eduIDIdp
            //or where the MFA level does not equal the requestedAuthnContext. If the requestedAuthnContext === TransparentAuthnContext,
            //then we skip the warning
            const mfaEntities = eduIDIdP.mfaEntities;
            const acrValue = invitation.requestedAuthnContext;
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
            setSelectedRoles([initialRole])
            setInvitation({
                ...invitation,
                intendedAuthority: isGuest ? AUTHORITIES.GUEST : AUTHORITIES.INVITER,
                enforceEmailEquality: initialRole.enforceEmailEquality,
                eduIDOnly: initialRole.eduIDOnly,
                requestedAuthnContext: initialRole.requestedAuthnContext,
                roleExpiryDate: deriveExpirationDate(initialRole.isUserRole ? initialRole.role : initialRole)
            })
            setOriginalRoleId(initialRole.isUserRole ? initialRole.role.id : initialRole.id);
        } else {
            let roleExpiryDate = futureDate(366);
            if (markedRoles.length === 1) {
                const role = markedRoles[0]
                roleExpiryDate = deriveExpirationDate(role.isUserRole ? role.role : role);
                setSelectedRoles(markedRoles);
            }
            setInvitation({
                ...invitation,
                intendedAuthority: isGuest ? AUTHORITIES.GUEST : AUTHORITIES.INVITER,
                roleExpiryDate: roleExpiryDate
            })
        }
    }

    const toggleDisplayAdvancedSettings = e => {
        stopEvent(e);
        setDisplayAdvancedSettings(!displayAdvancedSettings);
    }

    const submit = () => {
        setInitial(false);
        if (isValid() && !isSubmitting) {
            setIsSubmitting(true);
            const invitationRequest = {
                ...invitation,
                roleExpiryDate: removeRoleBy.value === "after"
                    ? futureDate(invitation.roleExpiryDays || DEFAULT_ROLE_EXPIRY_DAYS)
                    : invitation.roleExpiryDate,
                roleIdentifiers: selectedRoles.map(role => role.value),
                manageIdentifiers: selectedApplications.map(app => ({
                    manageType: app.manageType,
                    manageId: app.manageId,
                })),
                language: language.value
            };
            newInvitation(invitationRequest)
                .then(() => {
                    setFlash(I18n.t("invitations.createFlash"));
                    if (originalRoleId) {
                        navigate(`/roles/${originalRoleId}/invitations`);
                    } else if (!isEmpty(invitationRequest.roleIdentifiers)) {
                        navigate(`/roles/${invitationRequest.roleIdentifiers[0]}/invitations`);
                    } else {
                        navigate(-1);
                    }
                })
                .catch(e => {
                    setIsSubmitting(false);
                    throw e;
                });
        }
    }

    const isValid = () => {
        const invitationIsForAdmin = [AUTHORITIES.SUPER_USER, AUTHORITIES.INSTITUTION_ADMIN].includes(invitation.intendedAuthority);
        const res = required.every(attr => !isEmpty(invitation[attr])) &&
            (!isEmpty(selectedRoles) || invitationIsForAdmin || invitation.intendedAuthority === AUTHORITIES.APPLICATION_MANAGER) &&
            !(invitation.intendedAuthority === AUTHORITIES.INSTITUTION_ADMIN && isEmpty(invitation.organizationGUID)) &&
            (!isEmpty(selectedApplications) || invitation.intendedAuthority !== AUTHORITIES.INSTITUTION_ADMIN);
        return res;
    }

    const addEmails = emails => {
        const newEmails = [...new Set(invitation.invites.concat(emails))]
        setInvitation({...invitation, invites: newEmails});
    }

    const removeMail = mail => {
        setInvitation({...invitation, invites: invitation.invites.filter(email => mail !== email)});
    }

    const defaultRoleExpiryDate = newRoles => {
        const allDefaultExpiryDates = (newRoles || [])
            .map(role => deriveExpirationDate(role));

        return isEmpty(allDefaultExpiryDates) ? futureDate(365, new Date()) :
            new Date(Math.max(...allDefaultExpiryDates.map(d => d.getTime())));
    }

    const customRoleExpiryDateInfo = () => {
        let postfix = "Default";
        if (customRoleExpiryDate) {
            postfix = removeRoleBy.value === "on" ? "On" : "";
        }
        const expiryDate = removeRoleBy.value === "on"
            ? invitation.roleExpiryDate
            : futureDate(invitation.roleExpiryDays || DEFAULT_ROLE_EXPIRY_DAYS);
        return I18n.t(`invitations.roleExpiryDateInfo${postfix}`, {
            expiry: displayExpiryDate(expiryDate),
            date: longDateFormat(invitation.roleExpiryDate),
            days: DEFAULT_ROLE_EXPIRY_DAYS
        });
    }

    const eduIDOnlyChanged = val => {
        const requestedAuthnContext = val ? invitation.requestedAuthnContext : null;
        setInvitation({...invitation, eduIDOnly: val, requestedAuthnContext: requestedAuthnContext})
    }

    const requestedAuthnContextChanged = option => {
        setInvitation({...invitation, requestedAuthnContext: option ? option.value : null});
    }

    const applicationsChanged = selectedOptions => {
        if (selectedOptions === null) {
            setSelectedApplications([])
        } else {
            const newSelectedOptions = Array.isArray(selectedOptions) ? [...selectedOptions] : [selectedOptions];
            setSelectedApplications(newSelectedOptions);
        }
    }

    const rolesChanged = selectedOptions => {
        if (selectedOptions === null) {
            setSelectedRoles([])
            setInvitation({...invitation, enforceEmailEquality: false, eduIDOnly: false, requestedAuthnContext: null})
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
            let requestedAuthnContext = invitation.requestedAuthnContext;
            if (!overrideSettingsAllowed) {
                enforceEmailEquality = newSelectedOptions.some(role => role.enforceEmailEquality) || enforceEmailEquality;
                eduIDOnly = newSelectedOptions.some(role => role.eduIDOnly) || eduIDOnly;
                const rolesWithRequestedAuthnContext = newSelectedOptions.filter(role => isEmpty(role.requestedAuthnContext));
                requestedAuthnContext = isEmpty(rolesWithRequestedAuthnContext) ? requestedAuthnContext :
                    rolesWithRequestedAuthnContext[0].requestedAuthnContext;
            }
            setInvitation({
                ...invitation,
                intendedAuthority: intendedAuthority,
                enforceEmailEquality: enforceEmailEquality,
                eduIDOnly: eduIDOnly,
                requestedAuthnContext: requestedAuthnContext
            })
        }
    }

    const changeOrganizationGUIDIdentityProvider = option => {
        setOrganizationGUIDIdentityProvider(option);
        setInvitation({...invitation, organizationGUID: option.institutionGuid});
    }


    const authorityChanged = option => {
        setInvitation({
            ...invitation,
            intendedAuthority: option.value,
            roleExpiryDate: defaultRoleExpiryDate(selectedRoles),
            organizationGUID: option.value !== AUTHORITIES.INSTITUTION_ADMIN ? null :
                (user.institutionAdmin ? user.organizationGUID : null)
        });
        if (option.value === AUTHORITIES.SUPER_USER || option.value === AUTHORITIES.INSTITUTION_ADMIN) {
            setSelectedRoles([]);
        }
        if (option.value !== AUTHORITIES.INSTITUTION_ADMIN) {
            setOrganizationGUIDIdentityProvider({});
        }
        if (option.value === AUTHORITIES.APPLICATION_MANAGER) {
            setSelectedRoles([]);
            setOrganizationGUIDIdentityProvider({});
        }
        if (option.value !== AUTHORITIES.APPLICATION_MANAGER) {
            setSelectedApplications([]);
        }
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
                    <SelectField name={I18n.t("roles.identityProvider")}
                                 toolTip={I18n.t("tooltips.invitationIdentityProvider")}
                                 value={identityProviders.find(idp => idp.value === organizationGUIDIdentityProvider.value)}
                                 placeholder={I18n.t("roles.identityProviderPlaceholder")}
                                 options={identityProviders}
                                 onChange={changeOrganizationGUIDIdentityProvider}
                                 searchable={true}
                                 required={true}
                    />}
                {(!initial && isEmpty(invitation.organizationGUID) &&
                        invitation.intendedAuthority === AUTHORITIES.INSTITUTION_ADMIN && user.superUser) &&
                    <ErrorIndicator msg={I18n.t("forms.required", {
                        attribute: I18n.t("roles.identityProvider").toLowerCase()
                    })}/>}
                {!isEmpty(organizationGUIDIdentityProvider.institutionGuid) &&
                    <em className="info">{I18n.t("roles.organizationGUIDValue", {guid: organizationGUIDIdentityProvider.institutionGuid})}</em>}


                {(!isInviter && !skipRoles && invitation.intendedAuthority !== AUTHORITIES.APPLICATION_MANAGER) &&
                    <>
                        <SelectField value={selectedRoles}
                                     options={roles.filter(role => !selectedRoles.find(r => r.value === role.value)
                                         && isEmpty(role.crmRoleId))}
                                     name={I18n.t("invitations.roles")}
                                     toolTip={I18n.t("tooltips.rolesTooltip")}
                                     isMulti={true}
                                     required={true}
                                     error={!initial && isEmpty(selectedRoles)}
                                     searchable={true}
                                     placeholder={I18n.t(`invitations.rolesPlaceHolder${isEmpty(roles) ? "Loading" : ""}`)}
                                     onChange={rolesChanged}/>
                        {(!initial && isEmpty(selectedRoles) &&
                                !skipRoles) &&
                            <ErrorIndicator msg={I18n.t("invitations.requiredRole")}/>}
                    </>}

                {invitation.intendedAuthority === AUTHORITIES.APPLICATION_MANAGER &&
                    <>
                        <SelectField value={selectedApplications}
                                     options={applications.filter(app => !selectedApplications.find(a => a.value === app.value))}
                                     name={I18n.t("invitations.applications")}
                                     toolTip={I18n.t("tooltips.applicationsTooltip")}
                                     isMulti={true}
                                     required={true}
                                     error={!initial && isEmpty(selectedApplications)}
                                     searchable={true}
                                     placeholder={I18n.t(`invitations.applicationsPlaceHolder${isEmpty(applications) ? "Loading" : ""}`)}
                                     onChange={applicationsChanged}/>
                        {(!initial && isEmpty(selectedApplications)) &&
                            <ErrorIndicator msg={I18n.t("invitations.requiredApplication")}/>}
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

        const toggleRemoveBy = option => {
            setRemoveRoleBy(option);

            if (option.value === "after") {
                setInvitation({...invitation, roleExpiryDays: DEFAULT_ROLE_EXPIRY_DAYS, roleExpiryDate: null})
            } else {
                setInvitation({...invitation, roleExpiryDays: 0, roleExpiryDate: futureDate(DEFAULT_ROLE_EXPIRY_DAYS)})
            }
        }

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
                    </div>
                }
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

                            <SwitchField name={"enforceEmailEquality"}
                                         value={invitation.enforceEmailEquality || false}
                                         onChange={val => setInvitation({...invitation, enforceEmailEquality: val})}
                                         label={I18n.t("invitations.enforceEmailEquality")}
                                         disabled={!overrideSettingsAllowed}
                                         info={I18n.t("tooltips.enforceEmailEqualityTooltip")}
                            />

                            <SwitchField name={"eduIDOnly"}
                                         value={invitation.eduIDOnly || false}
                                         onChange={eduIDOnlyChanged}
                                         label={I18n.t("invitations.eduIDOnly")}
                                         disabled={!overrideSettingsAllowed}
                                         info={I18n.t("tooltips.eduIDOnlyTooltip")}
                                         last={invitation.eduIDOnly && !isEmpty(requestedAuthnContextOptions)}
                            />

                            {(invitation.eduIDOnly && !isEmpty(requestedAuthnContextOptions)) &&
                                <SelectField
                                    value={requestedAuthnContextOptions.find(option => option.value === invitation.requestedAuthnContext)}
                                    options={requestedAuthnContextOptions}
                                    className={"requested-authn-context"}
                                    name={I18n.t("invitations.requestedAuthnContext")}
                                    toolTip={I18n.t("tooltips.requestedAuthnContextTooltip")}
                                    placeholder={I18n.t("invitations.requestedAuthnContextPlaceHolder")}
                                    clearable={true}
                                    disabled={!overrideSettingsAllowed}
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
                                <ExpandableSwitchField
                                    name={"roleExpiryDate"}
                                    label={I18n.t(`invitations.roleExpiryDateQuestion`)}
                                    info={customRoleExpiryDateInfo()}
                                    defaultValue={customRoleExpiryDate}
                                    onChange={val => setCustomRoleExpiryDate(val)}
                                >
                                    <div className="role-expiry-date-container">
                                        <p className="label">{I18n.t("invitations.removeRole")}</p>
                                        <div className="role-expiry-date">
                                            <Select className="input-select-inner"
                                                    classNamePrefix={"select-inner"}
                                                    value={removeRoleBy}
                                                    options={removeByOptions}
                                                    onChange={toggleRemoveBy}
                                            />
                                            {removeRoleBy.value === "after" &&
                                                <>
                                                    <InputField value={invitation.roleExpiryDays}
                                                                isInteger={true}
                                                                onChange={e => {
                                                                    const val = parseInt(e.target.value);
                                                                    const defaultExpiryDays = Number.isInteger(val) && val > 0 ? val : 1;
                                                                    setInvitation({
                                                                        ...invitation,
                                                                        roleExpiryDays: defaultExpiryDays
                                                                    })
                                                                }}
                                                                customClassName="inner-switch"/>
                                                    <span>{I18n.t("invitations.days")}</span>
                                                </>
                                            }
                                            {removeRoleBy.value === "on" &&
                                                <DateField
                                                    value={invitation.roleExpiryDate || futureDate(DEFAULT_ROLE_EXPIRY_DAYS)}
                                                    onChange={e => setInvitation({...invitation, roleExpiryDate: e})}
                                                    showYearDropdown={true}
                                                    disabled={selectedRoles.some(role => !role.overrideSettingsAllowed)}
                                                    pastDatesAllowed={config.pastDateAllowed}
                                                    minDate={futureDate(1, invitation.expiryDate)}/>
                                            }
                                        </div>
                                    </div>
                                </ExpandableSwitchField>
                            }

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
                    <Button disabled={disabledSubmit || isSubmitting}
                            txt={I18n.t("invitations.invite")}
                            onClick={submit}/>
                </section>


            </>
        );
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
