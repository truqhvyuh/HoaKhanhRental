
import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { getRooms } from './services/roomApi'
import type { RoomFilters, RoomPage } from './types/room'
import './App.css'
import { Link, Routes, Route } from 'react-router-dom'
import RoomDetailPage from './pages/RoomDetailPage'
import LoginPage from './pages/LoginPage'
import ProfilePage from './pages/ProfilePage'
import { useAuth } from './contexts/useAuth'
import RegisterPage from './pages/RegisterPage'
import ProtectedRoute from './components/ProtectedRoute'
import LandlordDashboardPage from './pages/LandlordDashboardPage'
import CreateRoomPage from './pages/CreateRoomPage'
import LandlordRoomDetailPage from './pages/LandlordRoomDetailPage'
import EditRoomPage from './pages/EditRoomPage'
import AdminDashboardPage from './pages/AdminDashboardPage'
import PublicRoomPhotos from './components/PublicRoomPhotos'

const initialFilters: RoomFilters = {
  keyword: '',
  area: '',
  minPrice: '',
  maxPrice: '',
  minArea: '',
  maxArea: '',
  sort: 'newest',
}

function HomePage() {
  const { user, loading: authLoading, logout } = useAuth()
  const [draft, setDraft] = useState<RoomFilters>(initialFilters)
  const [filters, setFilters] = useState<RoomFilters>(initialFilters)
  const [page, setPage] = useState(0)

  const [result, setResult] = useState<RoomPage | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    getRooms(filters, page, 6, controller.signal)
      .then((data) => {
        setResult(data)
        setError('')
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return

        setError(
          err instanceof Error
            ? err.message
            : 'Có lỗi khi tải danh sách phòng'
        )
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      })

    return () => controller.abort()
  }, [filters, page])

  function updateDraft(
    key: keyof RoomFilters,
    value: string
  ) {
    setDraft((prev) => ({ ...prev, [key]: value }))
  }

  
function handleSearch(event: FormEvent<HTMLFormElement>) {
  event.preventDefault()

  const minPrice = draft.minPrice
    ? Number(draft.minPrice)
    : null

  const maxPrice = draft.maxPrice
    ? Number(draft.maxPrice)
    : null

  const minArea = draft.minArea
    ? Number(draft.minArea)
    : null

  const maxArea = draft.maxArea
    ? Number(draft.maxArea)
    : null

  const values = [minPrice, maxPrice, minArea, maxArea]

  if (
    values.some(
      (value) =>
        value !== null &&
        (!Number.isFinite(value) || value < 0)
    )
  ) {
    window.alert('Giá và diện tích phải là số không âm.')
    return
  }

  if (
    minPrice !== null &&
    maxPrice !== null &&
    minPrice > maxPrice
  ) {
    window.alert('Giá tối thiểu không được lớn hơn giá tối đa.')
    return
  }

  if (
    minArea !== null &&
    maxArea !== null &&
    minArea > maxArea
  ) {
    window.alert(
      'Diện tích tối thiểu không được lớn hơn diện tích tối đa.'
    )
    return
  }

  setLoading(true)
  setResult(null)
  setPage(0)
  setFilters({ ...draft })
}


  function handleReset() {
    setDraft({ ...initialFilters })
    setFilters({ ...initialFilters })
    setPage(0)
    setResult(null)
    setLoading(true)
  }

  function changePage(nextPage: number) {
    setPage(nextPage)
    setResult(null)
    setLoading(true)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function changeSort(value: string) {
    setDraft((prev) => ({ ...prev, sort: value }))
    setFilters((prev) => ({ ...prev, sort: value }))
    setPage(0)
    setResult(null)
    setLoading(true)
  }

  const formatPrice = (price: number) =>
    new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
      maximumFractionDigits: 0,
    }).format(price)

  return (
    <div className="app">
      <header className="site-header">
        <div className="container header-inner">
          <div className="brand">
            <span className="brand-icon">⌂</span>
            <span>HoaKhanh Rental</span>
          </div>
                    
          
          <div className="header-auth">
            {authLoading ? (
              <span>Đang kiểm tra tài khoản...</span>
            ) : user ? (
              <>
                <Link to="/profile" className="header-auth-link">
                  {user.fullName}
                </Link>

                {user.role === 'LANDLORD' && (
                  <Link to="/landlord" className="header-auth-link">
                    Quản lý phòng
                  </Link>
                )}
                                
                {user.role === 'ADMIN' && (
                  <Link to="/admin" className="admin-nav-link">
                    Quản trị HoaKhanh Rental
                  </Link>
                )}


                <button
                  type="button"
                  className="header-logout"
                  onClick={logout}
                >
                  Đăng xuất
                </button>
              </>
            ) : (
              <Link to="/login" className="header-auth-link">
                Đăng nhập
              </Link>
            )}
          </div>


        </div>
      </header>

      <main className="container">
        <section className="hero">
          <span className="eyebrow">HÒA KHÁNH · ĐÀ NẴNG</span>
          <h1>Tìm phòng trọ phù hợp với bạn</h1>
          <p>
            Khám phá phòng trọ sinh viên, lọc theo giá,
            diện tích và khu vực.
          </p>
        </section>

        <div className="layout">
          <aside className="filter-panel">
            <h2>Bộ lọc tìm kiếm</h2>

            <form onSubmit={handleSearch}>
              <label htmlFor="keyword">Từ khóa</label>
              <input
                id="keyword"
                placeholder="Tên phòng, địa chỉ..."
                value={draft.keyword}
                onChange={(e) =>
                  updateDraft('keyword', e.target.value)
                }
              />

              <label htmlFor="area">Khu vực</label>
              <input
                id="area"
                placeholder="Ví dụ: Hoa Khanh"
                value={draft.area}
                onChange={(e) =>
                  updateDraft('area', e.target.value)
                }
              />

              <label htmlFor="minPrice">Giá từ (VNĐ)</label>
              <input
                id="minPrice"
                type="number"
                min="0"
                placeholder="1000000"
                value={draft.minPrice}
                onChange={(e) =>
                  updateDraft('minPrice', e.target.value)
                }
              />

              <label htmlFor="maxPrice">Giá đến (VNĐ)</label>
              <input
                id="maxPrice"
                type="number"
                min="0"
                placeholder="3000000"
                value={draft.maxPrice}
                onChange={(e) =>
                  updateDraft('maxPrice', e.target.value)
                }
              />

              <label htmlFor="minArea">Diện tích từ (m²)</label>
              <input
                id="minArea"
                type="number"
                min="0"
                placeholder="15"
                value={draft.minArea}
                onChange={(e) =>
                  updateDraft('minArea', e.target.value)
                }
              />

              <label htmlFor="maxArea">Diện tích đến (m²)</label>
              <input
                id="maxArea"
                type="number"
                min="0"
                placeholder="40"
                value={draft.maxArea}
                onChange={(e) =>
                  updateDraft('maxArea', e.target.value)
                }
              />

              <button className="search-button" type="submit">
                Tìm phòng
              </button>

              <button
                className="reset-button"
                type="button"
                onClick={handleReset}
              >
                Xóa bộ lọc
              </button>
            </form>
          </aside>

          <section className="results-section">
            <div className="results-header">
              <div>
                <h2>Danh sách phòng trọ</h2>
                <p>
                  {result
                    ? `${result.totalElements} phòng phù hợp`
                    : 'Đang cập nhật kết quả'}
                </p>
              </div>

              <select
                aria-label="Sắp xếp phòng trọ"
                value={filters.sort}
                onChange={(e) => changeSort(e.target.value)}
              >
                <option value="newest">Mới nhất</option>
                <option value="oldest">Cũ nhất</option>
                <option value="price_asc">Giá thấp đến cao</option>
                <option value="price_desc">Giá cao đến thấp</option>
                <option value="area_asc">Diện tích tăng dần</option>
                <option value="area_desc">Diện tích giảm dần</option>
              </select>
            </div>

            {loading && (
              <div className="message">
                Đang tải danh sách phòng trọ...
              </div>
            )}

            {!loading && error && (
              <div className="message error">
                {error}. Hãy kiểm tra Spring Boot đang chạy.
              </div>
            )}

            {!loading && !error && result?.content.length === 0 && (
              <div className="message">
                Không tìm thấy phòng phù hợp.
                Hãy thử thay đổi bộ lọc.
              </div>
            )}

            {!loading && !error && result && (
              <>
                <div className="room-grid">
                  {result.content.map((room) => (
                    <article className="room-card" key={room.id}>
                      <PublicRoomPhotos roomId={room.id} coverOnly />

                      <div className="room-content">
                        <span className="room-status">
                          Còn phòng
                        </span>

                        <h3>{room.title}</h3>

                        <p className="room-address">
                          📍 {room.address}
                        </p>

                        <p className="room-description">
                          {room.description || 'Chưa có mô tả'}
                        </p>

                        <div className="room-details">
                          <span>◫ {room.areaSqm} m²</span>
                          <span>♙ {room.maxOccupants} người</span>
                        </div>

                                                
                        <div className="room-footer">
                          <div>
                            <strong>{formatPrice(room.price)}</strong>
                            <small>/tháng</small>
                          </div>

                          <Link
                            to={`/rooms/${room.id}`}
                            className="view-detail-button"
                          >
                            Xem chi tiết →
                          </Link>
                        </div>

                      </div>
                    </article>
                  ))}
                </div>

                {result.totalPages > 1 && (
                  <nav
                    className="pagination"
                    aria-label="Phân trang phòng trọ"
                  >
                    <button
                      disabled={result.first}
                      onClick={() => changePage(page - 1)}
                    >
                      ← Trước
                    </button>

                    <span>
                      Trang {result.page + 1} / {result.totalPages}
                    </span>

                    <button
                      disabled={result.last}
                      onClick={() => changePage(page + 1)}
                    >
                      Sau →
                    </button>
                  </nav>
                )}
              </>
            )}
          </section>
        </div>
      </main>

      <footer className="site-footer">
        HoaKhanh Rental · Dự án quản lý phòng trọ sinh viên
      </footer>
    </div>
  
  )
}

function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />

      <Route
        path="/rooms/:id"
        element={<RoomDetailPage />}
      />

            
      <Route
        path="/admin"
        element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <AdminDashboardPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/landlord"
        element={
          <ProtectedRoute allowedRoles={['LANDLORD']}>
            <LandlordDashboardPage />
          </ProtectedRoute>
        }
      />

            
      <Route
        path="/landlord/rooms/new"
        element={
          <ProtectedRoute allowedRoles={['LANDLORD']}>
            <CreateRoomPage />
          </ProtectedRoute>
        }
      />
      
      <Route
        path="/landlord/rooms/:id"
        element={
          <ProtectedRoute allowedRoles={['LANDLORD']}>
            <LandlordRoomDetailPage />
          </ProtectedRoute>
        }
      />

            
      <Route
        path="/landlord/rooms/:id/edit"
        element={
          <ProtectedRoute allowedRoles={['LANDLORD']}>
            <EditRoomPage />
          </ProtectedRoute>
        }
      />


      <Route
        path="/login"
        element={<LoginPage />}
      />

      <Route path="/register" element={<RegisterPage />} />

      <Route
      path="/profile"
      element={
        <ProtectedRoute>
          <ProfilePage />
        </ProtectedRoute>
  }
/>
    </Routes>
  )
}

export default App
