
import { useEffect, useState, type ChangeEvent } from 'react'
import PrivateRoomPhoto from './PrivateRoomPhoto'
import {
  getMyRoomPhotos,
  uploadRoomPhotos,
  deleteRoomPhoto,
  type RoomPhoto,
} from '../services/roomPhotoApi'

interface Props {
  roomId: number
  token: string
}

function LandlordRoomPhotos({ roomId, token }: Props) {
  const [photos, setPhotos] = useState<RoomPhoto[]>([])
  const [loading, setLoading] = useState(true)
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    getMyRoomPhotos(roomId, token)
      .then((data) => {
        if (!cancelled) setPhotos(data)
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(
            err instanceof Error ? err.message : 'Lỗi tải ảnh.'
          )
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [roomId, token])

  async function handleUpload(
    event: ChangeEvent<HTMLInputElement>
  ) {
    const files = Array.from(event.target.files ?? [])
    event.target.value = ''

    if (files.length === 0 || processing) return

    if (files.length + photos.length > 5) {
      setError('Mỗi phòng chỉ được tối đa 5 ảnh.')
      return
    }

    if (
      files.some(
        (file) =>
          !['image/jpeg', 'image/png'].includes(file.type) ||
          file.size > 5 * 1024 * 1024 ||
          file.size === 0
      )
    ) {
      setError('Chỉ chọn ảnh JPG/PNG, tối đa 5 MB mỗi ảnh.')
      return
    }

    setProcessing(true)
    setError('')

    try {
      const updated = await uploadRoomPhotos(
        roomId,
        files,
        token
      )
      setPhotos(updated)
    } catch (err) {
      setError(
        err instanceof Error ? err.message : 'Tải ảnh thất bại.'
      )
    } finally {
      setProcessing(false)
    }
  }

  async function handleDelete(photoId: number) {
    if (processing) return

    if (!window.confirm('Xóa ảnh này?')) return

    setProcessing(true)
    setError('')

    try {
      await deleteRoomPhoto(roomId, photoId, token)

      setPhotos((previous) =>
        previous.filter((photo) => photo.id !== photoId)
      )
    } catch (err) {
      setError(
        err instanceof Error ? err.message : 'Xóa ảnh thất bại.'
      )
    } finally {
      setProcessing(false)
    }
  }

  return (
    <section className="room-photo-section">
      <h2>Ảnh phòng trọ ({photos.length}/5)</h2>

      <p>Chọn ảnh JPG hoặc PNG, tối đa 5 MB mỗi ảnh.</p>

      {loading && <p>Đang tải ảnh...</p>}

      {error && (
        <div className="auth-error" role="alert">
          {error}
        </div>
      )}

      {!loading && (
        <>
          <div className="room-photo-grid">
            {photos.map((photo) => (
              <div className="room-photo-item" key={photo.id}>
                              
              <PrivateRoomPhoto
                roomId={roomId}
                photoId={photo.id}
                token={token}
              />


                <button
                  type="button"
                  disabled={processing}
                  onClick={() => handleDelete(photo.id)}
                >
                  Xóa ảnh
                </button>
              </div>
            ))}
          </div>

          {photos.length < 5 && (
            <label className="room-photo-upload">
              {processing ? 'Đang xử lý...' : '+ Thêm ảnh phòng'}
              <input
                type="file"
                accept="image/jpeg,image/png"
                multiple
                disabled={processing}
                onChange={handleUpload}
              />
            </label>
          )}
        </>
      )}
    </section>
  )
}

export default LandlordRoomPhotos
