import { NavLink, Outlet } from "react-router-dom";

function NavItem({ to, label }: { to: string; label: string }) {
    return (
        <NavLink
            to={to}
            end
            className={({ isActive }) =>
                [
                    "block rounded-md px-3 py-2 text-sm font-medium",
                    isActive
                        ? "bg-[var(--warriors-gold)]/20 text-[var(--warriors-blue)]"
                        : "text-slate-700 hover:bg-slate-50",
                ].join(" ")
            }
        >
            {label}
        </NavLink>
    );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
    return (
        <div className="mb-4">
            <div className="px-3 py-2 text-xs font-semibold uppercase tracking-wide text-slate-500">
                {title}
            </div>
            <div className="space-y-1">{children}</div>
        </div>
    );
}

export default function EditorialLayout() {
    return (
        <div className="page-pad">
            <div className="content-rail">
                <div className="flex gap-6">
                    {/* Left Nav */}
                    <aside className="w-60 shrink-0">
                        <nav className="sticky top-4 rounded-lg border border-slate-200 bg-white/70 backdrop-blur p-2">
                            <Section title="Editorial">
                                <NavItem to="/admin" label="Dashboard" />
                                <NavItem to="/admin/lemmas" label="Lemmas" />
                                <NavItem to="/admin/sentences" label="Sentences" />
                            </Section>

                            <Section title="Admin">
                                <NavItem to="/admin/languages" label="Languages" />
                            </Section>
                        </nav>
                    </aside>

                    {/* Main Content */}
                    <section className="min-w-0 flex-1">
                        <Outlet />
                    </section>
                </div>
            </div>
        </div>
    );
}
