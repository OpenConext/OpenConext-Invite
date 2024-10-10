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
import {Role} from "./Role";
import {RoleForm} from "./RoleForm";
import {InvitationForm} from "./InvitationForm";
import {isEmpty} from "../utils/Utils";
import {MissingAttributes} from "./MissingAttributes";
import {System} from "./System";
import {Inviter} from "./Inviter";
import {InvitationOverview} from "./InvitationOverview";
import {Application} from "./Application";


export const App = () => {

    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();
    const {impersonator, authenticated, reload} = useAppStore(state => state);

    useEffect(() => {
        setLoading(true);
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
                        const pathname = localStorage.getItem("location") || window.location.pathname;
                        const isInvitationAcceptFlow = window.location.pathname.startsWith("/invitation/accept");
                        if (res.name && !pathname.startsWith("/invitation/accept") && !isInvitationAcceptFlow) {
                            setLoading(false);
                            navigate("/deadend");
                        } else if (pathname === "/" || pathname.startsWith("/login") || pathname.startsWith("/invitation/accept") || isInvitationAcceptFlow) {
                            setLoading(false);
                            navigate(isInvitationAcceptFlow ? (window.location.pathname + window.location.search) : pathname);
                        } else {
                            //Bookmarked URL's trigger a direct login and skip the landing page
                            login(res);
                        }
                    } else {
                        me()
                            .then(res => {
                                useAppStore.setState(() => ({user: res, authenticated: true}));
                                setLoading(false);
                                const location = localStorage.getItem("location") || window.location.pathname + window.location.search;
                                const newLocation = location.startsWith("/login") ? "/home" : location;
                                localStorage.removeItem("location");
                                navigate(newLocation);
                            });
                    }
                })
                .catch(() => {
                    setLoading(false);
                    navigate("/deadend");
                })
        })
    }, [reload, impersonator]); // eslint-disable-line react-hooks/exhaustive-deps

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
                        <Route path="role/:id" element={<RoleForm/>}/>
                        <Route path="invitation/:id" element={<InvitationForm/>}/>
                        <Route path="invitations/:tab?" element={<InvitationOverview/>}/>
                        <Route path="inviter" element={<Inviter/>}/>
                        <Route path="roles/:id/:tab?" element={<Role/>}/>
                        <Route path="applications/:manageId" element={<Application/>}/>
                        <Route path="invitation/accept"
                               element={<Invitation authenticated={true}/>}/>
                        <Route path="login" element={<Login/>}/>
                        <Route path="refresh-route/:path" element={<RefreshRoute/>}/>
                        <Route path="system/:tab?" element={<System/>}/>
                        <Route path="*" element={<NotFound/>}/>
                    </Routes>}
                {/*  <Route path="invitations" element={<Invitation user={user}/>}/>*/}
                {/*  <Route path="institution/:institutionId" element={<InstitutionForm user={user}/>}/>*/}
                {!authenticated &&
                    <Routes>
                        <Route path="/" element={<Navigate replace to="login"/>}/>
                        <Route path="/home" element={<Navigate replace to="login"/>}/>
                        <Route path="invitation/accept"
                               element={<Invitation authenticated={false}/>}/>
                        <Route path="login" element={<Login/>}/>
                        <Route path="deadend" element={<InviteOnly/>}/>
                        <Route path="missingAttributes" element={<MissingAttributes/>}/>
                        <Route path="/*" element={<NotFound/>}/>
                    </Routes>}
            </div>
            {<Footer/>}
        </div>
    );
}
