import type { PublicVersion } from "../types/publicVersion";
import { Link } from "react-router-dom";

type FooterProps = {
    version: PublicVersion | null;
    loading: boolean;
};

export default function Footer({ version, loading }: FooterProps) {
    const versionText = loading
        ? "Loading…"
        : version
            ? `v${version.version}`
            : "v—";

    return (
        <footer className="relative z-10 border-t border-slate-200 bg-white/70 backdrop-blur">
            <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-2 px-6 py-4 text-sm sm:flex-row">
        <span className="text-slate-600">
          © {new Date().getFullYear()}{" "}
            <span className="font-semibold text-[var(--warriors-blue)]">
            Bhashamitra
          </span>
        </span>

                <div className="flex items-center gap-4">
                    <Link to="/privacy" className="link-primary text-sm">
                        Privacy Policy
                    </Link>

                    <span className="font-semibold text-[var(--warriors-blue)]">
            {versionText}
          </span>
                </div>
            </div>
        </footer>
    );
}
