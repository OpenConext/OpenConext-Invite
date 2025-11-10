import React, {useEffect, useRef, useState} from "react";
import {Tooltip} from "@surfnet/sds";
import "./EmailField.scss";
import {isEmpty, stopEvent} from "../utils/Utils";
import I18n from "../locale/I18n";
import {validEmailRegExp} from "../validations/regExps";
import CloseIcon from "@surfnet/sds/icons/functional-icons/close.svg";
import MailIcon from "@surfnet/sds/icons/functional-icons/id-2.svg";

export default function EmailField({
                                       name,
                                       emails,
                                       addEmails,
                                       removeMail,
                                       pinnedEmails = [],
                                       error = false,
                                       grabFocus = true,
                                       required = false,
                                       maxEmails = null,
                                       maxEmailsMessage = null
                                   }) {

    const [emailErrors, setEmailErrors] = useState([]);
    const [displayMaxEmailMessage, setDisplayMaxEmailMessage] = useState(false);
    const [value, setValue] = useState("");

    const refContainer = useRef(null);

    useEffect(() => {
        if (grabFocus && refContainer.current) {
            refContainer.current.focus();
        }
    }, [grabFocus]);

    const internalOnChange = e => {
        if (!["Enter", "Spacebar", "Backspace", "Tab"].includes(e.key)) {
            setEmailErrors([]);
        }
        setValue(e.target.value);
    }

    const displayEmail = email => {
        const indexOf = email.indexOf("<");
        if (indexOf > -1) {
            return <Tooltip tip={email.substring(indexOf + 1, email.length - 1)}
                            standalone={true}
                            children={<span>{email.substring(0, indexOf).trim()}</span>}/>;
        }
        return <span>{email}</span>;
    }

    const validateEmail = (part, invalidEmails) => {
        const hasLength = part.trim().length > 0;
        const valid = hasLength && validEmailRegExp.test(part);
        if (!valid && hasLength) {
            invalidEmails.push(part.trim());
        }
        return valid;
    }

    const internalAddEmail = e => {
        if (isEmpty(e.key) && isEmpty(e.target.value)) {
            return;
        }
        const email = e.target.value;
        const invalidEmails = [];
        const delimiters = [",", " ", ";", "\n", "\t"];
        let localEmails;
        if (!isEmpty(email) && email.indexOf("<") > -1) {
            localEmails = email.split(/[,\n\t;]/)
                .map(e => e.trim())
                .filter(part => {
                    const indexOf = part.indexOf("<");
                    part = indexOf > -1 ? part.substring(indexOf + 1, part.length - 1) : part;
                    return validateEmail(part, invalidEmails);
                });
        } else if (!isEmpty(email) && delimiters.some(delimiter => email.indexOf(delimiter) > -1)) {
            const replacedEmails = email.replace(/[;\s]/g, ",");
            const splitEmails = replacedEmails.split(",");
            localEmails = splitEmails
                .filter(part => validateEmail(part, invalidEmails));
        } else if (!isEmpty(email)) {
            const valid = validEmailRegExp.test(email.trim());
            if (valid) {
                localEmails = [email];
            } else {
                invalidEmails.push(email.trim());
            }
        }
        setEmailErrors((!isEmpty(e.target.value) && !isEmpty(invalidEmails)) ? invalidEmails : []);
        const uniqueEmails = [...new Set(localEmails)];
        const nbrOfEmails = (emails || []).length + uniqueEmails.length;
        if (!isEmpty(uniqueEmails) && (isEmpty(maxEmails) || nbrOfEmails <= maxEmails)) {
            addEmails(uniqueEmails);
        }
        if (!isEmpty(maxEmails) && nbrOfEmails > maxEmails) {
            setDisplayMaxEmailMessage(true);
        } else {
            setDisplayMaxEmailMessage(false);
        }
        setValue("");
    };

    const internalRemoveMail = mail => {
        setEmailErrors([]);
        removeMail(mail);
    }

    return (
        <div className={`email-field ${error ? "error" : ""}`}>
            <label htmlFor={name}>{name}{required && <sup className="required">*</sup>}
                <Tooltip
                    tip={`${I18n.t("tooltips.inviteesTooltip")}`}/>
            </label>
            <div className={`inner-email-field ${error ? "error" : ""}`}>
                {emails.map((mail, index) =>
                    <div key={index} className="email-tag">
                        {displayEmail(mail)}
                        {pinnedEmails.includes(mail) ?
                            <span className="disabled icon"><MailIcon/></span> :
                            <span className="icon" onClick={() => internalRemoveMail(mail)}>
                                <CloseIcon/>
                            </span>}

                    </div>)}
                <textarea id="email-field"
                          inputMode="email"
                          value={value}
                          ref={refContainer}
                          onChange={internalOnChange}
                          onBlur={internalAddEmail}
                          onKeyDown={e => {
                              if (e.key === "Enter" || e.key === " " || e.key === "Spacebar") {
                                  internalAddEmail(e);
                                  setTimeout(() => document.getElementById("email-field").focus(), 50);
                                  return stopEvent(e);
                              } else if (e.key === "Backspace" && isEmpty(value) && emails.length > 0) {
                                  const mail = emails[emails.length - 1];
                                  if (!pinnedEmails.includes(mail)) {
                                      internalRemoveMail(mail);
                                  }
                              }
                          }}
                          placeholder={emails.length === 0 ? I18n.t("invitations.inviteesPlaceholder") : ""} cols={3}/>
            </div>
            {(!isEmpty(emailErrors) && value === "") && <p className="error">
                {I18n.t("invitations.invalidEmails", {emails: Array.from(new Set(emailErrors)).join(", ")})}
            </p>}
            {(displayMaxEmailMessage && !isEmpty(maxEmailsMessage)) && <p className="error">
                {maxEmailsMessage}
            </p>}

        </div>
    );
}
