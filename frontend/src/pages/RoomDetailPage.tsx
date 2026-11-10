
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { getRoomById } from '../services/roomApi'
import type { Room } from '../types/room'

type RoomState = {
  roomId: number
  room: Room | null
  error: string
}

function RoomDetailPage() {
  const { id } = useParams<{ id: string }>()

  const roomId = id ? Number(id) : NaN

  const validRoomId =
    Number.isSafeInteger(roomId) && roomId > 0

  const [roomState, setRoomState] = useState<RoomState | null>(null)

  useEffect(() => {
    if (!validRoomId) {
      return
    }

    const controller = new AbortController()

    getRoomById(roomId, controller.signal)
      .then((data) => {
        if (controller.signal.aborted) return

        setRoomState({
          roomId,
          room: data,
          error: '',
        })
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return

        setRoomState({
          roomId,
          room: null,
          error:
            err instanceof Error
              ? err.message
              : 'Không thể tải thông tin phòng',
        })
      })

    return () => {
      controller.abort()
    }
  }, [roomId, validRoomId])

  const loading =
    validRoomId &&
    (roomState === null || roomState.roomId !== roomId)

  const error = !validRoomId
    ? 'ID phòng không hợp lệ'
    : roomState?.roomId === roomId
      ? roomState.error
      : ''

  const room =
    validRoomId && roomState?.roomId === roomId
      ? roomState.room
      : null

  const formatPrice = (price: number) =>
    new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
      maximumFractionDigits: 0,
    }).format(price)

  return (
    <div className="detail-page">
      <header className="detail-header">
        <div className="container detail-header-inner">
          <Link to="/" className="detail-brand">
            ⌂ HoaKhanh Rental
          </Link>

          <Link to="/" className="back-link">
            ← Danh sách phòng
          </Link>
        </div>
      </header>

      <main className="container detail-main">
        {loading && (
          <div className="detail-message">
            Đang tải thông tin phòng trọ...
          </div>
        )}

        {!loading && error && (
          <div className="detail-message">
            <h2>{error}</h2>
            <Link to="/">Quay lại danh sách phòng</Link>
          </div>
        )}

        {!loading && !error && room && (
          <>
            <div className="detail-heading">
              <span className="detail-badge">
                Phòng còn trống
              </span>

              <h1>{room.title}</h1>
              <p>📍 {room.address}</p>
            </div>

            <div className="detail-layout">
              <div className="detail-left">
                <div className="detail-image">
                  <span>⌂</span>
                  <p>Hình ảnh phòng trọ sẽ được cập nhật</p>
                </div>

                <section className="detail-section">
                  <h2>Thông tin phòng trọ</h2>

                  <div className="detail-facts">
                    <div>
                      <span>Diện tích</span>
                      <strong>{room.areaSqm} m²</strong>
                    </div>

                    <div>
                      <span>Số người tối đa</span>
                      <strong>{room.maxOccupants} người</strong>
                    </div>

                    <div>
                      <span>Khu vực</span>
                      <strong>
                        {room.areaName || 'Chưa cập nhật'}
                      </strong>
                    </div>
                  </div>
                </section>

                <section className="detail-section">
                  <h2>Mô tả phòng</h2>

                  <p className="detail-description">
                    {room.description ||
                      'Chưa có mô tả phòng trọ.'}
                  </p>
                </section>

                <section className="detail-section">
                  <h2>Địa chỉ</h2>
                  <p>{room.address}</p>
                </section>
              </div>

              <aside className="detail-sidebar">
                <div className="detail-price">
                  <strong>{formatPrice(room.price)}</strong>
                  <span>/ tháng</span>
                </div>

                <p>Phòng trọ đang có sẵn để tham khảo.</p>

                <div className="detail-notice">
                  Chức năng liên hệ chủ trọ sẽ được
                  xây dựng ở bước tiếp theo.
                </div>

                <Link
                  to="/"
                  className="detail-back-button"
                >
                  Xem các phòng khác
                </Link>
              </aside>
            </div>
          </>
        )}
      </main>
    </div>
  )
}

export default RoomDetailPage
