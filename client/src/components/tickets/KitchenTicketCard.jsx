import React from 'react';
import Card from '../common/Card';
import Button from '../common/Button';
import './KitchenTicketCard.css';

/**
 * Kitchen-view ticket card showing:
 * - Table number prominently
 * - Ticket comment banner (if present)
 * - Kitchen tally summary
 * - Order groups with optional order comment headers
 * - Items with optional item comments
 * - Complete button
 */
const KitchenTicketCard = ({ ticket, onComplete, isHighlighted }) => {
    const time = new Date(ticket.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    // Format tally entries, e.g. "2x Burger"
    const tallyEntries = Object.entries(ticket.kitchenTally || {}).map(([name, count]) => (
        <span key={name} className="tally-item">{count}× {name}</span>
    ));

    return (
        <Card className={`kitchen-ticket-card ${isHighlighted ? 'kitchen-ticket-highlight' : ''}`}>
            <div className="kitchen-header">
                <span className="kitchen-table">Table {ticket.tableNumber}</span>
                <span className="kitchen-time">{time}</span>
            </div>

            {ticket.comment && (
                <div className="ticket-comment-banner">{ticket.comment}</div>
            )}

            {tallyEntries.length > 0 && (
                <div className="kitchen-tally">
                    <span className="tally-label">Tally:</span>
                    <div className="tally-items">{tallyEntries}</div>
                </div>
            )}

            <div className="kitchen-orders-list">
                {(ticket.kitchenOrders || []).map((orderGroup, oIdx) => (
                    <div key={oIdx} className="kitchen-order-group">
                        {orderGroup.comment && (
                            <div className="order-comment-banner">{orderGroup.comment}</div>
                        )}
                        <ul className="kitchen-items-list">
                            {(orderGroup.items || []).map((item, idx) => (
                                <li key={idx} className="kitchen-item">
                                    {item.quantity > 1 && (
                                        <span className="item-quantity">{item.quantity}× </span>
                                    )}
                                    <span className="item-name">{item.name}</span>
                                    {item.selectedSide && item.selectedSide !== 'none' && (
                                        <span className="item-side"> + {item.selectedSide}</span>
                                    )}
                                    {item.selectedExtra && item.selectedExtra !== 'none' && (
                                        <span className="item-extra"> + {item.selectedExtra}</span>
                                    )}
                                    {item.comment && (
                                        <div className="item-comment">{item.comment}</div>
                                    )}
                                </li>
                            ))}
                        </ul>
                    </div>
                ))}
            </div>

            <div className="kitchen-actions">
                <Button variant="success" onClick={(e) => { e.stopPropagation(); onComplete(ticket.id); }}>
                    Complete
                </Button>
            </div>
        </Card>
    );
};

export default KitchenTicketCard;
