import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { useAuthStore } from "@/stores/authStore";
import { authApi } from "@/api/endpoints";

export function useAuth() {
  const {
    user,
    tokens,
    isAuthenticated,
    setAuth,
    logout: storeLogout,
  } = useAuthStore();
  const navigate = useNavigate();
  const qc = useQueryClient();

  const loginMutation = useMutation({
    mutationFn: ({ email, password }) => authApi.login(email, password),
    onSuccess: (data) => {
      const tokens = { accessToken: data.accessToken, refreshToken: data.refreshToken };
      setAuth(data.user, tokens);
      toast.success(`Chào mừng, ${data.user.fullName}!`);
      // Redirect by role
      const role = data.user.role;
      if (role === "ADMIN") navigate("/admin/dashboard");
      else if (role === "STAFF") navigate("/staff/dashboard");
      else navigate("/");
    },
    onError: (error) => {
      const msg = error?.response?.data?.message || "Tài khoản hoặc mật khẩu không chính xác";
      toast.error(msg);
    },
  });

  const registerMutation = useMutation({
    mutationFn: authApi.register,
    onSuccess: (data) => {
      const tokens = { accessToken: data.accessToken, refreshToken: data.refreshToken };
      setAuth(data.user, tokens);
      toast.success("Đăng ký thành công!");
      navigate("/");
    },
  });

  const logout = async () => {
    try {
      if (tokens) await authApi.logout(tokens.refreshToken);
    } catch {}
    storeLogout();
    qc.clear();
    navigate("/auth/login");
    toast.success("Đã đăng xuất");
  };

  const socialLoginMutation = useMutation({
    mutationFn: ({ idToken, provider }) => authApi.socialLogin({ idToken, provider }),
    onSuccess: (data) => {
      const tokens = { accessToken: data.accessToken, refreshToken: data.refreshToken };
      setAuth(data.user, tokens);
      toast.success(`Chào mừng, ${data.user.fullName}!`);
      // Redirect by role
      const role = data.user.role;
      if (role === "ADMIN") navigate("/admin/dashboard");
      else if (role === "STAFF") navigate("/staff/dashboard");
      else navigate("/");
    },
  });

  return {
    user,
    tokens,
    isAuthenticated,
    isAdmin: user?.role === "ADMIN",
    isStaff: user?.role === "STAFF",
    isCustomer: user?.role === "CUSTOMER",
    login: loginMutation.mutate,
    register: registerMutation.mutate,
    socialLogin: socialLoginMutation.mutate,
    logout,
    isLoggingIn: loginMutation.isPending,
    isRegistering: registerMutation.isPending,
    isSocialLoggingIn: socialLoginMutation.isPending,
  };
}
