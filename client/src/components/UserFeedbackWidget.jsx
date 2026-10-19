import React, {useCallback, useRef, useState} from "react";
import {Checkbox, Label, Textarea} from "@surfnet/curve-react";
import {useLocation} from "react-router";
import DOMPurify from "dompurify";
import html2canvas from "html2canvas-pro";
import I18n from "../locale/I18n";
import {sendFeedback} from "../api";
import {useAppStore} from "../stores/AppStore";
import ConfirmationDialog from "./ConfirmationDialog";
import "./UserFeedbackWidget.scss";

const MAX_SCREENSHOT_BYTES = 5 * 1024 * 1024;

export const UserFeedbackWidget = () => {
    const location = useLocation();
    const setFlash = useAppStore(state => state.setFlash);
    const user = useAppStore(state => state.user);
    const [open, setOpen] = useState(false);
    const [message, setMessage] = useState("");
    const [includeScreenshot, setIncludeScreenshot] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [previewScreenshot, setPreviewScreenshot] = useState(null);
    const inputRef = useRef(null);

    const closeModal = () => {
        setOpen(false);
        setMessage("");
        setIncludeScreenshot(true);
        setPreviewScreenshot(null);
        document.body.classList.remove("feedback-open");
    };

    const captureScreenshot = useCallback(async (hideModal = true) => {
        window.scrollTo({top: 0, behavior: "instant"});
        document.body.classList.add("feedback-capture");
        if (hideModal) {
            document.body.classList.add("feedback-capture--hide-modal");
        }
        const canvas = await html2canvas(document.body, {
            backgroundColor: null,
            useCORS: true,
            scale: 1,
            windowWidth: document.body.clientWidth,
            windowHeight: document.body.scrollHeight
        });
        document.body.classList.remove("feedback-capture");
        document.body.classList.remove("feedback-capture--hide-modal");
        return canvas.toDataURL("image/png");
    }, []);

    const handleOpen = useCallback(() => {
        // Capture the page first — before the feedback modal is in the DOM —
        // so any open confirmation modals (e.g. delete confirmations) are
        // included in the screenshot.
        captureScreenshot(false).then(dataUrl => {
            document.body.classList.add("feedback-open");
            setOpen(true);
            setPreviewScreenshot(dataUrl);
            setTimeout(() => inputRef.current?.focus(), 25);
        });
    }, [captureScreenshot]);

    const handleSubmit = useCallback(async () => {
        if (!message.trim()) {
            return;
        }
        setSubmitting(true);
        try {
            const payload = {
                message,
                url: `${window.location.origin}${location.pathname}${location.search}${location.hash}`,
                includeScreenshot
            };

            if (includeScreenshot && previewScreenshot) {
                const base64 = previewScreenshot.split(",")[1] || "";
                const estimatedBytes = Math.ceil((base64.length * 3) / 4);
                if (estimatedBytes > MAX_SCREENSHOT_BYTES) {
                    setFlash(I18n.t("feedback.tooLarge"));
                } else if (base64.length > 0) {
                    payload.screenshotBase64 = base64;
                    payload.screenshotContentType = "image/png";
                }
            }
            await sendFeedback(payload);
            closeModal();
            setFlash(I18n.t("feedback.flash"));
        } finally {
            setSubmitting(false);
        }
    }, [includeScreenshot, location.hash, location.pathname, location.search, message, previewScreenshot, setFlash]);

    const renderMailPreview = () => {
        const now = new Date();
        const dateStr = now.toLocaleDateString(I18n.locale, {
            weekday: "short",
            year: "numeric",
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        });

        return (
            <div className="mail-preview">
                <div className="mail-preview__toolbar">
                    <div className="mail-preview__dots">
                        <span className="dot red"/>
                        <span className="dot yellow"/>
                        <span className="dot green"/>
                    </div>
                    <span className="mail-preview__toolbar-title">
                        {I18n.t("feedback.preview.subjectLineInfo")}
                    </span>
                </div>
                <div className="mail-preview__header">
                    <div className="mail-preview__field">
                        <span className="lefty">{I18n.t("feedback.preview.to")}:</span>
                        <span className="value">support@surf.nl</span>
                    </div>
                    <div className="mail-preview__field">
                        <span className="lefty">{I18n.t("feedback.preview.from")}:</span>
                        <span className="value">no-reply@surf.nl</span>
                    </div>
                    <div className="mail-preview__field">
                        <span className="lefty">{I18n.t("feedback.preview.subject")}:</span>
                        <span className="value">{I18n.t("feedback.preview.subjectLine")}</span>
                    </div>
                    <div className="mail-preview__field">
                        <span className="lefty">{I18n.t("feedback.preview.date")}:</span>
                        <span className="value">{dateStr}</span>
                    </div>
                </div>
                <div className="mail-preview__body">
                    <p className="greeting">{I18n.t("feedback.preview.greeting")}</p>
                    <p className="intro">{I18n.t("feedback.preview.providedFeedback", {name: user?.name || ""})}</p>
                    <div className="mail-preview__quote">
                        <p className={message.trim() ? "" : "placeholder"}>
                            {message.trim() || I18n.t("feedback.preview.messagePlaceholder")}
                        </p>
                    </div>
                    {includeScreenshot && previewScreenshot && (
                        <div className="mail-preview__screenshot">
                            <img
                                src={previewScreenshot}
                                alt={I18n.t("feedback.preview.screenshotLabel")}
                            />
                        </div>
                    )}
                    {includeScreenshot && !previewScreenshot && (
                        <div className="mail-preview__screenshot-loading"/>
                    )}
                    <p className="follow-up">{I18n.t("feedback.preview.followUp", {email: user?.email || ""})}</p>
                </div>
            </div>
        );
    };

    const renderContent = () => (
        <div className="user-feedback-widget__layout">
            <div className="user-feedback-widget__form">
                <div className="user-feedback-widget__modal">
                    <p>{I18n.t("feedback.info")}</p>
                    <Textarea
                        name="feedback"
                        id="feedback"
                        value={message}
                        rows="10"
                        ref={inputRef}
                        onChange={e => setMessage(e.target.value)}
                    />
                    <Label className="user-feedback-widget__options">
                        <Checkbox
                            checked={includeScreenshot}
                            onCheckedChange={() => setIncludeScreenshot(!includeScreenshot)}
                        />
                        {I18n.t("feedback.includeScreenshot")}
                    </Label>
                    <section className="disclaimer">
                        <span
                            dangerouslySetInnerHTML={{
                                __html: DOMPurify.sanitize(I18n.t("feedback.disclaimer"), {
                                    ADD_ATTR: ["target", "rel"]
                                })
                            }}
                        />
                    </section>
                    <section className="help">
                        <h3
                            className="title"
                            dangerouslySetInnerHTML={{
                                __html: DOMPurify.sanitize(I18n.t("feedback.help"))
                            }}
                        />
                        <span
                            dangerouslySetInnerHTML={{
                                __html: DOMPurify.sanitize(I18n.t("feedback.helpInfo"))
                            }}
                        />
                    </section>
                </div>
            </div>
            {renderMailPreview()}
        </div>
    );

    if (open) {
        return (
            <ConfirmationDialog
                cancel={closeModal}
                confirm={handleSubmit}
                confirmationHeader={I18n.t("feedback.title")}
                confirmationTxt={I18n.t("forms.submit")}
                disabledConfirm={submitting || !message.trim()}
                children={renderContent()}
                largeWidth={true}
                className="feedback-preview-dialog"
            />
        );
    }
    return (
        <div className="user-feedback-widget">
            <button
                className="user-feedback-widget__trigger"
                onClick={handleOpen}
                type="button"
            >
                {I18n.t("feedback.widgetLabel")}
            </button>
        </div>
    );
};
