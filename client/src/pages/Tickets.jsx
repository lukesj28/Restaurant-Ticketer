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
    
    // Audio context ref
    const audioContextRef = useRef(null);
    const alertIntervalRef = useRef(null);
    
    // Track previous tickets for diffing
    const previousTicketsRef = useRef([]);
    // Track highlighted tickets
    const [highlightedTicketIds, setHighlightedTicketIds] = useState([]);
    const [isAlertModalOpen, setIsAlertModalOpen] = useState(false);
    const initialLoadRef = useRef(true);

    useEffect(() => {
        fetchTickets();

        // Poll for active tickets to keep UI fresh (both front active tab and back view)
        let interval;
        if (viewMode === 'back' || activeTab === 'active') {
            interval = setInterval(fetchTickets, 5000);
        }
        return () => clearInterval(interval);
    }, [activeTab, viewMode]);

    const playSingleChime = (ctx) => {
        const oscillator = ctx.createOscillator();
        const gainNode = ctx.createGain();

        oscillator.connect(gainNode);
        gainNode.connect(ctx.destination);

        // 2-tone chime
        const now = ctx.currentTime;
        
        // First tone
        oscillator.frequency.setValueAtTime(660, now);
        gainNode.gain.setValueAtTime(0.1, now);
        
        // Second tone
        oscillator.frequency.setValueAtTime(880, now + 0.1);
        gainNode.gain.linearRampToValueAtTime(0, now + 0.6);

        oscillator.start(now);
        oscillator.stop(now + 0.6);
    };

    const startAlertLoop = () => {
        if (alertIntervalRef.current) return; // Already running

        try {
            if (!audioContextRef.current) {
                audioContextRef.current = new (window.AudioContext || window.webkitAudioContext)();
            }
            
            const ctx = audioContextRef.current;
            if (ctx.state === 'suspended') {
                ctx.resume();
            }

            // Play immediately
            playSingleChime(ctx);

            // Loop every 2 seconds
            alertIntervalRef.current = setInterval(() => {
                playSingleChime(ctx);
            }, 2000);
            
        } catch (e) {
            console.error("Audio play failed", e);
        }
    };

    const stopAlertLoop = () => {
        if (alertIntervalRef.current) {
            clearInterval(alertIntervalRef.current);
            alertIntervalRef.current = null;
        }
    };

    const handleDismissAlert = () => {
        stopAlertLoop();
        setIsAlertModalOpen(false);
    };

    const checkForUpdates = (currentTickets) => {
        if (viewMode !== 'back') return;

        const prevTickets = previousTicketsRef.current;
        // if (prevTickets.length === 0) return; // metrics init, don't beep -> REPLACED by initialLoadRef logic in fetchTickets

        const changedIds = [];
        let shouldAlert = false;

        // Check for new tickets
        currentTickets.forEach(ticket => {
            const prev = prevTickets.find(p => p.id === ticket.id);
            if (!prev) {
                // New ticket
                changedIds.push(ticket.id);
                shouldAlert = true;
            } else {
                // Check if meaningful content changed
                const prevHash = JSON.stringify({
                    orders: prev.kitchenOrders,
                    tally: prev.kitchenTally,
                    comment: prev.comment
                });
                const currHash = JSON.stringify({
                    orders: ticket.kitchenOrders,
                    tally: ticket.kitchenTally,
                    comment: ticket.comment
                });
                
                if (prevHash !== currHash) {
                    changedIds.push(ticket.id);
                    shouldAlert = true;
                }
            }
        });

        if (shouldAlert) {
            startAlertLoop();
            setIsAlertModalOpen(true);
            setHighlightedTicketIds(prev => [...new Set([...prev, ...changedIds])]);
            
            // Clear highlights after 5 seconds
            setTimeout(() => {
                if (mounted.current) {
                    setHighlightedTicketIds(prev => prev.filter(id => !changedIds.includes(id)));
                }
            }, 5000);
        }
    };

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
                
                if (viewMode === 'back') {
                    if (initialLoadRef.current) {
                        initialLoadRef.current = false;
                    } else {
                        checkForUpdates(sorted);
                    }
                    previousTicketsRef.current = sorted;
                }
                
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
                    onClick={() => {
                        setTickets([]);
                        setViewMode('front');
                        previousTicketsRef.current = [];
                        initialLoadRef.current = true;
                    }}
                >
                    Front
                </button>
                <button
                    className={`view-btn ${viewMode === 'back' ? 'active' : ''}`}
                    onClick={() => {
                        setTickets([]);
                        setViewMode('back');
                        previousTicketsRef.current = [];
                        initialLoadRef.current = true;
                        // Initialize audio context on user interaction
                        if (!audioContextRef.current) {
                            audioContextRef.current = new (window.AudioContext || window.webkitAudioContext)();
                        }
                        if (audioContextRef.current.state === 'suspended') {
                            audioContextRef.current.resume();
                        }
                    }}
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
                        <KitchenTicketCard 
                            key={ticket.id} 
                            ticket={ticket} 
                            onComplete={handleCompleteKitchenTicket}
                            isHighlighted={highlightedTicketIds.includes(ticket.id)}
                        />
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

            {/* Alert Modal */}
            <Modal
                isOpen={isAlertModalOpen}
                onClose={handleDismissAlert}
                title="Kitchen Alert"
                footer={
                    <Button variant="primary" onClick={handleDismissAlert}>
                        Acknowledge
                    </Button>
                }
            >
                <div className="alert-content">
                    <p style={{ fontSize: '1.2rem', textAlign: 'center', margin: '20px 0' }}>
                        New or updated tickets available!
                    </p>
                </div>
            </Modal>
        </div>
    );
};

export default Tickets;
