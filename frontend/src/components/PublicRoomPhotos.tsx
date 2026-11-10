
import { useEffect, useState } from 'react'
import {
  getPublicRoomPhotos,
  type RoomPhoto,
} from '../services/roomPhotoApi'

interface Props {
  roomId: number
  coverOnly?: boolean
}

function PublicRoomPhotos({
  roomId,
  coverOnly = false,
}: Props) {
  const [photos, setPhotos] = useState<RoomPhoto[]>([])

  useEffect(() => {
    let cancelled = false

    getPublicRoomPhotos(roomId)
      .then((data) => {
        if (!cancelled) setPhotos(data)
      })
      .catch(() => {
        if (!cancelled) setPhotos([])
      })

    return () => {
      cancelled = true
    }
  }, [roomId])

  if (coverOnly) {
    return (
      <div className="room-image">
        {photos.length > 0 ? (
          <img
            src={photos[0].url}
            alt={`Ảnh phòng ${roomId}`}
            loading="lazy"
          />
        ) : (
          <>
            <span>⌂</span>
            <small>Chưa có ảnh</small>
          </>
        )}
      </div>
    )
  }

  return (
    <section className="room-photo-section">
      <h2>Hình ảnh phòng trọ</h2>

      {photos.length === 0 ? (
        <p>Phòng này chưa có ảnh.</p>
      ) : (
        <div className="room-photo-grid">
          {photos.map((photo) => (
            <div className="room-photo-item" key={photo.id}>
              <img
                src={photo.url}
                alt={`Ảnh phòng ${roomId}`}
                loading="lazy"
              />
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

export default PublicRoomPhotos
