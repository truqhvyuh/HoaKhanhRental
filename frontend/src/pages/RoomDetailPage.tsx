
import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'

import { getRoomById } from '../services/roomApi'
import type { Room } from '../types/room'
import PublicRoomPhotos from '../components/PublicRoomPhotos'
import { useAuth } from '../contexts/useAuth'
import {
  createContactRequestApi,
  type ContactRequest,
} from '../services/contactRequestApi'

type RoomState = {
  roomId: number
  room: Room | null
  error: string
}

function RoomDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { user, token, loading: authLoading } = useAuth()

  const [contactMessage, setContactMessage] = useState('')
  const [contactSubmitting, setContactSubmitting] = useState(false)
  const [contactError, setContactError] = useState('')
  const [contactSuccess, setContactSuccess] =
    useState<ContactRequest | null>(null)

  const roomId = id ? Number(id) : NaN

  const validRoomId =
    Number.isSafeInteger(roomId) && roomId > 0

  const [roomState, setRoomState] = useState<RoomState | null>(null)

  useEffect(() => {
    setContactMessage('')
    setContactError('')
    setContactSuccess(null)

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

  async function handleContactSubmit(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault()

    if (
      !room ||
      !token ||
      user?.role !== 'STUDENT' ||
      contactSubmitting
    ) {
      return
    }

    setContactSubmitting(true)
    setContactError('')

    try {
      const result = await createContactRequestApi(
        room.id,
        contactMessage,
        token
      )

      setContactSuccess(result)
      setContactMessage('')
    } catch (err) {
      setContactError(
        err instanceof Error
          ? err.message
          : 'Không thể gửi yêu cầu liên hệ.'
      )
    } finally {
      setContactSubmitting(false)
    }
  }

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
              <PublicRoomPhotos roomId={room.id} />
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

                {authLoading ? (
                  <div className="detail-notice">
                    Đang kiểm tra trạng thái đăng nhập...
                  </div>
                ) : !user || !token ? (
                  <div className="detail-notice">
                    <p>
                      Bạn cần đăng nhập để gửi yêu cầu
                      liên hệ chủ trọ.
                    </p>
                    <Link to="/login">
                      Đăng nhập ngay
                    </Link>
                  </div>
                ) : user.role === 'STUDENT' ? (
                  contactSuccess ? (
                    <div className="detail-notice" role="status">
                      <p>
                        <strong>
                          Đã gửi yêu cầu liên hệ thành công!
                        </strong>
                      </p>

                      <p>
                        Chủ trọ: {contactSuccess.landlordName}
                      </p>

                      {contactSuccess.landlordPhone ? (
                        <p>
                          Bạn có thể gọi trực tiếp cho chủ trọ:
                          {' '}
                          <a
                            href={`tel:${contactSuccess.landlordPhone}`}
                          >
                            {contactSuccess.landlordPhone}
                          </a>
                        </p>
                      ) : (
                        <p>
                          Chủ trọ chưa cung cấp số điện thoại.
                        </p>
                      )}
                    </div>
                  ) : (
                    <>
                      <h2>Liên hệ chủ trọ</h2>

                      {contactError && (
                        <div
                          className="auth-error"
                          role="alert"
                        >
                          {contactError}
                        </div>
                      )}

                      <form
                        className="room-edit-form"
                        onSubmit={handleContactSubmit}
                      >
                        <label htmlFor="contact-message">
                          Lời nhắn (không bắt buộc)
                          <textarea
                            id="contact-message"
                            value={contactMessage}
                            onChange={(event) =>
                              setContactMessage(event.target.value)
                            }
                            maxLength={1000}
                            rows={4}
                            placeholder="Ví dụ: Tôi muốn hỏi thêm về phòng và thời gian có thể xem phòng."
                          />
                        </label>

                        <button
                          type="submit"
                          disabled={contactSubmitting}
                        >
                          {contactSubmitting
                            ? 'Đang gửi...'
                            : 'Gửi yêu cầu liên hệ'}
                        </button>
                      </form>
                    </>
                  )
                ) : (
                  <div className="detail-notice">
                    Đây là chế độ xem dành cho chủ trọ
                    và quản trị viên. Chức năng gửi yêu cầu
                    liên hệ chỉ dành cho sinh viên.
                  </div>
                )}

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
