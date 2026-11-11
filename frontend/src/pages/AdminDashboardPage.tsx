
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/useAuth'
import {
  getPendingRoomsApi,
  approveRoomApi,
  rejectRoomApi,
  type AdminRoom,
} from '../services/adminRoomApi'

function AdminDashboardPage() {
  const { token } = useAuth()

  const [rooms, setRooms] = useState<AdminRoom[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [processingId, setProcessingId] = useState<number | null>(null)

  useEffect(() => {
    let cancelled = false

    async function loadRooms() {
      if (!token) {
        if (!cancelled) {
          setLoading(false)
          setError('Bạn chưa đăng nhập.')
        }
        return
      }

      try {
        const result = await getPendingRoomsApi(token)

        if (!cancelled) {
          setRooms(result)
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error
              ? err.message
              : 'Không thể tải danh sách.'
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

  async function handleModeration(
    roomId: number,
    action: 'approve' | 'reject'
  ) {
    if (!token || processingId !== null) return

    let rejectionReason = ''

    if (action === 'approve') {
      const confirmed = window.confirm(
        'Bạn muốn duyệt tin đăng này?'
      )

      if (!confirmed) return
    } else {
      const input = window.prompt(
        'Nhập lý do từ chối tin đăng này:'
      )

      if (input === null) return

      rejectionReason = input.trim()

      if (!rejectionReason) return
    }

    setProcessingId(roomId)
    setError('')

    try {
      if (action === 'approve') {
        await approveRoomApi(roomId, token)
      } else {
        await rejectRoomApi(
          roomId,
          rejectionReason,
          token
        )
      }

      setRooms((previous) =>
        previous.filter((room) => room.id !== roomId)
      )
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Không thể xử lý tin đăng.'
      )
    } finally {
      setProcessingId(null)
    }
  }

  return (
    <main className="admin-page">
      <Link to="/">← Về trang chủ</Link>

      <h1>Quản trị HoaKhanh Rental</h1>
      <p>Kiểm duyệt tin đăng phòng trọ</p>

      <h2>Danh sách chờ duyệt ({rooms.length})</h2>

      {loading && <p>Đang tải danh sách...</p>}

      {error && (
        <div className="auth-error" role="alert">
          {error}
        </div>
      )}

      {!loading && !error && rooms.length === 0 && (
        <p>Hiện không có tin đăng nào chờ duyệt.</p>
      )}

      <div className="admin-room-list">
        {rooms.map((room) => (
          <article className="admin-room-card" key={room.id}>
            <h3>{room.title}</h3>

            <p><strong>Mã phòng:</strong> #{room.id}</p>
            <p><strong>Chủ trọ ID:</strong> {room.landlordId}</p>
            <p><strong>Địa chỉ:</strong> {room.address}</p>
            <p>
              <strong>Giá:</strong>{' '}
              {Number(room.price).toLocaleString('vi-VN')} đ/tháng
            </p>
            <p><strong>Diện tích:</strong> {room.areaSqm} m²</p>
            <p>
              <strong>Số người tối đa:</strong> {room.maxOccupants}
            </p>
            <p>
              <strong>Mô tả:</strong>{' '}
              {room.description || 'Chưa có mô tả'}
            </p>

            <div className="admin-room-actions">
              <button
                type="button"
                className="admin-approve-button"
                disabled={processingId !== null}
                onClick={() =>
                  handleModeration(room.id, 'approve')
                }
              >
                {processingId === room.id
                  ? 'Đang xử lý...'
                  : 'Duyệt tin'}
              </button>

              <button
                type="button"
                className="admin-reject-button"
                disabled={processingId !== null}
                onClick={() =>
                  handleModeration(room.id, 'reject')
                }
              >
                Từ chối
              </button>
            </div>
          </article>
        ))}
      </div>
    </main>
  )
}

export default AdminDashboardPage
