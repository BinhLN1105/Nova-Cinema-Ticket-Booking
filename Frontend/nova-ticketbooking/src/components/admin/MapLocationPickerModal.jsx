import { useState, useEffect, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { X, Search, MapPin, Navigation, Check, Loader2, Copy, Sparkles } from 'lucide-react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import toast from 'react-hot-toast'

const CITY_COORDINATES = {
  'Ho Chi Minh': { lat: 10.7769, lng: 106.7009, label: 'TP. Hồ Chí Minh' },
  'Ha Noi':      { lat: 21.0285, lng: 105.8542, label: 'Hà Nội' },
  'Da Nang':     { lat: 16.0544, lng: 108.2022, label: 'Đà Nẵng' },
  'Can Tho':     { lat: 10.0452, lng: 105.7469, label: 'Cần Thơ' },
  'Hai Phong':   { lat: 20.8449, lng: 106.6881, label: 'Hải Phòng' },
}

const createPinIcon = () => {
  return L.divIcon({
    className: 'custom-cinema-pin',
    html: `
      <div style="position: relative; display: flex; flex-direction: column; align-items: center; transform: translate(-50%, -100%); pointer-events: none;">
        <div style="
          width: 38px;
          height: 38px;
          background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
          color: white;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 10px 25px -5px rgba(217, 119, 6, 0.6), 0 4px 10px -2px rgba(0, 0, 0, 0.3);
          border: 2.5px solid #ffffff;
        ">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0"/>
            <circle cx="12" cy="10" r="3"/>
          </svg>
        </div>
        <div style="
          width: 8px;
          height: 8px;
          background: #b45309;
          border-radius: 50%;
          margin-top: -2px;
          box-shadow: 0 2px 4px rgba(0,0,0,0.3);
        "></div>
      </div>
    `,
    iconSize: [0, 0],
    iconAnchor: [0, 0],
  })
}

export default function MapLocationPickerModal({
  open,
  onClose,
  initialLat,
  initialLng,
  initialAddress = '',
  initialCity = '',
  cinemaName = '',
  onSelectLocation,
}) {
  const mapContainerRef = useRef(null)
  const mapInstanceRef = useRef(null)
  const markerRef = useRef(null)

  const [selectedCoords, setSelectedCoords] = useState(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [isSearching, setIsSearching] = useState(false)
  const [searchResults, setSearchResults] = useState([])
  const [isReverseGeocoding, setIsReverseGeocoding] = useState(false)
  const [detectedAddress, setDetectedAddress] = useState('')
  const [isLocatingUser, setIsLocatingUser] = useState(false)

  // Parse default position
  const getInitialPosition = () => {
    const lat = parseFloat(initialLat)
    const lng = parseFloat(initialLng)
    if (!isNaN(lat) && !isNaN(lng) && lat !== 0 && lng !== 0) {
      return { lat, lng }
    }
    if (initialCity && CITY_COORDINATES[initialCity]) {
      return CITY_COORDINATES[initialCity]
    }
    return CITY_COORDINATES['Ho Chi Minh']
  }

  // Update marker & map position helper
  const updatePosition = (lat, lng, zoomLevel = null, skipReverse = false) => {
    setSelectedCoords({ lat, lng })
    
    if (mapInstanceRef.current) {
      if (zoomLevel) {
        mapInstanceRef.current.setView([lat, lng], zoomLevel, { animate: true })
      } else {
        mapInstanceRef.current.panTo([lat, lng], { animate: true })
      }

      if (markerRef.current) {
        markerRef.current.setLatLng([lat, lng])
      } else {
        const pin = L.marker([lat, lng], {
          icon: createPinIcon(),
          draggable: true,
        }).addTo(mapInstanceRef.current)

        pin.on('dragend', (e) => {
          const newPos = e.target.getLatLng()
          setSelectedCoords({ lat: newPos.lat, lng: newPos.lng })
          reverseGeocode(newPos.lat, newPos.lng)
        })

        markerRef.current = pin
      }
    }

    if (!skipReverse) {
      reverseGeocode(lat, lng)
    }
  }

  // Reverse Geocoding (Nominatim OpenStreetMap)
  const reverseGeocode = async (lat, lng) => {
    setIsReverseGeocoding(true)
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`,
        {
          headers: {
            'Accept-Language': 'vi,en',
          },
        }
      )
      if (res.ok) {
        const data = await res.json()
        if (data.display_name) {
          setDetectedAddress(data.display_name)
        }
      }
    } catch {
      // Fail silently
    } finally {
      setIsReverseGeocoding(false)
    }
  }

  // Initialize Leaflet Map
  useEffect(() => {
    if (!open) {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove()
        mapInstanceRef.current = null
        markerRef.current = null
      }
      return
    }

    const initPos = getInitialPosition()
    setSelectedCoords(initPos)

    const timer = setTimeout(() => {
      if (!mapContainerRef.current) return

      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove()
      }

      const map = L.map(mapContainerRef.current, {
        center: [initPos.lat, initPos.lng],
        zoom: initialLat && initialLng ? 16 : 13,
        zoomControl: false,
      })

      L.control.zoom({ position: 'bottomright' }).addTo(map)

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        maxZoom: 19,
      }).addTo(map)

      const pin = L.marker([initPos.lat, initPos.lng], {
        icon: createPinIcon(),
        draggable: true,
      }).addTo(map)

      pin.on('dragend', (e) => {
        const newPos = e.target.getLatLng()
        setSelectedCoords({ lat: newPos.lat, lng: newPos.lng })
        reverseGeocode(newPos.lat, newPos.lng)
      })

      markerRef.current = pin

      map.on('click', (e) => {
        const { lat, lng } = e.latlng
        updatePosition(lat, lng)
      })

      mapInstanceRef.current = map

      // Invalidate size once modal animation completes
      setTimeout(() => {
        map.invalidateSize()
      }, 200)

      if (initialLat && initialLng) {
        reverseGeocode(initPos.lat, initPos.lng)
      } else if (initialAddress) {
        // Auto search initial address if provided and no coordinates yet
        handleSearch(initialAddress + (initialCity ? `, ${initialCity}` : ''))
      }
    }, 100)

    return () => {
      clearTimeout(timer)
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove()
        mapInstanceRef.current = null
        markerRef.current = null
      }
    }
  }, [open])

  // Handle Coordinate paste or Text search
  const handleSearch = async (queryText) => {
    const text = (queryText || searchQuery).trim()
    if (!text) return

    // 1. Check if user pasted coordinates (e.g., "10.86513906366627, 106.61717311425997")
    const coordMatch = text.match(/^(-?\d+(\.\d+)?)[,\s]+(-?\d+(\.\d+)?)$/)
    if (coordMatch) {
      const lat = parseFloat(coordMatch[1])
      const lng = parseFloat(coordMatch[3])
      if (!isNaN(lat) && !isNaN(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) {
        updatePosition(lat, lng, 17)
        setSearchResults([])
        toast.success(`Đã định vị tọa độ: ${lat.toFixed(6)}, ${lng.toFixed(6)}`)
        return
      }
    }

    // 2. Otherwise search location via Nominatim API
    setIsSearching(true)
    try {
      const endpoint = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(
        text
      )}&limit=5&countrycodes=vn&addressdetails=1`
      const res = await fetch(endpoint, {
        headers: { 'Accept-Language': 'vi,en' },
      })
      const data = await res.json()
      if (data && data.length > 0) {
        setSearchResults(data)
        // Automatically jump to the top result
        const top = data[0]
        const lat = parseFloat(top.lat)
        const lng = parseFloat(top.lon)
        updatePosition(lat, lng, 16, true)
        setDetectedAddress(top.display_name)
      } else {
        setSearchResults([])
        toast.error('Không tìm thấy địa chỉ phù hợp. Vui lòng nhấp chọn trực tiếp trên bản đồ.')
      }
    } catch {
      toast.error('Lỗi kết nối khi tìm kiếm địa chỉ.')
    } finally {
      setIsSearching(false)
    }
  }

  // Handle current GPS location
  const handleLocateMe = () => {
    if (!navigator.geolocation) {
      toast.error('Trình duyệt không hỗ trợ định vị GPS')
      return
    }
    setIsLocatingUser(true)
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setIsLocatingUser(false)
        const lat = pos.coords.latitude
        const lng = pos.coords.longitude
        updatePosition(lat, lng, 17)
        toast.success('Đã lấy vị trí hiện tại của bạn')
      },
      () => {
        setIsLocatingUser(false)
        toast.error('Không thể lấy vị trí hiện tại. Vui lòng cho phép quyền truy cập vị trí.')
      },
      { timeout: 8000 }
    )
  }

  // Confirm selection
  const handleConfirm = () => {
    if (!selectedCoords) {
      toast.error('Vui lòng chọn một điểm trên bản đồ')
      return
    }
    onSelectLocation({
      latitude: parseFloat(selectedCoords.lat.toFixed(7)),
      longitude: parseFloat(selectedCoords.lng.toFixed(7)),
      detectedAddress,
    })
    toast.success('Đã áp dụng tọa độ thành công!')
    onClose()
  }

  return (
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-black/70 backdrop-blur-md"
            onClick={onClose}
          />

          {/* Modal Content */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 16 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 8 }}
            transition={{ duration: 0.25, ease: [0.34, 1.1, 0.64, 1] }}
            className="relative w-full max-w-4xl bg-white rounded-2xl shadow-2xl flex flex-col max-h-[92vh] overflow-hidden border border-gray-100"
          >
            {/* Header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100 bg-gray-50/50">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-amber-500/10 text-amber-600 flex items-center justify-center">
                  <MapPin className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-gray-900 text-base flex items-center gap-2">
                    Chọn vị trí trên bản đồ
                    {cinemaName && (
                      <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-brand-50 text-brand-600 border border-brand-100">
                        {cinemaName}
                      </span>
                    )}
                  </h3>
                  <p className="text-xs text-gray-500">
                    Nhấp bất kỳ điểm nào trên bản đồ để tự động ghim và lấy tọa độ chính xác
                  </p>
                </div>
              </div>
              <button
                onClick={onClose}
                className="p-2 rounded-xl text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Search & Actions Bar */}
            <div className="p-4 bg-white border-b border-gray-100 flex flex-col sm:flex-row gap-2.5 items-stretch sm:items-center">
              <div className="relative flex-1">
                <Search className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') handleSearch()
                  }}
                  placeholder="Nhập địa chỉ, tên địa điểm hoặc dán tọa độ (VD: 10.8651, 106.6171)..."
                  className="w-full pl-10 pr-24 py-2.5 text-sm bg-gray-50 border border-gray-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 transition-all placeholder:text-gray-400"
                />
                <button
                  onClick={() => handleSearch()}
                  disabled={isSearching}
                  className="absolute right-1.5 top-1/2 -translate-y-1/2 px-3 py-1.5 bg-amber-500 hover:bg-amber-600 active:scale-95 text-white text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5 shadow-sm"
                >
                  {isSearching ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  ) : (
                    'Tìm kiếm'
                  )}
                </button>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={handleLocateMe}
                  disabled={isLocatingUser}
                  title="Định vị vị trí hiện tại của bạn"
                  className="px-3 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-medium rounded-xl flex items-center gap-1.5 transition-colors cursor-pointer shrink-0"
                >
                  {isLocatingUser ? (
                    <Loader2 className="w-4 h-4 animate-spin text-gray-500" />
                  ) : (
                    <Navigation className="w-4 h-4 text-amber-600" />
                  )}
                  <span className="hidden md:inline">Vị trí của tôi</span>
                </button>

                {initialAddress && (
                  <button
                    onClick={() =>
                      handleSearch(
                        initialAddress + (initialCity ? `, ${initialCity}` : '')
                      )
                    }
                    title="Tìm theo địa chỉ rạp đã điền ở form"
                    className="px-3 py-2.5 bg-amber-50 hover:bg-amber-100 border border-amber-200/60 text-amber-700 text-xs font-semibold rounded-xl flex items-center gap-1.5 transition-all cursor-pointer shrink-0"
                  >
                    <Sparkles className="w-4 h-4 text-amber-500" />
                    <span>Tìm theo địa chỉ rạp</span>
                  </button>
                )}
              </div>
            </div>

            {/* Search Suggestions dropdown (if multiple matches) */}
            {searchResults.length > 1 && (
              <div className="bg-amber-50/80 border-b border-amber-100 px-4 py-2 flex items-center gap-2 overflow-x-auto text-xs text-amber-800">
                <span className="font-semibold shrink-0">Kết quả khác:</span>
                {searchResults.slice(1, 4).map((item, idx) => (
                  <button
                    key={idx}
                    onClick={() => {
                      const lat = parseFloat(item.lat)
                      const lng = parseFloat(item.lon)
                      updatePosition(lat, lng, 16, true)
                      setDetectedAddress(item.display_name)
                    }}
                    className="px-2.5 py-1 bg-white hover:bg-amber-100 rounded-lg border border-amber-200 text-left truncate max-w-xs transition-colors shrink-0"
                  >
                    {item.display_name}
                  </button>
                ))}
              </div>
            )}

            {/* Map Canvas Container */}
            <div className="relative flex-1 min-h-[380px] sm:min-h-[440px] bg-slate-100">
              <div ref={mapContainerRef} className="w-full h-full absolute inset-0 z-10" />

              {/* Floating Coordinate & Address Badge */}
              <div className="absolute top-3 left-3 z-20 max-w-md bg-white/95 backdrop-blur-md rounded-xl shadow-lg border border-gray-100 p-3 text-xs space-y-1 pointer-events-auto">
                <div className="flex items-center justify-between gap-4 font-bold text-gray-800">
                  <span className="flex items-center gap-1 text-amber-600">
                    <MapPin className="w-3.5 h-3.5" /> Tọa độ đã ghim:
                  </span>
                  {selectedCoords && (
                    <button
                      onClick={() => {
                        const str = `${selectedCoords.lat.toFixed(6)}, ${selectedCoords.lng.toFixed(6)}`
                        navigator.clipboard.writeText(str)
                        toast.success('Đã sao chép tọa độ vào bộ nhớ tạm')
                      }}
                      className="text-gray-400 hover:text-gray-700 flex items-center gap-1 font-normal hover:underline"
                    >
                      <Copy className="w-3 h-3" /> Sao chép
                    </button>
                  )}
                </div>

                {selectedCoords ? (
                  <div className="grid grid-cols-2 gap-2 pt-1 font-mono text-[11px]">
                    <div className="bg-gray-50 px-2 py-1 rounded border border-gray-100">
                      <span className="text-gray-400">Lat:</span>{' '}
                      <span className="font-semibold text-gray-900">
                        {selectedCoords.lat.toFixed(6)}
                      </span>
                    </div>
                    <div className="bg-gray-50 px-2 py-1 rounded border border-gray-100">
                      <span className="text-gray-400">Lng:</span>{' '}
                      <span className="font-semibold text-gray-900">
                        {selectedCoords.lng.toFixed(6)}
                      </span>
                    </div>
                  </div>
                ) : (
                  <p className="text-gray-400 italic">Chưa chọn vị trí</p>
                )}

                {/* Detected Address Display */}
                {isReverseGeocoding ? (
                  <div className="flex items-center gap-1.5 text-gray-400 italic pt-1">
                    <Loader2 className="w-3 h-3 animate-spin" /> Đang nhận diện địa chỉ...
                  </div>
                ) : detectedAddress ? (
                  <p className="text-gray-600 text-[11px] line-clamp-2 pt-1 border-t border-gray-100">
                    📍 {detectedAddress}
                  </p>
                ) : null}
              </div>

              {/* City quick buttons on map */}
              <div className="absolute top-3 right-3 z-20 hidden sm:flex flex-col gap-1 bg-white/90 backdrop-blur-md rounded-xl p-1.5 shadow-md border border-gray-100">
                <span className="text-[10px] font-semibold text-gray-400 px-2 py-0.5">
                  Thành phố
                </span>
                {Object.entries(CITY_COORDINATES).map(([key, item]) => (
                  <button
                    key={key}
                    onClick={() => updatePosition(item.lat, item.lng, 13)}
                    className="px-2.5 py-1 text-left text-xs text-gray-700 hover:bg-amber-50 hover:text-amber-700 rounded-lg transition-colors font-medium cursor-pointer"
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Footer Actions */}
            <div className="px-6 py-4 bg-white border-t border-gray-100 flex items-center justify-between gap-3">
              <p className="text-xs text-gray-500 hidden sm:block">
                💡 <span className="font-medium">Mẹo:</span> Bạn có thể nhấp chuột hoặc kéo thả biểu tượng ghim vàng để tinh chỉnh vị trí rạp.
              </p>
              <div className="flex items-center gap-3 w-full sm:w-auto">
                <button
                  type="button"
                  onClick={onClose}
                  className="flex-1 sm:flex-initial px-4 py-2.5 text-sm font-medium text-gray-600 hover:bg-gray-100 rounded-xl transition-colors cursor-pointer"
                >
                  Hủy
                </button>
                <button
                  type="button"
                  onClick={handleConfirm}
                  disabled={!selectedCoords}
                  className="flex-1 sm:flex-initial px-6 py-2.5 bg-amber-500 hover:bg-amber-600 active:scale-95 text-white text-sm font-semibold rounded-xl shadow-md shadow-amber-500/20 flex items-center justify-center gap-2 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Check className="w-4 h-4" />
                  Xác nhận chọn vị trí này
                </button>
              </div>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  )
}
