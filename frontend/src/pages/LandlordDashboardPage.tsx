
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/useAuth'
import {
  getMyRoomsApi,
  type LandlordRoom,
} from '../services/landlordRoomApi'

function getModerationLabel(status: string) {
  switch (status) {
    case 'PENDING':
      return 'Chờ duyệt'
    case 'APPROVED':
      return 'Đã duyệt'
    case 'REJECTED':
      return 'Bị từ chối'
    default:
      return status
  }
}

function LandlordDashboardPage() {
  const { user, token, logout } = useAuth()
  const navigate = useNavigate()

  const [rooms, setRooms] = useState<LandlordRoom[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  
useEffect(() => {
  let cancelled = false

  async function loadRooms() {
    if (!token) {
      if (!cancelled) {
        setLoading(false)
      }
      return
    }

    if (!cancelled) {
      setLoading(true)
      setError('')
    }

    try {
      const result = await getMyRoomsApi(token)

      if (!cancelled) {
        setRooms(result)
      }
    } catch (err) {
      if (!cancelled) {
        setError(
          err instanceof Error
            ? err.message
            : 'Không thể tải danh sách phòng.'
        )
      }
    } finally {
      if (!cancelled) {
        setLoading(false)
      }
    }
  }

  void loadRooms()

  return () => {
    cancelled = true
  }
}, [token])


  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  if (!user) {
    return null
  }

  return (
    <div className="auth-page">
      <div className="landlord-dashboard">
        <Link to="/" className="auth-back">
          ← Quay về trang chủ
        </Link>

        <h1>Quản lý phòng trọ</h1>

        <p className="auth-subtitle">
          Xin chào, {user.fullName}
        </p>

        <Link
          to="/landlord/rooms/new"
          className="auth-submit room-create-link"
        >
          + Đăng tin phòng trọ
        </Link>

        <h2>Danh sách phòng đã đăng</h2>

        {loading && <p>Đang tải danh sách phòng...</p>}

        {error && (
          <div className="auth-error" role="alert">
            {error}
          </div>
        )}

        {!loading && !error && rooms.length === 0 && (
          <p>Bạn chưa đăng phòng trọ nào.</p>
        )}

        {!loading && !error && rooms.length > 0 && (
          <div className="landlord-room-list">
            {rooms.map((room) => (
              <div className="landlord-room-card" key={room.id}>
                <div className="landlord-room-top">
                  <h3>{room.title}</h3>

                  <span
                    className={
                      'room-status room-status-' +
                      room.moderationStatus.toLowerCase()
                    }
                  >
                    {getModerationLabel(room.moderationStatus)}
                  </span>
                </div>

                <p>{room.address}</p>

                <div className="landlord-room-details">
                  <span>
                    Giá: {Number(room.price).toLocaleString('vi-VN')} đ/tháng
                  </span>

                  <span>
                    Diện tích: {room.areaSqm} m²
                  </span>

                  <span>
                    Tối đa: {room.maxOccupants} người
                  </span>
                </div>

                <p className="landlord-room-id">
                  Mã phòng: #{room.id}
                </p>
                                
                <Link
                  to={`/landlord/rooms/${room.id}`}
                  className="landlord-room-view-link"
                >
                  Xem chi tiết →
                </Link>


              </div>
            ))}
          </div>
        )}

        <button
          type="button"
          className="auth-submit landlord-logout"
          onClick={handleLogout}
        >
          Đăng xuất
        </button>
      </div>
    </div>
  )
}

export default LandlordDashboardPage
