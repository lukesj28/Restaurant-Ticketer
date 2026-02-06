import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/api';
import TicketCard from '../components/tickets/TicketCard';
import Button from '../components/common/Button';
import Modal from '../components/common/Modal';
import { useToast } from '../context/ToastContext';
import './Tickets.css';

const Tickets = () => {
    const navigate = useNavigate();
    const { toast } = useToast();
    const [activeTab, setActiveTab] = useState('active');
    const [tickets, setTickets] = useState([]);
    const [loading, setLoading] = useState(false);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [newTableNumber, setNewTableNumber] = useState('');

    useEffect(() => {
        fetchTickets();

        // Poll for active tickets to keep UI fresh
        // Only poll if on active tab
        let interval;
        if (activeTab === 'active') {
            interval = setInterval(fetchTickets, 5000);
        }
        return () => clearInterval(interval);
    }, [activeTab]);

    const fetchTickets = async () => {
        setLoading(true);
        try {
            let endpoint = '/tickets/active';
            if (activeTab === 'completed') endpoint = '/tickets/completed';
            if (activeTab === 'closed') endpoint = '/tickets/closed';

            const data = await api.get(endpoint);
            // sort by ID descending for newest first
            const sorted = (data || []).sort((a, b) => b.id - a.id);
            setTickets(sorted);
        } catch (error) {
            console.error("Failed to fetch tickets:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleCreateTicket = async (e) => {
        e.preventDefault();
        try {
            await api.post('/tickets', { tableNumber: newTableNumber });
            setNewTableNumber('');
            setIsCreateModalOpen(false);
            fetchTickets();
        } catch (error) {
            toast.error('Failed to create ticket: ' + error.message);
        }
    };

    const handleTicketClick = (ticket) => {
        navigate(`/tickets/${ticket.id}`);
    };

    return (
        <div className="tickets-page">
            <div className="tickets-header">
                <div className="tabs">
                    <button
                        className={`tab-btn ${activeTab === 'active' ? 'active' : ''}`}
                        onClick={() => setActiveTab('active')}
                    >
                        Active
                    </button>
                    <button
                        className={`tab-btn ${activeTab === 'completed' ? 'completed' : ''}`} // logic naming
                        onClick={() => setActiveTab('completed')}
                    >
                        Completed
                    </button>
                    <button
                        className={`tab-btn ${activeTab === 'closed' ? 'active' : ''}`} // reusing active class for simplicity or custom
                        onClick={() => setActiveTab('closed')}
                    >
                        Closed
                    </button>
                </div>
                {activeTab === 'active' && (
                    <Button onClick={() => setIsCreateModalOpen(true)}>
                        + New Ticket
                    </Button>
                )}
            </div>

            <div className="tickets-grid">
                {tickets.map(ticket => (
                    <TicketCard key={ticket.id} ticket={ticket} onClick={handleTicketClick} />
                ))}
                {tickets.length === 0 && !loading && (
                    <div className="empty-state">No {activeTab} tickets found.</div>
                )}
            </div>

            <Modal
                isOpen={isCreateModalOpen}
                onClose={() => setIsCreateModalOpen(false)}
                title="New Ticket"
                footer={
                    <>
                        <Button variant="secondary" onClick={() => setIsCreateModalOpen(false)}>Cancel</Button>
                        <Button variant="primary" onClick={handleCreateTicket}>Create</Button>
                    </>
                }
            >
                <form onSubmit={handleCreateTicket}>
                    <div className="form-group">
                        <label>Table Number</label>
                        <input
                            type="text"
                            value={newTableNumber}
                            onChange={(e) => setNewTableNumber(e.target.value)}
                            placeholder="e.g. 12"
                            autoFocus
                            required
                        />
                    </div>
                </form>
            </Modal>
        </div>
    );
};

export default Tickets;
