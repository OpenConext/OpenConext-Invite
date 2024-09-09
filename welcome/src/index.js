import React from 'react';
import {App} from './pages/App';
import ReactDOM from 'react-dom/client';
import {BrowserRouter,Routes, Route} from "react-router-dom";
//Always keep these two last
import './index.scss';
import '@surfnet/sds/styles/sds.css';
//Do not change the order of @surfnet.sds style imports
import '@surfnet/sds/cjs/index.css';
import "react-tooltip/dist/react-tooltip.css";


const root = ReactDOM.createRoot(document.getElementById("app"));
root.render(
        <BrowserRouter>
            <Routes>
                <Route path="/*" element={<App/>}/>
            </Routes>
        </BrowserRouter>
);
