import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/api';
import TicketCard from '../components/tickets/TicketCard';
import KitchenTicketCard from '../components/tickets/KitchenTicketCard';
import Button from '../components/common/Button';
import Modal from '../components/common/Modal';
import { useToast } from '../context/ToastContext';
import './Tickets.css';

const Tickets = () => {
    const navigate = useNavigate();
    const { toast } = useToast();
    const mounted = useRef(false);

    useEffect(() => {
        mounted.current = true;
        return () => { mounted.current = false; };
    }, []);

    const [viewMode, setViewMode] = useState('front'); // 'front' | 'back'
    const [activeTab, setActiveTab] = useState('active');
    const [tickets, setTickets] = useState([]);
    const [loading, setLoading] = useState(false);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [newTableNumber, setNewTableNumber] = useState('');

    useEffect(() => {
        fetchTickets();

        // Poll for active tickets to keep UI fresh (both front active tab and back view)
        let interval;
        if (viewMode === 'back' || activeTab === 'active') {
            interval = setInterval(fetchTickets, 5000);
        }
        return () => clearInterval(interval);
    }, [activeTab, viewMode]);

    const fetchTickets = async () => {
        if (!mounted.current) return;
        setLoading(true);
        try {
            let endpoint;
            if (viewMode === 'back') {
                endpoint = '/tickets/active/kitchen';
            } else {
                endpoint = '/tickets/active';
                if (activeTab === 'completed') endpoint = '/tickets/completed';
                if (activeTab === 'closed') endpoint = '/tickets/closed';
            }

            const data = await api.get(endpoint);

            if (mounted.current) {
                // sort by ID descending for newest first
                const sorted = (data || []).sort((a, b) => b.id - a.id);
                setTickets(sorted);
            }
        } catch (error) {
            if (mounted.current) console.error("Failed to fetch tickets:", error);
        } finally {
            if (mounted.current) setLoading(false);
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

    const handleCompleteKitchenTicket = async (ticketId) => {
        try {
            await api.post(`/tickets/${ticketId}/kitchen/complete`);
            fetchTickets();
        } catch (error) {
            toast.error('Failed to complete ticket: ' + error.message);
        }
    };

    return (
        <div className="tickets-page">
            {/* View Mode Toggle */}
            <div className="view-toggle">
                <button
                    className={`view-btn ${viewMode === 'front' ? 'active' : ''}`}
                    onClick={() => setViewMode('front')}
                >
                    Front
                </button>
                <button
                    className={`view-btn ${viewMode === 'back' ? 'active' : ''}`}
                    onClick={() => setViewMode('back')}
                >
                    Back
                </button>
            </div>

            <div className="tickets-header">
                {viewMode === 'front' && (
                    <div className="tabs">
                        <button
                            className={`tab-btn ${activeTab === 'active' ? 'active' : ''}`}
                            onClick={() => setActiveTab('active')}
                        >
                            Active
                        </button>
                        <button
                            className={`tab-btn ${activeTab === 'completed' ? 'active' : ''}`}
                            onClick={() => setActiveTab('completed')}
                        >
                            Completed
                        </button>
                        <button
                            className={`tab-btn ${activeTab === 'closed' ? 'active' : ''}`}
                            onClick={() => setActiveTab('closed')}
                        >
                            Closed
                        </button>
                    </div>
                )}
                {viewMode === 'back' && (
                    <div className="back-view-label">Active Kitchen Tickets</div>
                )}
                {viewMode === 'front' && activeTab === 'active' && (
                    <Button onClick={() => setIsCreateModalOpen(true)}>
                        + New Ticket
                    </Button>
                )}
            </div>

            <div className="tickets-grid">
                {viewMode === 'front' ? (
                    tickets.map(ticket => (
                        <TicketCard key={ticket.id} ticket={ticket} onClick={handleTicketClick} />
                    ))
                ) : (
                    tickets.map(ticket => (
                        <KitchenTicketCard key={ticket.id} ticket={ticket} onComplete={handleCompleteKitchenTicket} />
                    ))
                )}
                {tickets.length === 0 && !loading && (
                    <div className="empty-state">
                        {viewMode === 'back' ? 'No active kitchen tickets.' : `No ${activeTab} tickets found.`}
                    </div>
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
