import React, {useState} from "react";
import "./Header.scss";
import {Logo, LogoColor, LogoType} from "@surfnet/sds";
import {UserMenu} from "../components/UserMenu";
import {stopEvent} from "../utils/Utils";
import FeedbackDialog from "./Feedback";
import {Link} from "react-router-dom";

export const Header = ({user, config}) => {

    const [showFeedback, setShowFeedback] = useState(false);

    return (
        <div className="header-container">
            <FeedbackDialog isOpen={showFeedBack} close={() => this.setState({showFeedBack: false})}/>
            <div className="header-inner" onClick={this.toggleStyle}>
                <Link className="logo" to={"/"}>
                    <Logo label={"Research Access Management"} position={LogoType.Bottom} color={LogoColor.White}/>
                </Link>
                {user &&
                <UserMenu currentUser={user}
                          config={config}
                          provideFeedback={e => {
                              stopEvent(e);
                              setShowFeedback(true);
                          }}/>
                }
            </div>
        </div>
    );
}

