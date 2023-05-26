import './App.scss';
import {Navigate, Route, Routes, useNavigate} from "react-router-dom";
import {useEffect, useState} from "react";
import {Loader} from "@surfnet/sds";
import {useAppStore} from "../stores/AppStore";
import {configuration, me} from "../api";
import {Login} from "./Login";
import {Home} from "./Home";
import {Flash} from "../components/Flash";
import {Header} from "../components/Header";
import {Footer} from "../components/Footer";

export const App = () => {

    const [loading, setLoading] = useState(true);
    const [authenticated, setAuthenticated] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        configuration().then(res => {
            useAppStore.setState(() => ({config: res}));
            if (!res.authenticated) {
                localStorage.setItem("location", window.location.pathname + window.location.search);
                setLoading(false);
                setAuthenticated(false);
            } else {
                me().then(res => {
                    useAppStore.setState(() => ({user: res}));
                    setLoading(false);
                    setAuthenticated(true);
                    const location = localStorage.getItem("location");
                    const newLocation = location.startsWith("/login") ? "/home" : location
                    navigate(newLocation);
                });
            }
        })
    }, [navigate]);

    useEffect(() => {
        if (!authenticated) {
            navigate("/login");
        }
    }, [navigate, authenticated])

    if (loading) {
        return <Loader/>
    }

    return (
        <div className="access">
            <div className="container">
                <Flash/>
                <Header user={useAppStore.getState().user}
                        config={useAppStore.getState().config}/>
                {authenticated &&
                <Routes>
                    <Route path="/" element={<Navigate replace to="home"/>}/>
                    <Route path="home" element={<Home/>}/>
                </Routes>}

                {/*  <Route path="home">*/}
                {/*    <Route path=":tab" element={<Home user={user}/>}/>*/}
                {/*    <Route path="" element={<Home user={user}/>}/>*/}
                {/*  </Route>*/}
                {/*  <Route path="invitations" element={<Invitation user={user}/>}/>*/}
                {/*  <Route path="institution/:institutionId" element={<InstitutionForm user={user}/>}/>*/}
                {!authenticated &&
                <Routes>
                    <Route path="login" element={<Login/>}/>
                </Routes>}
            </div>
            {<Footer/>}
        </div>
    );
}
