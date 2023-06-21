import React from 'react';
import {App} from './pages/App';
import ReactDOM from 'react-dom/client';
import {BrowserRouter,Routes, Route} from "react-router-dom";
//Always keep these two last
import './index.scss';
import '@surfnet/sds/styles/sds.css';

const root = ReactDOM.createRoot(document.getElementById("app"));
root.render(
        <BrowserRouter>
            <Routes>
                <Route path="/*" element={<App/>}/>
            </Routes>
        </BrowserRouter>
);
