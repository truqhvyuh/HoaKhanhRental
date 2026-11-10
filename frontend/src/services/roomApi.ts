
import type { Room, RoomFilters, RoomPage } from '../types/room'

export async function getRooms(
  filters: RoomFilters,
  page: number,
  size: number = 6,
  signal?: AbortSignal
): Promise<RoomPage> {
  const params = new URLSearchParams()

  params.set('page', String(page))
  params.set('size', String(size))
  params.set('sort', filters.sort)

  if (filters.keyword.trim()) {
    params.set('keyword', filters.keyword.trim())
  }

  if (filters.area.trim()) {
    params.set('area', filters.area.trim())
  }

  if (filters.minPrice !== '') {
    params.set('minPrice', filters.minPrice)
  }

  if (filters.maxPrice !== '') {
    params.set('maxPrice', filters.maxPrice)
  }

  if (filters.minArea !== '') {
    params.set('minArea', filters.minArea)
  }

  if (filters.maxArea !== '') {
    params.set('maxArea', filters.maxArea)
  }

  const response = await fetch(
    `/api/v1/rooms?${params.toString()}`,
    { signal }
  )

  if (!response.ok) {
    throw new Error(
      `Không thể tải phòng trọ (HTTP ${response.status})`
    )
  }

  return (await response.json()) as RoomPage
}


export async function getRoomById(
  id: number,
  signal?: AbortSignal
): Promise<Room> {
  const response = await fetch(
    `/api/v1/rooms/${id}`,
    { signal }
  )

  if (response.status === 404) {
    throw new Error('Không tìm thấy phòng trọ')
  }

  if (!response.ok) {
    throw new Error(
      `Không thể tải chi tiết phòng (HTTP ${response.status})`
    )
  }

  return (await response.json()) as Room
}
