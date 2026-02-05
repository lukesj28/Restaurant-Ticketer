import React from 'react';
import Card from '../common/Card';
import './TicketCard.css';

const TicketCard = ({ ticket, onClick }) => {
    const itemCount = ticket.orders ? ticket.orders.reduce((acc, order) => acc + order.items.length, 0) : 0;
    // Parse timestamp to readable time
    const time = new Date(ticket.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    return (
        <Card className="ticket-card" onClick={() => onClick(ticket)}>
            <div className="ticket-header">
                <span className="ticket-table">Table {ticket.tableNumber}</span>
                <span className="ticket-id">#{ticket.id}</span>
            </div>
            <div className="ticket-body">
                <div className="ticket-info">
                    <span>{itemCount} Items</span>
                    <span className="ticket-time">{time}</span>
                </div>
                <div className="ticket-total">
                    ${(ticket.total / 100).toFixed(2)}
                </div>
            </div>
            <div className="ticket-status-indicator">
                {/* Could add status color strip if needed, though active/completed separation handles this */}
            </div>
        </Card>
    );
};

export default TicketCard;
