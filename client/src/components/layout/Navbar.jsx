import React from 'react';
import { NavLink } from 'react-router-dom';
import './Navbar.css';

const Navbar = () => {
    return (
        <nav className="navbar">
            <div className="navbar-brand">Ticketer</div>
            <div className="navbar-links">
                <NavLink to="/tickets" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
                    Tickets
                </NavLink>
                <NavLink to="/menu" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
                    Menu
                </NavLink>
                <NavLink to="/analysis" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
                    Analysis
                </NavLink>
                <NavLink to="/settings" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
                    Settings
                </NavLink>
            </div>
        </nav>
    );
};

export default Navbar;
