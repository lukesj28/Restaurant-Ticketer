import React, { useState, useEffect } from 'react';
import { api } from '../api/api';
import MenuItemCard from '../components/menu/MenuItemCard';
import Button from '../components/common/Button';
import Modal from '../components/common/Modal';
import './Menu.css';

const Menu = () => {
    const [categories, setCategories] = useState({});
    const [loading, setLoading] = useState(true);
    const [editItem, setEditItem] = useState(null); // Item being edited
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);

    // Create Mode
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [newItem, setNewItem] = useState({ name: '', category: '', price: '' });

    // Edit Form State
    const [editForm, setEditForm] = useState({ price: '', available: true, name: '', category: '' });

    useEffect(() => {
        fetchMenu();
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

    const handleEditClick = (item, categoryName) => {
        setEditItem({ ...item, originalName: item.name, originalCategory: categoryName });
        setEditForm({
            price: (item.price / 100).toFixed(2),
            available: item.available,
            name: item.name,
            category: categoryName,
            sides: item.sides ? JSON.parse(JSON.stringify(item.sides)) : {} // Deep copy to avoid mutating original ref
        });
        setIsEditModalOpen(true);
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

            // Sides
            if (editForm.sides) {
                for (const [sideName, sideData] of Object.entries(editForm.sides)) {
                    const originalSide = editItem.sides && editItem.sides[sideName];
                    if (!originalSide || originalSide.price !== sideData.price || originalSide.available !== sideData.available) {
                        await api.put(`/menu/items/${currentName}/sides/${sideName}`, {
                            price: sideData.price,
                            available: sideData.available
                        });
                    }
                }
            }

            setIsEditModalOpen(false);
            fetchMenu();
        } catch (e) {
            alert('Failed to update item: ' + e.message);
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
            setIsCreateModalOpen(false);
            setNewItem({ name: '', category: '', price: '' });
            fetchMenu();
        } catch (error) {
            alert('Failed to create item: ' + error.message);
        }
    };

    const handleDelete = async () => {
        if (!window.confirm(`Are you sure you want to delete ${editItem.name}?`)) return;
        try {
            await api.delete(`/menu/items/${editItem.originalName}`);
            setIsEditModalOpen(false);
            fetchMenu();
        } catch (e) {
            alert(e.message);
        }
    };

    return (
        <div className="menu-page">
            <div className="menu-header">
                <h1>Menu Management</h1>
                <Button onClick={() => setIsCreateModalOpen(true)}>+ Add Item</Button>
            </div>

            {loading ? <div>Loading...</div> : (
                <div className="menu-list">
                    {Object.entries(categories).map(([category, items]) => (
                        <div key={category} className="menu-category-section">
                            <h3 className="menu-category-title">{category} ({items.length})</h3>
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

                {/* Sides Editing */}
                {
                    editForm.sides && Object.keys(editForm.sides).length > 0 && (
                        <div className="sides-section">
                            <h4>Sides</h4>
                            <div className="sides-grid">
                                {Object.entries(editForm.sides).map(([sideName, sideData]) => (
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
                                    </div>
                                ))}
                            </div>
                        </div>
                    )
                }
            </Modal >

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
                </form>
            </Modal >
        </div >
    );
};

export default Menu;
