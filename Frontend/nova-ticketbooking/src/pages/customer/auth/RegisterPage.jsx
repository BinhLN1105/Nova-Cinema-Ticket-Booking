import { useState } from "react";
import { Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Eye, EyeOff, Mail, Lock, User, Phone, ArrowRight } from "lucide-react";
import { useAuth } from "@/hooks";
import { cn } from "@/utils";

const schema = z
  .object({
    fullName: z.string().min(2, "Tên ít nhất 2 ký tự"),
    email: z.string().email("Email không hợp lệ"),
    phone: z.string().optional(),
    password: z.string().min(6, "Mật khẩu ít nhất 6 ký tự"),
    confirm: z.string(),
  })
  .refine((d) => d.password === d.confirm, {
    message: "Mật khẩu không khớp",
    path: ["confirm"],
  });

export default function RegisterPage() {
  const { register: doRegister, isRegistering } = useAuth();
  const [showPass, setShowPass] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(schema),
  });

  const fields = [
    { name: "fullName", icon: User, type: "text", placeholder: "Họ và tên" },
    { name: "email", icon: Mail, type: "email", placeholder: "Email" },
    {
      name: "phone",
      icon: Phone,
      type: "tel",
      placeholder: "Số điện thoại (tuỳ chọn)",
    },
    { name: "password", icon: Lock, type: "password", placeholder: "Mật khẩu" },
    {
      name: "confirm",
      icon: Lock,
      type: "password",
      placeholder: "Xác nhận mật khẩu",
    },
  ];

  return (
    <div>
      <div className="mb-8">
        <h1 className="font-display text-3xl font-bold text-white mb-2">
          Tạo tài khoản
        </h1>
        <p className="text-cinema-300">
          Tham gia NovaTicket để đặt vé nhanh chóng
        </p>
      </div>

      <form
        onSubmit={handleSubmit((d) => doRegister(d))}
        className="space-y-3.5"
      >
        {fields.map(({ name, icon: Icon, type, placeholder }) => (
          <div key={name}>
            <div className="relative">
              <Icon className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-cinema-400" />
              <input
                {...register(name)}
                type={
                  type === "password" ? (showPass ? "text" : "password") : type
                }
                placeholder={placeholder}
                className={cn(
                  "input-cinema pl-10",
                  errors[name] && "border-brand-500",
                )}
              />

              {(name === "password" || name === "confirm") && (
                <button
                  type="button"
                  onClick={() => setShowPass(!showPass)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2
                    text-cinema-400 hover:text-white transition-colors"
                >
                  {showPass ? (
                    <EyeOff className="w-4 h-4" />
                  ) : (
                    <Eye className="w-4 h-4" />
                  )}
                </button>
              )}
            </div>
            {errors[name] && (
              <p className="text-brand-400 text-xs mt-1.5 ml-1">
                {errors[name]?.message}
              </p>
            )}
          </div>
        ))}

        <button
          type="submit"
          disabled={isRegistering}
          className="btn-primary w-full py-3.5 text-base mt-2 disabled:opacity-60"
        >
          {isRegistering ? (
            <span className="flex items-center gap-2">
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              Đang tạo tài khoản...
            </span>
          ) : (
            <span className="flex items-center gap-2">
              Đăng ký <ArrowRight className="w-4 h-4" />
            </span>
          )}
        </button>
      </form>

      <p className="text-center text-cinema-300 text-sm mt-8">
        Đã có tài khoản?{" "}
        <Link
          to="/auth/login"
          className="text-brand-400 hover:text-brand-300 font-medium"
        >
          Đăng nhập
        </Link>
      </p>
    </div>
  );
}
