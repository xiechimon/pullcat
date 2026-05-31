import {useEffect, useState} from 'react'
import {toast} from 'sonner'
import type {StatsOverview, Severity} from '../types/review'
import {getStatsOverview} from '../lib/api'
import {SeverityChart} from '../components/SeverityChart'
import {BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer} from 'recharts'

export function StatsPage() {
    const [stats, setStats] = useState<StatsOverview | null>(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        let cancelled = false
        getStatsOverview()
            .then(s => {
                if (!cancelled) setStats(s)
            })
            .catch(e => {
                if (!cancelled) toast.error(e.message)
            })
            .finally(() => {
                if (!cancelled) setLoading(false)
            })
        return () => {
            cancelled = true
        }
    }, [])

    if (loading) {
        return <div className="max-w-5xl mx-auto px-4 py-8 animate-pulse">
            <div className="h-8 w-48 bg-gray-200 dark:bg-gray-700 rounded mb-6"/>
            <div className="h-64 bg-gray-200 dark:bg-gray-700 rounded-xl"/>
        </div>
    }
    if (!stats) return null

    const issueTypeData = (stats.commonIssueTypes || []).map(t => ({
        name: t.type.length > 40 ? t.type.slice(0, 40) + '...' : t.type,
        count: t.count
    }))

    return (
        <div className="max-w-5xl mx-auto px-4 py-8 space-y-8">
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">数据统计</h1>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                {[
                    {label: '总审查次数', value: stats.totalReviews},
                    {label: '发现问题总数', value: stats.totalIssues},
                    {label: '覆盖仓库数', value: stats.repoCount},
                    {label: '平均问题数', value: stats.avgIssuesPerReview.toFixed(1)},
                ].map(c => (
                    <div key={c.label}
                         className="p-4 bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700">
                        <div className="text-2xl font-bold text-gray-900 dark:text-white">{c.value}</div>
                        <div className="text-sm text-gray-500 mt-1">{c.label}</div>
                    </div>
                ))}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
                    <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">严重度分布</h2>
                    <div className="flex justify-center">
                        <SeverityChart distribution={(stats.severityDistribution || {}) as Record<Severity, number>}
                                       size={200}/>
                    </div>
                </div>

                <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
                    <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">仓库审查分布</h2>
                    <div className="text-center text-sm text-gray-400 py-12">即将推出</div>
                </div>
            </div>

            <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
                <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">常见问题类型 Top 10</h2>
                {issueTypeData.length > 0 ? (
                    <ResponsiveContainer width="100%" height={issueTypeData.length * 40}>
                        <BarChart data={issueTypeData} layout="vertical" margin={{left: 10}}>
                            <CartesianGrid strokeDasharray="3 3" horizontal={false}/>
                            <XAxis type="number" fontSize={11}/>
                            <YAxis type="category" dataKey="name" width={140} fontSize={12}/>
                            <Tooltip/>
                            <Bar dataKey="count" fill="#059669" radius={[0, 4, 4, 0]}/>
                        </BarChart>
                    </ResponsiveContainer>
                ) : (
                    <div className="text-center text-sm text-gray-400 py-12">暂无数据</div>
                )}
            </div>
        </div>
    )
}
