import { useState, useRef } from 'react'
import { Upload, Link as LinkIcon, X, Check, Loader2, Image as ImageIcon, AlertCircle } from 'lucide-react'
import { cn } from '@/utils'
import toast from 'react-hot-toast'

/**
 * ImageUploader Component
 * Supports both File Upload and External URL fetching
 * 
 * @param {Object} props
 * @param {string} props.value - Current image URL
 * @param {Function} props.onUpload - Function to handle the actual upload/fetch (API call)
 * @param {string} props.label - Field label
 * @param {string} props.helperText - Subtext below label
 * @param {string} props.aspectRatio - Aspect ratio hint (e.g. "2:3", "16:9")
 * @param {boolean} props.isLoading - External loading state (if any)
 */
export default function ImageUploader({ 
  value = '', 
  onUpload, 
  label = 'Hình ảnh', 
  helperText = 'Định dạng JPG, PNG hoặc WebP. Tối đa 10MB.',
  aspectRatio = '2:3',
  isLoading = false,
  darkMode = false
}) {
  const [activeTab, setActiveTab] = useState('file') // 'file' | 'url'
  const [urlInput, setUrlInput] = useState('')
  const [isLocalUploading, setIsLocalUploading] = useState(false)
  const fileInputRef = useRef(null)

  const isWorking = isLoading || isLocalUploading

  // Determine container aspect ratio class
  const aspectRatioClass = aspectRatio === '16:9' ? 'aspect-video' : 'aspect-[2/3]'

  const handleFileChange = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return

    // Basic validation
    if (file.size > 10 * 1024 * 1024) {
      toast.error('Dung lượng ảnh tối đa 10MB')
      return
    }

    if (!file.type.startsWith('image/')) {
      toast.error('Vui lòng chọn file hình ảnh')
      return
    }

    try {
      setIsLocalUploading(true)
      await onUpload(file, 'file')
      toast.success('Tải ảnh lên thành công')
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Tải ảnh thất bại')
    } finally {
      setIsLocalUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  const handleUrlSubmit = async (e) => {
    e.preventDefault()
    if (!urlInput.trim()) return

    if (!urlInput.startsWith('http')) {
      toast.error('URL không hợp lệ (phải bắt đầu bằng http:// hoặc https://)')
      return
    }

    try {
      setIsLocalUploading(true)
      await onUpload(urlInput, 'url')
      toast.success('Đã lấy ảnh từ URL thành công')
      setUrlInput('')
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Không thể lấy ảnh từ URL này')
    } finally {
      setIsLocalUploading(false)
    }
  }

  return (
    <div className="space-y-4">
      {/* Label & Header */}
      <div className="flex justify-between items-end">
        <div>
          <label className={cn("block text-sm font-semibold mb-1", darkMode ? "text-white" : "text-gray-700")}>{label}</label>
          <p className={cn("text-xs italic", darkMode ? "text-cinema-400" : "text-gray-500")}>{helperText}</p>
        </div>
        <div className={cn(
          "text-[10px] font-bold uppercase tracking-wider px-2 py-1 rounded-md border",
          darkMode ? "bg-white/5 text-cinema-400 border-white/5" : "bg-gray-50 text-gray-400 border-gray-100"
        )}>
          Ratio {aspectRatio}
        </div>
      </div>

      <div className={cn(
        "grid grid-cols-1 md:grid-cols-12 gap-6 p-4 rounded-3xl transition-all duration-300 border",
        darkMode 
          ? "bg-cinema-800/40 backdrop-blur-md border-white/10 shadow-none" 
          : "bg-white border-gray-100 shadow-sm hover:shadow-md"
      )}>
        
        {/* Preview Area */}
        <div className="md:col-span-5 lg:col-span-4">
          <div className={cn(
            "relative group rounded-2xl overflow-hidden transition-all duration-500 border-2 border-dashed",
            aspectRatioClass,
            value 
              ? (darkMode ? "border-white/20" : "border-solid border-white ring-1 ring-gray-100") 
              : (darkMode ? "bg-white/5 border-white/10 hover:border-brand-500/50 hover:bg-white/10" : "hover:border-brand-300 hover:bg-brand-50/30 bg-gray-50 border-gray-200")
          )}>
            {value ? (
              <>
                <img 
                  src={value} 
                  alt="Preview" 
                  className={cn(
                    "w-full h-full object-cover transition-transform duration-700 group-hover:scale-110",
                    isWorking && "opacity-50 blur-sm grayscale"
                  )} 
                />
                <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                  <div className="bg-white/20 backdrop-blur-md p-2 rounded-full text-white">
                    <Check className="w-5 h-5" />
                  </div>
                </div>
              </>
            ) : (
              <div className="absolute inset-0 flex flex-col items-center justify-center space-y-2">
                <div className={cn(
                  "p-3 rounded-2xl shadow-sm mb-1 group-hover:scale-110 transition-transform duration-300",
                  darkMode ? "bg-white/5" : "bg-white"
                )}>
                  <ImageIcon className={cn("w-8 h-8", darkMode ? "text-cinema-400" : "text-gray-300")} />
                </div>
                <p className={cn("text-xs font-medium uppercase tracking-widest", darkMode ? "text-cinema-500" : "text-gray-300")}>No Image</p>
              </div>
            )}

            {/* Loading Overlay */}
            {isWorking && (
              <div className={cn(
                "absolute inset-0 backdrop-blur-[2px] flex flex-col items-center justify-center z-10 animate-in fade-in duration-300",
                darkMode ? "bg-cinema-900/60" : "bg-white/60"
              )}>
                <Loader2 className="w-8 h-8 text-brand-600 animate-spin mb-2" />
                <p className="text-[10px] font-bold text-brand-600 uppercase tracking-widest animate-pulse">Processing...</p>
              </div>
            )}
          </div>
        </div>

        {/* Action Area */}
        <div className="md:col-span-7 lg:col-span-8 flex flex-col">
          {/* Custom Underline Tabs */}
          <div className={cn("flex border-b mb-6", darkMode ? "border-white/5" : "border-gray-100")}>
            <button
              type="button"
              onClick={() => setActiveTab('file')}
              disabled={isWorking}
              className={cn(
                "pb-3 px-4 text-sm font-medium transition-all relative",
                activeTab === 'file' 
                  ? "text-brand-500" 
                  : (darkMode ? "text-cinema-400 hover:text-white" : "text-gray-400 hover:text-gray-600")
              )}
            >
              <div className="flex items-center gap-2">
                <Upload className="w-4 h-4" />
                Tải file lên
              </div>
              {activeTab === 'file' && (
                <div className="absolute bottom-0 left-0 w-full h-0.5 bg-brand-600 rounded-full animate-in slide-in-from-left-2 duration-300" />
              )}
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('url')}
              disabled={isWorking}
              className={cn(
                "pb-3 px-4 text-sm font-medium transition-all relative",
                activeTab === 'url' 
                  ? "text-brand-500" 
                  : (darkMode ? "text-cinema-400 hover:text-white" : "text-gray-400 hover:text-gray-600")
              )}
            >
              <div className="flex items-center gap-2">
                <LinkIcon className="w-4 h-4" />
                Dán liên kết
              </div>
              {activeTab === 'url' && (
                <div className="absolute bottom-0 left-0 w-full h-0.5 bg-brand-500 rounded-full animate-in slide-in-from-right-2 duration-300" />
              )}
            </button>
          </div>

          <div className="flex-1 flex flex-col">
            {activeTab === 'file' ? (
              <div className="animate-in fade-in slide-in-from-bottom-2">
                <p className={cn("text-sm mb-6 leading-relaxed", darkMode ? "text-cinema-300" : "text-gray-500")}>
                  Kéo thả ảnh vào đây hoặc nhấp để chọn từ thiết bị của bạn. 
                  Hệ thống hỗ trợ các định dạng phổ biến với dung lượng cao.
                </p>
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleFileChange}
                  accept="image/*"
                  className="hidden"
                />
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={isWorking}
                  className={cn(
                    "w-full flex items-center justify-center gap-2 py-4 text-white rounded-2xl font-bold transition-all shadow-lg active:scale-[0.98]",
                    isWorking 
                      ? (darkMode ? "bg-white/5 text-cinema-500" : "bg-gray-100 text-gray-400") 
                      : "bg-brand-500 hover:bg-brand-600 shadow-brand-500/20"
                  )}
                >
                  <Upload className="w-5 h-5" />
                  Chọn file từ máy
                </button>
              </div>
            ) : (
              <div className="animate-in fade-in slide-in-from-bottom-2">
                <p className={cn("text-sm mb-4 leading-relaxed", darkMode ? "text-cinema-300" : "text-gray-500")}>
                  Nhập địa chỉ URL của hình ảnh (từ IMDB, Unsplash, Google...). 
                  Hệ thống sẽ tự động tối ưu hóa và lưu trữ ảnh này.
                </p>
                <div className="space-y-4">
                  <div className="relative">
                    <div className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400">
                      <LinkIcon className="w-5 h-5" />
                    </div>
                    <input
                      type="url"
                      value={urlInput}
                      onChange={(e) => setUrlInput(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          e.preventDefault(); // Ngăn submit form cha
                          handleUrlSubmit(e);
                        }
                      }}
                      placeholder="https://example.com/image.jpg"
                      disabled={isWorking}
                      className={cn(
                        "w-full pl-12 pr-4 py-4 rounded-2xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all border",
                        darkMode ? "bg-white/5 border-white/10 text-white" : "bg-gray-50 border-gray-100"
                      )}
                    />
                  </div>
                  <button
                    type="button"
                    onClick={handleUrlSubmit}
                    disabled={isWorking || !urlInput.trim()}
                    className={cn(
                      "w-full flex items-center justify-center gap-2 py-4 text-white rounded-2xl font-bold transition-all shadow-lg active:scale-[0.98]",
                      isWorking || !urlInput.trim()
                        ? (darkMode ? "bg-white/5 text-cinema-500" : "bg-gray-100 text-gray-400") 
                        : "bg-brand-500 hover:bg-brand-600 shadow-brand-500/20"
                    )}
                  >
                    <Check className="w-5 h-5" />
                    Xác nhận URL
                  </button>
                </div>
              </div>
            )}

            {/* Info Box */}
            <div className={cn(
              "mt-auto pt-6 flex items-start gap-2 text-[11px] p-3 rounded-xl border",
              darkMode 
                ? "text-brand-400 bg-brand-500/10 border-brand-500/20" 
                : "text-orange-600 bg-orange-50/50 border-orange-100/50"
            )}>
              <AlertCircle className="w-3.5 h-3.5 mt-0.5 flex-shrink-0" />
              <span>
                <b>Lưu ý:</b> Thao tác tải lên sẽ thay đổi ảnh hiện tại ngay lập tức. 
                Hãy chắc chắn bạn đã chọn đúng tệp tin.
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
