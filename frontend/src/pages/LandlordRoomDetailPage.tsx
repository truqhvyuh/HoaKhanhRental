
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../contexts/useAuth'
import LandlordRoomPhotos from '../components/LandlordRoomPhotos'
import {
  getMyRoomByIdApi,
  updateRoomStatusApi,
  deleteMyRoomApi,
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

function getOccupancyLabel(status: string) {
  switch (status) {
    case 'AVAILABLE':
      return 'Còn phòng'
    case 'RENTED':
      return 'Đã có người thuê'
    default:
      return status
  }
}

function getVisibilityLabel(status: string) {
  switch (status) {
    case 'VISIBLE':
      return 'Đang hiển thị'
    case 'HIDDEN':
      return 'Đã ẩn'
    default:
      return status
  }
}

function LandlordRoomDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { token } = useAuth()
  const navigate = useNavigate()

  const [room, setRoom] = useState<LandlordRoom | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [processing, setProcessing] = useState(false)
  const [actionError, setActionError] = useState('')

  
  useEffect(() => {
    const roomId = Number(id)
    let cancelled = false

    async function loadRoom() {
      if (
        !token ||
        !id ||
        !Number.isSafeInteger(roomId) ||
        roomId <= 0
      ) {
        if (!cancelled) {
          setRoom(null)
          setError('Mã phòng không hợp lệ hoặc bạn chưa đăng nhập.')
          setLoading(false)
        }
        return
      }

      if (!cancelled) {
        setLoading(true)
        setError('')
        setRoom(null)
      }

      try {
        const result = await getMyRoomByIdApi(roomId, token)

        if (!cancelled) {
          setRoom(result)
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error
              ? err.message
              : 'Không thể tải thông tin phòng.'
          )
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void loadRoom()

    return () => {
      cancelled = true
    }
  }, [id, token])

  async function changeStatus(
    visibilityStatus: 'VISIBLE' | 'HIDDEN',
    occupancyStatus: 'AVAILABLE' | 'RENTED'
  ) {
    if (!room || !token || processing) return

    setProcessing(true)
    setActionError('')

    try {
      const updated = await updateRoomStatusApi(
        room.id,
        { visibilityStatus, occupancyStatus },
        token
      )

      setRoom(updated)
    } catch (err) {
      setActionError(
        err instanceof Error
          ? err.message
          : 'Không thể cập nhật trạng thái phòng.'
      )
    } finally {
      setProcessing(false)
    }
  }

  async function handleDelete() {
    if (!room || !token || processing) return

    const confirmed = window.confirm(
      'Bạn chắc chắn muốn xóa phòng này?'
    )

    if (!confirmed) return

    setProcessing(true)
    setActionError('')

    try {
      await deleteMyRoomApi(room.id, token)
      navigate('/landlord', { replace: true })
    } catch (err) {
      setActionError(
        err instanceof Error
          ? err.message
          : 'Không thể xóa phòng.'
      )
    } finally {
      setProcessing(false)
    }
  }

  return (
    <main className="landlord-detail-page">
      <Link to="/landlord" className="auth-back">
        ← Quay lại quản lý phòng
      </Link>

      {loading && <p>Đang tải thông tin phòng...</p>}

      {!loading && error && (
        <div className="auth-error" role="alert">
          {error}
        </div>
      )}

      {!loading && !error && room && (
        <div className="landlord-detail-card">
          <div className="landlord-room-top">
            <h1>{room.title}</h1>

            <span
              className={
                'room-status room-status-' +
                room.moderationStatus.toLowerCase()
              }
            >
              {getModerationLabel(room.moderationStatus)}
            </span>
          </div>

          <p className="landlord-room-id">
            Mã phòng: #{room.id}
          </p>

          <div className="landlord-detail-grid">
            <div>
              <strong>Giá thuê</strong>
              <p>
                {Number(room.price).toLocaleString('vi-VN')}
                {' '}đ/tháng
              </p>
            </div>

            <div>
              <strong>Diện tích</strong>
              <p>{room.areaSqm} m²</p>
            </div>

            <div>
              <strong>Số người tối đa</strong>
              <p>{room.maxOccupants} người</p>
            </div>

            <div>
              <strong>Khu vực</strong>
              <p>{room.areaName || 'Chưa cập nhật'}</p>
            </div>

            <div>
              <strong>Tình trạng phòng</strong>
              <p>{getOccupancyLabel(room.occupancyStatus)}</p>
            </div>

            <div>
              <strong>Trạng thái hiển thị</strong>
              <p>{getVisibilityLabel(room.visibilityStatus)}</p>
            </div>
          </div>

          <section className="landlord-detail-section">
            <h2>Địa chỉ</h2>
            <p>{room.address}</p>
          </section>

          <section className="landlord-detail-section">
            <h2>Mô tả phòng</h2>
            <p className="landlord-detail-description">
              {room.description || 'Chưa có mô tả.'}
            </p>
          </section>

          {actionError && (
            <div className="auth-error" role="alert">
              {actionError}
            </div>
          )}
          
          {token && (
            <LandlordRoomPhotos
              roomId={room.id}
              token={token}
            />
          )}

          <div className="landlord-detail-actions">
            <Link to="/landlord">
              ← Danh sách phòng
            </Link>

            <Link to={`/landlord/rooms/${room.id}/edit`}>
              Chỉnh sửa phòng
            </Link>

            <button
              type="button"
              disabled={processing}
              onClick={() =>
                changeStatus(
                  room.visibilityStatus === 'VISIBLE'
                    ? 'HIDDEN'
                    : 'VISIBLE',
                  room.occupancyStatus as 'AVAILABLE' | 'RENTED'
                )
              }
            >
              {room.visibilityStatus === 'VISIBLE'
                ? 'Ẩn tin đăng'
                : 'Hiện tin đăng'}
            </button>

            <button
              type="button"
              disabled={processing}
              onClick={() =>
                changeStatus(
                  room.visibilityStatus as 'VISIBLE' | 'HIDDEN',
                  room.occupancyStatus === 'AVAILABLE'
                    ? 'RENTED'
                    : 'AVAILABLE'
                )
              }
            >
              {room.occupancyStatus === 'AVAILABLE'
                ? 'Đánh dấu đã có người thuê'
                : 'Đánh dấu còn phòng'}
            </button>

            <button
              type="button"
              className="room-delete-button"
              disabled={processing}
              onClick={handleDelete}
            >
              {processing ? 'Đang xử lý...' : 'Xóa phòng'}
            </button>
          </div>
        </div>
      )}
    </main>
  )
}

export default LandlordRoomDetailPage
