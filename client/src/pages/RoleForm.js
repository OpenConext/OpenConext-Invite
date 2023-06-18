import React, {useEffect, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import I18n from "../locale/I18n";
import {AUTHORITIES, isUserAllowed} from "../utils/UserRole";
import {allProviders, createRole, deleteRole, roleByID, shortNameExists, updateRole} from "../api";
import {Button, ButtonType, Loader} from "@surfnet/sds";
import "./RoleForm.scss";
import {UnitHeader} from "../components/UnitHeader";
import {ReactComponent as RoleIcon} from "@surfnet/sds/icons/illustrative-icons/hierarchy.svg";
import InputField from "../components/InputField";
import {sanitizeShortName} from "../validations/regExps";
import {isEmpty} from "../utils/Utils";
import ErrorIndicator from "../components/ErrorIndicator";
import SelectField from "../components/SelectField";
import {providersToOptions, singleProviderToOption} from "../utils/Manage";
import ConfirmationDialog from "../components/ConfirmationDialog";

export const RoleForm = () => {

    const navigate = useNavigate();
    const {id} = useParams();

    const [role, setRole] = useState({});
    const [providers, setProviders] = useState([]);
    const [isNewRole, setNewRole] = useState(true);
    const {user, setFlash} = useAppStore(state => state);
    const [loading, setLoading] = useState(true);
    const [initial, setInitial] = useState(true);
    const required = ["name", "description", "manageId"];
    const [alreadyExists, setAlreadyExists] = useState({});
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
                    setProviders(res[1]);
                } else {
                    setProviders(user.providers);
                }
                setNewRole(newRole);
                const name = newRole ? "" : res[0].name;
                const breadcrumbPath = [
                    {path: "/home", value: I18n.t("tabs.home")},
                    {path: "/home/roles", value: I18n.t("tabs.roles")},
                ];
                if (newRole) {
                    const providerOption = singleProviderToOption(user.superUser ? res[1][0] : user.providers[0]);
                    setManagementOption(providerOption);
                    setRole({...role, manageId: providerOption.value, manageType: providerOption.type.toUpperCase()})
                } else {
                    breadcrumbPath.push({path: `/roles/${res[0].id}`, value: name});
                    setManagementOption(singleProviderToOption(res[0].application));
                }
                breadcrumbPath.push({value: I18n.t(`roles.${newRole ? "new" : "edit"}`, {name: name})});
                useAppStore.setState({breadcrumbPath: breadcrumbPath});
                setLoading(false);
            })
        },
        [id]); // eslint-disable-line react-hooks/exhaustive-deps

    const validateShortName = shortName => {
        if (!isEmpty(role.manageId) && !isEmpty(role.shortName)) {
            shortNameExists(shortName, role)
                .then(json => setAlreadyExists({...alreadyExists, shortName: json.exists}));
        }
        return true;
    }

    const submit = () => {
        //Todo management options
        setInitial(false);
        if (isValid()) {
            const promise = isNewRole ? createRole : updateRole;
            promise(role).then(() => {
                setFlash(I18n.t(`roles.${isNewRole ? "createFlash" : "updateFlash"}`, {name: role.name}));
                navigate("/home/roles");
            });
        }
    }

    const doDelete = showConfirmation => {
        if (showConfirmation) {
            setConfirmation({
                cancel: () => setConfirmationOpen(false),
                action: () => doDelete(true),
                warning: true,
                question: I18n.t("roles.deleteConfirmation")
            });
            setConfirmationOpen(true);
        } else {
            deleteRole(role).then(() => {
                setConfirmationOpen(false);
                setFlash(I18n.t("roles.deleteFlash", {name: role.name}));
            })
        }
    };
    const isValid = () => {
        return required.every(attr => !isEmpty(role[attr]))
            && Object.values(alreadyExists).every(attr => !attr);
    }

    const renderForm = () => {
        const disabledSubmit = isValid() && !initial;
        return (<>
                <InputField name={I18n.t("roles.name")}
                            value={role.name || ""}
                            placeholder={I18n.t("roles.namePlaceHolder")}
                            error={alreadyExists.name || (!initial && isEmpty(role.name))}
                            onBlur={e => validateShortName(sanitizeShortName(e.target.value))}
                            onChange={e => setRole(
                                {...role, name: e.target.value, shortName: sanitizeShortName(e.target.value)})}
                />
                {alreadyExists.name && <ErrorIndicator msg={I18n.t("forms.alreadyExistsParent", {
                    attribute: I18n.t("roles.shortName").toLowerCase(),
                    value: role.shortName,
                    parent: role.manageName
                })}/>}
                {(!initial && isEmpty(role.name)) &&
                    <ErrorIndicator msg={I18n.t("forms.required", {
                        attribute: I18n.t("roles.name").toLowerCase()
                    })}/>}

                <InputField name={I18n.t("roles.shortName")}
                            value={role.shortName || ""}
                            disabled={true}
                            toolTip={I18n.t("tooltips.roleShortName")}
                />

                <InputField name={I18n.t("roles.description")}
                            value={role.description || ""}
                            placeholder={I18n.t("roles.descriptionPlaceHolder")}
                            multiline={true}
                            onChange={e => setRole(
                                {...role, description: e.target.value})}
                />

                <SelectField name={I18n.t("roles.manage")}
                             value={managementOption}
                             options={providersToOptions(providers).filter(provider => provider.value !== managementOption.value)}
                             onChange={option => {
                                 setManagementOption(option);
                                 setRole({...role, manageId: option.value, manageType: option.type.toUpperCase()});
                             }}
                             searchable={true}
                             clearable={false}
                             toolTip={I18n.t("tooltips.manageService")}
                />

                <InputField name={I18n.t("roles.defaultExpiryDays")}
                            value={role.defaultExpiryDays || 0}
                            isInteger={true}
                            onChange={e => setRole(
                                {...role, defaultExpiryDays: e.target.value})}
                            toolTip={I18n.t("tooltips.defaultExpiryDays")}
                />

                <InputField name={I18n.t("roles.landingPage")}
                            value={role.landingPage || ""}
                            placeholder={I18n.t("roles.landingPagePlaceHolder")}
                            multiline={true}
                            onChange={e => setRole(
                                {...role, landingPage: e.target.value})}
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
                                                     isWarning={confirmation.warning}
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