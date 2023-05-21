import './App.scss';
import {Navigate, Route, Routes, useNavigate} from "react-router-dom";
import {useEffect, useState} from "react";
import {config} from "../api";

const App = () => {

  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    config().then(res => {

    })
    const options = cookieStorage.getItem("options");
    const accessToken = cookieStorage.getItem("accessToken");
    const urlSearchParams = new URLSearchParams(window.location.search);
    const inAuthRedirect = urlSearchParams.has("code");
    if (!accessToken && !options) {
      const previousPath = cookieStorage.getItem("path");
      if (!previousPath) {
        cookieStorage.setItem("path", `${window.location.pathname}${window.location.search}`);
      }
      oauth().then(r => {
        cookieStorage.setItem("options", JSON.stringify(r));
        window.location.href = r.authorizationUrl;
      });
    } else if (inAuthRedirect && options) {
      const optionsDict = JSON.parse(options);
      optionsDict.code = urlSearchParams.get("code");
      getTokensFrontChannel(optionsDict)
          .then(r => {
            cookieStorage.setItem("accessToken", r.access_token);
            cookieStorage.setItem("refreshToken", r.refresh_token);
            cookieStorage.setItem("clientId", optionsDict.clientId);
            cookieStorage.setItem("tokenUrl", optionsDict.tokenUrl);
            cookieStorage.removeItem("options");
            const path = cookieStorage.getItem("path") || "/";
            me()
                .then(user => {
                  cookieStorage.setItem("user", JSON.stringify(user));
                  const membershipsWithoutAup = institutionMembershipsWithNoAup(user);
                  const navigationPath = isEmpty(membershipsWithoutAup) ? path : "/aup";
                  navigate(navigationPath, {replace: true});
                  setLoading(false);
                })
                .catch(() => {
                  //Unknown user who has received an invitation
                  cookieStorage.removeItem("user");
                  navigate(path, {replace: true});
                  setLoading(false);
                });
          })
    } else {
      setLoading(false);
    }
  }, [navigate]);

  if (loading) {
    return null; // render null when app is not ready yet
  }

  const userJson = cookieStorage.getItem("user");
  const user = userJson ? JSON.parse(userJson) : null;
  return (
      <div className="invites">
        <div className="container">
          <Flash/>
          <Header user={user}/>
          {user && <Routes>
            <Route path="/" element={<Navigate replace to="home"/>}/>
            <Route path="home">
              <Route path=":tab" element={<Home user={user}/>}/>
              <Route path="" element={<Home user={user}/>}/>
            </Route>
            <Route path="profile" element={<Profile user={user}/>}/>
            <Route path="aup" element={<Aup user={user}/>}/>
            <Route path="invitations" element={<Invitation user={user}/>}/>
            <Route path="institution/:institutionId" element={<InstitutionForm user={user}/>}/>
            <Route path="institution-detail/:institutionId">
              <Route path=":tab" element={<InstitutionDetail user={user}/>}/>
              <Route path="" element={<InstitutionDetail user={user}/>}/>
            </Route>
            <Route path="institution-guest" element={<InstitutionGuest user={user}/>}/>
            <Route path="application/:institutionId/:applicationId" element={<ApplicationForm user={user}/>}/>
            <Route path="application-detail/:institutionId/:applicationId">
              <Route path=":tab" element={<ApplicationDetail user={user}/>}/>
              <Route path="" element={<ApplicationDetail user={user}/>}/>
            </Route>
            <Route path="role/:institutionId/:applicationId/:roleId" element={<RoleForm user={user}/>}/>
            <Route path="new-invitation/:institutionId" element={<NewInvitation user={user}/>}/>
            <Route path="user-detail/:userId/:institutionId" element={<User user={user}/>}/>
            <Route path="invitation-detail/:invitationId" element={<InvitationDetail/>}/>
            <Route path="scim-failure-detail/:institutionId/:failureId" element={<SCIMFailureDetail user={user}/>}/>
            <Route path="refresh-route/:path" element={<RefreshRoute/>}/>
            <Route path="test-landing" element={<Landing/>}/>
            <Route path="*" element={<NotFound/>}/>
          </Routes>}
          {!user && <Routes>
            <Route path="invitations" element={<Invitation/>}/>
            <Route path="*" element={<Landing/>}/>
          </Routes>}
        </div>
        <Footer/>
      </div>
  );
}

export default App;


function App() {
  return (
    <div className="App">
      <header className="App-header">
        <h1>Invite UI</h1>
        <p>
          Edit <code>src/App.js</code> and save to reload.
        </p>
        <a
          className="App-link"
          href="https://reactjs.org"
          target="_blank"
          rel="noopener noreferrer"
        >
          Learn React
        </a>
      </header>
    </div>
  );
}

export default App;
