import React, { useState, useEffect, useRef } from 'react';
import { api } from '../api/api';
import MenuItemCard from '../components/menu/MenuItemCard';
import Button from '../components/common/Button';
import Modal from '../components/common/Modal';
import { useToast } from '../context/ToastContext';
import './Menu.css';

const Menu = () => {
    const { toast } = useToast();
    const [categories, setCategories] = useState({});
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

    useEffect(() => {
        fetchMenu();
        fetchKitchenItems();
    }, []);

    const fetchMenu = async () => {
        setLoading(true);
        try {
            const data = await api.get('/menu/categories');
            setCategories(data);
        } catch (e) {
            console.error(e);
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
            sides: item.sides ? JSON.parse(JSON.stringify(item.sides)) : {} // Deep copy to avoid mutating original ref
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
                <div className="menu-list">
                    {Object.entries(categories).map(([category, items]) => (
                        <div key={category} className="menu-category-section">
                            <div className="menu-category-header">
                                <h3 className="menu-category-title">{category}</h3>
                                <button
                                    className="category-edit-btn"
                                    onClick={() => handleEditCategoryClick(category)}
                                    aria-label={`Edit ${category} category`}
                                >
                                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                    </svg>
                                </button>
                            </div>
                            <div className="menu-grid">
                                {items.map(item => (
                                    <MenuItemCard
                                        key={item.name}
                                        item={item}
                                        onEdit={(i) => handleEditClick(i, category)}
                                    />
                                ))}
                            </div>
                        </div>
                    ))}
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
                        {/* Existing sides (filter out 'none') */}
                        {editForm.sides && Object.entries(editForm.sides)
                            .filter(([sideName]) => sideName.toLowerCase() !== 'none' && !sidesToDelete.includes(sideName))
                            .map(([sideName, sideData]) => (
                                <div key={sideName} className="side-edit-row">
                                    <span className="side-name">{sideName}</span>
                                    <input
                                        type="number"
                                        step="0.01"
                                        className="side-price-input"
                                        value={sideData.price !== undefined ? (sideData.price / 100).toFixed(2) : ''}
                                        onChange={e => {
                                            const newPrice = e.target.value;
                                            setEditForm(prev => ({
                                                ...prev,
                                                sides: {
                                                    ...prev.sides,
                                                    [sideName]: { ...prev.sides[sideName], price: Math.round(parseFloat(newPrice) * 100) }
                                                }
                                            }));
                                        }}
                                    />
                                    <label className="side-avail-label">
                                        <input
                                            type="checkbox"
                                            checked={sideData.available}
                                            onChange={e => {
                                                const newAvail = e.target.checked;
                                                setEditForm(prev => ({
                                                    ...prev,
                                                    sides: {
                                                        ...prev.sides,
                                                        [sideName]: { ...prev.sides[sideName], available: newAvail }
                                                    }
                                                }));
                                            }}
                                        />
                                        Avail
                                    </label>
                                    <button
                                        type="button"
                                        className="side-delete-btn"
                                        onClick={() => setSidesToDelete(prev => [...prev, sideName])}
                                        aria-label={`Delete ${sideName}`}
                                    >
                                        ×
                                    </button>
                                </div>
                            ))}
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

export default Menu;
