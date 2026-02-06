import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../api/api';
import Button from '../components/common/Button';
import Modal from '../components/common/Modal'; // Will use for item selection
import { useToast } from '../context/ToastContext';
import './TicketDetail.css';

const TicketDetail = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const { toast } = useToast();
    const [ticket, setTicket] = useState(null);
    const [loading, setLoading] = useState(true);
    const [menu, setMenu] = useState({}); // Categories -> Items
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const [selectedOrderIndex, setSelectedOrderIndex] = useState(0); // Default to last or specific
    const [selectedItemForSides, setSelectedItemForSides] = useState(null);

    useEffect(() => {
        fetchTicket();
        fetchMenu();
    }, [id]);

    const fetchTicket = async () => {
        try {
            const data = await api.get(`/tickets/${id}`);
            setTicket(data);
            // Default to last order if exists, else -1
            if (data.orders.length > 0) {
                // But wait, user might want to add to specific order. 
                // For simplified UI, maybe always add to newest order or create new if explicitly requested.
                // Let's track active order index.
            }
        } catch (error) {
            console.error("Failed to fetch ticket", error);
            // navigate('/tickets'); 
        } finally {
            setLoading(false);
        }
    };

    const fetchMenu = async () => {
        try {
            const data = await api.get('/menu/categories');
            setMenu(data);
        } catch (e) {
            console.error("Failed to fetch menu");
        }
    };

    const handleAddOrder = async () => {
        try {
            await api.post(`/tickets/${id}/orders`);
            fetchTicket();
        } catch (e) {
            toast.error(e.message);
        }
    };

    const handleAddItem = async (itemName, sideName) => {
        // Add to specific order. If no orders, create one first?
        // Backend requirement: must add to existing order index.
        let orderIdx = selectedOrderIndex;
        if (!ticket.orders || ticket.orders.length === 0) {
            // Create order first
            await api.post(`/tickets/${id}/orders`);
            // Fetch to get the new order count, but efficient way is to assume index 0
            orderIdx = 0;
            // But simpler to just refresh and add.
            const t = await api.get(`/tickets/${id}`);
            setTicket(t);
        } else {
            // If selectedOrderIndex is out of bounds (e.g. from previous state), reset
            orderIdx = ticket.orders.length - 1; // Default to last order for now
        }

        try {
            await api.post(`/tickets/${id}/orders/${orderIdx}/items`, {
                name: itemName,
                selectedSide: sideName
            });
            setIsMenuOpen(false);
            fetchTicket(); // refresh
        } catch (e) {
            toast.error(e.message);
        }
    };

    const handleStatusChange = async (newStatus) => {
        try {
            if (newStatus === 'completed') {
                await api.put(`/tickets/${id}/completed`);
            } else if (newStatus === 'closed') {
                await api.put(`/tickets/${id}/closed`);
            } else if (newStatus === 'active') {
                await api.put(`/tickets/${id}/active`);
            }
            navigate('/tickets');
        } catch (e) {
            toast.error(e.message);
        }
    };

    if (loading) return <div className="loading">Loading...</div>;
    if (!ticket) return <div className="error">Ticket not found</div>;

    // Helper to render Menu Selection Modal
    const renderMenuModal = () => (
        <Modal
            isOpen={isMenuOpen}
            onClose={() => { setIsMenuOpen(false); setSelectedItemForSides(null); }}
            title="Add Item"
        >
            <div className="menu-selection">
                {selectedItemForSides ? (
                    <div className="side-selection-block">
                        <div className="side-selection-header">
                            <Button size="small" variant="secondary" onClick={() => setSelectedItemForSides(null)}>Back</Button>
                            <h4>Select Side for {selectedItemForSides.name}</h4>
                        </div>
                        <div className="menu-items-grid">
                            {/* Always offer 'None' option if that's the intent, or just list available sides */}
                            {/* The requirement said "sides" map in item has "none" if applicable, or we inject it?
                                 Looking at menu.json, "none" is explicit side with price 0.
                                 So we just iterate sides. */}
                            {Object.entries(selectedItemForSides.sides || {}).map(([sideName, sideData]) => (
                                <button
                                    key={sideName}
                                    className={`menu-item-btn ${!sideData.available ? 'unavailable' : ''}`}
                                    disabled={!sideData.available}
                                    onClick={() => handleAddItem(selectedItemForSides.name, sideName)}
                                >
                                    <div className="item-name">{sideName}</div>
                                    <div className="item-price">${(sideData.price / 100).toFixed(2)}</div>
                                </button>
                            ))}
                        </div>
                    </div>
                ) : (
                    Object.entries(menu).map(([category, items]) => (
                        <div key={category} className="menu-category-block">
                            <h4>{category}</h4>
                            <div className="menu-items-grid">
                                {items.map(item => (
                                    <button
                                        key={item.name}
                                        className={`menu-item-btn ${!item.available ? 'unavailable' : ''}`}
                                        disabled={!item.available}
                                        onClick={() => {
                                            if (item.sides && Object.keys(item.sides).length > 0) {
                                                setSelectedItemForSides(item);
                                            } else {
                                                handleAddItem(item.name, null);
                                            }
                                        }}
                                    >
                                        <div className="item-name">{item.name}</div>
                                        <div className="item-price">${(item.price / 100).toFixed(2)}</div>
                                    </button>
                                ))}
                            </div>
                        </div>
                    )))}
            </div>
        </Modal>
    );

    return (
        <div className="ticket-detail-page">
            <div className="detail-header">
                <Button variant="secondary" onClick={() => navigate('/tickets')}>&larr; Back</Button>
                <h2>Table {ticket.tableNumber} <span className="id-sub">#{ticket.id}</span></h2>
                <div className="header-actions">
                    {(!ticket.status || ticket.status === 'ACTIVE') && (
                        <Button variant="success" onClick={() => handleStatusChange('completed')}>Complete</Button>
                    )}
                    <div className={`status-badge status-${ticket.status ? ticket.status.toLowerCase() : 'active'}`}>
                        {ticket.status || 'ACTIVE'}
                    </div>
                </div>
            </div>

            <div className="orders-list">
                {(ticket.orders || []).map((order, idx) => (
                    <div key={idx} className="order-block">
                        <div className="order-header">
                            <span>Order #{idx + 1}</span>
                            <div className="order-totals">
                                <span className="order-subtotal">Sub: ${(order.subtotal / 100).toFixed(2)}</span>
                                <span className="order-total">Tot: ${(order.total / 100).toFixed(2)}</span>
                            </div>
                        </div>
                        <div className="order-items">
                            {order.items.map((item, iIdx) => (
                                <div key={iIdx} className="order-item-row">
                                    <div className="item-name-col">
                                        <span>{item.name}</span>
                                        {item.selectedSide && item.selectedSide !== 'none' && (
                                            <span className="item-side"> + {item.selectedSide}</span>
                                        )}
                                    </div>
                                    <span>${((item.mainPrice + item.sidePrice) / 100).toFixed(2)}</span>
                                </div>
                            ))}
                        </div>
                        <div className="order-actions">
                            <Button size="small" variant="secondary" onClick={() => {
                                // Remove item logic?
                            }}>Edit</Button>
                        </div>
                    </div>
                ))}
                {(ticket.orders || []).length === 0 && <div className="empty-orders">No orders yet.</div>}
            </div>

            <div className="detail-footer">
                <div className="totals-row">
                    <div className="totals-line">
                        <span className="label">Subtotal</span>
                        <span className="amount">${(ticket.subtotal / 100).toFixed(2)}</span>
                    </div>
                    <div className="totals-line total-final">
                        <span className="label">Total</span>
                        <span className="amount">${(ticket.total / 100).toFixed(2)}</span>
                    </div>
                </div>
                <div className="action-row">
                    <Button variant="secondary" onClick={handleAddOrder} disabled={ticket.status === 'CLOSED'}>+ Add Order</Button>
                    <Button variant="primary" onClick={() => setIsMenuOpen(true)} disabled={ticket.status === 'CLOSED'}>+ Add Item</Button>
                </div>
                <div className="status-actions">
                    {ticket.status === 'COMPLETED' && (
                        <Button variant="warning" onClick={() => handleStatusChange('active')}>Reopen Ticket</Button>
                    )}
                    {ticket.status !== 'CLOSED' && (
                        <Button variant="danger" onClick={() => handleStatusChange('closed')}>Close Ticket</Button>
                    )}
                </div>
            </div>

            {renderMenuModal()}
        </div>
    );
};

export default TicketDetail;
