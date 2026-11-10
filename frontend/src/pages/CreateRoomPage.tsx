
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/useAuth'
import { createRoomApi } from '../services/landlordRoomApi'

interface RoomForm {
  title: string
  address: string
  areaName: string
  price: string
  areaSqm: string
  maxOccupants: string
  description: string
}

const initialForm: RoomForm = {
  title: '',
  address: '',
  areaName: '',
  price: '',
  areaSqm: '',
  maxOccupants: '2',
  description: '',
}

function CreateRoomPage() {
  const { user, token } = useAuth()
  const [form, setForm] = useState<RoomForm>(initialForm)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function updateField(
    field: keyof RoomForm,
    value: string
  ) {
    setForm((prev) => ({
      ...prev,
      [field]: value,
    }))
  }

  
async function handleSubmit(
  event: FormEvent<HTMLFormElement>
) {
  event.preventDefault()

  if (submitting) return

  setError('')
  setSuccess('')

  if (!token) {
    setError('Bạn cần đăng nhập để đăng tin.')
    return
  }

  if (
    !form.title.trim() ||
    !form.address.trim() ||
    !form.areaName.trim()
  ) {
    setError('Vui lòng nhập đầy đủ thông tin phòng.')
    return
  }

  const price = Number(form.price)
  const areaSqm = Number(form.areaSqm)
  const maxOccupants = Number(form.maxOccupants)

  if (!Number.isFinite(price) || price <= 0) {
    setError('Giá thuê phải lớn hơn 0.')
    return
  }

  if (!Number.isFinite(areaSqm) || areaSqm <= 0) {
    setError('Diện tích phải lớn hơn 0.')
    return
  }

  if (
    !Number.isInteger(maxOccupants) ||
    maxOccupants < 1
  ) {
    setError('Số người tối đa phải là số nguyên từ 1 trở lên.')
    return
  }

  setSubmitting(true)

  try {
    const result = await createRoomApi(
      {
        title: form.title.trim(),
        address: form.address.trim(),
        areaName: form.areaName.trim(),
        price,
        areaSqm,
        maxOccupants,
        description: form.description.trim(),
      },
      token
    )

    setSuccess(
      `Đăng tin thành công! Mã phòng: ${result.id}. ` +
      'Tin đang chờ kiểm duyệt.'
    )

    setForm({ ...initialForm })
  } catch (err) {
    setError(
      err instanceof Error
        ? err.message
        : 'Không thể đăng tin phòng trọ.'
    )
  } finally {
    setSubmitting(false)
  }
}


  return (
    <div className="auth-page">
      <div className="room-form-card">
        <Link to="/landlord" className="auth-back">
          ← Quay về quản lý phòng
        </Link>

        <h1>Đăng tin phòng trọ</h1>

        <p className="auth-subtitle">
          Chủ trọ: {user?.fullName}
        </p>

        <p className="auth-subtitle">
          Lưu ý: Tin đăng sẽ được kiểm duyệt trước khi hiển thị
        </p>

        <form onSubmit={handleSubmit} className="room-form">
          <div className="auth-field">
            <label htmlFor="room-title">Tên phòng</label>
            <input
              id="room-title"
              value={form.title}
              onChange={(e) =>
                updateField('title', e.target.value)
              }
              placeholder="Phòng trọ gần Đại học Bách khoa"
              required
            />
          </div>

          <div className="auth-field">
            <label htmlFor="room-address">Địa chỉ</label>
            <input
              id="room-address"
              value={form.address}
              onChange={(e) =>
                updateField('address', e.target.value)
              }
              placeholder="Đường Tôn Đức Thắng, Đà Nẵng"
              required
            />
          </div>

          <div className="auth-field">
            <label htmlFor="room-area-name">Khu vực</label>
            <input
              id="room-area-name"
              value={form.areaName}
              onChange={(e) =>
                updateField('areaName', e.target.value)
              }
              placeholder="Hòa Khánh"
              required
            />
          </div>

          <div className="room-form-grid">
            <div className="auth-field">
              <label htmlFor="room-price">Giá thuê (VNĐ/tháng)</label>
              <input
                id="room-price"
                type="number"
                min="1"
                step="1"
                value={form.price}
                onChange={(e) =>
                  updateField('price', e.target.value)
                }
                placeholder="2000000"
                required
              />
            </div>

            <div className="auth-field">
              <label htmlFor="room-area">Diện tích (m²)</label>
              <input
                id="room-area"
                type="number"
                min="0.1"
                step="0.1"
                value={form.areaSqm}
                onChange={(e) =>
                  updateField('areaSqm', e.target.value)
                }
                placeholder="20"
                required
              />
            </div>
          </div>

          <div className="auth-field">
            <label htmlFor="room-occupants">
              Số người tối đa
            </label>
            <input
              id="room-occupants"
              type="number"
              min="1"
              step="1"
              value={form.maxOccupants}
              onChange={(e) =>
                updateField('maxOccupants', e.target.value)
              }
              required
            />
          </div>

          <div className="auth-field">
            <label htmlFor="room-description">Mô tả phòng</label>
            <textarea
              id="room-description"
              rows={5}
              value={form.description}
              onChange={(e) =>
                updateField('description', e.target.value)
              }
              placeholder="Mô tả tiện ích, nội thất, giờ giấc..."
            />
          </div>

          {error && (
            <div className="auth-error" role="alert">
              {error}
            </div>
          )}

          {success && (
            <div className="auth-success" role="status">
              {success}
            </div>
          )}

          <button
            type="submit"
            className="auth-submit"
            disabled={submitting}
          >
            {submitting ? 'Đang đăng tin...' : 'Đăng tin phòng trọ'}
          </button>
        </form>
      </div>
    </div>
  )
}

export default CreateRoomPage
