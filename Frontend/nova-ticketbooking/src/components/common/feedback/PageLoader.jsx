import { Film } from "lucide-react";
export function PageLoader() {
  return (
    <div className="min-h-screen bg-cinema-900 flex items-center justify-center">
      <div className="flex flex-col items-center gap-4">
        <div
          className="w-16 h-16 rounded-2xl bg-brand-500/10 border border-brand-500/20
          flex items-center justify-center animate-pulse"
        >
          <Film className="w-8 h-8 text-brand-500" />
        </div>
        <div className="flex gap-1.5">
          {[0, 1, 2].map((i) => (
            <div
              key={i}
              className="w-2 h-2 rounded-full bg-brand-500 animate-bounce"
              style={{ animationDelay: `${i * 0.15}s` }}
            />
          ))}
        </div>
      </div>
    </div>
  );
}
