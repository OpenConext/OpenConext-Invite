import React, {useEffect, useRef, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import I18n from "../locale/I18n";
import {AUTHORITIES, isUserAllowed, urnFromRole} from "../utils/UserRole";
import {allProviders, createRole, deleteRole, me, roleByID, shortNameExists, updateRole, validate} from "../api";
import {Button, ButtonType, Checkbox, Loader} from "@surfnet/sds";
import "./RoleForm.scss";
import {UnitHeader} from "../components/UnitHeader";
import {ReactComponent as RoleIcon} from "@surfnet/sds/icons/illustrative-icons/hierarchy.svg";
import InputField from "../components/InputField";
import {constructShortName} from "../validations/regExps";
import {isEmpty} from "../utils/Utils";
import ErrorIndicator from "../components/ErrorIndicator";
import SelectField from "../components/SelectField";
import {providersToOptions, singleProviderToOption} from "../utils/Manage";
import ConfirmationDialog from "../components/ConfirmationDialog";

export const RoleForm = () => {

    const navigate = useNavigate();
    const {id} = useParams();
    const nameRef = useRef();

    const {user, setFlash, config} = useAppStore(state => state);

    const [role, setRole] = useState({name: "", shortName: "", defaultExpiryDays: 0});
    const [providers, setProviders] = useState([]);
    const [isNewRole, setNewRole] = useState(true);
    const [loading, setLoading] = useState(true);
    const [initial, setInitial] = useState(true);
    const required = ["name", "description", "manageId"];
    const [alreadyExists, setAlreadyExists] = useState({});
    const [invalidValues, setInvalidValues] = useState({});
    const [managementOption, setManagementOption] = useState({});
    const [confirmation, setConfirmation] = useState({});
    const [confirmationOpen, setConfirmationOpen] = useState(false);

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
                    setProviders(res[newRole ? 0 : 1]);
                } else {
                    setProviders(user.userRoles.map(userRole => userRole.role.application));
                }
                setNewRole(newRole);
                const name = newRole ? "" : res[0].name;
                const breadcrumbPath = [
                    {path: "/home", value: I18n.t("tabs.home")},
                    {path: "/home/roles", value: I18n.t("tabs.roles")},
                ];
                if (newRole) {
                    const providerOption = singleProviderToOption(user.superUser ? res[0][0] : user.userRoles[0].role.application);
                    setManagementOption(providerOption);
                    setRole({...role, manageId: providerOption.value, manageType: providerOption.type.toUpperCase()})
                } else {
                    breadcrumbPath.push({path: `/roles/${res[0].id}`, value: name});
                    setManagementOption(singleProviderToOption(res[0].application));
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

    const validateShortName = (shortName, manageId) => {
        if (!isEmpty(manageId) && !isEmpty(shortName)) {
            shortNameExists(shortName, manageId, role.id)
                .then(json => setAlreadyExists({...alreadyExists, shortName: json.exists}));
        }
        return true;
    }

    const validateValue = (type, attribute, value) => {
        if (!isEmpty(value)) {
            validate(type, value)
                .then(json => setInvalidValues({...invalidValues, [attribute]: !json.valid}));
        }
        return true;
    }

    const submit = () => {
        setInitial(false);
        if (isValid()) {
            setLoading(true);
            const promise = isNewRole ? createRole : updateRole;
            promise(role)
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
            && Object.values(invalidValues).every(attr => !attr)
            && role.defaultExpiryDays > 0;
    }

    const renderForm = () => {
        const valid = isValid();
        const disabledSubmit = !valid && !initial;
        const options = providersToOptions(providers);
        return (<>
                <InputField name={I18n.t("roles.name")}
                            value={role.name || ""}
                            placeholder={I18n.t("roles.namePlaceHolder")}
                            error={alreadyExists.name || (!initial && isEmpty(role.name))}
                            onBlur={e => {
                                if (isNewRole) {
                                    validateShortName(constructShortName(e.target.value), role.manageId);
                                }
                            }}
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
                {alreadyExists.shortName &&
                    <ErrorIndicator msg={I18n.t("forms.alreadyExistsParent", {
                        attribute: I18n.t("roles.shortName").toLowerCase(),
                        value: role.shortName,
                        parent: managementOption.label
                    })}/>}

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

                <SelectField name={I18n.t("roles.manage")}
                             value={managementOption}
                             options={options.filter(provider => provider.value !== managementOption.value)}
                             onChange={option => {
                                 setManagementOption(option);
                                 setRole({...role, manageId: option.value, manageType: option.type.toUpperCase()});
                                 validateShortName(constructShortName(role.name), option.value);
                             }}
                             searchable={true}
                             clearable={false}
                             disabled={options.length === 1}
                             toolTip={I18n.t("tooltips.manageService")}
                />

                <Checkbox name={I18n.t("invitations.enforceEmailEquality")}
                          value={role.enforceEmailEquality || false}
                          info={I18n.t("invitations.enforceEmailEquality")}
                          onChange={e => setRole({...role, enforceEmailEquality: e.target.checked})}
                          tooltip={I18n.t("tooltips.enforceEmailEqualityTooltip")}
                />

                <Checkbox name={I18n.t("invitations.eduIDOnly")}
                          value={role.eduIDOnly || false}
                          info={I18n.t("invitations.eduIDOnly")}
                          onChange={e => setRole({...role, eduIDOnly: e.target.checked})}
                          tooltip={I18n.t("tooltips.eduIDOnlyTooltip")}
                />

                <InputField name={I18n.t("roles.defaultExpiryDays")}
                            value={role.defaultExpiryDays || 0}
                            isInteger={true}
                            onChange={e => {
                                const val = parseInt(e.target.value);
                                const defaultExpiryDays = Number.isInteger(val) && val > 0 ? val : 0;
                                setRole(
                                    {...role, defaultExpiryDays: defaultExpiryDays})
                            }}
                            toolTip={I18n.t("tooltips.defaultExpiryDays")}
                />
                {(!initial && (isEmpty(role.defaultExpiryDays) || role.defaultExpiryDays < 1)) &&
                    <ErrorIndicator msg={I18n.t("forms.required", {
                        attribute: I18n.t("roles.defaultExpiryDays").toLowerCase()
                    })}/>}

                <InputField name={I18n.t("roles.landingPage")}
                            value={role.landingPage || ""}
                            isUrl={true}
                            placeholder={I18n.t("roles.landingPagePlaceHolder")}
                            onBlur={e => validateValue("url", "landingPage", e.target.value)}
                            onChange={e => {
                                setRole(
                                    {...role, landingPage: e.target.value});
                                setInvalidValues({...invalidValues, landingPage: false})
                            }}
                />
                {invalidValues.landingPage &&
                    <ErrorIndicator msg={I18n.t("forms.invalid", {
                        attribute: I18n.t("roles.landingPage").toLowerCase(),
                        value: role.landingPage
                    })}/>}


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