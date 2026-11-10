
export interface AdminRoom {
  id: number
  title: string
  description: string | null
  address: string
  areaName: string | null
  price: number
  areaSqm: number
  maxOccupants: number
  moderationStatus: string
  occupancyStatus: string
  visibilityStatus: string
  landlordId: number
}

async function adminRequest<T>(
  path: string,
  token: string,
  method: 'GET' | 'PATCH' = 'GET'
): Promise<T> {
  const response = await fetch(`/api/v1/admin/rooms${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Phiên đăng nhập hết hạn.')
    }

    if (response.status === 403) {
      throw new Error('Tài khoản không có quyền ADMIN.')
    }

    if (response.status === 409) {
      throw new Error('Tin đăng đã được xử lý trước đó.')
    }

    throw new Error(`Lỗi HTTP ${response.status}`)
  }

  return response.json() as Promise<T>
}

export function getPendingRoomsApi(token: string) {
  return adminRequest<AdminRoom[]>('/pending', token)
}

export function approveRoomApi(id: number, token: string) {
  return adminRequest<AdminRoom>(
    `/${id}/approve`,
    token,
    'PATCH'
  )
}

export function rejectRoomApi(id: number, token: string) {
  return adminRequest<AdminRoom>(
    `/${id}/reject`,
    token,
    'PATCH'
  )
}
