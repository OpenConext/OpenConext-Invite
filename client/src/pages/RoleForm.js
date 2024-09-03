import React, {useEffect, useRef, useState} from "react";
import {useLocation, useNavigate, useParams} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import I18n from "../locale/I18n";
import {AUTHORITIES, isUserAllowed, urnFromRole} from "../utils/UserRole";
import {
    allProviders,
    consequencesRoleDeletion,
    createRole,
    deleteRole,
    me,
    roleByID,
    updateRole,
    validate
} from "../api";
import {Button, ButtonType, Loader} from "@surfnet/sds";
import "./RoleForm.scss";
import {UnitHeader} from "../components/UnitHeader";
import {ReactComponent as RoleIcon} from "@surfnet/sds/icons/illustrative-icons/hierarchy.svg";
import InputField from "../components/InputField";
import {constructShortName} from "../validations/regExps";
import {distinctValues, isEmpty} from "../utils/Utils";
import ErrorIndicator from "../components/ErrorIndicator";
import SelectField from "../components/SelectField";
import {providersToOptions} from "../utils/Manage";
import ConfirmationDialog from "../components/ConfirmationDialog";
import SwitchField from "../components/SwitchField";
import {dateFromEpoch, displayExpiryDate, futureDate} from "../utils/Date";

const DEFAULT_EXPIRY_DAYS = 365;
const CUT_OFF_DELETED_USER = 5;

export const RoleForm = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const {id} = useParams();
    const nameRef = useRef();

    const required = ["name", "description"];
    const {user, setFlash, config} = useAppStore(state => state);

    const [role, setRole] = useState({
        name: "",
        shortName: "",
        defaultExpiryDays: DEFAULT_EXPIRY_DAYS
    });
    const [providers, setProviders] = useState([]);
    const [isNewRole, setNewRole] = useState(true);
    const [loading, setLoading] = useState(true);
    const [initial, setInitial] = useState(true);
    const [alreadyExists, setAlreadyExists] = useState({});
    const [confirmation, setConfirmation] = useState({});
    const [confirmationOpen, setConfirmationOpen] = useState(false);
    const [customRoleExpiryDate, setCustomRoleExpiryDate] = useState(false);
    const [applications, setApplications] = useState([]);
    const [allowedToEditApplication, setAllowedToEditApplication] = useState(true);
    const [deletedUserRoles, setDeletedUserRoles] = useState(null);

    useEffect(() => {
            if (!isUserAllowed(AUTHORITIES.MANAGER, user)) {
                navigate("/404");
                return;
            }
            setAllowedToEditApplication(isUserAllowed(AUTHORITIES.INSTITUTION_ADMIN, user))
            const newRole = id === "new";
            const promises = [];
            if (!newRole) {
                promises.push(roleByID(parseInt(id, 10)));
            }
            if (user.superUser) {
                promises.push(allProviders());
            }
            Promise.all(promises).then(res => {
                if (!newRole) {
                    setRole(res[0]);
                    setCustomRoleExpiryDate(res[0].defaultExpiryDays !== DEFAULT_EXPIRY_DAYS)
                }
                let providers;
                if (user.superUser) {
                    providers = providersToOptions(res[newRole ? 0 : 1]);
                } else if (user.institutionAdmin) {
                    providers = providersToOptions(distinctValues(user.applications.concat(
                        user.userRoles.map(userRole => userRole.role.applicationMaps).flat()
                    ), "id"));
                } else {
                    providers = providersToOptions(distinctValues(user.userRoles
                        .map(userRole => userRole.role.applicationMaps).flat(), "id"));
                }
                setProviders(providers);
                setNewRole(newRole);
                const name = newRole ? "" : res[0].name;
                const breadcrumbPath = [
                    {path: "/home", value: I18n.t("tabs.home")},
                    {path: "/home/roles", value: I18n.t("tabs.roles")},
                ];
                if (newRole) {
                    setApplications([null]);
                    if (location.state) {
                        setApplications(providers.filter(provider => provider.value === location.state));
                    } else {
                        setApplications([null]);
                    }
                } else {
                    breadcrumbPath.push({path: `/roles/${res[0].id}`, value: name});

                    res[0].applicationMaps.forEach(m => m.landingPage = (res[0].applicationUsages.find(appUsage => appUsage.application.manageId === m.id) || {}).landingPage);
                    setApplications(providersToOptions(res[0].applicationMaps));
                }
                breadcrumbPath.push({value: I18n.t(`roles.${newRole ? "new" : "edit"}`, {name: name})});
                useAppStore.setState({breadcrumbPath: breadcrumbPath});
                setLoading(false);

                setTimeout(() => {
                    if (nameRef && nameRef.current) {
                        nameRef.current.focus();
                    }
                }, 500);
            })
        },
        [id]); // eslint-disable-line react-hooks/exhaustive-deps

    const validateApplication = (index, value) => {
        if (!isEmpty(value)) {
            validate("url", value)
                .then(json => {
                    const application = applications[index];
                    application.invalid = !json.valid;
                    setApplications([...applications])
                });
        }
        return true;
    }

    const submit = () => {
        setInitial(false);
        if (isValid()) {
            setLoading(true);
            const promise = isNewRole ? createRole : updateRole;
            const newRoleData = {
                ...role,
                applicationUsages: applications
                    .filter(app => !isEmpty(app))
                    .map(app => ({application: app, landingPage: app.landingPage}))
            };
            promise(newRoleData)
                .then(res => {
                    const flashMessage = I18n.t(`roles.${isNewRole ? "createFlash" : "updateFlash"}`, {name: role.name});
                    updateUserIfNecessary(`/roles/${res.id}`, flashMessage);
                }).catch(handleError);
        }
    }

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
            setDeletedUserRoles(null);
            setConfirmationOpen(true);
        })
    }

    const updateUserIfNecessary = (path, flashMessage) => {
        if (user.userRoles.some(userRole => userRole.role.id === role.id)) {
            //We need to refresh the roles of the User to ensure 100% consistency
            me()
                .then(res => {
                    useAppStore.setState(() => ({user: res, authenticated: true}));
                    navigate(path);
                    setFlash(flashMessage);
                });
        } else {
            navigate(path);
            setFlash(flashMessage);
        }
    }

    const doDelete = showConfirmation => {
        if (showConfirmation) {
            setLoading(true);
            consequencesRoleDeletion(role.id).then(res => {
                setLoading(false);
                setDeletedUserRoles(res);
                setConfirmation({
                    cancel: () => setConfirmationOpen(false),
                    action: () => doDelete(false),
                    warning: true,
                    error: false,
                    question: I18n.t("roles.deleteConfirmation"),
                    confirmationTxt: I18n.t("confirmationDialog.confirm"),
                    confirmationHeader: I18n.t("confirmationDialog.title")
                });
                setConfirmationOpen(true);
            })
        } else {
            setLoading(true);
            setDeletedUserRoles(null);
            deleteRole(role)
                .then(() => {
                    setConfirmationOpen(false);
                    updateUserIfNecessary("/home/roles", I18n.t("roles.deleteFlash", {name: role.name}));
                }).catch(handleError)
        }
    };

    const isValid = () => {
        return required.every(attr => !isEmpty(role[attr]))
            && Object.values(alreadyExists).every(attr => !attr)
            && !isEmpty(applications)
            && !isEmpty(applications[0])
            && applications.every(app => !app || (!app.invalid && !isEmpty(app.landingPage)))
            && role.defaultExpiryDays > 0;
    }

    const changeApplication = (index, application) => {
        applications.splice(index, 1, application);
        setApplications([...applications]);
    }

    const changeApplicationLandingPage = (index, e) => {
        const application = applications[index];
        const newApplication = {...application, landingPage: e.target.value, invalid: false};
        applications.splice(index, 1, newApplication);
        setApplications([...applications]);
    }

    const addApplication = () => {
        applications.push(null);
        setApplications([...applications]);
    }

    const deleteApplication = index => {
        applications.splice(index, 1);
        setApplications([...applications]);
    }

    const renderForm = () => {
        const valid = isValid();
        const disabledSubmit = !valid && !initial;
        const filteredProviders = providers.filter(option => !applications.some(app => app && option.value === app.value));
        return (<>
                <h2 className="section-separator">
                    {I18n.t("roles.roleDetails")}
                </h2>
                <InputField name={I18n.t("roles.name")}
                            value={role.name || ""}
                            placeholder={I18n.t("roles.namePlaceHolder")}
                            error={alreadyExists.name || (!initial && isEmpty(role.name))}
                            onRef={nameRef}
                            onChange={e => {
                                const shortName = isNewRole ? constructShortName(e.target.value) : role.shortName;
                                setRole(
                                    {...role, name: e.target.value, shortName: shortName});
                                setAlreadyExists({...alreadyExists, shortName: false})
                            }}
                />
                {(!initial && isEmpty(role.name)) &&
                    <ErrorIndicator msg={I18n.t("forms.required", {
                        attribute: I18n.t("roles.name").toLowerCase()
                    })}/>}

                {!isNewRole && <InputField
                            name={I18n.t("roles.urn")}
                            value={urnFromRole(config.groupUrnPrefix, role)}
                            disabled={true}
                            toolTip={I18n.t("tooltips.roleUrn")}
                />}

                <InputField name={I18n.t("roles.description")}
                            value={role.description || ""}
                            placeholder={I18n.t("roles.descriptionPlaceHolder")}
                            error={!initial && isEmpty(role.description)}
                            multiline={true}
                            onChange={e => setRole(
                                {...role, description: e.target.value})}
                />
                {(!initial && isEmpty(role.description)) &&
                    <ErrorIndicator msg={I18n.t("forms.required", {
                        attribute: I18n.t("roles.description").toLowerCase()
                    })}/>}

                <h2 className="section-separator">
                    {I18n.t("roles.applicationDetails")}
                </h2>
                {applications.map((application, index) =>
                    <div className="application-container" key={index}>
                        <div className="select-field-container">
                        <SelectField name={I18n.t("roles.manage")}
                                     value={application}
                                     placeholder={I18n.t("roles.applicationPlaceholder")}
                                     options={filteredProviders}
                                     onChange={option => changeApplication(index, option)}
                                     searchable={true}
                                     clearable={false}
                                     disabled={!allowedToEditApplication}
                        />
                            {(!initial && isEmpty(application) && index === 0) &&
                                <ErrorIndicator msg={I18n.t("forms.required", {
                                    attribute: I18n.t("roles.manage").toLowerCase()
                                })}/>}
                        </div>
                        <div className="input-field-container">
                            <InputField name={I18n.t("roles.landingPage")}
                                        value={application ? application.landingPage : null}
                                        isUrl={true}
                                        disabled={isEmpty(application) || !allowedToEditApplication}
                                        placeholder={I18n.t("roles.landingPagePlaceHolder")}
                                        onBlur={e => validateApplication(index, e.target.value)}
                                        onChange={e => changeApplicationLandingPage(index, e)}
                        />
                            {(!initial && application?.landingPage && application.invalid) &&
                                <ErrorIndicator msg={I18n.t("forms.invalid", {
                                    attribute: I18n.t("roles.landingPage").toLowerCase(),
                                    value: application?.landingPage
                                })}/>}
                            {(!initial && !isEmpty(application) && isEmpty(application.landingPage)) &&
                                <ErrorIndicator msg={I18n.t("forms.required", {
                                    attribute: I18n.t("roles.landingPage").toLowerCase()
                                })}/>}
                        </div>
                        {(index !== 0 && allowedToEditApplication)&&
                            <Button type={ButtonType.Delete}
                                    onClick={() => deleteApplication(index)}/>
                        }
                    </div>
                )}
                {(!initial && isEmpty(applications)) &&
                    <ErrorIndicator msg={I18n.t("forms.required", {
                        attribute: I18n.t("roles.manage").toLowerCase()
                    })}/>}
                {allowedToEditApplication &&
                <div className="application-actions">
                    <Button txt={I18n.t("roles.addApplication")}
                            disabled={providers.length === applications.length || isEmpty(applications[0])}
                            onClick={addApplication}/>
                </div>
                }

                <h2 className="section-separator">
                    {I18n.t("roles.invitationDetails")}
                </h2>

                <SwitchField name={"enforceEmailEquality"}
                             value={role.enforceEmailEquality || false}
                             onChange={val => setRole({...role, enforceEmailEquality: val})}
                             label={I18n.t("invitations.enforceEmailEquality")}
                             info={I18n.t("tooltips.enforceEmailEqualityTooltip")}
                />

                <SwitchField name={"eduIDOnly"}
                             value={role.eduIDOnly || false}
                             onChange={val => setRole({...role, eduIDOnly: val})}
                             label={I18n.t("invitations.eduIDOnly")}
                             info={I18n.t("tooltips.eduIDOnlyTooltip")}
                />

                <SwitchField name={"roleExpiryDate"}
                             value={customRoleExpiryDate}
                             onChange={() => setCustomRoleExpiryDate(!customRoleExpiryDate)}
                             label={I18n.t("invitations.roleExpiryDateQuestion")}
                             info={I18n.t("invitations.roleExpiryDateInfo", {
                                 expiry: displayExpiryDate(futureDate(role.defaultExpiryDays, new Date()))
                             })}
                />
                {customRoleExpiryDate && <InputField name={I18n.t("roles.defaultExpiryDays")}
                                                     value={role.defaultExpiryDays || 0}
                                                     isInteger={true}
                                                     onChange={e => {
                                                         const val = parseInt(e.target.value);
                                                         const defaultExpiryDays = Number.isInteger(val) && val > 0 ? val : 0;
                                                         setRole(
                                                             {...role, defaultExpiryDays: defaultExpiryDays})
                                                     }}
                                                     toolTip={I18n.t("tooltips.defaultExpiryDays")}
                />}

                {(!initial && (isEmpty(role.defaultExpiryDays) || role.defaultExpiryDays < 1)) &&
                    <ErrorIndicator msg={I18n.t("forms.required", {
                        attribute: I18n.t("roles.defaultExpiryDays").toLowerCase()
                    })}/>}

                <SwitchField name={"overrideSettingsAllowed"}
                             value={role.overrideSettingsAllowed}
                             onChange={value => setRole({...role, overrideSettingsAllowed: value})}
                             label={I18n.t("roles.override")}
                             info={I18n.t("tooltips.overrideSettingsAllowed")}
                             last={true}
                />

                <section className="actions">
                    {(!isNewRole && allowedToEditApplication) &&
                        <Button type={ButtonType.Delete}
                                onClick={() => doDelete(true)}/>}
                    <Button type={ButtonType.Secondary}
                            txt={I18n.t("forms.cancel")}
                            onClick={() => navigate(isNewRole ? "/home/roles" : `/roles/${role.id}`)}/>
                    <Button disabled={disabledSubmit}
                            txt={I18n.t("forms.save")}
                            onClick={submit}/>
                </section>

            </>
        );
    }

    if (loading) {
        return <Loader/>
    }

    return (
        <div className={"mod-role-form"}>
            {confirmationOpen && <ConfirmationDialog isOpen={confirmationOpen}
                                                     cancel={confirmation.cancel}
                                                     confirm={confirmation.action}
                                                     confirmationTxt={confirmation.confirmationTxt}
                                                     confirmationHeader={confirmation.confirmationHeader}
                                                     isError={confirmation.error}
                                                     question={confirmation.question}>
                {!isEmpty(deletedUserRoles) && <div className="consequences">
                    <p>{I18n.t("roles.consequences.info")}</p>
                    <ul>
                        {deletedUserRoles.slice(0, CUT_OFF_DELETED_USER).map(userRole => <li>
                            {I18n.t("roles.consequences.userInfo", {
                                name: userRole.userInfo.name,
                                authority: I18n.t(`access.${userRole.authority}`),
                                lastActivity: dateFromEpoch(userRole.userInfo.lastActivity)
                            })}
                        </li>)}
                    </ul>
                    {deletedUserRoles.length > CUT_OFF_DELETED_USER &&
                        <p>{I18n.t("roles.consequences.andMore", {nbr: deletedUserRoles.length - CUT_OFF_DELETED_USER})}</p>}
                </div>}
            </ConfirmationDialog>}

            <UnitHeader
                obj={({
                    name: I18n.t(`roles.${isNewRole ? "new" : "edit"}`, {name: role.name}),
                    svg: RoleIcon,
                    style: "small"
                })}/>
            <div className={"role-form"}>
                {renderForm()}
            </div>
        </div>
    );
}