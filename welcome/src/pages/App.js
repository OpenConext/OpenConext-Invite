import './App.scss';
import {Navigate, Route, Routes, useNavigate} from "react-router-dom";
import {useEffect, useState} from "react";
import {Loader} from "@surfnet/sds";
import {useAppStore} from "../stores/AppStore";
import {configuration, csrf, me} from "../api";
import {Login} from "./Login";
import {Flash} from "../components/Flash";
import {Header} from "../components/Header";
import {Footer} from "../components/Footer";
import {Invitation} from "./Invitation";
import {login} from "../utils/Login";
import NotFound from "./NotFound";
import {InviteOnly} from "./InviteOnly";
import {Profile} from "./Profile";
import {Proceed} from "./Proceed";
import {isEmpty} from "../utils/Utils";
import {MissingAttributes} from "./MissingAttributes";
import {flushSync} from "react-dom";


export const App = () => {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const {authenticated} = useAppStore(state => state);

    useEffect(() => {
        csrf().then(token => {
            useAppStore.setState(() => ({csrfToken: token.token}));
            configuration()
                .then(res => {
                    useAppStore.setState(() => ({config: res}));
                    let route;
                    if (!res.authenticated) {
                        if (!res.name) {
                            const direction = window.location.pathname + window.location.search;
                            localStorage.setItem("location", direction);
                        }
                        if (!isEmpty(res.missingAttributes)) {
                            route = "/missingAttributes"
                        } else {
                            const locationStored = localStorage.getItem("location");
                            const pathname = locationStored || window.location.pathname;
                            const isInvitationAcceptFlow = window.location.pathname.startsWith("/invitation/accept")
                                || pathname.startsWith("/invitation/accept");
                            if (res.name && !isInvitationAcceptFlow) {
                                route = "/deadend";
                            } else if (pathname === "/" || pathname.startsWith("/login") || isInvitationAcceptFlow) {
                                route = isInvitationAcceptFlow ? pathname : (window.location.pathname + window.location.search);
                            }
                        }
                        if (!isEmpty(route)) {
                            flushSync(() => {
                                navigate(route, {replace: true})
                            });
                        } else {
                            //Bookmarked URL's trigger a direct login and skip the landing page
                            login(res);
                        }
                        setTimeout(() => setLoading(false), 500);
                    } else {
                        me()
                            .then(res => {
                                useAppStore.setState(() => ({user: res, authenticated: true}));
                                const location = localStorage.getItem("location") || window.location.pathname + window.location.search;
                                const newLocation = location.startsWith("/login") ? "/profile" : location;
                                localStorage.removeItem("location");
                                flushSync(() => {
                                    navigate(newLocation, {replace: true})
                                });
                                setTimeout(() => setLoading(false), 500);
                            });
                    }
                })
                .catch(() => {
                    flushSync(() => {
                        navigate("/deadend", {replace: true})
                    });
                    setTimeout(() => setLoading(false), 500);
                })
        })
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    if (loading) {
        return <Loader/>
    }


    return (
        <div className="access">
            <div className="container">
                <Flash/>
                <Header/>
                {authenticated &&
                    <Routes>
                        <Route path="/" element={<Navigate replace to="profile"/>}/>
                        <Route path="profile" element={<Profile/>}/>
                        <Route path="proceed" element={<Proceed/>}/>
                        <Route path="invitation/accept"
                               element={<Invitation authenticated={true}/>}/>
                        <Route path="login" element={<Login/>}/>
                        <Route path="*" element={<NotFound/>}/>
                    </Routes>}
                {!authenticated &&
                    <Routes>
                        <Route path="/" element={<Navigate replace to="login"/>}/>
                        <Route path="proceed" element={<Proceed/>}/>
                        <Route path="invitation/accept" element={<Invitation authenticated={false}/>}/>
                        <Route path="login" element={<Login/>}/>
                        <Route path="deadend" element={<InviteOnly/>}/>
                        <Route path="missingAttributes" element={<MissingAttributes/>}/>
                        <Route path="*" element={<NotFound/>}/>
                    </Routes>}
            </div>
            {<Footer/>}
        </div>
    );
}
