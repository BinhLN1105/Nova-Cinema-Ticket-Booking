import { useState, useEffect } from "react";
import { Link, useSearchParams, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Lock, Eye, EyeOff, CheckCircle2, AlertCircle } from "lucide-react";
import { authApi } from "@/api/endpoints";
import { cn } from "@/utils";
import toast from "react-hot-toast";

const schema = z.object({
  password: z.string().min(6, "Mật khẩu ít nhất 6 ký tự"),
  confirmPassword: z.string().min(6, "Vui lòng xác nhận mật khẩu"),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Mật khẩu không khớp",
  path: ["confirmPassword"],
});

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get("token");
  
  const [showPass, setShowPass] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  useEffect(() => {
    if (!token) {
      toast.error("Thiếu token khôi phục mật khẩu!");
      navigate("/auth/login");
    }
  }, [token, navigate]);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (data) => {
    setIsSubmitting(true);
    try {
      await authApi.resetPassword(token, data.password);
      setIsSuccess(true);
      toast.success("Đặt lại mật khẩu thành công!");
    } catch (error) {
      const msg = error.response?.data?.message || "Có lỗi xảy ra, liên kết có thể đã hết hạn.";
      toast.error(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isSuccess) {
    return (
      <div className="text-center py-8">
        <div className="flex justify-center mb-6">
          <div className="w-20 h-20 bg-green-500/20 rounded-full flex items-center justify-center">
            <CheckCircle2 className="w-10 h-10 text-green-500" />
          </div>
        </div>
        <h1 className="font-display text-2xl font-bold text-white mb-4">
          Đặt lại mật khẩu thành công!
        </h1>
        <p className="text-cinema-300 mb-8 mx-auto">
          Bây giờ bạn có thể đăng nhập bằng mật khẩu mới của mình.
        </p>
        <Link
          to="/auth/login"
          className="btn-primary inline-flex items-center gap-2 px-10 py-3.5"
        >
          Đăng nhập ngay
        </Link>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-8 text-center">
        <h1 className="font-display text-3xl font-bold text-white mb-2">
          Mật khẩu mới
        </h1>
        <p className="text-cinema-300">
          Vui lòng nhập mật khẩu mới và bảo mật tuyệt đối.
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        {/* Password */}
        <div>
          <label className="block text-sm font-medium text-cinema-300 mb-2 ml-1">
            Mật khẩu mới
          </label>
          <div className="relative">
            <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-cinema-400" />
            <input
              {...register("password")}
              type={showPass ? "text" : "password"}
              placeholder="••••••••"
              disabled={isSubmitting}
              className={cn(
                "input-cinema pl-10 pr-10",
                errors.password && "border-brand-500",
              )}
            />
            <button
              type="button"
              onClick={() => setShowPass(!showPass)}
              className="absolute right-3.5 top-1/2 -translate-y-1/2 text-cinema-400"
            >
              {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
          </div>
          {errors.password && (
            <p className="text-brand-400 text-xs mt-1.5 ml-1">
              {errors.password.message}
            </p>
          )}
        </div>

        {/* Confirm Password */}
        <div>
          <label className="block text-sm font-medium text-cinema-300 mb-2 ml-1">
            Xác nhận mật khẩu mới
          </label>
          <div className="relative">
            <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-cinema-400" />
            <input
              {...register("confirmPassword")}
              type={showPass ? "text" : "password"}
              placeholder="••••••••"
              disabled={isSubmitting}
              className={cn(
                "input-cinema pl-10",
                errors.confirmPassword && "border-brand-500",
              )}
            />
          </div>
          {errors.confirmPassword && (
            <p className="text-brand-400 text-xs mt-1.5 ml-1">
              {errors.confirmPassword.message}
            </p>
          )}
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="btn-primary w-full py-3.5 text-base mt-2 disabled:opacity-60"
        >
          {isSubmitting ? "Đang xử lý..." : "Cập nhật mật khẩu"}
        </button>
      </form>
    </div>
  );
}
