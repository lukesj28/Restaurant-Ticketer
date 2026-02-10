import React, { useState, useEffect, useRef, useCallback } from 'react';
import { api } from '../api/api';
import Button from '../components/common/Button';
import { useToast } from '../context/ToastContext';
import './Settings.css';

const BLOCK_TYPES = [
    { value: 'RESTAURANT_NAME', label: 'Restaurant Name' },
    { value: 'ADDRESS', label: 'Address' },
    { value: 'PHONE', label: 'Phone' },
    { value: 'TIMESTAMP', label: 'Timestamp' },
    { value: 'TABLE_NUMBER', label: 'Table Number' },
    { value: 'CUSTOM_TEXT', label: 'Custom Text' },
    { value: 'DIVIDER', label: 'Divider' },
    { value: 'SPACE', label: 'Space' },
    { value: 'ITEMS', label: 'Items' },
    { value: 'TOTALS', label: 'Totals' },
];

const Settings = () => {
    const toast = useToast();
    // Fetched Server State
    const [settings, setSettings] = useState({ tax: 0, hours: {} });
    // Local Draft State
    const [draftTaxRate, setDraftTaxRate] = useState('');
    const [draftHours, setDraftHours] = useState({});

    // Restaurant details drafts
    const [draftRestaurant, setDraftRestaurant] = useState({ name: '', address: '', phone: '' });

    // Receipt layout drafts
    const [draftBlocks, setDraftBlocks] = useState([]);

    // Printer settings drafts
    const [draftPrinter, setDraftPrinter] = useState({
        portName: '', baudRate: 38400, paperWidthMm: 80, dpi: 203, enabled: false
    });
    const [availablePorts, setAvailablePorts] = useState([]);

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
        fetchAvailablePorts();
    }, []);

    const fetchAvailablePorts = async () => {
        try {
            const ports = await api.get('/settings/printer/ports');
            setAvailablePorts(ports || []);
        } catch (e) { console.error(e); }
    };

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

            // Restaurant details
            if (data.restaurant) {
                setDraftRestaurant({
                    name: data.restaurant.name || '',
                    address: data.restaurant.address || '',
                    phone: data.restaurant.phone || ''
                });
            }

            // Receipt blocks
            if (data.receipt && data.receipt.blocks) {
                setDraftBlocks(data.receipt.blocks.map((b, i) => ({ ...b, id: i })));
            }

            // Printer settings
            if (data.printer) {
                setDraftPrinter({
                    portName: data.printer.portName || '',
                    baudRate: data.printer.baudRate || 38400,
                    paperWidthMm: data.printer.paperWidthMm || 80,
                    dpi: data.printer.dpi || 203,
                    enabled: data.printer.enabled || false
                });
            }

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

            // 3. Save Restaurant Details
            promises.push(api.put('/settings/restaurant', draftRestaurant));

            // 4. Save Printer Settings
            promises.push(api.put('/settings/printer', draftPrinter));

            // 5. Save Receipt Layout
            const blocksToSave = draftBlocks.map(b => ({ type: b.type, content: b.content || null }));
            promises.push(api.put('/settings/receipt', { blocks: blocksToSave }));

            await Promise.all(promises);

            toast.success('Settings saved successfully.');

            // Optimistically update local state so UI reflects changes immediately
            const newSettings = {
                tax: basisPoints,
                hours: { ...draftHours },
                restaurant: { ...draftRestaurant },
                receipt: { blocks: blocksToSave }
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

    // --- Receipt Block Handlers ---
    const nextBlockId = useRef(100);

    const addBlock = (type) => {
        const newBlock = {
            id: nextBlockId.current++,
            type,
            content: type === 'CUSTOM_TEXT' ? '' : null
        };
        setDraftBlocks(prev => [...prev, newBlock]);
    };

    const removeBlock = (id) => {
        setDraftBlocks(prev => prev.filter(b => b.id !== id));
    };

    const updateBlockContent = (id, content) => {
        setDraftBlocks(prev => prev.map(b => b.id === id ? { ...b, content } : b));
    };

    // Drag state
    const dragItem = useRef(null);
    const dragOverItem = useRef(null);

    const handleDragStart = (index) => {
        dragItem.current = index;
    };

    const handleDragEnter = (index) => {
        dragOverItem.current = index;
    };

    const handleDragEnd = () => {
        if (dragItem.current === null || dragOverItem.current === null) return;
        const items = [...draftBlocks];
        const draggedItem = items[dragItem.current];
        items.splice(dragItem.current, 1);
        items.splice(dragOverItem.current, 0, draggedItem);
        dragItem.current = null;
        dragOverItem.current = null;
        setDraftBlocks(items);
    };

    const getBlockLabel = (type) => {
        const found = BLOCK_TYPES.find(bt => bt.value === type);
        return found ? found.label : type;
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
                <h2>Restaurant Details</h2>
                <div className="restaurant-fields">
                    <div className="form-group row">
                        <label>Restaurant Name</label>
                        <input
                            type="text"
                            value={draftRestaurant.name}
                            onChange={e => setDraftRestaurant(prev => ({ ...prev, name: e.target.value }))}
                            placeholder="Your Restaurant Name"
                        />
                    </div>
                    <div className="form-group row">
                        <label>Address</label>
                        <input
                            type="text"
                            value={draftRestaurant.address}
                            onChange={e => setDraftRestaurant(prev => ({ ...prev, address: e.target.value }))}
                            placeholder="123 Main St, City, State"
                        />
                    </div>
                    <div className="form-group row">
                        <label>Phone</label>
                        <input
                            type="text"
                            value={draftRestaurant.phone}
                            onChange={e => setDraftRestaurant(prev => ({ ...prev, phone: e.target.value }))}
                            placeholder="(555) 123-4567"
                        />
                    </div>
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

            <div className="settings-section">
                <h2>Printer</h2>
                <div className="printer-fields">
                    <div className="form-group row">
                        <label>Printer Port</label>
                        <select
                            value={draftPrinter.portName}
                            onChange={e => setDraftPrinter(prev => ({ ...prev, portName: e.target.value }))}
                        >
                            <option value="">-- Select Port --</option>
                            {availablePorts.map(port => (
                                <option key={port} value={port}>{port}</option>
                            ))}
                        </select>
                    </div>
                    <div className="printer-grid">
                        <div className="form-group row">
                            <label>Baud Rate</label>
                            <input
                                type="number"
                                value={draftPrinter.baudRate}
                                onChange={e => setDraftPrinter(prev => ({ ...prev, baudRate: parseInt(e.target.value) || 0 }))}
                            />
                        </div>
                        <div className="form-group row">
                            <label>Paper Width (mm)</label>
                            <input
                                type="number"
                                value={draftPrinter.paperWidthMm}
                                onChange={e => setDraftPrinter(prev => ({ ...prev, paperWidthMm: parseInt(e.target.value) || 0 }))}
                            />
                        </div>
                        <div className="form-group row">
                            <label>DPI</label>
                            <input
                                type="number"
                                value={draftPrinter.dpi}
                                onChange={e => setDraftPrinter(prev => ({ ...prev, dpi: parseInt(e.target.value) || 0 }))}
                            />
                        </div>
                    </div>
                    <div className="form-group row">
                        <label className="switch-label">
                            Enable Printer
                            <label className="switch-container">
                                <input
                                    type="checkbox"
                                    checked={draftPrinter.enabled}
                                    onChange={e => setDraftPrinter(prev => ({ ...prev, enabled: e.target.checked }))}
                                />
                                <span className="slider"></span>
                            </label>
                        </label>
                    </div>
                </div>
            </div>

            <div className="settings-section">
                <h2>Receipt Layout</h2>
                <div className="receipt-editor-container">
                    <div className="receipt-blocks-editor">
                        <p className="receipt-editor-hint">Drag blocks to reorder. Add or remove blocks to customize your receipt.</p>
                        <div className="receipt-blocks-list">
                            {draftBlocks.map((block, index) => (
                                <div
                                    key={block.id}
                                    className="receipt-block-card"
                                    draggable
                                    onDragStart={() => handleDragStart(index)}
                                    onDragEnter={() => handleDragEnter(index)}
                                    onDragEnd={handleDragEnd}
                                    onDragOver={(e) => e.preventDefault()}
                                >
                                    <div className="block-drag-handle">⠿</div>
                                    <div className="block-info">
                                        <span className="block-type-label">{getBlockLabel(block.type)}</span>
                                        {block.type === 'CUSTOM_TEXT' && (
                                            <input
                                                className="block-custom-input"
                                                type="text"
                                                value={block.content || ''}
                                                onChange={(e) => updateBlockContent(block.id, e.target.value)}
                                                placeholder="Enter custom text..."
                                            />
                                        )}
                                    </div>
                                    <button
                                        className="block-remove-btn"
                                        onClick={() => removeBlock(block.id)}
                                        title="Remove block"
                                    >
                                        ✕
                                    </button>
                                </div>
                            ))}
                        </div>
                        <div className="receipt-add-block">
                            <AddBlockDropdown onAdd={addBlock} />
                        </div>
                    </div>
                    <div className="receipt-preview-panel">
                        <h3>Preview</h3>
                        <ReceiptPreview blocks={draftBlocks} restaurant={draftRestaurant} />
                    </div>
                </div>
            </div>

            {/* Floating Save Button Mobile Friendly */}
            <div className="save-actions">
                <Button variant="primary" style={{ width: '100%', padding: '1rem' }} onClick={handleSaveAll}>Save All Settings</Button>
            </div>
        </div>
    );
};

// --- Add Block Dropdown ---
const AddBlockDropdown = ({ onAdd }) => {
    const [open, setOpen] = useState(false);
    const ref = useRef(null);

    useEffect(() => {
        const handleClick = (e) => {
            if (ref.current && !ref.current.contains(e.target)) setOpen(false);
        };
        document.addEventListener('mousedown', handleClick);
        return () => document.removeEventListener('mousedown', handleClick);
    }, []);

    return (
        <div className="add-block-dropdown" ref={ref}>
            <Button variant="secondary" onClick={() => setOpen(!open)}>+ Add Block</Button>
            {open && (
                <div className="add-block-menu">
                    {BLOCK_TYPES.map(bt => (
                        <button
                            key={bt.value}
                            className="add-block-option"
                            onClick={() => { onAdd(bt.value); setOpen(false); }}
                        >
                            {bt.label}
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
};

// --- Receipt Preview ---
const PREVIEW_WIDTH = 48;

const centerText = (text) => {
    if (text.length >= PREVIEW_WIDTH) return text.substring(0, PREVIEW_WIDTH);
    const totalPad = PREVIEW_WIDTH - text.length;
    const leftPad = Math.floor(totalPad / 2);
    const rightPad = totalPad - leftPad;
    return ' '.repeat(leftPad) + text + ' '.repeat(rightPad);
};

const formatLine = (left, right) => {
    let spaces = PREVIEW_WIDTH - left.length - right.length;
    if (spaces < 1) spaces = 1;
    return left + ' '.repeat(spaces) + right;
};

const ReceiptPreview = ({ blocks, restaurant }) => {
    const lines = [];

    const sampleItems = [
        { name: 'Burger', mainPrice: 1299, side: 'Fries', sidePrice: 399 },
        { name: 'Soda', mainPrice: 250, side: null, sidePrice: 0 },
    ];

    const formatPrice = (cents) => `$${(cents / 100).toFixed(2)}`;

    blocks.forEach((block) => {
        switch (block.type) {
            case 'RESTAURANT_NAME':
                if (restaurant.name) lines.push(centerText(restaurant.name));
                break;
            case 'ADDRESS':
                if (restaurant.address) {
                    const commaIdx = restaurant.address.indexOf(',');
                    if (commaIdx > 0) {
                        lines.push(centerText(restaurant.address.substring(0, commaIdx).trim()));
                        lines.push(centerText(restaurant.address.substring(commaIdx + 1).trim()));
                    } else {
                        lines.push(centerText(restaurant.address));
                    }
                }
                break;
            case 'PHONE':
                if (restaurant.phone) lines.push(centerText(restaurant.phone));
                break;
            case 'TIMESTAMP':
                lines.push(centerText(new Date().toLocaleString('en-US', {
                    year: 'numeric', month: '2-digit', day: '2-digit',
                    hour: '2-digit', minute: '2-digit', hour12: false
                })));
                break;
            case 'TABLE_NUMBER':
                lines.push(formatLine('Table: 5', ''));
                break;
            case 'CUSTOM_TEXT':
                if (block.content) lines.push(centerText(block.content));
                break;
            case 'DIVIDER':
                lines.push('-'.repeat(PREVIEW_WIDTH));
                break;
            case 'SPACE':
                lines.push(' '.repeat(PREVIEW_WIDTH));
                break;
            case 'ITEMS':
                sampleItems.forEach((item, idx) => {
                    lines.push(formatLine(item.name, formatPrice(item.mainPrice)));
                    if (item.side) {
                        lines.push(formatLine('  + ' + item.side, formatPrice(item.sidePrice)));
                    }
                    // Small gap between items (not after the last)
                    if (idx < sampleItems.length - 1) {
                        lines.push('__HALF_GAP__');
                    }
                });
                break;
            case 'TOTALS':
                lines.push(formatLine('Subtotal', formatPrice(1948)));
                lines.push(formatLine('Tax', formatPrice(253)));
                lines.push(formatLine('TOTAL', formatPrice(2201)));
                break;
            default:
                break;
        }
    });

    return (
        <div className="receipt-preview">
            <pre className="receipt-paper">
                {lines.map((line, i) =>
                    line === '__HALF_GAP__'
                        ? <span key={i} className="receipt-half-gap">{'\n'}</span>
                        : <span key={i}>{line}{'\n'}</span>
                )}
            </pre>
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
