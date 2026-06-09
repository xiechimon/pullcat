export function LoginPage() {
  return (
    <div className="page-shell py-8 animate-fade-in">
      <section className="surface-card overflow-hidden">
        <div className="grid gap-0 md:grid-cols-[minmax(0,1.08fr)_16rem]">
          <div className="px-5 py-8 md:px-6 md:py-10">
            <div className="space-y-1">
              <div className="text-sm font-medium text-slate-500 dark:text-slate-400">当前关注点</div>
              <h1 className="text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">连接 GitHub，再继续审查</h1>
            </div>
            <p className="mt-4 max-w-md text-sm text-slate-500 dark:text-slate-400">
              登录后可以读取仓库、保存记录，并在需要时把结果回写到 PR。
            </p>
            <a
              href="/oauth2/authorization/github"
              className="mt-6 inline-flex items-center gap-2 rounded-xl bg-slate-950 px-5 py-3 text-sm font-medium text-white transition-opacity hover:opacity-90 dark:bg-white dark:text-slate-950"
            >
              <svg className="h-5 w-5" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
              </svg>
              使用 GitHub 登录
            </a>
          </div>

          <div className="border-t border-slate-200 bg-slate-50/70 px-5 py-8 dark:border-slate-800 dark:bg-slate-900/50 md:border-l md:border-t-0 md:px-6">
            <div className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400 dark:text-slate-500">登录后可用</div>
            <div className="mt-4 space-y-3">
              <div className="rounded-xl bg-white px-4 py-4 text-sm text-slate-950 dark:bg-slate-950/70 dark:text-white">读取仓库与 PR</div>
              <div className="rounded-xl bg-white px-4 py-4 text-sm text-slate-950 dark:bg-slate-950/70 dark:text-white">保存历史与统计</div>
              <div className="rounded-xl bg-white px-4 py-4 text-sm text-slate-950 dark:bg-slate-950/70 dark:text-white">支持自动回写评论</div>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}
