import React, { useState, useEffect } from 'react';
import { NavLink } from 'react-router-dom';
import { api } from '../../api/api';
import './Navbar.css';

const Navbar = () => {
    const [brandName, setBrandName] = useState('Ticketer');

    useEffect(() => {
        const fetchName = async () => {
            try {
                const details = await api.get('/settings/restaurant');
                if (details && details.name && details.name.trim()) {
                    setBrandName(details.name);
                }
            } catch (e) {
                // Fall back to default
            }
        };
        fetchName();
    }, []);

    return (
        <nav className="navbar">
            <div className="navbar-brand">{brandName}</div>
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
