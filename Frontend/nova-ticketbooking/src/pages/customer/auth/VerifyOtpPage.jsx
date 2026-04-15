import { useState } from "react";
import { Link, useSearchParams, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Hash, ArrowLeft, ShieldCheck } from "lucide-react";
import { authApi } from "@/api/endpoints";
import { cn } from "@/utils";
import toast from "react-hot-toast";

const schema = z.object({
  otp: z.string().length(6, "Mã OTP phải có đúng 6 chữ số"),
});

export default function VerifyOtpPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const email = searchParams.get("email");
  
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (data) => {
    if (!email) {
      toast.error("Thiếu thông tin email!");
      return;
    }
    
    setIsSubmitting(true);
    try {
      const response = await authApi.verifyOtp(email, data.otp);
      // API helper đã tự động trả về r.data.data nên response chính là resetToken
      const resetToken = response; 
      
      toast.success("Xác thực thành công!");
      // Chuyển sang trang đặt lại mật khẩu kèm theo token
      navigate(`/auth/reset-password?token=${resetToken}`);
    } catch (error) {
      const msg = error.response?.data?.message || "Mã OTP không chính xác hoặc đã hết hạn.";
      toast.error(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!email) {
    return (
      <div className="text-center py-8">
        <h1 className="text-white text-xl mb-4">Lỗi truy cập</h1>
        <p className="text-cinema-300 mb-6">Không tìm thấy thông tin email để xác thực.</p>
        <Link to="/auth/forgot-password" className="btn-primary px-6 py-2">Quay lại</Link>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-8">
        <div className="w-12 h-12 bg-brand-500/10 rounded-xl flex items-center justify-center mb-4">
          <ShieldCheck className="w-6 h-6 text-brand-400" />
        </div>
        <h1 className="font-display text-3xl font-bold text-white mb-2">
          Xác thực OTP
        </h1>
        <p className="text-cinema-300">
          Vui lòng nhập mã 6 số chúng tôi vừa gửi đến <br />
          <span className="text-white font-medium">{email}</span>
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-cinema-300 mb-2 ml-1">
            Mã xác thực
          </label>
          <div className="relative">
            <Hash className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-cinema-400" />
            <input
              {...register("otp")}
              type="text"
              maxLength={6}
              placeholder="000000"
              disabled={isSubmitting}
              className={cn(
                "input-cinema pl-10 tracking-[0.5em] font-mono text-lg",
                errors.otp && "border-brand-500"
              )}
            />
          </div>
          {errors.otp && (
            <p className="text-brand-400 text-xs mt-1.5 ml-1">
              {errors.otp.message}
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
              Đang xác thực...
            </span>
          ) : (
            "Xác nhận mã"
          )}
        </button>
      </form>

      <div className="mt-8 text-center space-y-4">
        <p className="text-cinema-400 text-sm">
          Không nhận được mã?{" "}
          <button 
            onClick={() => navigate(-1)}
            className="text-brand-400 hover:text-brand-300 font-medium"
          >
            Gửi lại
          </button>
        </p>
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
