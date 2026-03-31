import { useState } from "react";
import { Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Mail, ArrowLeft, CheckCircle2 } from "lucide-react";
import { authApi } from "@/api/endpoints";
import { cn } from "@/utils";
import toast from "react-hot-toast";

const schema = z.object({
  email: z.string().email("Email không hợp lệ"),
});

export default function ForgotPasswordPage() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

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
      setIsSuccess(true);
      toast.success("Yêu cầu đã được gửi!");
    } catch (error) {
      // Vì lý do bảo mật, backend luôn trả về thành công giả lập.
      // Chỉ lỗi server thật sự mới rơi vào đây.
      toast.error("Có lỗi xảy ra, vui lòng thử lại sau.");
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
        <h1 className="font-display text-3xl font-bold text-white mb-4">
          Kiểm tra email của bạn
        </h1>
        <p className="text-cinema-300 mb-8 max-w-sm mx-auto">
          Nếu email của bạn tồn tại trong hệ thống, chúng tôi đã gửi một liên kết đặt lại mật khẩu đến đó. Vui lòng kiểm tra cả hòm thư rác.
        </p>
        <Link
          to="/auth/login"
          className="btn-primary inline-flex items-center gap-2 px-8 py-3"
        >
          <ArrowLeft className="w-4 h-4" /> Quay lại đăng nhập
        </Link>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-8">
        <h1 className="font-display text-3xl font-bold text-white mb-2">
          Quên mật khẩu?
        </h1>
        <p className="text-cinema-300">
          Đừng lo lắng, hãy nhập email của bạn và chúng tôi sẽ gửi liên kết khôi phục.
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
              Đang xử lý...
            </span>
          ) : (
            "Gửi liên kết khôi phục"
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
