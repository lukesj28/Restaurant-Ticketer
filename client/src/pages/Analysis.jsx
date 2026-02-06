import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../api/api';
import Button from '../components/common/Button';
import { useToast } from '../context/ToastContext';
import './Analysis.css';

const VIEW_TYPES = {
    DAY: 'day',
    WEEK: 'week',
    MONTH: 'month',
    YEAR: 'year',
    CUSTOM: 'custom'
};

const Analysis = () => {
    const { toast } = useToast();
    const [viewType, setViewType] = useState(VIEW_TYPES.DAY);
    const [referenceDate, setReferenceDate] = useState(new Date());
    const [customStartDate, setCustomStartDate] = useState(new Date().toISOString().split('T')[0]);
    const [customEndDate, setCustomEndDate] = useState(new Date().toISOString().split('T')[0]);
    const [report, setReport] = useState(null);
    const [loading, setLoading] = useState(false);

    // Show more toggles for rankings
    const [showAllDays, setShowAllDays] = useState(false);
    const [showAllItems, setShowAllItems] = useState(false);
    const [showAllSides, setShowAllSides] = useState(false);

    // Limits for initial display
    const DAY_LIMIT = 10;
    const ITEM_LIMIT = 10;
    const SIDE_LIMIT = 4;

    // Get Monday of the week containing the given date
    const getMonday = (date) => {
        const d = new Date(date);
        const day = d.getDay();
        const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Adjust if Sunday
        return new Date(d.setDate(diff));
    };

    // Get Sunday of the week containing the given date
    const getSunday = (date) => {
        const monday = getMonday(date);
        const sunday = new Date(monday);
        sunday.setDate(monday.getDate() + 6);
        return sunday;
    };

    // Check if the reference date is in the current period
    const isCurrentPeriod = useCallback((type, refDate) => {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const ref = new Date(refDate);
        ref.setHours(0, 0, 0, 0);

        switch (type) {
            case VIEW_TYPES.DAY:
                return ref.toDateString() === today.toDateString();
            case VIEW_TYPES.WEEK:
                return getMonday(ref).getTime() === getMonday(today).getTime();
            case VIEW_TYPES.MONTH:
                return ref.getFullYear() === today.getFullYear() && ref.getMonth() === today.getMonth();
            case VIEW_TYPES.YEAR:
                return ref.getFullYear() === today.getFullYear();
            default:
                return false;
        }
    }, []);

    // Calculate start and end dates based on view type
    const getDateRange = useCallback((type, refDate) => {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const ref = new Date(refDate);
        ref.setHours(0, 0, 0, 0);

        let startDate, endDate;

        switch (type) {
            case VIEW_TYPES.DAY:
                startDate = ref;
                endDate = ref;
                break;
            case VIEW_TYPES.WEEK:
                startDate = getMonday(ref);
                endDate = getSunday(ref);
                // If current week, end at today
                if (isCurrentPeriod(type, refDate) && endDate > today) {
                    endDate = today;
                }
                break;
            case VIEW_TYPES.MONTH:
                startDate = new Date(ref.getFullYear(), ref.getMonth(), 1);
                endDate = new Date(ref.getFullYear(), ref.getMonth() + 1, 0);
                // If current month, end at today
                if (isCurrentPeriod(type, refDate) && endDate > today) {
                    endDate = today;
                }
                break;
            case VIEW_TYPES.YEAR:
                startDate = new Date(ref.getFullYear(), 0, 1);
                endDate = new Date(ref.getFullYear(), 11, 31);
                // If current year, end at today
                if (isCurrentPeriod(type, refDate) && endDate > today) {
                    endDate = today;
                }
                break;
            default:
                startDate = ref;
                endDate = ref;
        }

        return {
            startDate: startDate.toISOString().split('T')[0],
            endDate: endDate.toISOString().split('T')[0]
        };
    }, [isCurrentPeriod]);

    // Format date for display label
    const getDisplayLabel = useCallback((type, refDate) => {
        const ref = new Date(refDate);
        const options = { timeZone: 'UTC' };

        switch (type) {
            case VIEW_TYPES.DAY:
                return ref.toLocaleDateString('en-US', {
                    weekday: 'long',
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                    ...options
                });
            case VIEW_TYPES.WEEK: {
                const monday = getMonday(ref);
                const sunday = getSunday(ref);
                const startStr = monday.toLocaleDateString('en-US', { month: 'short', day: 'numeric', ...options });
                const endStr = sunday.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', ...options });
                return `${startStr} – ${endStr}`;
            }
            case VIEW_TYPES.MONTH:
                return ref.toLocaleDateString('en-US', { month: 'long', year: 'numeric', ...options });
            case VIEW_TYPES.YEAR:
                return ref.getFullYear().toString();
            case VIEW_TYPES.CUSTOM:
                return 'Custom Range';
            default:
                return '';
        }
    }, []);

    // Fetch report data
    const fetchReport = useCallback(async (startDate, endDate) => {
        setLoading(true);
        // Reset show more toggles when fetching new data
        setShowAllDays(false);
        setShowAllItems(false);
        setShowAllSides(false);
        try {
            const query = new URLSearchParams({ startDate, endDate }).toString();
            const data = await api.get(`/analysis?${query}`);
            setReport(data);
        } catch (e) {
            console.error("Fetch report failed", e);
            toast.error("Fetch failed: " + e.message);
        } finally {
            setLoading(false);
        }
    }, [toast]);

    // Navigate to previous period
    const goToPrevious = () => {
        const ref = new Date(referenceDate);
        switch (viewType) {
            case VIEW_TYPES.DAY:
                ref.setDate(ref.getDate() - 1);
                break;
            case VIEW_TYPES.WEEK:
                ref.setDate(ref.getDate() - 7);
                break;
            case VIEW_TYPES.MONTH:
                ref.setMonth(ref.getMonth() - 1);
                break;
            case VIEW_TYPES.YEAR:
                ref.setFullYear(ref.getFullYear() - 1);
                break;
            default:
                return;
        }
        setReferenceDate(ref);
    };

    // Navigate to next period
    const goToNext = () => {
        if (isCurrentPeriod(viewType, referenceDate)) {
            return; // Already at current period
        }

        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const ref = new Date(referenceDate);
        ref.setHours(0, 0, 0, 0);

        switch (viewType) {
            case VIEW_TYPES.DAY:
                ref.setDate(ref.getDate() + 1);
                break;
            case VIEW_TYPES.WEEK:
                ref.setDate(ref.getDate() + 7);
                break;
            case VIEW_TYPES.MONTH:
                ref.setMonth(ref.getMonth() + 1);
                break;
            case VIEW_TYPES.YEAR:
                ref.setFullYear(ref.getFullYear() + 1);
                break;
            default:
                return;
        }

        // Allow navigation up to and including today
        if (ref.getTime() <= today.getTime()) {
            setReferenceDate(ref);
        } else {
            // If navigating would go past today, snap to today
            setReferenceDate(today);
        }
    };

    // Check if next navigation is disabled
    const isNextDisabled = () => {
        return isCurrentPeriod(viewType, referenceDate);
    };

    // Auto-fetch on mount and when view/reference changes (except custom)
    useEffect(() => {
        if (viewType !== VIEW_TYPES.CUSTOM) {
            const { startDate, endDate } = getDateRange(viewType, referenceDate);
            fetchReport(startDate, endDate);
        }
    }, [viewType, referenceDate, getDateRange, fetchReport]);

    // Handle view type change
    const handleViewTypeChange = (e) => {
        const newViewType = e.target.value;
        setViewType(newViewType);
        // Reset to today when changing view type
        if (newViewType !== VIEW_TYPES.CUSTOM) {
            setReferenceDate(new Date());
        }
    };

    // Handle custom range submit
    const handleCustomSubmit = () => {
        fetchReport(customStartDate, customEndDate);
    };

    // Helper to get max value for heatmap normalization
    const getMaxTraffic = () => {
        if (!report || !report.hourlyTraffic) return 0;
        return Math.max(...Object.values(report.hourlyTraffic), 1);
    };

    const isToday = viewType === VIEW_TYPES.DAY && isCurrentPeriod(VIEW_TYPES.DAY, referenceDate);
    const showToDate = viewType !== VIEW_TYPES.CUSTOM && viewType !== VIEW_TYPES.DAY && isCurrentPeriod(viewType, referenceDate);

    return (
        <div className="analysis-page">
            <h1>Analysis Report</h1>

            <div className="analysis-controls">
                <div className="control-group">
                    <label>View</label>
                    <select
                        className="view-selector"
                        value={viewType}
                        onChange={handleViewTypeChange}
                    >
                        <option value={VIEW_TYPES.DAY}>Day</option>
                        <option value={VIEW_TYPES.WEEK}>Week</option>
                        <option value={VIEW_TYPES.MONTH}>Month</option>
                        <option value={VIEW_TYPES.YEAR}>Year</option>
                        <option value={VIEW_TYPES.CUSTOM}>Custom</option>
                    </select>
                </div>

                {viewType !== VIEW_TYPES.CUSTOM && (
                    <div className="nav-controls">
                        <button
                            className="nav-arrow"
                            onClick={goToPrevious}
                            aria-label="Previous period"
                        >
                            ←
                        </button>
                        <div className="period-label">
                            <span className="period-text">{getDisplayLabel(viewType, referenceDate)}</span>
                            {isToday && <span className="today-badge">Today</span>}
                            {showToDate && <span className="to-date-badge">(To Date)</span>}
                        </div>
                        <button
                            className="nav-arrow"
                            onClick={goToNext}
                            disabled={isNextDisabled()}
                            aria-label="Next period"
                        >
                            →
                        </button>
                    </div>
                )}

                {viewType === VIEW_TYPES.CUSTOM && (
                    <div className="custom-range">
                        <div className="control-group">
                            <label>Start Date</label>
                            <input
                                type="date"
                                value={customStartDate}
                                onChange={e => setCustomStartDate(e.target.value)}
                            />
                        </div>
                        <div className="control-group">
                            <label>End Date</label>
                            <input
                                type="date"
                                value={customEndDate}
                                onChange={e => setCustomEndDate(e.target.value)}
                            />
                        </div>
                        <Button onClick={handleCustomSubmit} disabled={loading}>
                            Generate Report
                        </Button>
                    </div>
                )}

                {loading && <span className="loading-indicator">Loading...</span>}
            </div>

            {report && (
                <div className="report-content">
                    <div className="kpi-grid">
                        <div className="kpi-card">
                            <h3>Total Revenue</h3>
                            <div className="big-number">${(report.totalTotalCents / 100).toFixed(2)}</div>
                        </div>
                        <div className="kpi-card">
                            <h3>Total Subtotal</h3>
                            <div className="big-number">${((report.totalSubtotalCents || 0) / 100).toFixed(2)}</div>
                        </div>
                        <div className="kpi-card">
                            <h3>Total Orders</h3>
                            <div className="big-number">{report.totalOrderCount}</div>
                        </div>
                        <div className="kpi-card">
                            <h3>Ticket Count</h3>
                            <div className="big-number">{report.totalTicketCount}</div>
                        </div>
                        <div className="kpi-card">
                            <h3>Avg Ticket</h3>
                            <div className="big-number">${((report.averageTicketTotalCents || 0) / 100).toFixed(2)}</div>
                        </div>
                        <div className="kpi-card">
                            <h3>Avg Turnover</h3>
                            <div className="big-number">{report.averageTurnoverTimeMinutes || 0}m</div>
                        </div>
                    </div>

                    <div className="charts-row">
                        <div className="chart-section">
                            <h2>Hourly Traffic (Heatmap)</h2>
                            <div className="heatmap-container">
                                {Array.from({ length: 24 }).map((_, hour) => {
                                    const count = report.hourlyTraffic ? (report.hourlyTraffic[String(hour)] || 0) : 0;
                                    const intensity = count / getMaxTraffic();
                                    return (
                                        <div
                                            key={hour}
                                            className="heatmap-cell"
                                            style={{ backgroundColor: `rgba(0, 123, 255, ${Math.max(intensity, 0.1)})` }}
                                            title={`${hour}:00 - ${count} orders`}
                                        >
                                            <span className="heatmap-label">{hour}</span>
                                            <span className="heatmap-value">{count}</span>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>

                        {viewType !== VIEW_TYPES.DAY && (
                            <div className="rankings-section">
                                <h2>Daily Revenue</h2>
                                <table className="rankings-table">
                                    <thead>
                                        <tr>
                                            <th>Date</th>
                                            <th>Revenue</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {(() => {
                                            const sortedDays = (report.dayRankings || [])
                                                .sort((a, b) => new Date(b.date) - new Date(a.date));
                                            const displayDays = showAllDays ? sortedDays : sortedDays.slice(0, DAY_LIMIT);
                                            return displayDays.map((day) => (
                                                <tr key={day.date}>
                                                    <td>{day.date}</td>
                                                    <td>${(day.totalTotalCents / 100).toFixed(2)}</td>
                                                </tr>
                                            ));
                                        })()}
                                    </tbody>
                                </table>
                                {(report.dayRankings || []).length > DAY_LIMIT && (
                                    <button
                                        className="show-more-btn"
                                        onClick={() => setShowAllDays(!showAllDays)}
                                    >
                                        {showAllDays ? 'Show Less' : `Show All (${(report.dayRankings || []).length})`}
                                    </button>
                                )}
                            </div>
                        )}
                    </div>

                    <div className="rankings-section full-width">
                        <h2>Item Rankings</h2>
                        <table className="rankings-table">
                            <thead>
                                <tr>
                                    <th>Item</th>
                                    <th>Count</th>
                                    <th>Revenue</th>
                                </tr>
                            </thead>
                            <tbody>
                                {(() => {
                                    const sortedItems = (report.itemRankings || [])
                                        .sort((a, b) => b.totalRevenueCents - a.totalRevenueCents);
                                    const displayItems = showAllItems ? sortedItems : sortedItems.slice(0, ITEM_LIMIT);
                                    return displayItems.map((stats) => (
                                        <tr key={stats.name}>
                                            <td>{stats.name}</td>
                                            <td>{stats.count}</td>
                                            <td>${(stats.totalRevenueCents / 100).toFixed(2)}</td>
                                        </tr>
                                    ));
                                })()}
                            </tbody>
                        </table>
                        {(report.itemRankings || []).length > ITEM_LIMIT && (
                            <button
                                className="show-more-btn"
                                onClick={() => setShowAllItems(!showAllItems)}
                            >
                                {showAllItems ? 'Show Less' : `Show All (${(report.itemRankings || []).length})`}
                            </button>
                        )}
                    </div>

                    <div className="rankings-section full-width">
                        <h2>Side Rankings</h2>
                        <div className="side-rankings-grid">
                            {(() => {
                                const sideEntries = Object.entries(report.sideRankings || {});
                                const displayEntries = showAllSides ? sideEntries : sideEntries.slice(0, SIDE_LIMIT);
                                return displayEntries.map(([mainItem, sides]) => (
                                    <div key={mainItem} className="side-ranking-card">
                                        <h3>{mainItem}</h3>
                                        <table className="mini-table">
                                            <thead>
                                                <tr><th>Side</th><th>Cnt</th></tr>
                                            </thead>
                                            <tbody>
                                                {sides.sort((a, b) => b.count - a.count).map((side) => (
                                                    <tr key={side.name}>
                                                        <td>{side.name}</td>
                                                        <td>{side.count}</td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                ));
                            })()}
                        </div>
                        {Object.entries(report.sideRankings || {}).length > SIDE_LIMIT && (
                            <button
                                className="show-more-btn"
                                onClick={() => setShowAllSides(!showAllSides)}
                            >
                                {showAllSides ? 'Show Less' : `Show All (${Object.entries(report.sideRankings || {}).length})`}
                            </button>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default Analysis;
