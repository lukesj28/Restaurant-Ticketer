import React from 'react';
import Card from '../common/Card';
import Button from '../common/Button';
import './MenuItemCard.css';

const MenuItemCard = ({ item, onEdit, onDelete }) => {
    return (
        <Card className={`menu-item-card ${!item.available ? 'unavailable' : ''}`}>
            <div className="menu-item-content">
                <div className="menu-item-top">
                    <span className="menu-item-name">{item.name}</span>
                    <span className="menu-item-price">${(item.price / 100).toFixed(2)}</span>
                </div>
                <div className="menu-item-status">
                    {item.available ? 'Available' : 'Unavailable'}
                </div>
            </div>
            <div className="menu-item-actions">
                <Button size="small" variant="secondary" onClick={() => onEdit(item)}>Edit</Button>
                {/* Delete is dangerous, maybe distinct button or inside edit? For now, button. */}
            </div>
        </Card>
    );
};

export default MenuItemCard;
