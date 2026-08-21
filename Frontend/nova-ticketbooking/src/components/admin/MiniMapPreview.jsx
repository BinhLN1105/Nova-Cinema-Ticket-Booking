import { useEffect, useRef, useState } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { MapPin, Maximize2, Sparkles, Loader2, X } from 'lucide-react'

const createPinIcon = () => {
  return L.divIcon({
    className: 'custom-cinema-pin-mini',
    html: `
      <div style="position: relative; display: flex; flex-direction: column; align-items: center; transform: translate(-50%, -100%); pointer-events: none;">
        <div style="
          width: 32px;
          height: 32px;
          background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
          color: white;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 8px 20px -4px rgba(217, 119, 6, 0.6), 0 2px 6px -1px rgba(0, 0, 0, 0.3);
          border: 2px solid #ffffff;
        ">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0"/>
            <circle cx="12" cy="10" r="3"/>
          </svg>
        </div>
        <div style="
          width: 6px;
          height: 6px;
          background: #b45309;
          border-radius: 50%;
          margin-top: -2px;
        "></div>
      </div>
    `,
    iconSize: [0, 0],
    iconAnchor: [0, 0],
  })
}

export default function MiniMapPreview({
  lat,
  lng,
  onLocationChange,
  onOpenFullMap,
  onClear,
}) {
  const containerRef = useRef(null)
  const mapRef = useRef(null)
  const markerRef = useRef(null)

  const [address, setAddress] = useState('')
  const [loadingAddress, setLoadingAddress] = useState(false)

  const numLat = parseFloat(lat)
  const numLng = parseFloat(lng)
  const isValid =
    !isNaN(numLat) &&
    !isNaN(numLng) &&
    numLat >= -90 &&
    numLat <= 90 &&
    numLng >= -180 &&
    numLng <= 180 &&
    numLat !== 0 &&
    numLng !== 0

  // Reverse geocode
  useEffect(() => {
    if (!isValid) {
      setAddress('')
      return
    }

    let isMounted = true
    setLoadingAddress(true)

    fetch(
      `https://nominatim.openstreetmap.org/reverse?format=json&lat=${numLat}&lon=${numLng}&zoom=18&addressdetails=1`,
      { headers: { 'Accept-Language': 'vi,en' } }
    )
      .then((res) => res.json())
      .then((data) => {
        if (isMounted && data?.display_name) {
          setAddress(data.display_name)
        }
      })
      .catch(() => {})
      .finally(() => {
        if (isMounted) setLoadingAddress(false)
      })

    return () => {
      isMounted = false
    }
  }, [numLat, numLng, isValid])

  // Initialize or update map
  useEffect(() => {
    if (!isValid) {
      if (mapRef.current) {
        mapRef.current.remove()
        mapRef.current = null
        markerRef.current = null
      }
      return
    }

    const timer = setTimeout(() => {
      if (!containerRef.current) return

      if (!mapRef.current) {
        const map = L.map(containerRef.current, {
          center: [numLat, numLng],
          zoom: 16,
          zoomControl: false,
          attributionControl: false,
        })

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          maxZoom: 19,
        }).addTo(map)

        const marker = L.marker([numLat, numLng], {
          icon: createPinIcon(),
          draggable: true,
        }).addTo(map)

        marker.on('dragend', (e) => {
          const newPos = e.target.getLatLng()
          if (onLocationChange) {
            onLocationChange({
              lat: parseFloat(newPos.lat.toFixed(7)),
              lng: parseFloat(newPos.lng.toFixed(7)),
            })
          }
        })

        map.on('click', (e) => {
          const { lat: clickLat, lng: clickLng } = e.latlng
          if (onLocationChange) {
            onLocationChange({
              lat: parseFloat(clickLat.toFixed(7)),
              lng: parseFloat(clickLng.toFixed(7)),
            })
          }
        })

        mapRef.current = map
        markerRef.current = marker

        setTimeout(() => {
          map.invalidateSize()
        }, 150)
      } else {
        mapRef.current.setView([numLat, numLng], 16, { animate: true })
        if (markerRef.current) {
          markerRef.current.setLatLng([numLat, numLng])
        }
      }
    }, 100)

    return () => {
      clearTimeout(timer)
    }
  }, [numLat, numLng, isValid])

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (mapRef.current) {
        mapRef.current.remove()
        mapRef.current = null
        markerRef.current = null
      }
    }
  }, [])

  if (!isValid) return null

  return (
    <div className="mt-3 space-y-2 pt-2 border-t border-slate-200/60 animate-in fade-in duration-300">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1.5 text-xs text-emerald-800 font-semibold">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          <span>Vị trí ghim thực tế trên bản đồ:</span>
        </div>
        <div className="flex items-center gap-2">
          {onOpenFullMap && (
            <button
              type="button"
              onClick={onOpenFullMap}
              className="text-[11px] font-semibold text-amber-700 hover:text-amber-800 bg-amber-50 hover:bg-amber-100 px-2 py-1 rounded-lg border border-amber-200/80 flex items-center gap-1 transition-all cursor-pointer"
            >
              <Maximize2 className="w-3 h-3" />
              Mở rộng bản đồ
            </button>
          )}
          {onClear && (
            <button
              type="button"
              onClick={onClear}
              className="text-[11px] text-gray-400 hover:text-red-500 p-1 rounded-lg hover:bg-red-50 transition-colors"
              title="Xóa tọa độ"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      </div>

      {/* Mini Map Container */}
      <div className="relative w-full h-44 rounded-xl overflow-hidden border border-slate-300 shadow-sm bg-slate-100">
        <div ref={containerRef} className="w-full h-full absolute inset-0 z-10" />

        {/* Floating coordinates badge */}
        <div className="absolute top-2 left-2 z-20 bg-white/90 backdrop-blur-md px-2.5 py-1 rounded-lg shadow-sm border border-gray-100 text-[11px] font-mono text-gray-800 flex items-center gap-1.5 pointer-events-none">
          <MapPin className="w-3 h-3 text-amber-600" />
          <span>
            {numLat.toFixed(6)}, {numLng.toFixed(6)}
          </span>
        </div>

        {/* Interactive hint */}
        <div className="absolute bottom-2 right-2 z-20 bg-black/60 backdrop-blur-sm text-white text-[10px] px-2 py-0.5 rounded-md pointer-events-none">
          Nhấp hoặc kéo ghim để đổi vị trí
        </div>
      </div>

      {/* Reverse Geocoded Address */}
      {loadingAddress ? (
        <div className="flex items-center gap-1 text-[11px] text-gray-400 italic">
          <Loader2 className="w-3 h-3 animate-spin text-amber-500" /> Đang nhận diện địa điểm...
        </div>
      ) : address ? (
        <p className="text-[11px] text-gray-600 line-clamp-1 bg-white/80 p-1.5 rounded-lg border border-slate-200/50">
          📍 <span className="font-medium">Địa điểm:</span> {address}
        </p>
      ) : null}
    </div>
  )
}
