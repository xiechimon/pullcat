import type {Severity} from '../types/review'
import {SEVERITY_BAR_COLORS} from '../types/review'
import {PieChart, Pie, Cell, Tooltip, ResponsiveContainer} from 'recharts'

interface SeverityChartProps {
    distribution: Record<Severity, number>
    size?: number
}

export function SeverityChart({distribution, size = 160}: SeverityChartProps) {
    const data = (['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'] as Severity[]).map(s => ({
        name: s,
        value: distribution[s] || 0,
        color: SEVERITY_BAR_COLORS[s],
    })).filter(d => d.value > 0)

    if (data.length === 0) {
        return <div className="text-center text-sm text-gray-400 py-8">暂无数据</div>
    }

    const total = data.reduce((sum, d) => sum + d.value, 0)

    return (
        <div className="relative" style={{width: size, height: size}}>
            <ResponsiveContainer>
                <PieChart>
                    <Pie data={data} cx="50%" cy="50%" innerRadius={size * 0.35} outerRadius={size * 0.45}
                         dataKey="value" strokeWidth={2}>
                        {data.map(entry => <Cell key={entry.name} fill={entry.color} stroke="white"/>)}
                    </Pie>
                    <Tooltip/>
                </PieChart>
            </ResponsiveContainer>
            <div className="absolute inset-0 flex items-center justify-center">
                <span className="text-lg font-bold text-gray-900 dark:text-white">{total}</span>
            </div>
        </div>
    )
}
