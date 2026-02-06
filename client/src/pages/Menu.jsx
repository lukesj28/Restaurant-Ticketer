import React, { useState, useEffect, useRef } from 'react';
import { api } from '../api/api';
import MenuItemCard from '../components/menu/MenuItemCard';
import Button from '../components/common/Button';
import Modal from '../components/common/Modal';
import { useToast } from '../context/ToastContext';
import './Menu.css';

import {
    DndContext,
    closestCenter,
    KeyboardSensor,
    PointerSensor,
    useSensor,
    useSensors,
    DragOverlay,
    defaultDropAnimationSideEffects,
} from '@dnd-kit/core';
import {
    arrayMove,
    SortableContext,
    sortableKeyboardCoordinates,
    verticalListSortingStrategy,
    rectSortingStrategy,
    useSortable,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

const Menu = () => {
    const { toast } = useToast();
    const [categories, setCategories] = useState({}); // Map: category -> items[]
    const [categoryOrder, setCategoryOrder] = useState([]); // Array of category names
    const [loading, setLoading] = useState(true);
    const [editItem, setEditItem] = useState(null); // Item being edited
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);

    // Create Mode
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [newItem, setNewItem] = useState({ name: '', category: '', price: '' });

    // Edit Form State
    const [editForm, setEditForm] = useState({ price: '', available: true, name: '', category: '' });

    const [editingCategory, setEditingCategory] = useState(null); // Category being edited
    const [newCategoryName, setNewCategoryName] = useState('');
    const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);

    // Kitchen Items State
    const [kitchenItems, setKitchenItems] = useState([]);
    const [isKitchenItem, setIsKitchenItem] = useState(false); // For Create/Edit modals

    // New Side Input State
    const [newSide, setNewSide] = useState({ name: '', price: '' });
    const [sidesToDelete, setSidesToDelete] = useState([]); // Track sides to delete on save

    // Confirmation Modal State
    const [confirmationModal, setConfirmationModal] = useState({
        isOpen: false,
        title: '',
        message: ''
    });
    const confirmAction = useRef(null);
    const [isProcessing, setIsProcessing] = useState(false);

    // DnD Sensors
    const sensors = useSensors(
        useSensor(PointerSensor, {
            activationConstraint: {
                distance: 8, // Require 8px movement before drag starts (prevents accidental drags on click)
            },
        }),
        useSensor(KeyboardSensor, {
            coordinateGetter: sortableKeyboardCoordinates,
        })
    );

    const [activeId, setActiveId] = useState(null); // ID of currently dragged item
    const [activeDragType, setActiveDragType] = useState(null); // 'CATEGORY', 'ITEM', 'SIDE'

    useEffect(() => {
        fetchMenu();
        fetchKitchenItems();
    }, []);

    const fetchMenu = async () => {
        setLoading(true);
        try {
            const [categoriesData, orderData] = await Promise.all([
                api.get('/menu/categories'),
                api.get('/menu/category-order')
            ]);
            setCategories(categoriesData);

            // If orderData is empty or missing keys, fallback to object keys (sorted roughly) or maintain existing
            let order = orderData || [];
            if (Object.keys(categoriesData).length > 0) {
                const keys = Object.keys(categoriesData);
                // If order is missing, use keys
                if (!order || order.length === 0) {
                    order = keys;
                } else {
                    // Ensure all current categories are in the order (handle new categories appearing before order sync)
                    const missing = keys.filter(k => !order.includes(k.toLowerCase()) && !order.includes(k));
                    if (missing.length > 0) order = [...order, ...missing];
                }
            }
            // Ensure order uses correct casing from keys if possible, or just rely on backend normalization
            setCategoryOrder(order);

        } catch (e) {
            console.error(e);
            toast.error("Failed to load menu");
        } finally {
            setLoading(false);
        }
    };

    const fetchKitchenItems = async () => {
        try {
            const data = await api.get('/menu/kitchen-items');
            setKitchenItems(data || []);
        } catch (e) {
            console.error("Failed to fetch kitchen items:", e);
        }
    };

    const handleEditClick = (item, categoryName) => {
        setEditItem({ ...item, originalName: item.name, originalCategory: categoryName });
        const isKitchen = kitchenItems.includes(item.name);
        setIsKitchenItem(isKitchen);
        setEditForm({
            price: (item.price / 100).toFixed(2),
            available: item.available,
            name: item.name,
            category: categoryName,
            sides: item.sides ? JSON.parse(JSON.stringify(item.sides)) : {}, // Deep copy to avoid mutating original ref
            sideOrder: item.sideOrder ? [...item.sideOrder] : [] // Clone explicit order
        });
        setNewSide({ name: '', price: '' });
        setSidesToDelete([]);
        setIsEditModalOpen(true);
    };

    const handleEditCategoryClick = (categoryName) => {
        setEditingCategory(categoryName);
        setNewCategoryName(categoryName);
        setIsCategoryModalOpen(true);
    };

    const handleCategoryRename = async () => {
        if (!newCategoryName || !newCategoryName.trim()) {
            toast.error('Category name cannot be empty');
            return;
        }
        if (newCategoryName === editingCategory) {
            setIsCategoryModalOpen(false);
            return;
        }

        try {
            await api.put(`/menu/categories/${editingCategory}/rename`, { newCategory: newCategoryName });
            setIsCategoryModalOpen(false);
            fetchMenu();
            toast.success('Category renamed successfully');
        } catch (e) {
            toast.error('Failed to rename category: ' + (e.message || 'Unknown error'));
        }
    };

    const findContainer = (id) => {
        if (id in categories) return id;
        return Object.keys(categories).find(key =>
            categories[key]?.find(item => item.name === id)
        );
    };

    const handleDragStart = (event) => {
        const { active } = event;
        setActiveId(active.id);

        // Determine type based on ID format or data
        if (active.data.current?.type) {
            setActiveDragType(active.data.current.type);
        } else if (categoryOrder.includes(active.id)) {
            setActiveDragType('CATEGORY');
        } else {
            setActiveDragType('ITEM');
        }
    };

    const handleDragOver = (event) => {
        const { active, over } = event;

        // Fill in specific item moving logic if needed for visual smoothness across containers
        // For items, we want to move them in the state as we drag over different containers
        if (!over || active.id === over.id || activeDragType !== 'ITEM') return;

        const activeContainer = findContainer(active.id);
        const overContainer = findContainer(over.id) || (over.id in categories ? over.id : null); // If over a category container directly

        if (!activeContainer || !overContainer || activeContainer === overContainer) {
            return;
        }

        // Move item to new container in state
        setCategories((prev) => {
            const activeItems = prev[activeContainer];
            const overItems = prev[overContainer];
            const activeIndex = activeItems.findIndex(i => i.name === active.id);
            const overIndex = overItems.findIndex(i => i.name === over.id);

            let newIndex;
            if (over.id in prev) {
                newIndex = overItems.length + 1;
            } else {
                const isBelowOverItem =
                    over &&
                    active.rect.current.translated &&
                    active.rect.current.translated.top >
                    over.rect.top + over.rect.height;

                const modifier = isBelowOverItem ? 1 : 0;
                newIndex = overIndex >= 0 ? overIndex + modifier : overItems.length + 1;
            }

            return {
                ...prev,
                [activeContainer]: [...prev[activeContainer].filter(item => item.name !== active.id)],
                [overContainer]: [
                    ...prev[overContainer].slice(0, newIndex),
                    activeItems[activeIndex], // Move the item object with its data
                    ...prev[overContainer].slice(newIndex, prev[overContainer].length)
                ]
            };
        });
    };

    const handleDragEnd = async (event) => {
        const { active, over } = event;
        const type = activeDragType; // capture current type
        setActiveId(null);
        setActiveDragType(null);

        if (!over) return;

        // Category Reordering
        if (type === 'CATEGORY') {
            if (active.id !== over.id) {
                const oldIndex = categoryOrder.indexOf(active.id);
                const newIndex = categoryOrder.indexOf(over.id);
                if (oldIndex !== -1 && newIndex !== -1) {
                    const newOrder = arrayMove(categoryOrder, oldIndex, newIndex);
                    setCategoryOrder(newOrder);
                    try {
                        await api.put('/menu/categories/reorder', { order: newOrder });
                    } catch (e) {
                        console.error(e);
                        toast.error("Failed to reorder categories");
                        fetchMenu();
                    }
                }
            }
            return;
        }

        // Item Reordering
        if (type === 'ITEM') {
            const activeContainer = findContainer(active.id);
            const overContainer = findContainer(over.id) || (over.id in categories ? over.id : null);

            if (activeContainer && overContainer) {
                const activeIndex = categories[activeContainer].findIndex(i => i.name === active.id);
                const overIndex = over.id in categories ? categories[overContainer].length + 1 : categories[overContainer].findIndex(i => i.name === over.id);

                if (activeContainer === overContainer) {
                    // Same container reorder
                    if (activeIndex !== overIndex && activeIndex !== -1 && overIndex !== -1) {
                        const newItems = arrayMove(categories[activeContainer], activeIndex, overIndex);
                        setCategories(prev => ({
                            ...prev,
                            [activeContainer]: newItems
                        }));

                        // API Call
                        try {
                            await api.put(`/menu/categories/${activeContainer}/items/reorder`, {
                                order: newItems.map(i => i.name)
                            });
                        } catch (e) {
                            toast.error("Failed to reorder items");
                            fetchMenu();
                        }
                    }
                } else {
                    // Different container (already handled in dragOver for state, but need to finalize via API)
                    // Actually, if we handled it in dragOver, the active item is ALREADY in the overContainer in state!
                    // So identifying "activeContainer" via findContainer might return the NEW container if we look at current state.
                    // But we need to know it changed.

                    // In dnd-kit, dragOver mutations persist. So activeContainer === overContainer usually at dragEnd.
                    // We just need to persist the new order of the target category.
                    // AND we need to call the API to update the item's category.

                    // Problem: How do we know it changed category if the state is already updated?
                    // We can compare with `active.data.current.category` (if we updated SortableItem to pass it).

                    // Let's assume dragOver updated the state.
                    // The item is now in `activeContainer` (which matches `overContainer`).
                    // Use `active.data.current` to get ORIGINAL category.

                    const originalCategory = active.data.current?.category;
                    const currentCategory = activeContainer;

                    if (originalCategory && currentCategory && originalCategory !== currentCategory) {
                        // 1. Update backend category
                        try {
                            await api.put(`/menu/items/${active.id}/category`, { newCategory: currentCategory });
                            // 2. Update backend order in new category
                            const newItems = categories[currentCategory];
                            await api.put(`/menu/categories/${currentCategory}/items/reorder`, {
                                order: newItems.map(i => i.name)
                            });
                        } catch (e) {
                            toast.error("Failed to save move");
                            fetchMenu();
                        }
                    } else {
                        // It was a reorder within the same category (after dragOver settled it)
                        const newItems = categories[activeContainer];
                        try {
                            await api.put(`/menu/categories/${activeContainer}/items/reorder`, {
                                order: newItems.map(i => i.name)
                            });
                        } catch (e) {
                            // ignore trivial errors or revert
                        }
                    }
                }
            }
        }

        // Side Reordering
        if (type === 'SIDE') {
            const activeIdStripped = active.id.replace('side-', '');
            const overIdStripped = over.id.replace('side-', '');

            // We use editForm.sideOrder for state source of truth during edit
            const currentOrder = editForm.sideOrder && editForm.sideOrder.length > 0
                ? editForm.sideOrder
                : Object.keys(editForm.sides || {}).filter(k => k.toLowerCase() !== 'none' && !sidesToDelete.includes(k));

            const oldIndex = currentOrder.indexOf(activeIdStripped);
            const newIndex = currentOrder.indexOf(overIdStripped);

            if (oldIndex !== -1 && newIndex !== -1 && oldIndex !== newIndex) {
                const newOrder = arrayMove(currentOrder, oldIndex, newIndex);

                setEditForm(prev => ({
                    ...prev,
                    sideOrder: newOrder
                }));
            }
        }
    };

    const handleCategoryDelete = async () => {
        confirmAction.current = async () => {
            // ... existing delete logic
            try {
                await api.delete(`/menu/categories/${editingCategory}`);
                setIsCategoryModalOpen(false);
                fetchMenu();
                toast.success('Category deleted successfully');
            } catch (e) {
                toast.error('Failed to delete category: ' + (e.message || 'Unknown error'));
            }
        };

        setConfirmationModal({
            isOpen: true,
            title: 'Delete Category',
            message: `Are you sure you want to delete category "${editingCategory}"? ALL items in this category will be deleted!`
        });
    };

    const handleSaveEdit = async () => {
        try {
            // Check what changed and call appropriate APIs
            // Price
            const newPriceCents = Math.round(parseFloat(editForm.price) * 100);
            if (newPriceCents !== editItem.price) {
                await api.put(`/menu/items/${editItem.originalName}/price`, { newPrice: newPriceCents });
            }
            // Availability
            if (editForm.available !== editItem.available) {
                await api.put(`/menu/items/${editItem.originalName}/availability`, { available: editForm.available });
            }
            // Rename
            let currentName = editItem.originalName;
            if (editForm.name !== currentName) {
                await api.put(`/menu/items/${currentName}/rename`, { newName: editForm.name });
                currentName = editForm.name;
            }
            // Category
            if (editForm.category !== editItem.originalCategory) {
                await api.put(`/menu/items/${currentName}/category`, { newCategory: editForm.category });
            }

            // Kitchen Status
            const wasKitchen = kitchenItems.includes(editItem.originalName);
            // Note: If renamed, we use the new name for kitchen status update if needed, but the backend rename should handle the old name removal.
            // However, renameItem in backend typically handles migrating the name in kitchenItems list.
            // So we just need to check if the state changed.
            if (isKitchenItem && !wasKitchen) {
                await api.post(`/menu/kitchen-items/${currentName}`);
            } else if (!isKitchenItem && wasKitchen) {
                await api.delete(`/menu/kitchen-items/${currentName}`);
            }

            // Sides updates (only for existing sides that changed)
            if (editForm.sides) {
                for (const [sideName, sideData] of Object.entries(editForm.sides)) {
                    if (sideName === 'none') continue; // Skip none
                    if (sidesToDelete.includes(sideName)) continue; // Will be deleted
                    const originalSide = editItem.sides && editItem.sides[sideName];
                    if (originalSide && (originalSide.price !== sideData.price || originalSide.available !== sideData.available)) {
                        await api.put(`/menu/items/${currentName}/sides/${sideName}`, {
                            price: sideData.price,
                            available: sideData.available
                        });
                    }
                }
            }

            // Delete sides
            for (const sideName of sidesToDelete) {
                await api.delete(`/menu/items/${currentName}/sides/${encodeURIComponent(sideName)}`);
            }

            setIsEditModalOpen(false);
            fetchMenu();
            fetchKitchenItems();
            toast.success('Item updated successfully');
        } catch (e) {
            toast.error('Failed to update item: ' + e.message);
        }
    };

    const handleCreateItem = async (e) => {
        e.preventDefault();
        try {
            await api.post('/menu/items', {
                name: newItem.name,
                category: newItem.category,
                price: Math.round(parseFloat(newItem.price) * 100),
                sides: {} // Default empty
            });

            if (isKitchenItem) {
                await api.post(`/menu/kitchen-items/${newItem.name}`);
            }

            setIsCreateModalOpen(false);
            setNewItem({ name: '', category: '', price: '' });
            setIsKitchenItem(false);
            fetchMenu();
            fetchKitchenItems();
            toast.success('Item created successfully');
        } catch (error) {
            toast.error('Failed to create item: ' + error.message);
        }
    };

    const handleDelete = async () => {
        confirmAction.current = async () => {
            try {
                await api.delete(`/menu/items/${editItem.originalName}`);
                setIsEditModalOpen(false);
                fetchMenu();
                fetchKitchenItems();
                toast.success('Item deleted successfully');
            } catch (e) {
                toast.error(e.message);
            }
        };

        setConfirmationModal({
            isOpen: true,
            title: 'Delete Item',
            message: `Are you sure you want to delete ${editItem.name}?`
        });
    };

    const closeConfirmation = () => {
        setConfirmationModal(prev => ({ ...prev, isOpen: false }));
    };

    return (
        <div className="menu-page">
            <div className="menu-header">
                <h1>Menu Management</h1>
                <Button onClick={() => {
                    setIsCreateModalOpen(true);
                    setIsKitchenItem(false);
                }}>+ Add Item</Button>
            </div>

            {loading ? <div>Loading...</div> : (
                <DndContext
                    sensors={sensors}
                    collisionDetection={closestCenter}
                    onDragStart={handleDragStart}
                    onDragOver={handleDragOver}
                    onDragEnd={handleDragEnd}
                >
                    <SortableContext
                        items={categoryOrder}
                        strategy={verticalListSortingStrategy}
                    >
                        <div className="menu-list">
                            {categoryOrder.map(categoryName => {
                                const items = categories[categoryName] || [];
                                // Only render if category exists in map (it should)
                                return (
                                    <SortableCategory
                                        key={categoryName}
                                        category={categoryName}
                                        items={items}
                                        onEditCategory={handleEditCategoryClick}
                                        onEditItem={handleEditClick}
                                    />
                                );
                            })}
                        </div>
                    </SortableContext>

                    <DragOverlay>
                        {activeId ? (
                            activeDragType === 'CATEGORY' ? (
                                <div className="menu-category-section" style={{ opacity: 0.8, background: 'var(--color-bg-primary)', border: '1px solid var(--color-border)' }}>
                                    <div className="menu-category-header">
                                        <h3 className="menu-category-title">{activeId}</h3>
                                    </div>
                                </div>
                            ) : activeDragType === 'ITEM' ? (
                                <div style={{ width: '200px' }}>
                                    {/* We don't have easy access to the item object here without searching. 
                                         Ideally we pass it in active.data.current.item */}
                                    <div className="menu-item-card" style={{ height: '100px', background: 'white', border: '1px solid #ccc' }}>
                                        Dragging...
                                    </div>
                                </div>
                            ) : null
                        ) : null}
                    </DragOverlay>
                </DndContext>
            )}

            {/* Edit Modal */}
            <Modal
                isOpen={isEditModalOpen}
                onClose={() => setIsEditModalOpen(false)}
                title="Edit Item"
                footer={
                    <div className="edit-footer">
                        <Button variant="danger" onClick={handleDelete}>Delete</Button>
                        <div className="edit-actions-right">
                            <Button variant="secondary" onClick={() => setIsEditModalOpen(false)}>Cancel</Button>
                            <Button variant="primary" onClick={handleSaveEdit}>Save</Button>
                        </div>
                    </div>
                }
            >
                <div className="form-group">
                    <label>Name</label>
                    <input
                        value={editForm.name}
                        onChange={e => setEditForm({ ...editForm, name: e.target.value })}
                    />
                </div>
                <div className="form-group">
                    <label>Price ($)</label>
                    <input
                        type="number"
                        step="0.01"
                        value={editForm.price}
                        onChange={e => setEditForm({ ...editForm, price: e.target.value })}
                    />
                </div>
                <div className="form-group">
                    <label>Category</label>
                    <input
                        value={editForm.category}
                        onChange={e => setEditForm({ ...editForm, category: e.target.value })}
                    />
                </div>
                <div className="form-group checkbox-group">
                    <label>
                        <input
                            type="checkbox"
                            checked={editForm.available}
                            onChange={e => setEditForm({ ...editForm, available: e.target.checked })}
                        />
                        Available
                    </label>
                </div>
                <div className="form-group checkbox-group">
                    <label>
                        <input
                            type="checkbox"
                            checked={isKitchenItem}
                            onChange={e => setIsKitchenItem(e.target.checked)}
                        />
                        Kitchen Item
                    </label>
                </div>
                {/* Sides Editing */}
                <div className="sides-section">
                    <h4>Sides</h4>
                    <div className="sides-grid">
                        {(() => {
                            const sides = editForm.sides || {};
                            const rawSideNames = Object.keys(sides).filter(k => k.toLowerCase() !== 'none' && !sidesToDelete.includes(k));
                            let orderedSides = [];
                            if (editForm.sideOrder && editForm.sideOrder.length > 0) {
                                orderedSides = editForm.sideOrder.filter(k => sides[k] && !sidesToDelete.includes(k));
                                const missing = rawSideNames.filter(k => !orderedSides.includes(k));
                                orderedSides = [...orderedSides, ...missing];
                            } else {
                                orderedSides = rawSideNames;
                            }

                            return orderedSides.map(sideName => (
                                <SideRow
                                    key={sideName}
                                    sideName={sideName}
                                    sideData={sides[sideName]}
                                    onPriceChange={e => {
                                        const newPrice = e.target.value;
                                        setEditForm(prev => ({
                                            ...prev,
                                            sides: {
                                                ...prev.sides,
                                                [sideName]: { ...prev.sides[sideName], price: Math.round(parseFloat(newPrice) * 100) }
                                            }
                                        }));
                                    }}
                                    onAvailableChange={e => {
                                        const newAvail = e.target.checked;
                                        setEditForm(prev => ({
                                            ...prev,
                                            sides: {
                                                ...prev.sides,
                                                [sideName]: { ...prev.sides[sideName], available: newAvail }
                                            }
                                        }));
                                    }}
                                    onDelete={() => setSidesToDelete(prev => [...prev, sideName])}
                                />
                            ));
                        })()}
                    </div>
                    {/* Add New Side */}
                    <div className="add-side-row">
                        <input
                            type="text"
                            placeholder="Side name"
                            className="side-name-input"
                            value={newSide.name}
                            onChange={e => setNewSide(prev => ({ ...prev, name: e.target.value }))}
                        />
                        <input
                            type="number"
                            step="0.01"
                            placeholder="Price"
                            className="side-price-input"
                            value={newSide.price}
                            onChange={e => setNewSide(prev => ({ ...prev, price: e.target.value }))}
                        />
                        <Button
                            variant="secondary"
                            className="add-side-btn"
                            onClick={async () => {
                                if (!newSide.name.trim()) {
                                    toast.error('Side name is required');
                                    return;
                                }
                                const priceCents = Math.round(parseFloat(newSide.price || '0') * 100);
                                try {
                                    await api.post(`/menu/items/${editForm.name}/sides`, {
                                        name: newSide.name.trim(),
                                        price: priceCents
                                    });
                                    // Update local state
                                    setEditForm(prev => ({
                                        ...prev,
                                        sides: {
                                            ...prev.sides,
                                            [newSide.name.trim()]: { price: priceCents, available: true }
                                        }
                                    }));
                                    setNewSide({ name: '', price: '' });
                                    toast.success(`Side "${newSide.name}" added`);
                                } catch (e) {
                                    toast.error('Failed to add side: ' + e.message);
                                }
                            }}
                        >
                            Add
                        </Button>
                    </div>
                </div>
            </Modal >

            {/* Category Edit Modal */}
            <Modal
                isOpen={isCategoryModalOpen}
                onClose={() => setIsCategoryModalOpen(false)}
                title="Edit Category"
                footer={
                    <div className="edit-footer">
                        <Button variant="danger" onClick={handleCategoryDelete}>Delete Category</Button>
                        <div className="edit-actions-right">
                            <Button variant="secondary" onClick={() => setIsCategoryModalOpen(false)}>Cancel</Button>
                            <Button variant="primary" onClick={handleCategoryRename}>Save</Button>
                        </div>
                    </div>
                }
            >
                <div className="form-group">
                    <label>Category Name</label>
                    <input
                        value={newCategoryName}
                        onChange={e => setNewCategoryName(e.target.value)}
                        placeholder="Category Name"
                    />
                </div>
            </Modal>

            {/* Create Modal */}
            < Modal
                isOpen={isCreateModalOpen}
                onClose={() => setIsCreateModalOpen(false)}
                title="Create New Item"
                footer={
                    <>
                        <Button variant="secondary" onClick={() => setIsCreateModalOpen(false)}>Cancel</Button>
                        <Button variant="primary" onClick={handleCreateItem}>Create</Button>
                    </>
                }
            >
                <form onSubmit={handleCreateItem}>
                    <div className="form-group">
                        <label>Name</label>
                        <input
                            value={newItem.name}
                            onChange={e => setNewItem({ ...newItem, name: e.target.value })}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Category</label>
                        <input
                            value={newItem.category}
                            onChange={e => setNewItem({ ...newItem, category: e.target.value })}
                            required
                            placeholder="e.g. Mains"
                        />
                    </div>
                    <div className="form-group">
                        <label>Price ($)</label>
                        <input
                            type="number"
                            step="0.01"
                            value={newItem.price}
                            onChange={e => setNewItem({ ...newItem, price: e.target.value })}
                            required
                        />
                    </div>
                    <div className="form-group checkbox-group">
                        <label>
                            <input
                                type="checkbox"
                                checked={isKitchenItem}
                                onChange={e => setIsKitchenItem(e.target.checked)}
                            />
                            Kitchen Item
                        </label>
                    </div>
                </form>
            </Modal >

            {/* Confirmation Modal */}
            <Modal
                isOpen={confirmationModal.isOpen}
                onClose={closeConfirmation}
                title={confirmationModal.title}
                footer={
                    <>
                        <Button variant="secondary" onClick={closeConfirmation} disabled={isProcessing}>Cancel</Button>
                        <Button variant="danger" disabled={isProcessing} onClick={async () => {
                            if (confirmAction.current) {
                                setIsProcessing(true);
                                try {
                                    await confirmAction.current();
                                } finally {
                                    setIsProcessing(false);
                                    closeConfirmation();
                                }
                            }
                        }}>{isProcessing ? 'Processing...' : 'Confirm'}</Button>
                    </>
                }
            >
                <p>{confirmationModal.message}</p>
            </Modal >
        </div >
    );
};

const SortableCategory = ({ category, items, onEditCategory, onEditItem }) => {
    const {
        attributes,
        listeners,
        setNodeRef,
        transform,
        transition,
    } = useSortable({
        id: category,
        data: {
            type: 'CATEGORY',
            category
        }
    });

    const style = {
        transform: CSS.Transform.toString(transform),
        transition,
    };

    return (
        <div ref={setNodeRef} style={style} className="menu-category-section">
            <div className="menu-category-header" {...attributes} {...listeners}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    {/* Drag Handle Icon could go here, or just clicking header works */}
                    <span style={{ cursor: 'grab' }}>:::</span>
                    <h3 className="menu-category-title">{category}</h3>
                </div>
                <button
                    className="category-edit-btn"
                    onClick={(e) => {
                        e.stopPropagation(); // Prevent drag start when clicking edit
                        onEditCategory(category);
                    }}
                    onPointerDown={(e) => e.stopPropagation()} // Prevent drag start
                    aria-label={`Edit ${category} category`}
                >
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                </button>
            </div>

            <SortableContext
                id={category}
                items={items.map(i => i.name)}
                strategy={rectSortingStrategy}
            >
                <div className="menu-grid">
                    {items.map(item => (
                        <SortableItem
                            key={item.name}
                            item={item}
                            category={category}
                            onEdit={onEditItem}
                        />
                    ))}
                </div>
            </SortableContext>
        </div>
    );
};

const SortableItem = ({ item, category, onEdit }) => {
    const {
        attributes,
        listeners,
        setNodeRef,
        transform,
        transition,
    } = useSortable({
        id: item.name,
        data: {
            type: 'ITEM',
            item,
            category
        }
    });

    const style = {
        transform: CSS.Transform.toString(transform),
        transition,
    };

    return (
        <div ref={setNodeRef} style={style} {...attributes} {...listeners}>
            <MenuItemCard
                item={item}
                onEdit={(i) => onEdit(i, category)}
            />
        </div>
    );
};

const SideRow = ({ sideName, sideData, onPriceChange, onAvailableChange, onDelete }) => {
    return (
        <div className="side-edit-row">
            <span className="side-name">{sideName}</span>
            <input
                type="number"
                step="0.01"
                className="side-price-input"
                value={sideData.price !== undefined ? (sideData.price / 100).toFixed(2) : ''}
                onChange={onPriceChange}
            />
            <label className="side-avail-label">
                <input
                    type="checkbox"
                    checked={sideData.available}
                    onChange={onAvailableChange}
                />
                Avail
            </label>
            <button
                type="button"
                className="side-delete-btn"
                onClick={onDelete}
                aria-label={`Delete ${sideName}`}
            >
                ×
            </button>
        </div>
    );
};

export default Menu;
