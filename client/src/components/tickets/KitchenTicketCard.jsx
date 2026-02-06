import React from 'react';
import Card from '../common/Card';
import Button from '../common/Button';
import './KitchenTicketCard.css';

/**
 * Kitchen-view ticket card showing:
 * - Table number prominently
 * - Kitchen tally summary
 * - Stacked kitchen items sorted by category (no order grouping)
 * - Complete button
 */
const KitchenTicketCard = ({ ticket, onComplete }) => {
    const time = new Date(ticket.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    // Format tally entries, e.g. "2× Burger"
    const tallyEntries = Object.entries(ticket.kitchenTally || {}).map(([name, count]) => (
        <span key={name} className="tally-item">{count}× {name}</span>
    ));

    return (
        <Card className="kitchen-ticket-card">
            <div className="kitchen-header">
                <span className="kitchen-table">Table {ticket.tableNumber}</span>
                <span className="kitchen-time">{time}</span>
            </div>

            {tallyEntries.length > 0 && (
                <div className="kitchen-tally">
                    <span className="tally-label">Tally:</span>
                    <div className="tally-items">{tallyEntries}</div>
                </div>
            )}

            <ul className="kitchen-items-list">
                {(ticket.kitchenItems || []).map((item, idx) => (
                    <li key={idx} className="kitchen-item">
                        {item.quantity > 1 && (
                            <span className="item-quantity">{item.quantity}× </span>
                        )}
                        <span className="item-name">{item.name}</span>
                        {item.selectedSide && item.selectedSide !== 'none' && (
                            <span className="item-side"> + {item.selectedSide}</span>
                        )}
                    </li>
                ))}
            </ul>

            <div className="kitchen-actions">
                <Button variant="success" onClick={(e) => { e.stopPropagation(); onComplete(ticket.id); }}>
                    Complete
                </Button>
            </div>
        </Card>
    );
};

export default KitchenTicketCard;

