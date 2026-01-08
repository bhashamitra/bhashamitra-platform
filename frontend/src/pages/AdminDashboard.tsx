export default function AdminDashboard() {
    return (
        <div className="min-h-screen bg-[var(--page-bg)] px-6 py-12">
            <div className="mx-auto max-w-4xl rounded-2xl border border-slate-200 bg-white/90 backdrop-blur p-8 shadow-sm">
                <h1 className="text-3xl font-extrabold text-[var(--warriors-blue)]">
                    Editorial Dashboard
                </h1>

                <p className="mt-4 text-slate-600">
                    Editorial & admin tools coming soon.
                </p>

                <div className="mt-6 inline-block rounded-full bg-[var(--warriors-gold)]/20 px-4 py-2 text-sm font-semibold text-slate-800">
                    Editor / Admin only
                </div>
            </div>
        </div>
    );
}
