import { useState } from "react";
import { Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Eye, EyeOff, Mail, Lock, ArrowRight } from "lucide-react";
import { useGoogleLogin } from "@react-oauth/google";
import FacebookLogin from "react-facebook-login/dist/facebook-login-render-props";
import { useAuth } from "@/hooks";
import { cn } from "@/utils";
import toast from "react-hot-toast";

const schema = z.object({
  email: z.string().email("Email không hợp lệ"),
  password: z.string().min(6, "Mật khẩu ít nhất 6 ký tự"),
});

export default function LoginPage() {
  const { login, socialLogin, isLoggingIn, isSocialLoggingIn } = useAuth();
  const [showPass, setShowPass] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(schema),
  });

  const handleGoogleSuccess = (tokenResponse) => {
    // google sends access_token when flow is 'implicit', backend expects 'accessToken' for userinfo verification
    socialLogin({ idToken: tokenResponse.access_token, provider: "GOOGLE" });
  };

  const loginGoogle = useGoogleLogin({
    onSuccess: handleGoogleSuccess,
    onError: () => toast.error("Đăng nhập Google thất bại"),
  });

  const handleFacebookResponse = (response) => {
    if (response.accessToken) {
      socialLogin({ idToken: response.accessToken, provider: "FACEBOOK" });
    } else {
      toast.error("Đăng nhập Facebook thất bại");
    }
  };

  const isFormLoading = isLoggingIn || isSocialLoggingIn;

  return (
    <div>
      <div className="mb-8">
        <h1 className="font-display text-3xl font-bold text-white mb-2">
          Chào mừng trở lại
        </h1>
        <p className="text-cinema-300">Đăng nhập để tiếp tục trải nghiệm</p>
      </div>

      <form onSubmit={handleSubmit((d) => login(d))} className="space-y-4">
        {/* Email */}
        <div>
          <div className="relative">
            <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-cinema-400" />
            <input
              {...register("email")}
              type="email"
              placeholder="Email của bạn"
              disabled={isFormLoading}
              className={cn(
                "input-cinema pl-10",
                errors.email && "border-brand-500",
              )}
            />
          </div>
          {errors.email && (
            <p className="text-brand-400 text-xs mt-1.5 ml-1">
              {errors.email.message}
            </p>
          )}
        </div>

        {/* Password */}
        <div>
          <div className="relative">
            <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-cinema-400" />
            <input
              {...register("password")}
              type={showPass ? "text" : "password"}
              placeholder="Mật khẩu"
              disabled={isFormLoading}
              className={cn(
                "input-cinema pl-10 pr-10",
                errors.password && "border-brand-500",
              )}
            />

            <button
              type="button"
              onClick={() => setShowPass(!showPass)}
              className="absolute right-3.5 top-1/2 -translate-y-1/2 text-cinema-400
                hover:text-white transition-colors"
            >
              {showPass ? (
                <EyeOff className="w-4 h-4" />
              ) : (
                <Eye className="w-4 h-4" />
              )}
            </button>
          </div>
          {errors.password && (
            <p className="text-brand-400 text-xs mt-1.5 ml-1">
              {errors.password.message}
            </p>
          )}
        </div>

        <button
          type="submit"
          disabled={isFormLoading}
          className="btn-primary w-full py-3.5 text-base mt-2 disabled:opacity-60"
        >
          {isLoggingIn ? (
            <span className="flex items-center gap-2">
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              Đang đăng nhập...
            </span>
          ) : (
            <span className="flex items-center gap-2">
              Đăng nhập <ArrowRight className="w-4 h-4" />
            </span>
          )}
        </button>
      </form>

      <div className="mt-6 flex items-center justify-between">
        <span className="border-b border-cinema-700 w-1/5 lg:w-1/4"></span>
        <span className="text-xs text-center text-cinema-400 uppercase">Hoặc đăng nhập bằng</span>
        <span className="border-b border-cinema-700 w-1/5 lg:w-1/4"></span>
      </div>

      <div className="mt-6 flex flex-col sm:flex-row gap-4">
        <button
          type="button"
          disabled={isFormLoading}
          onClick={() => loginGoogle()}
          className="flex-1 flex items-center justify-center gap-2 py-2.5 bg-white text-gray-800 rounded-md border border-gray-200 font-medium hover:bg-gray-50 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
        >
          {isSocialLoggingIn ? (
            <div className="w-5 h-5 border-2 border-gray-300 border-t-gray-800 rounded-full animate-spin" />
          ) : (
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                fill="#4285F4"
              />
              <path
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                fill="#34A853"
              />
              <path
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                fill="#FBBC05"
              />
              <path
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                fill="#EA4335"
              />
            </svg>
          )}
          <span className="text-[14px]">Google</span>
        </button>

        <FacebookLogin
          appId={import.meta.env.VITE_FACEBOOK_APP_ID}
          fields="name,email,picture"
          scope="public_profile,email"
          authType="rerequest"
          callback={handleFacebookResponse}
          render={(renderProps) => (
            <button
              type="button"
              disabled={isFormLoading}
              onClick={renderProps.onClick}
              className="flex-1 flex items-center justify-center gap-2 py-2.5 bg-[#1877F2] text-white rounded-md border border-[#1877F2] font-medium hover:bg-[#1865F2] transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {isSocialLoggingIn ? (
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.469h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.469h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
                </svg>
              )}
              <span className="text-[14px] opacity-95">Facebook</span>
            </button>
          )}
        />
      </div>

      <p className="text-center text-cinema-300 text-sm mt-8">
        Chưa có tài khoản?{" "}
        <Link
          to="/auth/register"
          className="text-brand-400 hover:text-brand-300 font-medium"
        >
          Đăng ký ngay
        </Link>
      </p>
    </div>
  );
}
