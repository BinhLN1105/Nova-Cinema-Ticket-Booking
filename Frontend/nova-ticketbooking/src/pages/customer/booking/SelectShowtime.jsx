import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, MapPin, Clock, Monitor } from "lucide-react";
import { movieApi, cinemaApi, showtimeApi } from "@/api/endpoints";
import { useBookingStore } from "@/stores/bookingStore";
import { formatDate, getNext7Days, cn } from "@/utils";

const SCREEN_TYPE_COLOR = {
  "2D": "badge-gray",
  "3D": "badge-blue",
  IMAX: "badge-gold",
  "4DX": "badge-red",
};

export default function SelectShowtime() {
  const { movieId } = useParams();
  const navigate = useNavigate();
  const { setShowtime, setMovie, setDate, selectedDate } = useBookingStore();

  const [selectedCinema, setSelectedCinema] = useState("");
  const days = getNext7Days();

  const { data: movie } = useQuery({
    queryKey: ["movies", "detail", movieId],
    queryFn: () => movieApi.getById(movieId),
    enabled: !!movieId,
  });

  const { data: cinemas } = useQuery({
    queryKey: ["cinemas"],
    queryFn: () => cinemaApi.getAll(),
  });

  const { data: showtimes, isLoading } = useQuery({
    queryKey: ["showtimes", movieId, selectedCinema, selectedDate],
    queryFn: () =>
      showtimeApi.getByMovie(
        movieId,
        selectedCinema || undefined,
        selectedDate,
      ),
    enabled: !!movieId,
  });

  // Group by cinema
  const grouped =
    showtimes?.reduce((acc, st) => {
      if (!acc[st.cinemaId]) acc[st.cinemaId] = [];
      acc[st.cinemaId].push(st);
      return acc;
    }, {}) ?? {};

  const handleSelect = (showtime) => {
    setShowtime(showtime);
    if (movie) setMovie(movie);
    navigate(`/booking/seats/${showtime.id}`);
  };

  return (
    <div className="min-h-screen bg-cinema-900 pt-24 pb-16">
      <div className="max-w-4xl mx-auto px-4 sm:px-6">
        {/* Header */}
        <div className="flex items-start gap-4 mb-8">
          <button
            onClick={() => navigate(-1)}
            className="p-2.5 mt-1 rounded-xl glass border border-white/8 text-cinema-200
              hover:text-white transition-all flex-shrink-0"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="font-display text-2xl font-bold text-white mb-1">
              Chọn suất chiếu
            </h1>
            {movie && (
              <p className="text-brand-400 font-medium">{movie.title}</p>
            )}
          </div>
        </div>

        {/* Date selector */}
        <div className="mb-6">
          <p className="text-cinema-400 text-xs uppercase tracking-widest mb-3 font-medium">
            Chọn ngày
          </p>
          <div className="flex gap-2 overflow-x-auto scrollbar-hide pb-2">
            {days.map((day) => {
              const isToday = day === days[0];
              const isSelected = day === selectedDate;
              return (
                <button
                  key={day}
                  onClick={() => setDate(day)}
                  className={cn(
                    "flex-shrink-0 flex flex-col items-center px-4 py-3 rounded-xl",
                    "border transition-all duration-200 min-w-[72px]",
                    isSelected
                      ? "bg-brand-500/15 border-brand-500/60 text-white"
                      : "border-white/8 text-cinema-300 hover:border-white/20 hover:text-white",
                  )}
                >
                  <span className="text-xs mb-0.5 font-medium">
                    {isToday ? "Hôm nay" : formatDate(day, "EEE")}
                  </span>
                  <span
                    className={cn(
                      "text-xl font-bold font-display",
                      isSelected ? "text-brand-400" : "",
                    )}
                  >
                    {formatDate(day, "dd")}
                  </span>
                  <span className="text-xs text-cinema-500">
                    {formatDate(day, "MM/yy")}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Cinema filter */}
        <div className="mb-6">
          <p className="text-cinema-400 text-xs uppercase tracking-widest mb-3 font-medium">
            Lọc theo rạp
          </p>
          <div className="flex flex-wrap gap-2">
            <button
              onClick={() => setSelectedCinema("")}
              className={cn(
                "px-4 py-2 rounded-xl border text-sm transition-all",
                !selectedCinema
                  ? "bg-brand-500/15 border-brand-500/40 text-white"
                  : "border-white/8 text-cinema-300 hover:text-white",
              )}
            >
              Tất cả rạp
            </button>
            {cinemas?.map((c) => (
              <button
                key={c.id}
                onClick={() => setSelectedCinema(c.id)}
                className={cn(
                  "px-4 py-2 rounded-xl border text-sm transition-all",
                  selectedCinema === c.id
                    ? "bg-brand-500/15 border-brand-500/40 text-white"
                    : "border-white/8 text-cinema-300 hover:text-white",
                )}
              >
                {c.name}
              </button>
            ))}
          </div>
        </div>

        {/* Showtimes by cinema */}
        {isLoading ? (
          <div className="space-y-4">
            {[1, 2].map((i) => (
              <div key={i} className="skeleton h-36 rounded-2xl" />
            ))}
          </div>
        ) : Object.keys(grouped).length === 0 ? (
          <div className="text-center py-16 text-cinema-400">
            <Clock className="w-12 h-12 mx-auto mb-3 opacity-30" />
            <p>Không có suất chiếu nào</p>
          </div>
        ) : (
          <div className="space-y-4">
            {Object.entries(grouped).map(([cinemaId, sts]) => {
              const cinema = cinemas?.find((c) => c.id === cinemaId);
              return (
                <motion.div
                  key={cinemaId}
                  initial={{ opacity: 0, y: 12 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="card-cinema p-5"
                >
                  <div className="flex items-start gap-3 mb-4">
                    <div
                      className="w-9 h-9 rounded-xl bg-brand-500/10 flex items-center
                      justify-center flex-shrink-0"
                    >
                      <MapPin className="w-5 h-5 text-brand-400" />
                    </div>
                    <div>
                      <h3 className="font-display font-bold text-white">
                        {sts[0].cinemaName}
                      </h3>
                      <p className="text-cinema-400 text-sm">
                        {cinema?.address}
                      </p>
                    </div>
                  </div>

                  {/* Group by screen */}
                  {Object.entries(
                    sts.reduce((acc, st) => {
                      const key = `${st.screenName}__${st.screenType}`;
                      if (!acc[key]) acc[key] = [];
                      acc[key].push(st);
                      return acc;
                    }, {}),
                  ).map(([screenKey, screenSts]) => {
                    const [screenName, screenType] = screenKey.split("__");
                    return (
                      <div key={screenKey} className="mb-3">
                        <div className="flex items-center gap-2 mb-2">
                          <Monitor className="w-3.5 h-3.5 text-cinema-400" />
                          <span className="text-cinema-300 text-sm">
                            {screenName}
                          </span>
                          <span
                            className={cn(
                              "badge text-xs",
                              SCREEN_TYPE_COLOR[screenType] ?? "badge-gray",
                            )}
                          >
                            {screenType}
                          </span>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          {screenSts.map((st) => {
                            const isFull = st.availableSeats === 0;
                            const isLow =
                              st.availableSeats <= 10 && st.availableSeats > 0;
                            return (
                              <button
                                key={st.id}
                                disabled={isFull}
                                onClick={() => handleSelect(st)}
                                className={cn(
                                  "relative px-4 py-2.5 rounded-xl border text-sm font-medium",
                                  "transition-all duration-200",
                                  isFull
                                    ? "border-cinema-700 text-cinema-600 cursor-not-allowed bg-cinema-800/40"
                                    : "border-white/10 text-white hover:border-brand-500/60 hover:bg-brand-500/8",
                                )}
                              >
                                <span className="font-mono">
                                  {st.startTime.slice(11, 16)}
                                </span>
                                {isLow && (
                                  <span
                                    className="absolute -top-1.5 -right-1 text-[10px] px-1.5 py-0.5
                                    rounded-full bg-gold-400/20 border border-gold-400/40 text-gold-400"
                                  >
                                    {st.availableSeats} ghế
                                  </span>
                                )}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    );
                  })}
                </motion.div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
