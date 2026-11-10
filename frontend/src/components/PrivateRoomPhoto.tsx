
import { useEffect, useState } from 'react'
import { getMyRoomPhotoFile } from '../services/roomPhotoApi'

interface Props {
  roomId: number
  photoId: number
  token: string
}

function PrivateRoomPhoto({ roomId, photoId, token }: Props) {
  const [src, setSrc] = useState<string | null>(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    let objectUrl: string | null = null

    async function loadPhoto() {
      try {
        const blob = await getMyRoomPhotoFile(
          roomId,
          photoId,
          token,
          controller.signal
        )

        if (controller.signal.aborted) return

        objectUrl = URL.createObjectURL(blob)
        setSrc(objectUrl)
      } catch {
        if (!controller.signal.aborted) {
          setError(true)
        }
      }
    }

    void loadPhoto()

    return () => {
      controller.abort()

      if (objectUrl) {
        URL.revokeObjectURL(objectUrl)
      }
    }
  }, [roomId, photoId, token])

  if (error) {
    return <p>Không tải được ảnh.</p>
  }

  if (!src) {
    return <p>Đang tải ảnh...</p>
  }

  return (
    <img
      src={src}
      alt={`Ảnh phòng ${photoId}`}
      loading="lazy"
    />
  )
}

export default PrivateRoomPhoto
