import React from 'react';
import {render} from 'react-dom';
import './index.scss';
import App from './pages/App';
import {BrowserRouter, Route, Routes} from "react-router-dom";


render(
    <React.StrictMode>
        <BrowserRouter>
            <Routes>
                <Route path="/*" element={<App/>}/>
            </Routes>
        </BrowserRouter>
    </React.StrictMode>,
    document.getElementById("app")
);
