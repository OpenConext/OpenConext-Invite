import "./Tokens.scss";
import {useAppStore} from "../stores/AppStore";
import React, {useCallback, useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {Button, ButtonType, Loader} from "@surfnet/sds";
import {useNavigate} from "react-router-dom";
import {apiTokens, createToken, deleteToken, generateToken} from "../api";
import {dateFromEpoch} from "../utils/Date";
import {ReactComponent as TrashIcon} from "@surfnet/sds/icons/functional-icons/bin.svg";
import {ReactComponent as ChevronLeft} from "@surfnet/sds/icons/functional-icons/arrow-left-2.svg";
import ConfirmationDialog from "../components/ConfirmationDialog";
import DOMPurify from "dompurify";
import InputField from "../components/InputField";
import ErrorIndicator from "../components/ErrorIndicator";
import {isEmpty, stopEvent} from "../utils/Utils";
import {Page} from "../components/Page";

export const Tokens = () => {
    const {user, setFlash} = useAppStore(state => state);
    const navigate = useNavigate();
    const [tokens, setTokens] = useState(true);
    const [tokenValue, setTokenValue] = useState(null);
    const [description, setDescription] = useState("");
    const [newToken, setNewToken] = useState(false);
    const [loading, setLoading] = useState(true);
    const [initial, setInitial] = useState(true);
    const [confirmation, setConfirmation] = useState({});
    const [confirmationOpen, setConfirmationOpen] = useState(false);

    const fetchTokens = useCallback(() => {
        apiTokens()
            .then(res => {
                setTokens(res);
                setLoading(false);
                setNewToken(false);
                setDescription("");
                setTokenValue(null);
                setConfirmationOpen(false);
            });
    }, []);

    useEffect(() => {
        if (user.institutionAdmin) {
            fetchTokens();
        } else {
            navigate("/404");
        }
    }, [user])// eslint-disable-line react-hooks/exhaustive-deps

    const removeAPIToken = (token, showConfirmation) => {
        if (showConfirmation) {
            setConfirmation({
                cancel: () => setConfirmationOpen(false),
                action: () => removeAPIToken(token, false),
                warning: true,
                question: I18n.t("tokens.deleteConfirmation"),
            });
            setConfirmationOpen(true);
        } else {
            setLoading(true);
            deleteToken(token).then(() => fetchTokens())
        }
    };

    const submitNewToken = () => {
        if (isEmpty(description)) {
            setInitial(false);
        } else {
            setLoading(true);
            createToken(description).then(() => {
                setFlash(I18n.t("tokens.createFlash"));
                fetchTokens();
            });
        }

    }

    const cancelSideScreen = e => {
        stopEvent(e);
        setNewToken(false);
    }

    const createNewToken = () => {
        setLoading(true);
        generateToken().then(res => {
            setNewToken(true);
            setTokenValue(res.token);
            setLoading(false);
        });
    }

    const renderNewToken = () => {
        return (
            <Page className={"page new-token"}>
                <div className="back-to-tokens-container">
                    <a href={"/cancel"}
                       className={"back-to-tokens"}
                       onClick={cancelSideScreen}>
                        <ChevronLeft/>{I18n.t("tokens.backToOverview")}
                    </a>
                </div>
                <div className="new-token">
                    <p dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("tokens.secretDisclaimer"))}}/>
                    <InputField value={tokenValue}
                                name={I18n.t("tokens.secret")}
                                toolTip={I18n.t("tokens.secretTooltip")}
                                disabled={true}
                                copyClipBoard={true}/>

                    <InputField value={description}
                                onChange={e => setDescription(e.target.value)}
                                placeholder={I18n.t("tokens.descriptionPlaceHolder")}
                                name={I18n.t("tokens.description")}
                                error={(!initial && isEmpty(description))}
                                toolTip={I18n.t("tokens.descriptionTooltip")}
                    />
                    {(!initial && isEmpty(description)) && <ErrorIndicator
                        msg={I18n.t("tokens.required")}/>}

                    <section className="actions">
                        <Button type={ButtonType.Secondary}
                                txt={I18n.t("forms.cancel")}
                                onClick={() => setNewToken(false)}/>
                        <Button txt={I18n.t("forms.save")}
                                disabled={!initial && isEmpty(description)}
                                onClick={() => submitNewToken()}/>
                    </section>
                </div>
            </Page>
        );

    }

    const columns = [
        {
            key: "secret",
            header: I18n.t("tokens.secret"),
            mapper: () => I18n.t("tokens.secretValue"),
        },
        {
            key: "description",
            header: I18n.t("tokens.description"),
            mapper: token => <span className={"cut-of-lines"}>{token.description}</span>
        },
        {
            key: "created_at",
            header: I18n.t("tokens.createdAt"),
            mapper: token => dateFromEpoch(token.createdAt)
        },
        {
            nonSortable: true,
            key: "trash",
            header: "",
            mapper: token =>
                <span onClick={() => removeAPIToken(token, true)}>
                    <TrashIcon/>
                </span>
        },
    ]

    if (loading) {
        return <Loader/>
    }

    return (
        <div className={"mod-tokens"}>
            {newToken && renderNewToken()}
            {confirmationOpen && <ConfirmationDialog isOpen={confirmationOpen}
                                                     cancel={confirmation.cancel}
                                                     confirm={confirmation.action}
                                                     isWarning={confirmation.warning}
                                                     question={confirmation.question}/>}
            {!newToken && <Entities
                entities={tokens}
                modelName="tokens"
                showNew={true}
                newLabel={I18n.t("tokens.new")}
                newEntityFunc={() => createNewToken()}
                defaultSort="description"
                columns={columns}
                searchAttributes={["description"]}
                customNoEntities={I18n.t(`tokens.noEntities`)}
                loading={false}
                inputFocus={true}
                hideTitle={false}
            />}
        </div>
    );

}
