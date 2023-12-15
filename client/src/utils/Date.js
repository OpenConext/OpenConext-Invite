import I18n from "../locale/I18n";
import {format, register} from "timeago.js";
import {isEmpty} from "./Utils";

let timeAgoInitialized = false;

export const futureDate = (daysAhead, fromDate = new Date()) => {
    const time = fromDate.getTime() + (1000 * 60 * 60 * 24 * daysAhead);
    return new Date(time);
}

export const shortDateFromEpoch = epoch => {
    const options = {month: "short", day: "numeric", year: "numeric"};
    const dateTimeFormat = new Intl.DateTimeFormat(`${I18n.locale}-${I18n.locale.toUpperCase()}`, options)
    return dateTimeFormat.format(new Date(epoch * 1000));
}

export const dateFromEpoch = epoch => {
    if (isEmpty(epoch)) {
        return "-";
    }
    const options = {month: "long", day: "numeric", year: "numeric"};
    const dateTimeFormat = new Intl.DateTimeFormat(`${I18n.locale}-${I18n.locale.toUpperCase()}`, options)
    return dateTimeFormat.format(new Date(epoch * 1000));
}

export const formatDate = date => {
    const day = String(date.getDate()).padStart(2, "0");
    const month = String(date.getMonth() + 1).padStart(2, "0");
    return `${day}/${month}/${date.getFullYear()}`;
}

export const languageSwitched = () => {
    timeAgoInitialized = false;
}

const TIME_AGO_LOCALE = "time-ago-locale";
const LAST_ACTIVITY_LOCALE = "last-activity-locale";
const relativeTimeNotation = (date, translations) => {
    if (!timeAgoInitialized) {
        const timeAgoLocale = (number, index) => {
            return [
                [I18n.t("expirations.ago.now"), I18n.t("expirations.in.now")],
                [I18n.t("expirations.ago.seconds"), I18n.t("expirations.in.seconds")],
                [I18n.t("expirations.ago.minute"), I18n.t("expirations.in.minute")],
                [I18n.t("expirations.ago.minutes"), I18n.t("expirations.in.minutes")],
                [I18n.t("expirations.ago.hour"), I18n.t("expirations.in.hour")],
                [I18n.t("expirations.ago.hours"), I18n.t("expirations.in.hours")],
                [I18n.t("expirations.ago.day"), I18n.t("expirations.in.day")],
                [I18n.t("expirations.ago.days"), I18n.t("expirations.in.days")],
                [I18n.t("expirations.ago.week"), I18n.t("expirations.in.week")],
                [I18n.t("expirations.ago.weeks"), I18n.t("expirations.in.weeks")],
                [I18n.t("expirations.ago.month"), I18n.t("expirations.in.month")],
                [I18n.t("expirations.ago.months"), I18n.t("expirations.in.months")],
                [I18n.t("expirations.ago.year"), I18n.t("expirations.in.year")],
                [I18n.t("expirations.ago.years"), I18n.t("expirations.in.years")]
            ][index];
        };
        const lastActivityLocale = (number, index) => {
            return [
                [I18n.t("expirations.activity.now"), I18n.t("expirations.activity.now")],
                [I18n.t("expirations.activity.seconds"), I18n.t("expirations.activity.seconds")],
                [I18n.t("expirations.activity.minute"), I18n.t("expirations.activity.minute")],
                [I18n.t("expirations.activity.minutes"), I18n.t("expirations.activity.minutes")],
                [I18n.t("expirations.activity.hour"), I18n.t("expirations.activity.hour")],
                [I18n.t("expirations.activity.hours"), I18n.t("expirations.activity.hours")],
                [I18n.t("expirations.activity.day"), I18n.t("expirations.activity.day")],
                [I18n.t("expirations.activity.days"), I18n.t("expirations.activity.days")],
                [I18n.t("expirations.activity.week"), I18n.t("expirations.activity.week")],
                [I18n.t("expirations.activity.weeks"), I18n.t("expirations.activity.weeks")],
                [I18n.t("expirations.activity.month"), I18n.t("expirations.activity.month")],
                [I18n.t("expirations.activity.months"), I18n.t("expirations.activity.months")],
                [I18n.t("expirations.activity.year"), I18n.t("expirations.activity.year")],
                [I18n.t("expirations.activity.years"), I18n.t("expirations.activity.years")]
            ][index];
        };
        register(TIME_AGO_LOCALE, timeAgoLocale);
        register(LAST_ACTIVITY_LOCALE, lastActivityLocale);
        timeAgoInitialized = true;
    }
    const expired = date < new Date();
    const relativeTime = format(date, translations);
    return {expired, relativeTime};
}

export const displayExpiryDate = date => {
    const oneDayAhead = futureDate(1 , date);
    const {relativeTime} = relativeTimeNotation(oneDayAhead, TIME_AGO_LOCALE);
    return relativeTime;
}
