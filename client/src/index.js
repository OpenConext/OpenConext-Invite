import React from 'react';
import './index.scss';
import {App} from './pages/App';
import '@surfnet/sds/styles/sds.css';
import ReactDOM from 'react-dom/client';
import {BrowserRouter,Routes, Route} from "react-router-dom";

const root = ReactDOM.createRoot(document.getElementById("app"));
root.render(
        <BrowserRouter>
            <Routes>
                <Route path="/*" element={<App/>}/>
            </Routes>
        </BrowserRouter>
);
