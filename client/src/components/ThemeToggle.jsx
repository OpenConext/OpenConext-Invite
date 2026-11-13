import React, {useEffect} from "react";
import {Button} from "@surfnet/curve-react";
import {MoonIcon, SunIcon} from "@phosphor-icons/react";
import I18n from "../locale/I18n";
import {useAppStore} from "../stores/AppStore";

export const ThemeToggle = () => {
    const dark = useAppStore(state => state.dark);
    const toggleDark = useAppStore(state => state.toggleDark);
    const themeSwitcherEnabled = useAppStore(state => state.config.themeSwitcherEnabled);

    // With the switcher disabled a previously stored dark preference must not stick
    useEffect(() => {
        if (themeSwitcherEnabled === false) {
            document.documentElement.classList.remove("dark");
        }
    }, [themeSwitcherEnabled]);

    if (themeSwitcherEnabled === false) {
        return null;
    }

    return (
        <Button variant="ghost" onClick={toggleDark} data-testid="theme-toggle">
            {dark ? <SunIcon/> : <MoonIcon/>}
            {dark ? I18n.t("navigation.lightMode") : I18n.t("navigation.darkMode")}
        </Button>
    );
}
