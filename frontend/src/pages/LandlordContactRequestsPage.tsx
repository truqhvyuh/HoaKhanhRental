import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

import { useAuth } from '../contexts/useAuth'
import {
  getLandlordContactRequestsApi,
  updateContactRequestStatusApi,
  type ContactRequest,
  type ContactRequestStatus,
} from '../services/contactRequestApi'

function getStatusLabel(status: ContactRequestStatus) {
  switch (status) {
    case 'PENDING':
      return 'Chờ liên hệ'
    case 'RESPONDED':
      return 'Đã liên hệ'
    case 'CLOSED':
      return 'Đã đóng'
    default:
      return status
  }
}

function getStatusClass(status: ContactRequestStatus) {
  switch (status) {
    case 'PENDING':
      return 'room-status room-status-pending'
    case 'RESPONDED':
      return 'room-status room-status-approved'
    default:
      return 'room-status'
  }
}

function formatDate(value: string) {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString('vi-VN')
}

function LandlordContactRequestsPage() {
  const { token } = useAuth()

  const [requests, setRequests] = useState<ContactRequest[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [processingId, setProcessingId] = useState<number | null>(null)

  useEffect(() => {
    let cancelled = false

    async function loadRequests() {
      if (!token) {
        if (!cancelled) {
          setLoading(false)
          setError('Bạn chưa đăng nhập.')
        }
        return
      }

      if (!cancelled) {
        setLoading(true)
        setError('')
      }

      try {
        const result = await getLandlordContactRequestsApi(token)

        if (!cancelled) {
          setRequests(result)
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error
              ? err.message
              : 'Không thể tải danh sách yêu cầu liên hệ.'
          )
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void loadRequests()

    return () => {
      cancelled = true
    }
  }, [token])

  async function handleStatusChange(
    contactRequestId: number,
    status: ContactRequestStatus
  ) {
    if (!token || processingId !== null) return

    setProcessingId(contactRequestId)
    setError('')

    try {
      const updatedRequest =
        await updateContactRequestStatusApi(
          contactRequestId,
          status,
          token
        )

      setRequests((previous) =>
        previous.map((request) =>
          request.id === contactRequestId
            ? updatedRequest
            : request
        )
      )
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Không thể cập nhật trạng thái yêu cầu.'
      )
    } finally {
      setProcessingId(null)
    }
  }

  return (
    <div className="auth-page">
      <div className="landlord-dashboard">
        <Link to="/landlord" className="auth-back">
          ← Quay lại quản lý phòng
        </Link>

        <h1>Yêu cầu liên hệ</h1>

        <p className="auth-subtitle">
          Danh sách sinh viên đã gửi yêu cầu liên hệ
          tới các phòng trọ của bạn.
        </p>

        {loading && (
          <p>Đang tải danh sách yêu cầu liên hệ...</p>
        )}

        {error && (
          <div className="auth-error" role="alert">
            {error}
          </div>
        )}

        {!loading && !error && requests.length === 0 && (
          <p>Hiện chưa có yêu cầu liên hệ nào.</p>
        )}

        {!loading && requests.length > 0 && (
          <div className="landlord-room-list">
            {requests.map((request) => (
              <article
                className="landlord-room-card"
                key={request.id}
              >
                <div className="landlord-room-top">
                  <h3>{request.roomTitle}</h3>

                  <span className={getStatusClass(request.status)}>
                    {getStatusLabel(request.status)}
                  </span>
                </div>

                <p>
                  <strong>Mã phòng:</strong> #{request.roomId}
                </p>

                <p>
                  <strong>Sinh viên:</strong> {request.studentName}
                </p>

                <p>
                  <strong>Số điện thoại:</strong>{' '}
                  {request.studentPhone ? (
                    <a href={`tel:${request.studentPhone}`}>
                      {request.studentPhone}
                    </a>
                  ) : (
                    'Chưa cung cấp'
                  )}
                </p>

                <p>
                  <strong>Email:</strong>{' '}
                  {request.studentEmail ? (
                    <a href={`mailto:${request.studentEmail}`}>
                      {request.studentEmail}
                    </a>
                  ) : (
                    'Chưa cung cấp'
                  )}
                </p>

                <p>
                  <strong>Lời nhắn:</strong>
                </p>

                <p className="landlord-detail-description">
                  {request.message || 'Sinh viên không để lại lời nhắn.'}
                </p>

                <p className="landlord-room-id">
                  Thời gian gửi: {formatDate(request.createdAt)}
                </p>

                {request.status !== 'CLOSED' && (
                  <div className="landlord-detail-actions">
                    {request.status === 'PENDING' && (
                      <button
                        type="button"
                        disabled={processingId !== null}
                        onClick={() =>
                          handleStatusChange(
                            request.id,
                            'RESPONDED'
                          )
                        }
                      >
                        {processingId === request.id
                          ? 'Đang cập nhật...'
                          : 'Đã liên hệ'}
                      </button>
                    )}

                    <button
                      type="button"
                      disabled={processingId !== null}
                      onClick={() =>
                        handleStatusChange(
                          request.id,
                          'CLOSED'
                        )
                      }
                    >
                      {processingId === request.id
                        ? 'Đang cập nhật...'
                        : 'Đóng'}
                    </button>
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default LandlordContactRequestsPage
