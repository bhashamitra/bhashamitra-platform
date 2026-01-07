type LandingCardProps = {
    loading: boolean;
    displayName?: string;
    onOpenPrivacy: () => void;
};

export default function LandingCard({
                                        loading,
                                        displayName,
                                        onOpenPrivacy,
                                    }: LandingCardProps) {
    return (
        <div className="relative z-0 flex min-h-screen items-center justify-center px-4">
            <div className="w-full max-w-xl rounded-2xl border border-slate-200 bg-white/80 backdrop-blur p-8 shadow-sm text-center">
                <h1 className="text-4xl sm:text-5xl font-extrabold text-[var(--warriors-blue)]">
                    Bhashamitra
                </h1>

                <p className="mt-4 text-base sm:text-lg text-slate-700">
                    Indian language learning, coming soon.
                </p>

                <div className="mt-6">
          <span className="inline-block rounded-full bg-[var(--warriors-gold)] px-5 py-2 text-sm font-semibold text-slate-900">
            Early access coming soon
          </span>
                </div>

                <div className="mt-6 text-sm text-slate-500">
                    {loading ? (
                        <span>Checking session…</span>
                    ) : displayName ? (
                        <span>
              Signed in as{" "}
                            <span className="font-semibold text-slate-700">{displayName}</span>
            </span>
                    ) : null}
                </div>

                <div className="mt-6 text-sm text-slate-500">
                    <button
                        onClick={onOpenPrivacy}
                        className="underline hover:text-[var(--warriors-blue)]"
                    >
                        Privacy Policy
                    </button>
                </div>
            </div>
        </div>
    );
}
