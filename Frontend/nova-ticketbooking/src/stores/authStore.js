import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";

export const useAuthStore = create()(
  persist(
    (set) => ({
      user: null,
      tokens: null,
      isAuthenticated: false,

      setAuth: (user, tokens) => set({ user, tokens, isAuthenticated: true }),

      setTokens: (tokens) => set({ tokens }),

      setUser: (user) => set({ user }),

      logout: () => set({ user: null, tokens: null, isAuthenticated: false }),
    }),
    {
      name: "nova-auth",
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        user: state.user,
        tokens: state.tokens,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
);
