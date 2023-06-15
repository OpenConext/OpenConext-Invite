import './App.scss';
import {Navigate, Route, Routes, useNavigate} from "react-router-dom";
import {useEffect, useState} from "react";
import {Loader} from "@surfnet/sds";
import {useAppStore} from "../stores/AppStore";
import {configuration, csrf, me} from "../api";
import {Login} from "./Login";
import {Home} from "./Home";
import {Flash} from "../components/Flash";
import {Header} from "../components/Header";
import {Footer} from "../components/Footer";
import {BreadCrumb} from "../components/BreadCrumb";
import {Invitation} from "./Invitation";
import {login} from "../utils/Login";
import NotFound from "./NotFound";
import {Impersonating} from "../components/Impersonating";
import RefreshRoute from "./RefreshRoute";
import {InviteOnly} from "./InviteOnly";
import {Profile} from "./Profile";


export const App = () => {

    const [loading, setLoading] = useState(true);
    const [authenticated, setAuthenticated] = useState(false);
    const navigate = useNavigate();
    const {impersonator} = useAppStore(state => state);

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
                        }
                        setLoading(false);
                        setAuthenticated(false);
                        const pathname = localStorage.getItem("location") || window.location.pathname;
                        if (pathname === "/" || pathname.startsWith("/login")) {
                            navigate(pathname);
                        } else if (pathname.startsWith("/invitation/accept")) {
                            navigate(pathname);
                        } else {
                            //Bookmarked URL's trigger a direct login and skip the landing page
                            login(res);
                        }
                    } else {
                        me()
                            .then(res => {
                                useAppStore.setState(() => ({user: res}));
                                setLoading(false);
                                setAuthenticated(true);
                                const location = localStorage.getItem("location") || window.location.pathname + window.location.search;
                                ;
                                const newLocation = location.startsWith("/login") ? "/home" : location;
                                localStorage.removeItem("location");
                                navigate(newLocation);
                            });
                    }
                }).catch(() => {
                setLoading(false);
                navigate("/deadend");
            })
        })
    }, [impersonator]); // eslint-disable-line react-hooks/exhaustive-deps

    if (loading) {
        return <Loader/>
    }
    return (
        <div className="access">
            <div className="container">
                <Flash/>
                <Header/>
                {impersonator && <Impersonating/>}

                {authenticated && <BreadCrumb/>}
                {authenticated &&
                    <Routes>
                        <Route path="/" element={<Navigate replace to="home"/>}/>
                        <Route path="home/:tab?" element={<Home/>}/>
                        <Route path="profile/:id?" element={<Profile/>}/>
                        <Route path="invitation/accept"
                               element={<Invitation authenticated={true}/>}/>
                        <Route path="login" element={<Login/>}/>
                        <Route path="refresh-route/:path" element={<RefreshRoute/>}/>
                        <Route path="*" element={<NotFound/>}/>
                    </Routes>}
                {/*  <Route path="invitations" element={<Invitation user={user}/>}/>*/}
                {/*  <Route path="institution/:institutionId" element={<InstitutionForm user={user}/>}/>*/}
                {!authenticated &&
                    <Routes>
                        <Route path="/" element={<Navigate replace to="login"/>}/>
                        <Route path="invitation/accept"
                               element={<Invitation authenticated={false}/>}/>
                        <Route path="login" element={<Login/>}/>
                        <Route path="deadend" element={<InviteOnly/>}/>
                        <Route path="/*" element={<NotFound/>}/>
                    </Routes>}
            </div>
            {<Footer/>}
        </div>
    );
}
