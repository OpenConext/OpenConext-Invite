import React, {useEffect, useRef, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import I18n from "../locale/I18n";
import {AUTHORITIES, isUserAllowed, urnFromRole} from "../utils/UserRole";
import {allProviders, createRole, deleteRole, me, roleByID, updateRole, validate} from "../api";
import {Button, ButtonType, Loader} from "@surfnet/sds";
import "./RoleForm.scss";
import {UnitHeader} from "../components/UnitHeader";
import {ReactComponent as RoleIcon} from "@surfnet/sds/icons/illustrative-icons/hierarchy.svg";
import InputField from "../components/InputField";
import {constructShortName} from "../validations/regExps";
import {distinctValues, isEmpty} from "../utils/Utils";
import ErrorIndicator from "../components/ErrorIndicator";
import SelectField from "../components/SelectField";
import {providersToOptions, singleProviderToOption} from "../utils/Manage";
import ConfirmationDialog from "../components/ConfirmationDialog";
import SwitchField from "../components/SwitchField";
import {displayExpiryDate, futureDate} from "../utils/Date";

export const RoleForm = () => {

    const navigate = useNavigate();
    const {id} = useParams();
    const nameRef = useRef();

    const required = ["name", "description"];
    const {user, setFlash, config} = useAppStore(state => state);

    const [role, setRole] = useState({
        name: "",
        shortName: "",
        defaultExpiryDays: 365,
        identifier: crypto.randomUUID()
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

    useEffect(() => {
            if (!isUserAllowed(AUTHORITIES.MANAGER, user)) {
                navigate("/404");
                return;
            }
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
                    setRole(res[0])
                }
                if (user.superUser) {
                    setProviders(providersToOptions(res[newRole ? 0 : 1]));
                } else if (user.institutionAdmin) {
                    setProviders(providersToOptions(distinctValues(user.applications
                        .concat(user.userRoles.map(userRole => userRole.role.applicationMaps)
                            .flat()), "id")));
                } else {
                    setProviders(providersToOptions(distinctValues(user.userRoles
                        .map(userRole => userRole.role.applicationMaps).flat(), "id")));
                }
                setNewRole(newRole);
                const name = newRole ? "" : res[0].name;
                const breadcrumbPath = [
                    {path: "/home", value: I18n.t("tabs.home")},
                    {path: "/home/roles", value: I18n.t("tabs.roles")},
                ];
                if (newRole) {
                    const providerOption = singleProviderToOption(user.superUser ? res[0][0] :
                        user.institutionAdmin ? user.applications[0] : user.userRoles[0].role.applicationMaps[0]);
                    setApplications([providerOption]);
                    setRole({...role, applications: [providerOption]})
                } else {
                    breadcrumbPath.push({path: `/roles/${res[0].id}`, value: name});
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
            const newRoleData = {...role, applications: applications};
            promise(newRoleData)
                .then(res => {
                    const flashMessage = I18n.t(`roles.${isNewRole ? "createFlash" : "updateFlash"}`, {name: role.name});
                    updateUserIfNecessary(`/roles/${res.id}`, flashMessage);
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
                confirmationTxt: I18n.t("forms.ok"),
                confirmationHeader: I18n.t("confirmationDialog.error")
            });
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
        } else {
            setLoading(true);
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
            && applications.every(app => !app.invalid)
            && !isEmpty(applications)
            && role.defaultExpiryDays > 0;
    }

    const changeApplication = (index, application) => {
        applications.splice(index, 1, application);
        setApplications([...applications]);
    }

    const changeApplicationLandingPage = (index, e) => {
        const application = applications[index];
        const newApplication = {...application, landingPage: e.target.value, invalid: false, changed: true};
        applications.splice(index, 1, newApplication);
        setApplications([...applications]);
    }

    const addApplication = () => {
        const filteredProviders = providers.filter(option => !applications.some(app => option.value === app.value));
        applications.push(filteredProviders[0]);
        setApplications([...applications]);
    }

    const deleteApplication = index => {
        applications.splice(index, 1);
        setApplications([...applications]);
    }

    const renderForm = () => {
        const valid = isValid();
        const disabledSubmit = !valid && !initial;
        const filteredProviders = providers.filter(option => !applications.some(app => option.value === app.value));
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

                <InputField name={I18n.t("roles.urn")}
                            value={urnFromRole(config.groupUrnPrefix, role)}
                            disabled={true}
                            toolTip={I18n.t("tooltips.roleUrn")}
                />

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
                        <SelectField name={I18n.t("roles.manage")}
                                     value={application}
                                     options={filteredProviders}
                                     onChange={option => changeApplication(index, option)}
                                     searchable={true}
                                     clearable={false}
                        />
                        <div className="input-field-container">
                            <InputField name={I18n.t("roles.landingPage")}
                                        value={application.changed ? (application.landingPage || "") : (application.url || "")}
                                        isUrl={true}
                                        placeholder={I18n.t("roles.landingPagePlaceHolder")}
                                        onBlur={e => validateApplication(index, e.target.value)}
                                        onChange={e => changeApplicationLandingPage(index, e)}
                        />
                            {application.invalid &&
                                <ErrorIndicator msg={I18n.t("forms.invalid", {
                                    attribute: I18n.t("roles.landingPage").toLowerCase(),
                                    value: application.landingPage
                                })}/>}
                        </div>
                        {index !== 0 &&
                            <Button type={ButtonType.Delete}
                                    onClick={() => deleteApplication(index)}/>
                        }
                    </div>
                )}
                {(!initial && isEmpty(applications)) &&
                    <ErrorIndicator msg={I18n.t("forms.required", {
                        attribute: I18n.t("roles.manage").toLowerCase()
                    })}/>}

                <div className="application-actions">
                    <Button txt={I18n.t("roles.addApplication")}
                            disabled={providers.length === applications.length}
                            onClick={addApplication}/>
                </div>

                <h2 className="section-separator">
                    {I18n.t("roles.invitationDetails")}
                </h2>

                <span className={"label"}>{I18n.t("roles.advanced")}</span>

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
                             onChange={() => {
                                 setCustomRoleExpiryDate(!customRoleExpiryDate);
                                 setRole({...role, defaultExpiryDays: 366})
                             }}
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
                    {!isNewRole &&
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
                                                     isWarning={confirmation.warning}
                                                     isError={confirmation.error}
                                                     question={confirmation.question}/>}

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