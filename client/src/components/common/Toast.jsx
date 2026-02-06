import React, { useEffect, useState } from 'react';
import './Toast.css';

const Toast = ({ id, message, type = 'info', duration = 3000, onClose }) => {
    const [isExiting, setIsExiting] = useState(false);

    useEffect(() => {
        const timer = setTimeout(() => {
            handleClose();
        }, duration);

        return () => clearTimeout(timer);
    }, [duration]);

    const handleClose = (e) => {
        if (e) e.stopPropagation();
        setIsExiting(true);
        // Wait for animation to finish before actually removing
        setTimeout(() => {
            onClose(id);
        }, 300);
    };

    return (
        <div className={`toast ${type} ${isExiting ? 'toast-exiting' : ''}`} onClick={handleClose}>
            <div className="toast-message">{message}</div>
            <button className="toast-close" type="button" onClick={handleClose}>
                &times;
            </button>
        </div>
    );
};

export default Toast;
