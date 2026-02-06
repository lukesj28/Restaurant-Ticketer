import React, { useState, useEffect, useRef } from 'react';
import { api } from '../api/api';
import Button from '../components/common/Button';
import { useToast } from '../context/ToastContext';
import './Settings.css';

const Settings = () => {
    const toast = useToast();
    // Fetched Server State
    const [settings, setSettings] = useState({ tax: 0, hours: {} });
    // Local Draft State
    const [draftTaxRate, setDraftTaxRate] = useState('');
    const [draftHours, setDraftHours] = useState({});

    const [loading, setLoading] = useState(true);
    const [systemStatus, setSystemStatus] = useState(false);

    // Day Mapping
    const dayMap = {
        'MONDAY': 'mon',
        'TUESDAY': 'tue',
        'WEDNESDAY': 'wed',
        'THURSDAY': 'thu',
        'FRIDAY': 'fri',
        'SATURDAY': 'sat',
        'SUNDAY': 'sun'
    };

    const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

    useEffect(() => {
        fetchSettings();
        fetchSystemStatus();
    }, []);

    const fetchSettings = async () => {
        try {
            const data = await api.get('/settings/refresh');
            setSettings(data);

            // Initialize drafts
            setDraftTaxRate((data.tax / 100).toFixed(2));

            // Map array of day keys to draft object
            const hoursDraft = {};
            days.forEach(day => {
                const key = dayMap[day];
                hoursDraft[key] = data.hours[key] || 'Closed';
            });
            setDraftHours(hoursDraft);

        } catch (e) {
            console.error(e);
        } finally {
            setLoading(false);
        }
    };

    const fetchSystemStatus = async () => {
        try {
            const isOpen = await api.get('/status');
            setSystemStatus(isOpen);
        } catch (e) { console.error(e); }
    };

    const handleSystemAction = async (action) => {
        try {
            await api.post(action === 'open' ? '/open' : '/shutdown');
            fetchSystemStatus();
            toast.success(`System ${action === 'open' ? 'opened' : 'shutdown'} successfully.`);
        } catch (e) {
            toast.error('Action failed: ' + e.message);
        }
    };

    // Timer ref for cleanup
    const timerRef = useRef(null);

    useEffect(() => {
        return () => {
            if (timerRef.current) clearTimeout(timerRef.current);
        };
    }, []);

    // Global Save Handler
    const handleSaveAll = async () => {
        try {
            const promises = [];

            // 1. Save Tax
            const basisPoints = Math.round(parseFloat(draftTaxRate) * 100);
            promises.push(api.put('/settings/tax', { tax: basisPoints }));

            // 2. Save Hours (only updated ones, or all - reusing API logic)
            Object.keys(draftHours).forEach(key => {
                promises.push(api.put(`/settings/hours/${key}`, { hours: draftHours[key] }));
            });

            await Promise.all(promises);

            toast.success('Settings saved successfully.');

            // Optimistically update local state so UI reflects changes immediately
            const newSettings = {
                tax: basisPoints,
                hours: { ...draftHours }
            };
            setSettings(newSettings);

            // Refresh system status - wait a bit longer to be safe
            if (timerRef.current) clearTimeout(timerRef.current);
            timerRef.current = setTimeout(() => {
                fetchSystemStatus();
            }, 1000);

        } catch (e) {
            toast.error('Failed to save settings: ' + e.message);
        }
    };

    const updateDraftHours = (dayKey, newValue) => {
        setDraftHours(prev => ({ ...prev, [dayKey]: newValue }));
    };

    if (loading) return <div>Loading...</div>;

    return (
        <div className="settings-page">
            <div className="settings-header">
                <h1>Settings</h1>
                <Button variant="primary" className="save-all-btn" onClick={handleSaveAll}>
                    Save Changes
                </Button>
            </div>

            <div className="settings-section">
                <h2>System Control</h2>
                <div className="system-controls">
                    <div className="status-display">
                        Restaurant is currently:
                        <span className={`status-badge ${systemStatus ? 'open' : 'closed'}`}>
                            {systemStatus ? 'OPEN' : 'CLOSED'}
                        </span>
                    </div>
                    <label className="switch-container">
                        <input
                            type="checkbox"
                            checked={systemStatus}
                            onChange={() => handleSystemAction(systemStatus ? 'shutdown' : 'open')}
                        />
                        <span className="slider"></span>
                    </label>
                </div>
            </div>

            <div className="settings-section">
                <h2>Financials</h2>
                <div className="form-group row">
                    <label>Current Tax Rate</label>
                    <div className="stat-display">
                        {(settings.tax / 100).toFixed(2)}%
                    </div>
                    <label className="edit-label">Update Rate (%)</label>
                    <div className="input-with-action">
                        <input
                            type="number"
                            step="0.01"
                            value={draftTaxRate}
                            onChange={(e) => setDraftTaxRate(e.target.value)}
                        />
                    </div>
                </div>
            </div>

            <div className="settings-section">
                <h2>Opening Hours</h2>
                <div className="hours-grid">
                    <div className="hours-header-row">
                        <span>Day</span>
                        <span>Current Schedule</span>
                        <span>Update Schedule</span>
                    </div>
                    {days.map(day => (
                        <HoursRow
                            key={day}
                            dayDisplayName={day}
                            currentHours={settings.hours[dayMap[day]] || 'Closed'}
                            draftValue={draftHours[dayMap[day]] || 'Closed'}
                            onChange={(val) => updateDraftHours(dayMap[day], val)}
                        />
                    ))}
                </div>
            </div>

            {/* Floating Save Button Mobile Friendly */}
            <div className="save-actions">
                <Button variant="primary" style={{ width: '100%', padding: '1rem' }} onClick={handleSaveAll}>Save All Settings</Button>
            </div>
        </div>
    );
};

// Controlled Subcomponent
const HoursRow = ({ dayDisplayName, currentHours, draftValue, onChange }) => {
    // Parse helper
    const parse = (str) => {
        if (!str || str.toLowerCase() === 'closed') return { open: '09:00', close: '17:00', isClosed: true };
        const parts = str.split(' - ');
        if (parts.length === 2) return { open: parts[0], close: parts[1], isClosed: false };
        return { open: '09:00', close: '17:00', isClosed: true };
    };

    const state = parse(draftValue);

    // Initial Default Logic happens when toggling closed
    const toggleClosed = (e) => {
        if (e.target.checked) {
            // Became closed
            onChange('closed');
        } else {
            // Became open -> Default to 09:00 - 17:00
            onChange('09:00 - 17:00');
        }
    };

    const handleTimeChange = (field, val) => {
        // Allow numbers and colon only
        const cleaned = val.replace(/[^0-9]/g, '');
        let formatted = cleaned;
        if (cleaned.length > 2) {
            formatted = cleaned.slice(0, 2) + ':' + cleaned.slice(2, 4);
        }

        let newOpen = field === 'open' ? formatted : state.open;
        let newClose = field === 'close' ? formatted : state.close;

        onChange(`${newOpen} - ${newClose}`);
    };

    return (
        <div className="hours-row">
            <span className="day-label">{dayDisplayName}</span>
            <span className="hours-display">{currentHours}</span>
            <div className="hours-edit-group">
                <div className="time-inputs">
                    <input
                        className="time-input"
                        placeholder="HH:MM"
                        value={state.open}
                        disabled={state.isClosed}
                        maxLength={5}
                        onChange={(e) => handleTimeChange('open', e.target.value)}
                    />
                    <span>-</span>
                    <input
                        className="time-input"
                        placeholder="HH:MM"
                        value={state.close}
                        disabled={state.isClosed}
                        maxLength={5}
                        onChange={(e) => handleTimeChange('close', e.target.value)}
                    />
                </div>
                <div className="hours-actions">
                    <label className="closed-check">
                        <input
                            type="checkbox"
                            checked={state.isClosed}
                            onChange={toggleClosed}
                        />
                        Closed
                    </label>
                </div>
            </div>
        </div>
    );
};

export default Settings;
