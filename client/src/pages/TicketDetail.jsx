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
    const toast = useToast();
    const [ticket, setTicket] = useState(null);
    const [loading, setLoading] = useState(true);
    const [menu, setMenu] = useState({}); // Categories -> Items
    const [categoryOrder, setCategoryOrder] = useState([]); // Explicit order
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
            const [categoriesData, orderData] = await Promise.all([
                api.get('/menu/categories'),
                api.get('/menu/category-order')
            ]);
            setMenu(categoriesData);
            setCategoryOrder(orderData || []);
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
        // Add to the specifically selected order index.
        let orderIdx = selectedOrderIndex;
        // If for some reason index is invalid (should not happen with UI), fallback to last
        if (typeof orderIdx !== 'number' || orderIdx < 0) {
            if (ticket.orders && ticket.orders.length > 0) {
                orderIdx = ticket.orders.length - 1;
            } else {
                // No orders exist?
                return;
            }
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
            fetchTicket();
        } catch (e) {
            toast.error(e.message);
        }
    };

    const handleSendToKitchen = async () => {
        try {
            await api.post(`/tickets/${id}/kitchen`);
            toast.success('Ticket sent to kitchen!');
        } catch (e) {
            toast.error(e.message);
        }
    };

    const handlePrintReceipt = async () => {
        try {
            await api.post(`/print/ticket/${id}`);
            toast.success('Receipt printed!');
        } catch (e) {
            toast.error('Print failed: ' + e.message);
        }
    };

    const handlePrintOrder = async (orderIndex) => {
        try {
            await api.post(`/print/ticket/${id}/order/${orderIndex}`);
            toast.success(`Order #${orderIndex + 1} receipt printed!`);
        } catch (e) {
            toast.error('Print failed: ' + e.message);
        }
    };

    // Confirmation modal state
    const [confirmationModal, setConfirmationModal] = useState({
        isOpen: false,
        title: '',
        message: ''
    });
    const confirmAction = React.useRef(null);
    const [isProcessing, setIsProcessing] = React.useState(false);

    const handleDeleteTicket = () => {
        confirmAction.current = async () => {
            try {
                await api.delete(`/tickets/${id}`);
                toast.success('Ticket deleted');
                navigate('/tickets');
                return true; // Signal that we navigated, don't fetch ticket
            } catch (e) {
                toast.error('Failed to delete ticket: ' + e.message);
                return false;
            }
        };
        setConfirmationModal({
            isOpen: true,
            title: 'Delete Ticket',
            message: `Are you sure you want to delete Ticket #${ticket.id}? This cannot be undone.`
        });
    };

    const handleDeleteOrder = (orderIndex) => {
        confirmAction.current = async () => {
            try {
                await api.delete(`/tickets/${id}/orders/${orderIndex}`);
                toast.success(`Order #${orderIndex + 1} deleted`);
            } catch (e) {
                toast.error('Failed to delete order: ' + e.message);
            }
        };
        setConfirmationModal({
            isOpen: true,
            title: 'Delete Order',
            message: `Are you sure you want to delete Order #${orderIndex + 1}?`
        });
    };

    const handleDeleteItem = async (orderIndex, item) => {
        try {
            await api.delete(`/tickets/${id}/orders/${orderIndex}/items`, {
                name: item.name,
                selectedSide: item.selectedSide
            });
            fetchTicket();
        } catch (e) {
            toast.error('Failed to delete item: ' + e.message);
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
                            {Object.entries(selectedItemForSides.sides || {})
                                .sort(([sideA], [sideB]) => {
                                    const order = selectedItemForSides.sideOrder || [];
                                    const idxA = order.indexOf(sideA);
                                    const idxB = order.indexOf(sideB);
                                    if (idxA === -1 && idxB === -1) return 0; // Maintain original order if both missing
                                    if (idxA === -1) return 1;
                                    if (idxB === -1) return -1;
                                    return idxA - idxB;
                                })
                                .map(([sideName, sideData]) => (
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
                    Object.entries(menu)
                        .sort(([catA], [catB]) => {
                            const idxA = categoryOrder.indexOf(catA);
                            const idxB = categoryOrder.indexOf(catB);
                            if (idxA === -1 && idxB === -1) return catA.localeCompare(catB);
                            if (idxA === -1) return 1;
                            if (idxB === -1) return -1;
                            return idxA - idxB;
                        })
                        .map(([category, items]) => (
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
                        <>
                            <Button variant="warning" onClick={handleSendToKitchen}>Send to Kitchen</Button>
                            <Button variant="success" onClick={() => handleStatusChange('completed')}>Complete</Button>
                        </>
                    )}
                    <div className={`status-badge status-${ticket.status ? ticket.status.toLowerCase() : 'active'}`}>
                        {ticket.status || 'ACTIVE'}
                    </div>
                </div>
            </div>

            <div className="orders-list">
                {(ticket.orders || []).map((order, idx) => (
                    <div key={idx} className={`order-block ${ticket.status !== 'CLOSED' && selectedOrderIndex === idx ? 'selected-order' : ''}`}>
                        <div className="order-header">
                            <span>Order #{idx + 1}</span>
                            <div className="order-totals">
                                <span className="order-subtotal">Sub: ${(order.subtotal / 100).toFixed(2)}</span>
                                <span className="order-total">Tot: ${(order.total / 100).toFixed(2)}</span>
                                {ticket.status !== 'CLOSED' && (
                                    <button
                                        className="delete-order-btn"
                                        onClick={() => handleDeleteOrder(idx)}
                                        title="Delete Order"
                                    >🗑️</button>
                                )}
                                {ticket.status === 'CLOSED' && (
                                    <button
                                        className="print-order-btn"
                                        onClick={() => handlePrintOrder(idx)}
                                        title="Print Order"
                                    >🖨️</button>
                                )}
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
                                    <div className="item-price-col">
                                        <span>${((item.mainPrice + item.sidePrice) / 100).toFixed(2)}</span>
                                        {ticket.status !== 'CLOSED' && (
                                            <button
                                                className="delete-item-btn"
                                                onClick={() => handleDeleteItem(idx, item)}
                                                title="Remove Item"
                                            >✕</button>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                        {ticket.status !== 'CLOSED' && (
                            <div className="order-actions">
                                <Button size="small" variant="primary" onClick={() => {
                                    setSelectedOrderIndex(idx);
                                    setIsMenuOpen(true);
                                }}>+ Add Item</Button>
                            </div>
                        )}
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
                    {ticket.status !== 'CLOSED' && (
                        <Button variant="secondary" onClick={handleAddOrder}>+ Add Order</Button>
                    )}
                </div>
                <div className="status-actions">
                    {ticket.status === 'COMPLETED' && (
                        <Button variant="warning" onClick={() => handleStatusChange('active')}>Reopen Ticket</Button>
                    )}
                    {ticket.status !== 'CLOSED' && (
                        <Button variant="danger" onClick={() => handleStatusChange('closed')}>Close Ticket</Button>
                    )}
                    {ticket.status !== 'CLOSED' && (
                        <Button variant="danger" onClick={handleDeleteTicket}>Delete Ticket</Button>
                    )}
                    {ticket.status === 'CLOSED' && (
                        <Button variant="primary" onClick={handlePrintReceipt}>🖨️ Print Receipt</Button>
                    )}
                </div>
            </div>

            {renderMenuModal()}

            {/* Confirmation Modal */}
            <Modal
                isOpen={confirmationModal.isOpen}
                onClose={() => !isProcessing && setConfirmationModal({ isOpen: false, title: '', message: '' })}
                title={confirmationModal.title}
            >
                <p>{confirmationModal.message}</p>
                <div className="modal-actions" style={{ marginTop: '1rem', display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                    <Button variant="secondary" onClick={() => setConfirmationModal({ isOpen: false, title: '', message: '' })} disabled={isProcessing}>Cancel</Button>
                    <Button variant="danger" disabled={isProcessing} onClick={async () => {
                        if (confirmAction.current && !isProcessing) {
                            setIsProcessing(true);
                            const action = confirmAction.current;
                            let navigated = false;
                            try {
                                navigated = await action();
                            } catch (err) {
                                console.error('Confirmation action failed:', err);
                            } finally {
                                setIsProcessing(false);
                                // Always close modal after action completes
                                setConfirmationModal({ isOpen: false, title: '', message: '' });
                            }
                            // Only refresh ticket data if we didn't navigate away
                            if (!navigated) {
                                try {
                                    fetchTicket();
                                } catch (err) {
                                    console.error('Failed to refresh ticket:', err);
                                }
                            }
                        }
                    }}>{isProcessing ? 'Processing...' : 'Confirm'}</Button>
                </div>
            </Modal>
        </div >
    );
};

export default TicketDetail;
