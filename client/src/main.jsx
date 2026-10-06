import React from 'react';
import {App} from './pages/App';
import ReactDOM from 'react-dom/client';
import {BrowserRouter, Route, Routes} from "react-router";
import "react-tooltip/dist/react-tooltip.css";
import './styles/sds-layered.css';
import {Toaster, TooltipProvider} from "@surfnet/curve-react";
import "@surfnet/curve-react/styles.css";
import "./tailwind.css";
import './index.scss';

const root = ReactDOM.createRoot(document.getElementById("app"));
root.render(
    <TooltipProvider>
        <BrowserRouter>
            <Routes>
                <Route path="/*" element={<App/>}/>
            </Routes>
        </BrowserRouter>
        <Toaster/>
    </TooltipProvider>
);
