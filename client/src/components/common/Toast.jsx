import React, { useEffect, useState } from 'react';
import './Toast.css';

const Toast = ({ id, message, type = 'info', duration = 3000, onClose }) => {
    const [isExiting, setIsExiting] = useState(false);

    const timerRef = React.useRef(null);

    useEffect(() => {
        const timer = setTimeout(() => {
            handleClose();
        }, duration);

        return () => clearTimeout(timer);
    }, [duration]);

    useEffect(() => {
        return () => {
            if (timerRef.current) {
                clearTimeout(timerRef.current);
            }
            // Ensure removal from context state if unmounted externally (e.g. navigation)
            onClose(id);
        };
    }, []);

    const handleClose = (e) => {
        if (e) e.stopPropagation();
        setIsExiting(true);
        // Wait for animation to finish before actually removing
        timerRef.current = setTimeout(() => {
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
