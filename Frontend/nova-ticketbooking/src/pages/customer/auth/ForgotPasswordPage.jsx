import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Mail, ArrowLeft, KeyRound } from "lucide-react";
import { authApi } from "@/api/endpoints";
import { cn } from "@/utils";
import toast from "react-hot-toast";

const schema = z.object({
  email: z.string().email("Email không hợp lệ"),
});

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);

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
      await authApi.forgotPassword(data.email);
      toast.success("Mã xác thực đã được gửi!");
      // Chuyển sang trang nhập OTP kèm email qua query param
      navigate(`/auth/verify-otp?email=${encodeURIComponent(data.email)}`);
    } catch (error) {
      // Backend thường trả về thành công giả lập để bảo mật, 
      // nhưng nếu lỗi hệ thống thật sự thì thông báo.
      toast.error("Có lỗi xảy ra, vui lòng thử lại sau.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div>
      <div className="mb-8">
        <div className="w-12 h-12 bg-brand-500/10 rounded-xl flex items-center justify-center mb-4">
          <KeyRound className="w-6 h-6 text-brand-400" />
        </div>
        <h1 className="font-display text-3xl font-bold text-white mb-2">
          Quên mật khẩu?
        </h1>
        <p className="text-cinema-300">
          Đừng lo lắng, hãy nhập email của bạn và chúng tôi sẽ gửi mã xác thực (OTP) để khôi phục.
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-cinema-300 mb-2 ml-1">
            Email đăng ký
          </label>
          <div className="relative">
            <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-cinema-400" />
            <input
              {...register("email")}
              type="email"
              placeholder="example@gmail.com"
              disabled={isSubmitting}
              className={cn(
                "input-cinema pl-10",
                errors.email && "border-brand-500"
              )}
            />
          </div>
          {errors.email && (
            <p className="text-brand-400 text-xs mt-1.5 ml-1">
              {errors.email.message}
            </p>
          )}
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="btn-primary w-full py-3.5 text-base disabled:opacity-60"
        >
          {isSubmitting ? (
            <span className="flex items-center justify-center gap-2">
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              Đang gửi mã...
            </span>
          ) : (
            "Tiếp tục"
          )}
        </button>
      </form>

      <div className="mt-8 text-center">
        <Link
          to="/auth/login"
          className="inline-flex items-center gap-2 text-cinema-400 hover:text-white transition-colors text-sm font-medium"
        >
          <ArrowLeft className="w-4 h-4" /> Quay lại đăng nhập
        </Link>
      </div>
    </div>
  );
}
