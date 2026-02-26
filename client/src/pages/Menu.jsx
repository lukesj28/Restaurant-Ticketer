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
    const [categories, setCategories] = useState({}); // Map: category -> ItemDto[]
    const [categoryOrder, setCategoryOrder] = useState([]); // Array of category names
    const [loading, setLoading] = useState(true);
    const [editItem, setEditItem] = useState(null); // Item being edited (ItemDto + originalCategory)
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);

    // Create Mode
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [newItem, setNewItem] = useState({ name: '', category: '', price: '' });
    const [isCreatingNewCategory, setIsCreatingNewCategory] = useState(false);
    const [newItemSideSources, setNewItemSideSources] = useState([]);

    // Edit Form State
    const [editForm, setEditForm] = useState({
        price: '', available: true, name: '', category: '', kitchen: false, sideSources: []
    });

    const [editingCategory, setEditingCategory] = useState(null);
    const [newCategoryName, setNewCategoryName] = useState('');
    const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);

    // Kitchen Item State (for create modal)
    const [isKitchenItem, setIsKitchenItem] = useState(false);

    // Confirmation Modal State
    const [confirmationModal, setConfirmationModal] = useState({
        isOpen: false, title: '', message: ''
    });
    const confirmAction = useRef(null);
    const [isProcessing, setIsProcessing] = useState(false);

    // Combo state
    const [combos, setCombos] = useState([]);
    const [baseItems, setBaseItems] = useState([]);

    // Create Combo
    const [isCreateComboModalOpen, setIsCreateComboModalOpen] = useState(false);
    const [newComboForm, setNewComboForm] = useState({ name: '', category: '', price: '', kitchen: false });
    const [isCreatingComboCat, setIsCreatingComboCat] = useState(false);
    const [newComboComponentIds, setNewComboComponentIds] = useState([]);
    const [newComboSlots, setNewComboSlots] = useState([]);

    // Edit Combo
    const [isEditComboModalOpen, setIsEditComboModalOpen] = useState(false);
    const [editComboData, setEditComboData] = useState(null);
    const [editComboForm, setEditComboForm] = useState({ name: '', price: '', available: true, kitchen: false });

    // DnD Sensors
    const sensors = useSensors(
        useSensor(PointerSensor, {
            activationConstraint: { distance: 8 },
        }),
        useSensor(KeyboardSensor, {
            coordinateGetter: sortableKeyboardCoordinates,
        })
    );

    const [activeId, setActiveId] = useState(null);
    const [activeDragType, setActiveDragType] = useState(null);

    useEffect(() => {
        fetchMenu();
    }, []);

    const fetchMenu = async () => {
        setLoading(true);
        try {
            const menuData = await api.get('/menu');
            const cats = {};
            for (const [name, cat] of Object.entries(menuData.categories || {})) {
                cats[name] = cat.items || [];
            }
            setCategories(cats);
            setCategoryOrder(menuData.categoryOrder || []);
            setBaseItems(menuData.baseItems || []);
            setCombos(menuData.combos || []);
        } catch (e) {
            console.error(e);
            toast.error("Failed to load menu");
        } finally {
            setLoading(false);
        }
    };

    const handleEditClick = (item, categoryName) => {
        setEditItem({ ...item, originalCategory: categoryName });
        setEditForm({
            price: (item.price / 100).toFixed(2),
            available: item.available,
            kitchen: item.kitchen || false,
            name: item.name,
            category: categoryName,
            sideSources: item.sideSources ? [...item.sideSources] : [],
        });
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
            categories[key]?.find(item => item.baseItemId === id)
        );
    };

    const handleDragStart = (event) => {
        const { active } = event;
        setActiveId(active.id);
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
        if (!over || active.id === over.id || activeDragType !== 'ITEM') return;

        const activeContainer = findContainer(active.id);
        const overContainer = findContainer(over.id) || (over.id in categories ? over.id : null);

        if (!activeContainer || !overContainer || activeContainer === overContainer) return;

        setCategories((prev) => {
            const activeItems = prev[activeContainer];
            const overItems = prev[overContainer];
            const activeIndex = activeItems.findIndex(i => i.baseItemId === active.id);
            const overIndex = overItems.findIndex(i => i.baseItemId === over.id);

            let newIndex;
            if (over.id in prev) {
                newIndex = overItems.length + 1;
            } else {
                const isBelowOverItem =
                    over &&
                    active.rect.current.translated &&
                    active.rect.current.translated.top > over.rect.top + over.rect.height;
                const modifier = isBelowOverItem ? 1 : 0;
                newIndex = overIndex >= 0 ? overIndex + modifier : overItems.length + 1;
            }

            return {
                ...prev,
                [activeContainer]: prev[activeContainer].filter(item => item.baseItemId !== active.id),
                [overContainer]: [
                    ...prev[overContainer].slice(0, newIndex),
                    activeItems[activeIndex],
                    ...prev[overContainer].slice(newIndex)
                ]
            };
        });
    };

    const handleDragEnd = async (event) => {
        const { active, over } = event;
        const type = activeDragType;
        setActiveId(null);
        setActiveDragType(null);

        if (!over) return;

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

        if (type === 'ITEM') {
            const activeContainer = findContainer(active.id);
            const overContainer = findContainer(over.id) || (over.id in categories ? over.id : null);

            if (activeContainer && overContainer) {
                const activeIndex = categories[activeContainer].findIndex(i => i.baseItemId === active.id);
                const overIndex = over.id in categories
                    ? categories[overContainer].length + 1
                    : categories[overContainer].findIndex(i => i.baseItemId === over.id);

                if (activeContainer === overContainer) {
                    if (activeIndex !== overIndex && activeIndex !== -1 && overIndex !== -1) {
                        const newItems = arrayMove(categories[activeContainer], activeIndex, overIndex);
                        setCategories(prev => ({ ...prev, [activeContainer]: newItems }));
                        try {
                            await api.put(`/menu/categories/${activeContainer}/items/reorder`, {
                                order: newItems.map(i => i.baseItemId)
                            });
                        } catch (e) {
                            toast.error("Failed to reorder items");
                            fetchMenu();
                        }
                    }
                } else {
                    const originalCategory = active.data.current?.category;
                    const currentCategory = activeContainer;
                    if (originalCategory && currentCategory && originalCategory !== currentCategory) {
                        try {
                            await api.put(`/menu/categories/${encodeURIComponent(originalCategory)}/items/${active.id}/category`, { newCategory: currentCategory });
                            const newItems = categories[currentCategory];
                            await api.put(`/menu/categories/${currentCategory}/items/reorder`, {
                                order: newItems.map(i => i.baseItemId)
                            });
                        } catch (e) {
                            toast.error("Failed to save move");
                            fetchMenu();
                        }
                    } else {
                        const newItems = categories[activeContainer];
                        try {
                            await api.put(`/menu/categories/${activeContainer}/items/reorder`, {
                                order: newItems.map(i => i.baseItemId)
                            });
                        } catch (e) {
                            // ignore trivial reorder error
                        }
                    }
                }
            }
        }
    };

    const handleCategoryDelete = async () => {
        confirmAction.current = async () => {
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
            const itemId = editItem.baseItemId;
            let currentCategory = editItem.originalCategory;

            const newPriceCents = Math.round(parseFloat(editForm.price) * 100);
            const ops = [];
            if (newPriceCents !== editItem.price)
                ops.push(api.put(`/menu/items/${itemId}/price`, { newPrice: newPriceCents }));
            if (editForm.available !== editItem.available)
                ops.push(api.put(`/menu/items/${itemId}/availability`, { available: editForm.available }));
            if (editForm.kitchen !== (editItem.kitchen || false))
                ops.push(api.put(`/menu/items/${itemId}/kitchen`, { kitchen: editForm.kitchen }));
            if (editForm.name !== editItem.name)
                ops.push(api.put(`/menu/items/${itemId}/rename`, { newName: editForm.name }));

            await Promise.all(ops);

            // Category change (sequential — affects path for side-sources call below)
            if (editForm.category !== editItem.originalCategory) {
                await api.put(`/menu/categories/${encodeURIComponent(currentCategory)}/items/${itemId}/category`, { newCategory: editForm.category });
                currentCategory = editForm.category;
            }

            // Side sources change
            const originalSources = [...(editItem.sideSources || [])].sort();
            const newSources = [...(editForm.sideSources || [])].sort();
            if (JSON.stringify(originalSources) !== JSON.stringify(newSources)) {
                await api.put(`/menu/categories/${encodeURIComponent(currentCategory)}/items/${itemId}/side-sources`, { sideSources: editForm.sideSources });
            }

            setIsEditModalOpen(false);
            fetchMenu();
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
                kitchen: isKitchenItem,
                sideSources: newItemSideSources,
            });
            setIsCreateModalOpen(false);
            setNewItem({ name: '', category: '', price: '' });
            setIsKitchenItem(false);
            setIsCreatingNewCategory(false);
            setNewItemSideSources([]);
            fetchMenu();
            toast.success('Item created successfully');
        } catch (error) {
            toast.error('Failed to create item: ' + error.message);
        }
    };

    const handleDelete = async () => {
        confirmAction.current = async () => {
            try {
                await api.delete(`/menu/items/${editItem.baseItemId}`);
                setIsEditModalOpen(false);
                fetchMenu();
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

    const toggleSideSourceInEditForm = (catName, checked) => {
        setEditForm(prev => ({
            ...prev,
            sideSources: checked
                ? [...prev.sideSources, catName]
                : prev.sideSources.filter(s => s !== catName)
        }));
    };

    const toggleNewItemSideSource = (catName, checked) => {
        setNewItemSideSources(prev =>
            checked ? [...prev, catName] : prev.filter(s => s !== catName)
        );
    };

    // ── Combo management ───────────────────────────────────────────────────────

    const handleCreateCombo = async (e) => {
        e.preventDefault();
        try {
            await api.post('/menu/combos', {
                name: newComboForm.name,
                category: newComboForm.category,
                componentIds: newComboComponentIds,
                slots: newComboSlots.map(s => ({
                    name: s.name,
                    optionIds: s.optionIds,
                    required: s.required,
                })),
                price: newComboForm.price ? Math.round(parseFloat(newComboForm.price) * 100) : null,
                kitchen: newComboForm.kitchen,
            });
            setIsCreateComboModalOpen(false);
            setNewComboForm({ name: '', category: '', price: '', kitchen: false });
            setNewComboComponentIds([]);
            setNewComboSlots([]);
            setIsCreatingComboCat(false);
            fetchMenu();
            toast.success('Combo created successfully');
        } catch (e) {
            toast.error('Failed to create combo: ' + e.message);
        }
    };

    const handleEditComboClick = (combo) => {
        setEditComboData(combo);
        setEditComboForm({
            name: combo.name,
            price: combo.price != null ? (combo.price / 100).toFixed(2) : '',
            available: combo.available,
            kitchen: combo.kitchen,
        });
        setIsEditComboModalOpen(true);
    };

    const handleSaveEditCombo = async () => {
        try {
            await api.put(`/menu/combos/${editComboData.id}`, {
                name: editComboForm.name,
                price: editComboForm.price ? Math.round(parseFloat(editComboForm.price) * 100) : null,
                available: editComboForm.available,
                kitchen: editComboForm.kitchen,
            });
            setIsEditComboModalOpen(false);
            fetchMenu();
            toast.success('Combo updated successfully');
        } catch (e) {
            toast.error('Failed to update combo: ' + e.message);
        }
    };

    const handleDeleteCombo = () => {
        confirmAction.current = async () => {
            try {
                await api.delete(`/menu/combos/${editComboData.id}`);
                setIsEditComboModalOpen(false);
                fetchMenu();
                toast.success('Combo deleted successfully');
            } catch (e) {
                toast.error(e.message);
            }
        };
        setConfirmationModal({
            isOpen: true,
            title: 'Delete Combo',
            message: `Are you sure you want to delete combo "${editComboData.name}"?`,
        });
    };

    const addNewSlot = () => {
        setNewComboSlots(prev => [...prev, { tempId: Date.now(), name: '', required: false, optionIds: [] }]);
    };

    const removeNewSlot = (tempId) => {
        setNewComboSlots(prev => prev.filter(s => s.tempId !== tempId));
    };

    const updateNewSlot = (tempId, updates) => {
        setNewComboSlots(prev => prev.map(s => s.tempId === tempId ? { ...s, ...updates } : s));
    };

    const toggleComboComponent = (itemId) => {
        setNewComboComponentIds(prev =>
            prev.includes(itemId) ? prev.filter(id => id !== itemId) : [...prev, itemId]
        );
    };

    const toggleSlotOption = (slotTempId, itemId) => {
        setNewComboSlots(prev => prev.map(s => {
            if (s.tempId !== slotTempId) return s;
            const already = s.optionIds.includes(itemId);
            return { ...s, optionIds: already ? s.optionIds.filter(id => id !== itemId) : [...s.optionIds, itemId] };
        }));
    };

    return (
        <div className="menu-page">
            <div className="menu-header">
                <h1>Menu Management</h1>
                <div style={{ display: 'flex', gap: 'var(--spacing-sm)' }}>
                    <Button variant="secondary" onClick={() => {
                        setIsCreateComboModalOpen(true);
                        setIsCreatingComboCat(false);
                        setNewComboForm({ name: '', category: '', price: '', kitchen: false });
                        setNewComboComponentIds([]);
                        setNewComboSlots([]);
                    }}>+ Add Combo</Button>
                    <Button onClick={() => {
                        setIsCreateModalOpen(true);
                        setIsCreatingNewCategory(false);
                        setIsKitchenItem(false);
                        setNewItemSideSources([]);
                    }}>+ Add Item</Button>
                </div>
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
                                    <div className="menu-item-card" style={{ height: '100px', background: 'white', border: '1px solid #ccc' }}>
                                        Dragging...
                                    </div>
                                </div>
                            ) : null
                        ) : null}
                    </DragOverlay>
                </DndContext>
            )}

            {/* Combos Section */}
            {!loading && combos.length > 0 && (
                <div className="combos-section">
                    <div className="combos-section-header">
                        <h2 className="combos-section-title">Combos</h2>
                    </div>
                    <div className="combos-grid">
                        {combos.map(combo => (
                            <div
                                key={combo.id}
                                className="combo-card"
                                onClick={() => handleEditComboClick(combo)}
                            >
                                <div className="combo-card-top">
                                    <span className="combo-card-name">{combo.name}</span>
                                    {!combo.available && <span className="combo-unavailable-badge">Off</span>}
                                    {combo.kitchen && <span className="combo-kitchen-badge">Kitchen</span>}
                                </div>
                                {combo.components?.length > 0 && (
                                    <div className="combo-card-components">
                                        {combo.components.map(c => c.name).join(' + ')}
                                    </div>
                                )}
                                {combo.slots?.length > 0 && (
                                    <div className="combo-card-slots">
                                        {combo.slots.map(s => (
                                            <span key={s.id} className="combo-slot-tag">
                                                {s.name}{s.required ? '*' : ''}
                                            </span>
                                        ))}
                                    </div>
                                )}
                                {combo.price != null && (
                                    <div className="combo-card-price">${(combo.price / 100).toFixed(2)}</div>
                                )}
                            </div>
                        ))}
                    </div>
                </div>
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
                    <select
                        value={editForm.category}
                        onChange={e => setEditForm({ ...editForm, category: e.target.value })}
                        className="form-select"
                    >
                        {categoryOrder.map(cat => (
                            <option key={cat} value={cat}>{cat}</option>
                        ))}
                    </select>
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
                            checked={editForm.kitchen || false}
                            onChange={e => setEditForm({ ...editForm, kitchen: e.target.checked })}
                        />
                        Kitchen Item
                    </label>
                </div>
                {categoryOrder.length > 0 && (
                    <div className="form-group">
                        <label>Side Sources</label>
                        <div className="side-sources-list">
                            {categoryOrder.map(cat => (
                                <label key={cat} className="side-source-checkbox">
                                    <input
                                        type="checkbox"
                                        checked={editForm.sideSources.includes(cat)}
                                        onChange={e => toggleSideSourceInEditForm(cat, e.target.checked)}
                                    />
                                    {cat}
                                </label>
                            ))}
                        </div>
                    </div>
                )}
            </Modal>

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
            <Modal
                isOpen={isCreateModalOpen}
                onClose={() => {
                    setIsCreateModalOpen(false);
                    setIsCreatingNewCategory(false);
                }}
                title="Create New Item"
                footer={
                    <>
                        <Button variant="secondary" onClick={() => {
                            setIsCreateModalOpen(false);
                            setIsCreatingNewCategory(false);
                        }}>Cancel</Button>
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
                        {isCreatingNewCategory ? (
                            <div className="category-input-group">
                                <input
                                    value={newItem.category}
                                    onChange={e => setNewItem({ ...newItem, category: e.target.value })}
                                    required
                                    placeholder="New Category Name"
                                    autoFocus
                                />
                                <Button
                                    type="button"
                                    variant="secondary"
                                    onClick={() => {
                                        setIsCreatingNewCategory(false);
                                        setNewItem({ ...newItem, category: '' });
                                    }}
                                    style={{ marginLeft: '8px' }}
                                >
                                    Cancel
                                </Button>
                            </div>
                        ) : (
                            <select
                                value={newItem.category}
                                onChange={e => {
                                    if (e.target.value === '__NEW__') {
                                        setIsCreatingNewCategory(true);
                                        setNewItem({ ...newItem, category: '' });
                                    } else {
                                        setNewItem({ ...newItem, category: e.target.value });
                                    }
                                }}
                                required
                                className="form-select"
                            >
                                <option value="" disabled>Select a category</option>
                                {Object.keys(categories).map(cat => (
                                    <option key={cat} value={cat}>{cat}</option>
                                ))}
                                <option value="__NEW__">+ Create New Category...</option>
                            </select>
                        )}
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
                    {categoryOrder.length > 0 && (
                        <div className="form-group">
                            <label>Side Sources</label>
                            <div className="side-sources-list">
                                {categoryOrder.map(cat => (
                                    <label key={cat} className="side-source-checkbox">
                                        <input
                                            type="checkbox"
                                            checked={newItemSideSources.includes(cat)}
                                            onChange={e => toggleNewItemSideSource(cat, e.target.checked)}
                                        />
                                        {cat}
                                    </label>
                                ))}
                            </div>
                        </div>
                    )}
                </form>
            </Modal>

            {/* Create Combo Modal */}
            <Modal
                isOpen={isCreateComboModalOpen}
                onClose={() => setIsCreateComboModalOpen(false)}
                title="Create Combo"
                footer={
                    <>
                        <Button variant="secondary" onClick={() => setIsCreateComboModalOpen(false)}>Cancel</Button>
                        <Button variant="primary" onClick={handleCreateCombo}>Create</Button>
                    </>
                }
            >
                <form onSubmit={handleCreateCombo}>
                    <div className="form-group">
                        <label>Name</label>
                        <input
                            value={newComboForm.name}
                            onChange={e => setNewComboForm({ ...newComboForm, name: e.target.value })}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Category</label>
                        {isCreatingComboCat ? (
                            <div className="category-input-group">
                                <input
                                    value={newComboForm.category}
                                    onChange={e => setNewComboForm({ ...newComboForm, category: e.target.value })}
                                    required
                                    placeholder="New Category Name"
                                    autoFocus
                                />
                                <Button type="button" variant="secondary" onClick={() => { setIsCreatingComboCat(false); setNewComboForm({ ...newComboForm, category: '' }); }} style={{ marginLeft: '8px' }}>Cancel</Button>
                            </div>
                        ) : (
                            <select
                                value={newComboForm.category}
                                onChange={e => {
                                    if (e.target.value === '__NEW__') {
                                        setIsCreatingComboCat(true);
                                        setNewComboForm({ ...newComboForm, category: '' });
                                    } else {
                                        setNewComboForm({ ...newComboForm, category: e.target.value });
                                    }
                                }}
                                required
                                className="form-select"
                            >
                                <option value="" disabled>Select a category</option>
                                {categoryOrder.map(cat => (
                                    <option key={cat} value={cat}>{cat}</option>
                                ))}
                                <option value="__NEW__">+ Create New Category...</option>
                            </select>
                        )}
                    </div>
                    <div className="form-group">
                        <label>Price ($) <span style={{ fontWeight: 'normal', color: 'var(--color-text-muted)', fontSize: '0.8rem' }}>(optional — computed from components if blank)</span></label>
                        <input
                            type="number"
                            step="0.01"
                            value={newComboForm.price}
                            onChange={e => setNewComboForm({ ...newComboForm, price: e.target.value })}
                            placeholder="e.g. 9.99"
                        />
                    </div>
                    <div className="form-group checkbox-group">
                        <label>
                            <input
                                type="checkbox"
                                checked={newComboForm.kitchen}
                                onChange={e => setNewComboForm({ ...newComboForm, kitchen: e.target.checked })}
                            />
                            Kitchen Item
                        </label>
                    </div>
                    <div className="form-group">
                        <label>Fixed Components</label>
                        <ItemPicker
                            items={baseItems}
                            selectedIds={newComboComponentIds}
                            onToggle={toggleComboComponent}
                        />
                    </div>
                    <div className="form-group">
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--spacing-sm)' }}>
                            <label style={{ margin: 0 }}>Choice Slots</label>
                            <Button type="button" variant="secondary" onClick={addNewSlot} style={{ padding: '4px 10px', fontSize: '0.85rem' }}>+ Add Slot</Button>
                        </div>
                        {newComboSlots.map(slot => (
                            <div key={slot.tempId} className="slot-builder">
                                <div className="slot-builder-header">
                                    <input
                                        className="slot-name-input"
                                        placeholder="Slot name (e.g., Drink)"
                                        value={slot.name}
                                        onChange={e => updateNewSlot(slot.tempId, { name: e.target.value })}
                                    />
                                    <label className="slot-required-label">
                                        <input
                                            type="checkbox"
                                            checked={slot.required}
                                            onChange={e => updateNewSlot(slot.tempId, { required: e.target.checked })}
                                        />
                                        Required
                                    </label>
                                    <button type="button" className="slot-remove-btn" onClick={() => removeNewSlot(slot.tempId)}>✕</button>
                                </div>
                                <ItemPicker
                                    items={baseItems}
                                    selectedIds={slot.optionIds}
                                    onToggle={(id) => toggleSlotOption(slot.tempId, id)}
                                />
                            </div>
                        ))}
                        {newComboSlots.length === 0 && (
                            <div className="combo-slots-empty">No slots yet. Add slots for customer choices (e.g., Drink, Size).</div>
                        )}
                    </div>
                </form>
            </Modal>

            {/* Edit Combo Modal */}
            <Modal
                isOpen={isEditComboModalOpen}
                onClose={() => setIsEditComboModalOpen(false)}
                title="Edit Combo"
                footer={
                    <div className="edit-footer">
                        <Button variant="danger" onClick={handleDeleteCombo}>Delete</Button>
                        <div className="edit-actions-right">
                            <Button variant="secondary" onClick={() => setIsEditComboModalOpen(false)}>Cancel</Button>
                            <Button variant="primary" onClick={handleSaveEditCombo}>Save</Button>
                        </div>
                    </div>
                }
            >
                <div className="form-group">
                    <label>Name</label>
                    <input
                        value={editComboForm.name}
                        onChange={e => setEditComboForm({ ...editComboForm, name: e.target.value })}
                    />
                </div>
                <div className="form-group">
                    <label>Price ($) <span style={{ fontWeight: 'normal', color: 'var(--color-text-muted)', fontSize: '0.8rem' }}>(leave blank to compute from components)</span></label>
                    <input
                        type="number"
                        step="0.01"
                        value={editComboForm.price}
                        onChange={e => setEditComboForm({ ...editComboForm, price: e.target.value })}
                        placeholder="Computed from components"
                    />
                </div>
                <div className="form-group checkbox-group">
                    <label>
                        <input
                            type="checkbox"
                            checked={editComboForm.available}
                            onChange={e => setEditComboForm({ ...editComboForm, available: e.target.checked })}
                        />
                        Available
                    </label>
                </div>
                <div className="form-group checkbox-group">
                    <label>
                        <input
                            type="checkbox"
                            checked={editComboForm.kitchen}
                            onChange={e => setEditComboForm({ ...editComboForm, kitchen: e.target.checked })}
                        />
                        Kitchen Item
                    </label>
                </div>
                {editComboData?.components?.length > 0 && (
                    <div className="form-group">
                        <label>Components</label>
                        <div className="combo-info-chips">
                            {editComboData.components.map(c => (
                                <span key={c.id} className="combo-info-chip">{c.name}</span>
                            ))}
                        </div>
                    </div>
                )}
                {editComboData?.slots?.length > 0 && (
                    <div className="form-group">
                        <label>Slots</label>
                        <div className="combo-info-chips">
                            {editComboData.slots.map(s => (
                                <span key={s.id} className="combo-info-chip">{s.name}{s.required ? '*' : ''}</span>
                            ))}
                        </div>
                    </div>
                )}
            </Modal>

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
            </Modal>
        </div>
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
        data: { type: 'CATEGORY', category }
    });

    const style = {
        transform: CSS.Transform.toString(transform),
        transition,
    };

    return (
        <div ref={setNodeRef} style={style} className="menu-category-section">
            <div className="menu-category-header" {...attributes} {...listeners}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ cursor: 'grab' }}>:::</span>
                    <h3 className="menu-category-title">{category}</h3>
                </div>
                <button
                    className="category-edit-btn"
                    onClick={(e) => {
                        e.stopPropagation();
                        onEditCategory(category);
                    }}
                    onPointerDown={(e) => e.stopPropagation()}
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
                items={items.map(i => i.baseItemId)}
                strategy={rectSortingStrategy}
            >
                <div className="menu-grid">
                    {items.map(item => (
                        <SortableItem
                            key={item.baseItemId}
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
        id: item.baseItemId,
        data: { type: 'ITEM', item, category }
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

const ItemPicker = ({ items, selectedIds, onToggle }) => {
    const [search, setSearch] = useState('');
    const filtered = search
        ? items.filter(i => i.name.toLowerCase().includes(search.toLowerCase()))
        : items;
    return (
        <div className="item-picker">
            <input
                className="item-picker-search"
                placeholder="Search items..."
                value={search}
                onChange={e => setSearch(e.target.value)}
            />
            <div className="item-picker-list">
                {filtered.map(item => (
                    <label
                        key={item.id}
                        className={`item-picker-row ${selectedIds.includes(item.id) ? 'item-picker-selected' : ''}`}
                    >
                        <input
                            type="checkbox"
                            checked={selectedIds.includes(item.id)}
                            onChange={() => onToggle(item.id)}
                        />
                        <span className="item-picker-name">{item.name}</span>
                        <span className="item-picker-price">${(item.price / 100).toFixed(2)}</span>
                    </label>
                ))}
                {filtered.length === 0 && (
                    <div className="item-picker-empty">No items found</div>
                )}
            </div>
        </div>
    );
};

export default Menu;
