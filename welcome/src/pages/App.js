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
import {BreadCrumb} from "../components/BreadCrumb";
import {Invitation} from "./Invitation";
import {login} from "../utils/Login";
import NotFound from "./NotFound";
import {InviteOnly} from "./InviteOnly";
import {Profile} from "./Profile";
import {Proceed} from "./Proceed";
import {isEmpty} from "../utils/Utils";
import {MissingAttributes} from "./MissingAttributes";


export const App = () => {

    const [loading, setLoading] = useState(true);
    const {authenticated} = useAppStore(state => state);
    const navigate = useNavigate();

    useEffect(() => {
        csrf().then(token => {
            useAppStore.setState(() => ({csrfToken: token.token}));
            configuration()
                .then(res => {
                    useAppStore.setState(() => ({config: res}));
                    if (!res.authenticated) {
                        if (!res.name) {
                            const direction = window.location.pathname + window.location.search;
                            localStorage.setItem("location", direction);
                        } else if (!isEmpty(res.missingAttributes)) {
                            setLoading(false);
                            navigate("/missingAttributes");
                            return;
                        }
                        setLoading(false);
                        const pathname = localStorage.getItem("location") || window.location.pathname;
                        if (res.name && !pathname.startsWith("/invitation/accept")) {
                            navigate("/deadend");
                        } else if (pathname === "/" || pathname.startsWith("/login") || pathname.startsWith("/invitation/accept")) {
                            navigate(pathname);
                        } else {
                            //Bookmarked URL's trigger a direct login and skip the landing page
                            login(res);
                        }
                    } else {
                        me()
                            .then(res => {
                                useAppStore.setState(() => ({user: res, authenticated: true}));
                                const location = localStorage.getItem("location") || window.location.pathname + window.location.search;
                                const newLocation = location.startsWith("/login") ? "/home" : location;
                                localStorage.removeItem("location");
                                setLoading(false);
                                navigate(newLocation);
                            });
                    }
                }).catch(() => {
                setLoading(false);
                navigate("/deadend");
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
                {authenticated && <BreadCrumb/>}
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
                        <Route path="invitation/accept"
                               element={<Invitation authenticated={false}/>}/>
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
