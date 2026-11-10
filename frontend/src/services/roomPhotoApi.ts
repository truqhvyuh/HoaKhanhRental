
export interface RoomPhoto {
  id: number
  url: string
}

export async function getPublicRoomPhotos(
  roomId: number
): Promise<RoomPhoto[]> {
  const response = await fetch(
    `/api/v1/rooms/${roomId}/photos`
  )

  if (!response.ok) {
    throw new Error('Không thể tải ảnh phòng.')
  }

  return response.json()
}

export async function getMyRoomPhotos(
  roomId: number,
  token: string
): Promise<RoomPhoto[]> {
  const response = await fetch(
    `/api/v1/landlord/rooms/${roomId}/photos`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  )

  if (!response.ok) {
    throw new Error('Không thể tải ảnh của bạn.')
  }

  return response.json()
}

export async function uploadRoomPhotos(
  roomId: number,
  files: File[],
  token: string
): Promise<RoomPhoto[]> {
  const formData = new FormData()

  files.forEach((file) => {
    formData.append('files', file)
  })

  const response = await fetch(
    `/api/v1/landlord/rooms/${roomId}/photos`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
      },
      body: formData,
    }
  )

  if (!response.ok) {
    throw new Error(
      `Tải ảnh thất bại (HTTP ${response.status}).`
    )
  }

  return response.json()
}

export async function deleteRoomPhoto(
  roomId: number,
  photoId: number,
  token: string
): Promise<void> {
  const response = await fetch(
    `/api/v1/landlord/rooms/${roomId}/photos/${photoId}`,
    {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  )

  if (!response.ok) {
    throw new Error(
      `Không thể xóa ảnh (HTTP ${response.status}).`
    )
  }
}

export async function getMyRoomPhotoFile(
  roomId: number,
  photoId: number,
  token: string,
  signal?: AbortSignal
): Promise<Blob> {
  const response = await fetch(
    `/api/v1/landlord/rooms/${roomId}/photos/${photoId}/file`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
      signal,
    }
  )

  if (!response.ok) {
    throw new Error(`Không thể tải ảnh (HTTP ${response.status}).`)
  }

  return response.blob()
}
