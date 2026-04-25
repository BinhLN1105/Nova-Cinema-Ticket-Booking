import React, { useState, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { 
  MessageCircle, 
  X, 
  Send, 
  Bot, 
  User, 
  Loader2, 
  ChevronDown,
  RefreshCw
} from "lucide-react";
import { chatbotApi } from "@/api/endpoints";
import { useAuthStore } from "@/stores/authStore";
import { cn } from "@/utils";
import toast from "react-hot-toast";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

const WELCOME_MESSAGE = {
  id: "welcome",
  text: "Xin chào! Em là Nova, trợ lý ảo của NovaTicket. Em có thể giúp gì cho anh/chị ạ?",
  sender: "bot",
  time: new Date()
};

export function AiChatbot() {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([WELCOME_MESSAGE]);
  const [inputValue, setInputValue] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const { isAuthenticated } = useAuthStore();
  
  const scrollRef = useRef(null);
  const inputRef = useRef(null);

  // Auto scroll to bottom
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, isLoading]);

  // Focus input when opened
  useEffect(() => {
    if (isOpen && inputRef.current) {
      setTimeout(() => inputRef.current.focus(), 300);
    }
  }, [isOpen]);

  const handleSend = async (e) => {
    if (e) e.preventDefault();
    if (!inputValue.trim() || isLoading) return;

    if (!isAuthenticated) {
      toast.error("Vui lòng đăng nhập để trò chuyện với AI");
      return;
    }

    const userMsg = {
      id: Date.now().toString(),
      text: inputValue.trim(),
      sender: "user",
      time: new Date()
    };

    setMessages(prev => [...prev, userMsg]);
    setInputValue("");
    setIsLoading(true);

    try {
      const response = await chatbotApi.chat(userMsg.text);
      
      const botMsg = {
        id: (Date.now() + 1).toString(),
        text: response.reply || "Em xin lỗi, em chưa hiểu ý anh/chị lắm.",
        sender: "bot",
        time: new Date()
      };
      
      setMessages(prev => [...prev, botMsg]);
    } catch (error) {
      console.error("Chatbot API error:", error);
      // toast.error sẽ được handle bởi apiClient interceptor nếu status != 401
    } finally {
      setIsLoading(false);
    }
  };

  const handleClearChat = async () => {
    if (!isAuthenticated) return;
    try {
      await chatbotApi.clearSession();
      setMessages([WELCOME_MESSAGE]);
      toast.success("Đã làm mới cuộc hội thoại");
    } catch (error) {
      console.error("Clear session error:", error);
    }
  };

  return (
    <div className="fixed bottom-6 right-6 z-[60] font-sans">
      {/* ── Chat Window ── */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, scale: 0.9, y: 20, transformOrigin: "bottom right" }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9, y: 20 }}
            className="absolute bottom-20 right-0 w-[380px] max-w-[calc(100vw-48px)] h-[550px] max-h-[calc(100vh-120px)]
              glass-dark border border-white/10 rounded-3xl shadow-card-float overflow-hidden flex flex-col"
          >
            {/* Header */}
            <div className="px-6 py-4 bg-white/[0.03] border-b border-white/5 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-brand-500/20 flex items-center justify-center border border-brand-500/30">
                  <Bot className="w-6 h-6 text-brand-400" />
                </div>
                <div>
                  <h3 className="font-bold text-base text-white leading-tight">Nova Assistant</h3>
                  <div className="flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
                    <span className="text-[11px] text-cinema-400 uppercase font-bold tracking-wider">Trực tuyến</span>
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-1">
                <button 
                  onClick={handleClearChat}
                  title="Xóa hội thoại"
                  className="p-2 rounded-lg text-cinema-400 hover:text-white hover:bg-white/5 transition-all"
                >
                  <RefreshCw className="w-4 h-4" />
                </button>
                <button 
                  onClick={() => setIsOpen(false)}
                  className="p-2 rounded-lg text-cinema-400 hover:text-white hover:bg-white/5 transition-all"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>

            {/* Messages Body */}
            <div 
              ref={scrollRef}
              className="flex-1 overflow-y-auto p-6 space-y-6 scrollbar-hide"
            >
              {messages.map((msg) => (
                <div 
                  key={msg.id}
                  className={cn(
                    "flex flex-col max-w-[85%]",
                    msg.sender === "user" ? "ml-auto items-end" : "items-start"
                  )}
                >
                  <div className={cn(
                    "px-4 py-3 rounded-2xl text-sm leading-relaxed",
                    msg.sender === "user" 
                      ? "bg-brand-500 text-white rounded-tr-none shadow-glow-red" 
                      : "bg-white/8 text-cinema-100 rounded-tl-none border border-white/5"
                  )}>
                    {msg.sender === "bot" ? (
                      <ReactMarkdown 
                        remarkPlugins={[remarkGfm]}
                        components={{
                          table: ({node, ...props}) => (
                            <div className="overflow-x-auto my-3 -mx-1">
                              <table className="border-collapse border border-white/10 w-full text-xs" {...props} />
                            </div>
                          ),
                          th: ({node, ...props}) => <th className="border border-white/10 px-2 py-1.5 bg-white/5 text-left font-bold" {...props} />,
                          td: ({node, ...props}) => <td className="border border-white/10 px-2 py-1.5 text-cinema-300" {...props} />,
                          ul: ({node, ...props}) => <ul className="list-disc ml-4 my-2 space-y-1" {...props} />,
                          ol: ({node, ...props}) => <ol className="list-decimal ml-4 my-2 space-y-1" {...props} />,
                          p: ({node, ...props}) => <p className="mb-2 last:mb-0" {...props} />,
                          a: ({node, ...props}) => <a className="text-brand-400 hover:underline" {...props} />,
                          code: ({node, ...props}) => <code className="bg-white/10 px-1 rounded text-xs" {...props} />,
                          strong: ({node, ...props}) => <strong className="font-bold text-white" {...props} />
                        }}
                      >
                        {msg.text}
                      </ReactMarkdown>
                    ) : (
                      msg.text
                    )}
                  </div>
                  <span className="text-[10px] text-cinema-500 mt-1.5 px-1 font-medium">
                    {msg.time.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </span>
                </div>
              ))}
              
              {isLoading && (
                <div className="flex items-start gap-2.5 max-w-[85%]">
                  <div className="bg-white/8 text-cinema-200 px-4 py-3 rounded-2xl rounded-tl-none border border-white/5">
                    <div className="flex gap-1">
                      <motion.span 
                        animate={{ opacity: [0.3, 1, 0.3] }} 
                        transition={{ repeat: Infinity, duration: 1, delay: 0 }}
                        className="w-1.5 h-1.5 rounded-full bg-cinema-400" 
                      />
                      <motion.span 
                        animate={{ opacity: [0.3, 1, 0.3] }} 
                        transition={{ repeat: Infinity, duration: 1, delay: 0.2 }}
                        className="w-1.5 h-1.5 rounded-full bg-cinema-400" 
                      />
                      <motion.span 
                        animate={{ opacity: [0.3, 1, 0.3] }} 
                        transition={{ repeat: Infinity, duration: 1, delay: 0.4 }}
                        className="w-1.5 h-1.5 rounded-full bg-cinema-400" 
                      />
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* Footer / Input */}
            <div className="p-4 bg-white/[0.02] border-t border-white/5">
              <form 
                onSubmit={handleSend}
                className="relative flex items-center gap-2"
              >
                <input
                  ref={inputRef}
                  type="text"
                  value={inputValue}
                  onChange={(e) => setInputValue(e.target.value)}
                  placeholder="Hỏi Nova về lịch chiếu, vé, rạp..."
                  className="flex-1 bg-white/5 border border-white/10 rounded-2xl px-5 py-3 pr-12
                    text-sm text-white placeholder:text-cinema-500 focus:outline-none focus:border-brand-500/50 
                    focus:bg-white/[0.08] transition-all"
                />
                <button
                  type="submit"
                  disabled={!inputValue.trim() || isLoading}
                  className="absolute right-2 p-2 rounded-xl bg-brand-500 text-white 
                    hover:bg-brand-600 disabled:opacity-50 disabled:bg-cinema-700 
                    transition-all shadow-glow-red"
                >
                  {isLoading ? <Loader2 className="w-5 h-5 animate-spin" /> : <Send className="w-5 h-5" />}
                </button>
              </form>
              <p className="text-[10px] text-center text-cinema-500 mt-3 font-medium">
                NOVA CINEMA WITH LOVE &lt;3
              </p>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Trigger Button (FAB) ── */}
      <motion.button
        whileHover={{ scale: 1.05 }}
        whileTap={{ scale: 0.95 }}
        onClick={() => setIsOpen(!isOpen)}
        className={cn(
          "relative w-14 h-14 rounded-2xl flex items-center justify-center transition-all duration-300 shadow-card-float",
          isOpen 
            ? "bg-cinema-800 text-white border border-white/10 rotate-90" 
            : "bg-brand-500 text-white shadow-glow-red"
        )}
      >
        <AnimatePresence mode="wait">
          {isOpen ? (
            <motion.div
              key="close"
              initial={{ opacity: 0, rotate: -45 }}
              animate={{ opacity: 1, rotate: 0 }}
              exit={{ opacity: 0, rotate: 45 }}
            >
              <X className="w-7 h-7" />
            </motion.div>
          ) : (
            <motion.div
              key="open"
              initial={{ opacity: 0, rotate: 45 }}
              animate={{ opacity: 1, rotate: 0 }}
              exit={{ opacity: 0, rotate: -45 }}
              className="relative"
            >
              <MessageCircle className="w-7 h-7 fill-white/20" />
              <div className="absolute -top-1 -right-1 w-3 h-3 bg-white rounded-full border-2 border-brand-500 animate-pulse" />
            </motion.div>
          )}
        </AnimatePresence>
      </motion.button>
    </div>
  );
}
