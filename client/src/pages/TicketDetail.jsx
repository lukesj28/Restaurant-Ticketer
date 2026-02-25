import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../api/api';
import Button from '../components/common/Button';
import Modal from '../components/common/Modal'; // Will use for item selection
import { useToast } from '../context/ToastContext';
import {
    DndContext,
    DragOverlay,
    PointerSensor,
    KeyboardSensor,
    useSensor,
    useSensors,
    closestCenter,
} from '@dnd-kit/core';
import { useDraggable, useDroppable } from '@dnd-kit/core';
import './TicketDetail.css';

const DraggableItem = ({ orderIndex, itemIndex, children }) => {
    const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
        id: `item-${orderIndex}-${itemIndex}`,
        data: { type: 'ITEM', orderIndex, itemIndex },
    });

    return (
        <div
            ref={setNodeRef}
            style={{ opacity: isDragging ? 0.4 : 1 }}
            className="order-item-row draggable-item"
        >
            <div className="item-drag-handle" {...listeners} {...attributes}>
                ⠿
            </div>
            {children}
        </div>
    );
};

const DraggableOrderHandle = ({ orderIndex, children }) => {
    const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
        id: `order-drag-${orderIndex}`,
        data: { type: 'ORDER', orderIndex },
    });

    return (
        <div
            ref={setNodeRef}
            className="order-header"
            style={{ opacity: isDragging ? 0.4 : 1 }}
        >
            {children(listeners, attributes)}
        </div>
    );
};

const DroppableOrder = ({ orderIndex, isOver, children }) => {
    const { setNodeRef, isOver: droppableIsOver } = useDroppable({
        id: `order-${orderIndex}`,
        data: { type: 'ORDER', orderIndex },
    });

    const highlighted = isOver || droppableIsOver;

    return (
        <div
            ref={setNodeRef}
            className={`order-drop-zone ${highlighted ? 'drop-target-active' : ''}`}
        >
            {children}
        </div>
    );
};

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
    const [selectedItemForExtras, setSelectedItemForExtras] = useState(null);
    const [pendingSideName, setPendingSideName] = useState(null);
    const [menuSearch, setMenuSearch] = useState('');
    const [commentDrafts, setCommentDrafts] = useState({});
    const commentTimers = React.useRef({});
    const [activeDrag, setActiveDrag] = useState(null);
    const [selectedOrdersForPrint, setSelectedOrdersForPrint] = useState(new Set());
    const [editingPrice, setEditingPrice] = useState(null);
    const [editingPriceValue, setEditingPriceValue] = useState('');
    const [itemView, setItemView] = useState(false);
    const hasInitializedView = React.useRef(false);

    const sensors = useSensors(
        useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
        useSensor(KeyboardSensor)
    );

    useEffect(() => {
        hasInitializedView.current = false;
        fetchTicket();
        fetchMenu();
    }, [id]);

    const fetchTicket = async () => {
        try {
            const data = await api.get(`/tickets/${id}`);
            setTicket(data);
            if (!hasInitializedView.current) {
                setItemView(data.status === 'COMPLETED');
                hasInitializedView.current = true;
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

    const handleAddItem = async (category, itemName, sideName, extraName) => {
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
                category: category,
                name: itemName,
                selectedSide: sideName,
                selectedExtra: extraName
            });
            setIsMenuOpen(false);
            setSelectedItemForExtras(null);
            setPendingSideName(null);
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
        setItemView(true);
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

    const handlePrintSelectedOrders = async () => {
        if (selectedOrdersForPrint.size === 0) return;
        try {
            await api.post(`/print/ticket/${id}/orders`, {
                orderIndices: Array.from(selectedOrdersForPrint).sort((a, b) => a - b)
            });
            toast.success(`Printed ${selectedOrdersForPrint.size} selected order(s)!`);
            setSelectedOrdersForPrint(new Set());
        } catch (e) {
            toast.error('Print failed: ' + e.message);
        }
    };

    const toggleOrderForPrint = (idx) => {
        setSelectedOrdersForPrint(prev => {
            const next = new Set(prev);
            if (next.has(idx)) {
                next.delete(idx);
            } else {
                next.add(idx);
            }
            return next;
        });
    };

    const handleUpdateItemPrice = async (orderIndex, itemIndex, newPriceCents) => {
        try {
            await api.put(`/tickets/${id}/orders/${orderIndex}/items/${itemIndex}/price`, { newPrice: newPriceCents });
            setEditingPrice(null);
            fetchTicket();
        } catch (e) {
            toast.error('Failed to update price: ' + e.message);
        }
    };

    const startEditingPrice = (orderIndex, itemIndex, currentPriceCents) => {
        setEditingPrice({ orderIndex, itemIndex, originalCents: currentPriceCents });
        setEditingPriceValue((currentPriceCents / 100).toFixed(2));
    };

    const commitPriceEdit = (orderIndex, itemIndex) => {
        const parsed = parseFloat(editingPriceValue);
        if (isNaN(parsed) || parsed < 0) {
            setEditingPrice(null);
            return;
        }
        const cents = Math.round(parsed * 100);
        if (cents === editingPrice?.originalCents) {
            setEditingPrice(null);
            return;
        }
        handleUpdateItemPrice(orderIndex, itemIndex, cents);
    };

    const getCommentValue = (key, serverValue) => {
        return key in commentDrafts ? commentDrafts[key] : (serverValue || '');
    };

    const handleCommentChange = (key, value, saveFn) => {
        setCommentDrafts(prev => ({ ...prev, [key]: value }));
        if (commentTimers.current[key]) {
            clearTimeout(commentTimers.current[key]);
        }
        commentTimers.current[key] = setTimeout(async () => {
            try {
                await saveFn(value);
                setCommentDrafts(prev => {
                    const next = { ...prev };
                    delete next[key];
                    return next;
                });
                fetchTicket();
            } catch (e) {
                toast.error('Failed to save comment: ' + e.message);
            }
        }, 600);
    };

    const saveTicketComment = (comment) =>
        api.put(`/tickets/${id}/comment`, { comment });

    const saveOrderComment = (orderIndex, comment) =>
        api.put(`/tickets/${id}/orders/${orderIndex}/comment`, { comment });

    const saveItemComment = (orderIndex, itemIndex, comment) =>
        api.put(`/tickets/${id}/orders/${orderIndex}/items/${itemIndex}/comment`, { comment });

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
                selectedSide: item.selectedSide,
                selectedExtra: item.selectedExtra
            });
            fetchTicket();
        } catch (e) {
            toast.error('Failed to delete item: ' + e.message);
        }
    };

    // DnD handlers
    const handleDragStart = (event) => {
        const { active } = event;
        setActiveDrag(active.data.current);
    };

    const handleDragEnd = async (event) => {
        const { active, over } = event;
        setActiveDrag(null);

        if (!over) return;

        const activeData = active.data.current;
        // Determine which order was dropped on
        let targetOrderIndex = null;

        if (over.data.current && over.data.current.type === 'ORDER') {
            targetOrderIndex = over.data.current.orderIndex;
        } else if (over.id && typeof over.id === 'string' && over.id.startsWith('order-')) {
            targetOrderIndex = parseInt(over.id.replace('order-', ''), 10);
        }

        if (targetOrderIndex === null || targetOrderIndex === undefined) return;

        if (activeData.type === 'ITEM') {
            const fromOrderIndex = activeData.orderIndex;
            const itemIndex = activeData.itemIndex;

            if (fromOrderIndex === targetOrderIndex) return;

            // Optimistic update
            const prevTicket = ticket;
            try {
                const newOrders = ticket.orders.map(o => ({
                    ...o,
                    items: [...o.items]
                }));
                const [movedItem] = newOrders[fromOrderIndex].items.splice(itemIndex, 1);
                newOrders[targetOrderIndex].items.push(movedItem);
                // Remove empty source order
                const filtered = newOrders.filter(o => o.items.length > 0);
                setTicket({ ...ticket, orders: filtered });

                await api.put(`/tickets/${id}/orders/${fromOrderIndex}/items/${itemIndex}/move`, {
                    targetOrderIndex
                });
                fetchTicket();
            } catch (e) {
                setTicket(prevTicket);
                toast.error('Failed to move item: ' + e.message);
            }
        } else if (activeData.type === 'ORDER') {
            const fromOrderIndex = activeData.orderIndex;

            if (fromOrderIndex === targetOrderIndex) return;

            // Optimistic update
            const prevTicket = ticket;
            try {
                const newOrders = ticket.orders.map(o => ({
                    ...o,
                    items: [...o.items]
                }));
                const sourceItems = newOrders[fromOrderIndex].items;
                newOrders[targetOrderIndex].items.push(...sourceItems);
                newOrders.splice(fromOrderIndex, 1);
                setTicket({ ...ticket, orders: newOrders });

                await api.put(`/tickets/${id}/orders/${fromOrderIndex}/merge`, {
                    targetOrderIndex
                });
                fetchTicket();
            } catch (e) {
                setTicket(prevTicket);
                toast.error('Failed to merge orders: ' + e.message);
            }
        }
    };

    const handleDragCancel = () => {
        setActiveDrag(null);
    };

    if (loading) return <div className="loading">Loading...</div>;
    if (!ticket) return <div className="error">Ticket not found</div>;

    // Helper to render Menu Selection Modal
    const hasExtras = (item) => item.extras && Object.keys(item.extras).length > 0;

    const proceedAfterSide = (item, sideName) => {
        if (hasExtras(item)) {
            setSelectedItemForExtras(item);
            setPendingSideName(sideName);
            setSelectedItemForSides(null);
        } else {
            handleAddItem(item.category, item.name, sideName, null);
        }
    };

    const isDndEnabled = (ticket.orders || []).length > 1;

    const renderDragOverlay = () => {
        if (!activeDrag || !ticket) return null;

        if (activeDrag.type === 'ITEM') {
            const order = ticket.orders[activeDrag.orderIndex];
            if (!order) return null;
            const item = order.items[activeDrag.itemIndex];
            if (!item) return null;
            return (
                <div className="drag-overlay-item">
                    <span>{item.name}</span>
                    {item.selectedSide && item.selectedSide !== 'none' && (
                        <span className="item-side"> + {item.selectedSide}</span>
                    )}
                </div>
            );
        }

        if (activeDrag.type === 'ORDER') {
            const order = ticket.orders[activeDrag.orderIndex];
            if (!order) return null;
            return (
                <div className="drag-overlay-order">
                    Order #{activeDrag.orderIndex + 1} — {order.items.length} item{order.items.length !== 1 ? 's' : ''}
                </div>
            );
        }

        return null;
    };

    const isActive = !ticket.status || ticket.status === 'ACTIVE';

    const renderOrderBlock = (order, idx) => (
        <div key={idx} className={`order-block ${isActive && selectedOrderIndex === idx ? 'selected-order' : ''}`}>
            {isDndEnabled ? (
                <DraggableOrderHandle orderIndex={idx}>
                    {(listeners, attributes) => (
                        <>
                            <div className="order-header-left">
                                {ticket.status === 'CLOSED' && (
                                    <input
                                        type="checkbox"
                                        className="order-print-checkbox"
                                        checked={selectedOrdersForPrint.has(idx)}
                                        onChange={() => toggleOrderForPrint(idx)}
                                    />
                                )}
                                <span className="order-label-handle" {...listeners} {...attributes}>Order #{idx + 1}</span>
                            </div>
                            <div className="order-totals">
                                <span className="order-subtotal">Sub: ${(order.subtotal / 100).toFixed(2)}</span>
                                <span className="order-total">Tot: ${(order.total / 100).toFixed(2)}</span>
                                {isActive && (
                                    <button
                                        className="delete-order-btn"
                                        onClick={() => handleDeleteOrder(idx)}
                                        title="Delete Order"
                                    >&#128465;&#65039;</button>
                                )}
                                {ticket.status === 'CLOSED' && (
                                    <button
                                        className="print-order-btn"
                                        onClick={() => handlePrintOrder(idx)}
                                        title="Print Order"
                                    >&#128424;&#65039;</button>
                                )}
                            </div>
                        </>
                    )}
                </DraggableOrderHandle>
            ) : (
                <div className="order-header">
                    <div className="order-header-left">
                        {ticket.status === 'CLOSED' && (
                            <input
                                type="checkbox"
                                className="order-print-checkbox"
                                checked={selectedOrdersForPrint.has(idx)}
                                onChange={() => toggleOrderForPrint(idx)}
                            />
                        )}
                        <span>Order #{idx + 1}</span>
                    </div>
                    <div className="order-totals">
                        <span className="order-subtotal">Sub: ${(order.subtotal / 100).toFixed(2)}</span>
                        <span className="order-total">Tot: ${(order.total / 100).toFixed(2)}</span>
                        {isActive && (
                            <button
                                className="delete-order-btn"
                                onClick={() => handleDeleteOrder(idx)}
                                title="Delete Order"
                            >&#128465;&#65039;</button>
                        )}
                        {ticket.status === 'CLOSED' && (
                            <button
                                className="print-order-btn"
                                onClick={() => handlePrintOrder(idx)}
                                title="Print Order"
                            >&#128424;&#65039;</button>
                        )}
                    </div>
                </div>
            )}
            {isActive && (
                <div className="order-comment">
                    <input
                        type="text"
                        className="comment-input order-comment-input"
                        placeholder="Order comment..."
                        value={getCommentValue(`order-${idx}`, order.comment)}
                        onChange={(e) => handleCommentChange(`order-${idx}`, e.target.value, (v) => saveOrderComment(idx, v))}
                    />
                </div>
            )}
            {!isActive && order.comment && (
                <div className="order-comment">
                    <span className="comment-display">{order.comment}</span>
                </div>
            )}
            <div className="order-items">
                {order.items.map((item, iIdx) => {
                    const itemContent = (
                        <>
                            <div className="item-name-col">
                                <span>{item.name}</span>
                                {item.selectedSide && item.selectedSide !== 'none' && (
                                    <span className="item-side"> + {item.selectedSide}</span>
                                )}
                                {item.selectedExtra && item.selectedExtra !== 'none' && (
                                    <div className="item-extra">+ {item.selectedExtra} (${(item.extraPrice / 100).toFixed(2)})</div>
                                )}
                                {isActive && (
                                    <input
                                        type="text"
                                        className="comment-input item-comment-input"
                                        placeholder="Note..."
                                        value={getCommentValue(`item-${idx}-${iIdx}`, item.comment)}
                                        onChange={(e) => handleCommentChange(`item-${idx}-${iIdx}`, e.target.value, (v) => saveItemComment(idx, iIdx, v))}
                                    />
                                )}
                                {!isActive && item.comment && (
                                    <span className="comment-display item-comment-display">{item.comment}</span>
                                )}
                            </div>
                            <div className="item-price-col">
                                {ticket.status === 'CLOSED' && editingPrice && editingPrice.orderIndex === idx && editingPrice.itemIndex === iIdx ? (
                                    <input
                                        type="text"
                                        inputMode="decimal"
                                        className="item-price-input"
                                        value={editingPriceValue}
                                        onChange={(e) => setEditingPriceValue(e.target.value)}
                                        onBlur={() => commitPriceEdit(idx, iIdx)}
                                        onKeyDown={(e) => {
                                            if (e.key === 'Enter') commitPriceEdit(idx, iIdx);
                                            if (e.key === 'Escape') setEditingPrice(null);
                                        }}
                                        autoFocus
                                    />
                                ) : (
                                    <span
                                        className={ticket.status === 'CLOSED' ? 'item-price-editable' : ''}
                                        onClick={ticket.status === 'CLOSED' ? () => startEditingPrice(idx, iIdx, item.mainPrice + item.sidePrice + (item.extraPrice || 0)) : undefined}
                                    >
                                        ${((item.mainPrice + item.sidePrice + (item.extraPrice || 0)) / 100).toFixed(2)}
                                    </span>
                                )}
                                {isActive && (
                                    <button
                                        className="delete-item-btn"
                                        onClick={() => handleDeleteItem(idx, item)}
                                        title="Remove Item"
                                    >&#10005;</button>
                                )}
                            </div>
                        </>
                    );

                    if (isDndEnabled) {
                        return (
                            <DraggableItem key={iIdx} orderIndex={idx} itemIndex={iIdx}>
                                {itemContent}
                            </DraggableItem>
                        );
                    }

                    return (
                        <div key={iIdx} className="order-item-row">
                            {itemContent}
                        </div>
                    );
                })}
            </div>
            {isActive && (
                <div className="order-actions">
                    <Button variant="primary" className="add-item-btn" onClick={() => openMenuModal(idx)}>+ Add Item</Button>
                </div>
            )}
        </div>
    );

    const sortedCategories = Object.entries(menu)
        .sort(([catA], [catB]) => {
            const idxA = categoryOrder.indexOf(catA);
            const idxB = categoryOrder.indexOf(catB);
            if (idxA === -1 && idxB === -1) return catA.localeCompare(catB);
            if (idxA === -1) return 1;
            if (idxB === -1) return -1;
            return idxA - idxB;
        });

    const closeMenuModal = () => {
        setIsMenuOpen(false);
        setSelectedItemForSides(null);
        setSelectedItemForExtras(null);
        setPendingSideName(null);
        setMenuSearch('');
    };

    const openMenuModal = (orderIdx) => {
        setSelectedOrderIndex(orderIdx);
        setSelectedItemForSides(null);
        setSelectedItemForExtras(null);
        setPendingSideName(null);
        setMenuSearch('');
        setIsMenuOpen(true);
    };

    const handleItemClick = (category, item) => {
        const hasSides = item.sides && Object.keys(item.sides).length > 0;
        if (hasSides) {
            setSelectedItemForSides({ ...item, category });
        } else if (hasExtras(item)) {
            setSelectedItemForExtras({ ...item, category });
            setPendingSideName(null);
        } else {
            handleAddItem(category, item.name, null, null);
        }
    };

    const renderItemGrid = (items, category) => (
        <div className="menu-items-grid">
            {items.map(item => (
                <button
                    key={item.name}
                    className={`menu-item-btn ${!item.available ? 'unavailable' : ''}`}
                    disabled={!item.available}
                    onClick={() => handleItemClick(category, item)}
                >
                    <div className="item-name">{item.name}</div>
                    <div className="item-price">${(item.price / 100).toFixed(2)}</div>
                </button>
            ))}
        </div>
    );

    const isSelectingSidesOrExtras = !!(selectedItemForSides || selectedItemForExtras);
    const searchTerm = menuSearch.trim().toLowerCase();
    const isSearching = searchTerm.length > 0 && !isSelectingSidesOrExtras;

    const renderMenuModalContent = () => {
        // Side selection view — full panel, no search/sidebar
        if (selectedItemForExtras) {
            return (
                <div className="menu-panel-content">
                    <div className="menu-panel-heading">
                        <Button size="small" variant="secondary" onClick={() => {
                            setSelectedItemForExtras(null);
                            setPendingSideName(null);
                            if (selectedItemForExtras.sides && Object.keys(selectedItemForExtras.sides).length > 0) {
                                setSelectedItemForSides(selectedItemForExtras);
                            }
                        }}>Back</Button>
                        <h4>Select Extra for {selectedItemForExtras.name}</h4>
                    </div>
                    <div className="menu-items-grid">
                        {Object.entries(selectedItemForExtras.extras || {})
                            .sort(([extraA], [extraB]) => {
                                const order = selectedItemForExtras.extraOrder || [];
                                const idxA = order.indexOf(extraA);
                                const idxB = order.indexOf(extraB);
                                if (idxA === -1 && idxB === -1) return 0;
                                if (idxA === -1) return 1;
                                if (idxB === -1) return -1;
                                return idxA - idxB;
                            })
                            .map(([extraName, extraData]) => (
                                <button
                                    key={extraName}
                                    className={`menu-item-btn ${!extraData.available ? 'unavailable' : ''}`}
                                    disabled={!extraData.available}
                                    onClick={() => handleAddItem(selectedItemForExtras.category, selectedItemForExtras.name, pendingSideName, extraName)}
                                >
                                    <div className="item-name">{extraName}</div>
                                    <div className="item-price">${(extraData.price / 100).toFixed(2)}</div>
                                </button>
                            ))}
                    </div>
                </div>
            );
        }

        if (selectedItemForSides) {
            return (
                <div className="menu-panel-content">
                    <div className="menu-panel-heading">
                        <Button size="small" variant="secondary" onClick={() => setSelectedItemForSides(null)}>Back</Button>
                        <h4>Select Side for {selectedItemForSides.name}</h4>
                    </div>
                    <div className="menu-items-grid">
                        {Object.entries(selectedItemForSides.sides || {})
                            .sort(([sideA], [sideB]) => {
                                const order = selectedItemForSides.sideOrder || [];
                                const idxA = order.indexOf(sideA);
                                const idxB = order.indexOf(sideB);
                                if (idxA === -1 && idxB === -1) return 0;
                                if (idxA === -1) return 1;
                                if (idxB === -1) return -1;
                                return idxA - idxB;
                            })
                            .map(([sideName, sideData]) => (
                                <button
                                    key={sideName}
                                    className={`menu-item-btn ${!sideData.available ? 'unavailable' : ''}`}
                                    disabled={!sideData.available}
                                    onClick={() => proceedAfterSide(selectedItemForSides, sideName)}
                                >
                                    <div className="item-name">{sideName}</div>
                                    <div className="item-price">${(sideData.price / 100).toFixed(2)}</div>
                                </button>
                            ))}
                    </div>
                </div>
            );
        }

        // Search results — continuous scroll filtered
        if (isSearching) {
            return (
                <div className="menu-scroll-content">
                    {sortedCategories.map(([category, items]) => {
                        const filtered = items.filter(item =>
                            item.name.toLowerCase().includes(searchTerm)
                        );
                        if (filtered.length === 0) return null;
                        return (
                            <div key={category} className="menu-section">
                                <div className="menu-section-title">{category}</div>
                                {renderItemGrid(filtered, category)}
                            </div>
                        );
                    })}
                </div>
            );
        }

        // Default: continuous scroll with section titles
        return (
            <div className="menu-scroll-content">
                {sortedCategories.map(([category, items]) => (
                    <div key={category} className="menu-section">
                        <div className="menu-section-title">{category}</div>
                        {renderItemGrid(items, category)}
                    </div>
                ))}
            </div>
        );
    };

    const renderItemView = () => {
        const allItems = (ticket.orders || []).flatMap(order => order.items);
        if (allItems.length === 0) return <div className="empty-orders">No items yet.</div>;
        return (
            <div className="item-view-list">
                {allItems.map((item, idx) => (
                    <div key={idx} className="item-view-row">
                        <span className="item-view-name">{item.name}</span>
                        {item.selectedSide && item.selectedSide !== 'none' && (
                            <span className="item-view-sub">+ {item.selectedSide}</span>
                        )}
                        {item.selectedExtra && item.selectedExtra !== 'none' && (
                            <span className="item-view-sub">+ {item.selectedExtra}</span>
                        )}
                        {item.comment && (
                            <span className="item-view-note">{item.comment}</span>
                        )}
                    </div>
                ))}
            </div>
        );
    };

    const renderMenuModal = () => (
        <Modal
            isOpen={isMenuOpen}
            onClose={closeMenuModal}
            title="Add Item"
            className="modal-large"
        >
            <div className="menu-modal-inner">
                {!isSelectingSidesOrExtras && (
                    <div className="menu-search-bar">
                        <input
                            type="text"
                            className="menu-search-input"
                            placeholder="Search menu..."
                            value={menuSearch}
                            onChange={(e) => setMenuSearch(e.target.value)}
                        />
                    </div>
                )}
                {renderMenuModalContent()}
            </div>
        </Modal>
    );

    const ordersContent = (ticket.orders || []).map((order, idx) => {
        if (isDndEnabled) {
            return (
                <DroppableOrder key={idx} orderIndex={idx}>
                    {renderOrderBlock(order, idx)}
                </DroppableOrder>
            );
        }
        return renderOrderBlock(order, idx);
    });

    return (
        <div className="ticket-detail-page">
            <div className="detail-header">
                <Button variant="secondary" onClick={() => {
                        const status = ticket.status?.toLowerCase() || 'active';
                        navigate('/tickets', { state: { tab: status } });
                    }}>&larr; Back</Button>
                <h2>Table {ticket.tableNumber} <span className="id-sub">#{ticket.id}</span></h2>
                <div className="header-actions">
                    <button
                        className={`view-toggle-btn ${itemView ? 'view-toggle-active' : ''}`}
                        onClick={() => setItemView(v => !v)}
                        title={itemView ? 'Switch to order view' : 'Switch to item view'}
                    >
                        {itemView ? 'Orders' : 'Items'}
                    </button>
                    <div className={`status-badge status-${ticket.status ? ticket.status.toLowerCase() : 'active'}`}>
                        {ticket.status || 'ACTIVE'}
                    </div>
                </div>
            </div>

            {isActive && (
                <div className="ticket-comment">
                    <input
                        type="text"
                        className="comment-input ticket-comment-input"
                        placeholder="Ticket comment..."
                        value={getCommentValue('ticket', ticket.comment)}
                        onChange={(e) => handleCommentChange('ticket', e.target.value, saveTicketComment)}
                    />
                </div>
            )}
            {ticket.status !== 'ACTIVE' && ticket.comment && (
                <div className="ticket-comment">
                    <span className="comment-display">{ticket.comment}</span>
                </div>
            )}

            {itemView ? renderItemView() : (
                <div className="orders-list">
                    {isDndEnabled ? (
                        <DndContext
                            sensors={sensors}
                            collisionDetection={closestCenter}
                            onDragStart={handleDragStart}
                            onDragEnd={handleDragEnd}
                            onDragCancel={handleDragCancel}
                        >
                            {ordersContent}
                            <DragOverlay>
                                {renderDragOverlay()}
                            </DragOverlay>
                        </DndContext>
                    ) : (
                        ordersContent
                    )}
                    {(ticket.orders || []).length === 0 && <div className="empty-orders">No orders yet.</div>}
                </div>
            )}

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

                {isActive && (
                    <div className="footer-primary-actions">
                        <Button variant="secondary" onClick={handleAddOrder}>+ Add Order</Button>
                        <Button variant="warning" onClick={handleSendToKitchen}>Send to Kitchen</Button>
                    </div>
                )}

                {ticket.status === 'COMPLETED' && (
                    <div className="footer-primary-actions">
                        <Button variant="danger" onClick={() => handleStatusChange('closed')}>Close Ticket</Button>
                        <Button variant="secondary" onClick={handleAddOrder}>+ Add Order</Button>
                    </div>
                )}

                <div className="footer-secondary-actions">
                    {isActive && (
                        <>
                            <Button variant="success" className="btn-ghost" onClick={() => handleStatusChange('completed')}>Mark Complete</Button>
                            <Button variant="danger" className="btn-ghost" onClick={() => handleStatusChange('closed')}>Close Ticket</Button>
                            <Button variant="danger" className="btn-ghost" onClick={handleDeleteTicket}>Delete Ticket</Button>
                        </>
                    )}
                    {ticket.status === 'COMPLETED' && (
                        <>
                            <Button variant="warning" className="btn-ghost" onClick={() => handleStatusChange('active')}>Reopen Ticket</Button>
                            <Button variant="danger" className="btn-ghost" onClick={handleDeleteTicket}>Delete Ticket</Button>
                        </>
                    )}
                    {ticket.status === 'CLOSED' && (
                        <>
                            <Button variant="secondary" onClick={handleAddOrder}>+ Add Order</Button>
                            <Button variant="primary" onClick={handlePrintReceipt}>&#128424;&#65039; Print Receipt</Button>
                            {selectedOrdersForPrint.size > 0 && (
                                <Button variant="primary" className="print-selected-btn" onClick={handlePrintSelectedOrders}>
                                    &#128424;&#65039; Print Selected ({selectedOrdersForPrint.size})
                                </Button>
                            )}
                        </>
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
