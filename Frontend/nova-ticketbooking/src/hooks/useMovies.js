import { useQuery } from "@tanstack/react-query";
import { movieApi } from "@/api/endpoints";

export const movieKeys = {
  all: () => ["movies"],
  nowShowing: (page) => ["movies", "now-showing", page],
  comingSoon: (page) => ["movies", "coming-soon", page],
  detail: (id) => ["movies", "detail", id],
  search: (q) => ["movies", "search", q],
};

export function useMovies() {
  const nowShowing = useQuery({
    queryKey: movieKeys.nowShowing(),
    queryFn: () => movieApi.getNowShowing(),
    staleTime: 5 * 60 * 1000,
  });
  const comingSoon = useQuery({
    queryKey: movieKeys.comingSoon(),
    queryFn: () => movieApi.getComingSoon(),
    staleTime: 5 * 60 * 1000,
  });
  return { nowShowing, comingSoon };
}

export function useMovieDetail(id) {
  return useQuery({
    queryKey: movieKeys.detail(id),
    queryFn: () => movieApi.getById(id),
    enabled: !!id,
    staleTime: 10 * 60 * 1000,
  });
}
