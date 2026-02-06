import React, { useState } from 'react';
import { api } from '../api/api';
import Button from '../components/common/Button';
import { useToast } from '../context/ToastContext';
import './Analysis.css';


const Analysis = () => {
    const { toast } = useToast();
    // Default to today or current range
    const [startDate, setStartDate] = useState(new Date().toISOString().split('T')[0]);
    const [endDate, setEndDate] = useState(new Date().toISOString().split('T')[0]);
    const [report, setReport] = useState(null);
    const [loading, setLoading] = useState(false);

    const fetchReport = async () => {
        setLoading(true);
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
    };


    // Helper to get max value for heatmap normalization
    const getMaxTraffic = () => {
        if (!report || !report.hourlyTraffic) return 0;
        return Math.max(...Object.values(report.hourlyTraffic), 1);
    };

    return (
        <div className="analysis-page">
            <h1>Analysis Report</h1>

            <div className="analysis-controls">
                <div className="control-group">
                    <label>Start Date</label>
                    <input type="date" value={startDate} onChange={e => setStartDate(e.target.value)} />
                </div>
                <div className="control-group">
                    <label>End Date</label>
                    <input type="date" value={endDate} onChange={e => setEndDate(e.target.value)} />
                </div>
                <Button onClick={fetchReport} disabled={loading}>Generate Report</Button>
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
                                    {(report.dayRankings || [])
                                        .sort((a, b) => new Date(b.date) - new Date(a.date))
                                        .map((day) => (
                                            <tr key={day.date}>
                                                <td>{day.date}</td>
                                                <td>${(day.totalTotalCents / 100).toFixed(2)}</td>
                                            </tr>
                                        ))}
                                </tbody>
                            </table>
                        </div>
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
                                {(report.itemRankings || [])
                                    .sort((a, b) => b.totalRevenueCents - a.totalRevenueCents)
                                    .map((stats) => (
                                        <tr key={stats.name}>
                                            <td>{stats.name}</td>
                                            <td>{stats.count}</td>
                                            <td>${(stats.totalRevenueCents / 100).toFixed(2)}</td>
                                        </tr>
                                    ))}
                            </tbody>
                        </table>
                    </div>

                    <div className="rankings-section full-width">
                        <h2>Side Rankings</h2>
                        <div className="side-rankings-grid">
                            {Object.entries(report.sideRankings || {}).map(([mainItem, sides]) => (
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
                            ))}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Analysis;
