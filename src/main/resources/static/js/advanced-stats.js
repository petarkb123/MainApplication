(function () {
    'use strict';

    async function fetchAdvancedStatsData() {
        try {
            const urlParams = new URLSearchParams(window.location.search);
            const from = urlParams.get('from');
            const to = urlParams.get('to');
            
            let apiUrl = '/api/stats/advanced';
            if (from || to) {
                const params = new URLSearchParams();
                if (from) params.append('from', from);
                if (to) params.append('to', to);
                apiUrl += '?' + params.toString();
            }

            const response = await fetch(apiUrl, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include'
            });

            if (!response.ok) {
                if (response.status === 401) {
                    console.error('Unauthorized: Please log in');
                    return null;
                } else if (response.status === 403) {
                    console.error('Forbidden: PRO subscription required');
                    return null;
                }
                throw new Error('Failed to fetch stats: ' + response.status);
            }

            return await response.json();
        } catch (error) {
            console.error('Error fetching advanced stats:', error);
            return null;
        }
    }

    function initializeCharts(data) {
        if (!data) {
            console.warn('No data available for charts');
            return;
        }

        const trainingFrequency = data.trainingFrequency || {};
        const dayOfWeekData = trainingFrequency.workoutsByDayOfWeek || {};
        const weeklyBreakdown = Array.isArray(trainingFrequency.weeklyBreakdown) ? trainingFrequency.weeklyBreakdown : [];
        const volumeTrends = Array.isArray(data.volumeTrends) ? data.volumeTrends : [];
        const progressiveOverload = Array.isArray(data.progressiveOverload) ? data.progressiveOverload : [];

        window.__pageDataAdvancedStats = {
            dayOfWeekData: dayOfWeekData,
            weeklyBreakdown: weeklyBreakdown,
            volumeTrends: volumeTrends,
            progressiveOverload: progressiveOverload
        };

        initializeDayOfWeekChart(dayOfWeekData);
        initializeWeeklyBreakdownChart(weeklyBreakdown);
        initializeVolumeTrendCharts(volumeTrends);
        initializeProgressiveOverloadCharts(progressiveOverload);
    }

    document.addEventListener('DOMContentLoaded', async function() {
        const statCards = document.querySelectorAll('.stat-card');
        statCards.forEach((card, index) => {
            card.style.opacity = '0';
            card.style.transform = 'translateY(20px)';
            setTimeout(() => {
                card.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
                card.style.opacity = '1';
                card.style.transform = 'translateY(0)';
            }, 100 + (index * 100));
        });

        const milestoneCards = document.querySelectorAll('.milestone-card');
        milestoneCards.forEach((card, index) => {
            card.style.opacity = '0';
            card.style.transform = 'translateY(20px)';
            setTimeout(() => {
                card.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
                card.style.opacity = '1';
                card.style.transform = 'translateY(0)';
            }, 300 + (index * 80));
        });

        if (typeof Chart === 'undefined') {
            console.error('Chart.js is not loaded');
            return;
        }

        const statsData = await fetchAdvancedStatsData();
        initializeCharts(statsData);
    });

    function initializeDayOfWeekChart(dayOfWeekData) {
        const dayOfWeekCtx = document.getElementById('dayOfWeekChart');
        if (!dayOfWeekCtx) return;

        new Chart(dayOfWeekCtx, {
            type: 'bar',
            data: {
                labels: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'],
                datasets: [{
                    label: 'Workouts',
                    data: [
                        dayOfWeekData.MONDAY || 0,
                        dayOfWeekData.TUESDAY || 0,
                        dayOfWeekData.WEDNESDAY || 0,
                        dayOfWeekData.THURSDAY || 0,
                        dayOfWeekData.FRIDAY || 0,
                        dayOfWeekData.SATURDAY || 0,
                        dayOfWeekData.SUNDAY || 0
                    ],
                    backgroundColor: 'rgba(102, 126, 234, 0.8)',
                    borderColor: 'rgba(102, 126, 234, 1)',
                    borderWidth: 2,
                    borderRadius: 8
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1,
                            color: 'rgba(255, 255, 255, 0.7)'
                        },
                        grid: {
                            color: 'rgba(255, 255, 255, 0.1)'
                        }
                    },
                    x: {
                        ticks: {
                            color: 'rgba(255, 255, 255, 0.7)'
                        },
                        grid: {
                            display: false
                        }
                    }
                }
            }
        });
    }

    function initializeWeeklyBreakdownChart(weeklyBreakdown) {
        const weeklyBreakdownCtx = document.getElementById('weeklyBreakdownChart');
        if (!weeklyBreakdownCtx || !Array.isArray(weeklyBreakdown) || weeklyBreakdown.length === 0) {
            return;
        }

        new Chart(weeklyBreakdownCtx, {
            type: 'line',
            data: {
                labels: weeklyBreakdown.map(w => {
                    const date = new Date(w.weekStart);
                    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
                }),
                datasets: [{
                    label: 'Workouts per Week',
                    data: weeklyBreakdown.map(w => w.workoutCount),
                    borderColor: 'rgba(138, 95, 184, 1)',
                    backgroundColor: 'rgba(138, 95, 184, 0.1)',
                    borderWidth: 3,
                    fill: true,
                    tension: 0.4,
                    pointRadius: 5,
                    pointHoverRadius: 7,
                    pointBackgroundColor: 'rgba(138, 95, 184, 1)'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1,
                            color: 'rgba(255, 255, 255, 0.7)'
                        },
                        grid: {
                            color: 'rgba(255, 255, 255, 0.1)'
                        }
                    },
                    x: {
                        ticks: {
                            color: 'rgba(255, 255, 255, 0.7)'
                        },
                        grid: {
                            color: 'rgba(255, 255, 255, 0.1)'
                        }
                    }
                }
            }
        });
    }

    function initializeVolumeTrendCharts(volumeTrends) {
        if (!Array.isArray(volumeTrends)) return;
        
        volumeTrends.forEach((trend, index) => {
            const canvas = document.getElementById('volumeChart' + index);
            if (!canvas || !trend.weeklyData || !Array.isArray(trend.weeklyData) || trend.weeklyData.length === 0) {
                return;
            }

            new Chart(canvas, {
                type: 'line',
                data: {
                    labels: trend.weeklyData.map(w => {
                        const date = new Date(w.weekStart);
                        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
                    }),
                    datasets: [{
                        label: 'Volume (lbs)',
                        data: trend.weeklyData.map(w => parseFloat(w.volume) || 0),
                        borderColor: trend.trend === 'increasing' ? 'rgba(46, 213, 115, 1)' :
                                    trend.trend === 'decreasing' ? 'rgba(255, 71, 87, 1)' :
                                    'rgba(102, 126, 234, 1)',
                        backgroundColor: trend.trend === 'increasing' ? 'rgba(46, 213, 115, 0.1)' :
                                        trend.trend === 'decreasing' ? 'rgba(255, 71, 87, 0.1)' :
                                        'rgba(102, 126, 234, 0.1)',
                        borderWidth: 2,
                        fill: true,
                        tension: 0.4,
                        pointRadius: 4,
                        pointHoverRadius: 6
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            display: false
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                color: 'rgba(255, 255, 255, 0.7)'
                            },
                            grid: {
                                color: 'rgba(255, 255, 255, 0.1)'
                            }
                        },
                        x: {
                            ticks: {
                                color: 'rgba(255, 255, 255, 0.7)'
                            },
                            grid: {
                                display: false
                            }
                        }
                    }
                }
            });
        });
    }

    function initializeProgressiveOverloadCharts(progressiveOverload) {
        if (!Array.isArray(progressiveOverload)) return;
        
        progressiveOverload.forEach((overload, index) => {
            const canvas = document.getElementById('overloadChart' + index);
            if (!canvas || !overload.progressPoints || !Array.isArray(overload.progressPoints) || overload.progressPoints.length === 0) {
                return;
            }

            new Chart(canvas, {
                type: 'line',
                data: {
                    labels: overload.progressPoints.map(p => {
                        const date = new Date(p.date);
                        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
                    }),
                    datasets: [{
                        label: 'Max Weight (lbs)',
                        data: overload.progressPoints.map(p => parseFloat(p.weight) || 0),
                        borderColor: 'rgba(255, 159, 64, 1)',
                        backgroundColor: 'rgba(255, 159, 64, 0.1)',
                        borderWidth: 3,
                        fill: true,
                        tension: 0.4,
                        pointRadius: 5,
                        pointHoverRadius: 7,
                        pointBackgroundColor: 'rgba(255, 159, 64, 1)'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            display: false
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: false,
                            ticks: {
                                color: 'rgba(255, 255, 255, 0.7)'
                            },
                            grid: {
                                color: 'rgba(255, 255, 255, 0.1)'
                            }
                        },
                        x: {
                            ticks: {
                                color: 'rgba(255, 255, 255, 0.7)'
                            },
                            grid: {
                                display: false
                            }
                        }
                    }
                }
            });
        });
    }
})();
