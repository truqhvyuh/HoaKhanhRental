
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../contexts/useAuth'
import {
  getMyRoomByIdApi,
  updateMyRoomApi,
  type UpdateRoomRequest,
} from '../services/landlordRoomApi'

const emptyForm: UpdateRoomRequest = {
  title: '',
  address: '',
  areaName: '',
  price: 0,
  areaSqm: 0,
  maxOccupants: 1,
  description: '',
}

function EditRoomPage() {
  const { id } = useParams<{ id: string }>()
  const roomId = Number(id)
  const { token } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState<UpdateRoomRequest>(emptyForm)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    

    let cancelled = false

    async function loadRoom() {
      if (
        !token ||
        !id ||
        !Number.isSafeInteger(roomId) ||
        roomId <= 0
    ) {
        setError('Mã phòng không hợp lệ.')
        setLoading(false)
        return
    }

    setLoading(true)
    setError('')


      try {
        const room = await getMyRoomByIdApi(roomId, token!)

        if (!cancelled) {
          setForm({
            title: room.title,
            address: room.address,
            areaName: room.areaName ?? '',
            price: Number(room.price),
            areaSqm: Number(room.areaSqm),
            maxOccupants: room.maxOccupants,
            description: room.description ?? '',
          })
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error
              ? err.message
              : 'Không thể tải phòng.'
          )
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    loadRoom()

    return () => {
      cancelled = true
    }
  }, [id, roomId, token])

  function updateField<K extends keyof UpdateRoomRequest>(
    field: K,
    value: UpdateRoomRequest[K]
  ) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!token || saving) return

    if (
      !form.title.trim() ||
      !form.address.trim() ||
      !Number.isFinite(form.price) ||
      form.price <= 0 ||
      !Number.isFinite(form.areaSqm) ||
      form.areaSqm <= 0 ||
      !Number.isSafeInteger(form.maxOccupants) ||
      form.maxOccupants < 1
    ) {
      setError('Vui lòng nhập đầy đủ thông tin hợp lệ.')
      return
    }

    setSaving(true)
    setError('')

    try {
      await updateMyRoomApi(
        roomId,
        {
          ...form,
          title: form.title.trim(),
          address: form.address.trim(),
        },
        token
      )

      navigate(`/landlord/rooms/${roomId}`, {
        replace: true,
      })
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Không thể cập nhật phòng.'
      )
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <main className="room-edit-page">Đang tải phòng...</main>
  }

  return (
    <main className="room-edit-page">
      <Link to={`/landlord/rooms/${roomId}`}>
        ← Quay lại chi tiết phòng
      </Link>

      <h1>Chỉnh sửa phòng trọ</h1>
      <p>Sau khi lưu, tin đăng sẽ chuyển về trạng thái chờ duyệt.</p>

      {error && <div className="auth-error" role="alert">{error}</div>}

      <form onSubmit={handleSubmit} className="room-edit-form">
        <label>
          Tên phòng
          <input
            required
            maxLength={200}
            value={form.title}
            onChange={(e) => updateField('title', e.target.value)}
          />
        </label>

        <label>
          Địa chỉ
          <input
            required
            maxLength={255}
            value={form.address}
            onChange={(e) => updateField('address', e.target.value)}
          />
        </label>

        <label>
          Khu vực
          <input
            maxLength={150}
            value={form.areaName}
            onChange={(e) => updateField('areaName', e.target.value)}
          />
        </label>

        <label>
          Giá thuê (VNĐ/tháng)
          <input
            required
            type="number"
            min="1"
            step="0.01"
            value={form.price}
            onChange={(e) =>
              updateField('price', Number(e.target.value))
            }
          />
        </label>

        <label>
          Diện tích (m²)
          <input
            required
            type="number"
            min="0.01"
            step="0.01"
            value={form.areaSqm}
            onChange={(e) =>
              updateField('areaSqm', Number(e.target.value))
            }
          />
        </label>

        <label>
          Số người tối đa
          <input
            required
            type="number"
            min="1"
            step="1"
            value={form.maxOccupants}
            onChange={(e) =>
              updateField('maxOccupants', Number(e.target.value))
            }
          />
        </label>

        <label>
          Mô tả phòng
          <textarea
            rows={6}
            value={form.description}
            onChange={(e) =>
              updateField('description', e.target.value)
            }
          />
        </label>

        <button type="submit" disabled={saving}>
          {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
        </button>
      </form>
    </main>
  )
}

export default EditRoomPage
