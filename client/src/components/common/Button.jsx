import React from 'react';
import './Button.css';

const Button = ({ children, onClick, variant = 'primary', size = 'medium', className = '', disabled = false, type = 'button' }) => {
    return (
        <button
            type={type}
            className={`btn btn-${variant} btn-${size} ${className}`}
            onClick={onClick}
            disabled={disabled}
        >
            {children}
        </button>
    );
};

export default Button;
