import {useAppStore} from "../stores/AppStore";
import I18n from "../locale/I18n";
import {useEffect, useState} from "react";
import owl from "../icons/owl.wav";
import {ReactComponent as Logo} from "../icons/owl01.svg";
import Tabs from "../components/Tabs";
import {UnitHeader} from "../components/UnitHeader";

export const Home = () => {

    const [currentTab, setCurrentTab] = useState("roles")
    const user = useAppStore((state) => state.user)

    useEffect(() => {
        useAppStore.setState({breadcrumbPath: [{path: "/home", value: I18n.t("paths.home")}]})
    }, []);


    return (
        <div className="home">
            <div className="mod-home-container">
                {<UnitHeader obj={({name: I18n.t("home.access"), svg: Logo})}
                                                              svgClick={() => new Audio(owl).play()}/>}
                <Tabs standAlone={!user.superUser} activeTab={tab} tabChanged={this.tabChanged}>
                    {tabs}
                </Tabs>
            </div>);

        </div>
    );
}
