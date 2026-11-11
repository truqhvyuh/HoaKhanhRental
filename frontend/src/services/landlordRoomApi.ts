
export interface CreateRoomRequest {
  title: string
  address: string
  areaName: string
  price: number
  areaSqm: number
  maxOccupants: number
  description: string
}

export interface CreateRoomResponse {
  id: number
  message: string
  moderationStatus: string
}

export async function createRoomApi(
  data: CreateRoomRequest,
  token: string
): Promise<CreateRoomResponse> {
  const response = await fetch('/api/v1/landlord/rooms', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(data),
  })

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Phiên đăng nhập không hợp lệ hoặc đã hết hạn.')
    }

    if (response.status === 403) {
      throw new Error('Tài khoản không có quyền đăng tin.')
    }

    if (response.status === 400) {
      throw new Error('Thông tin phòng không hợp lệ.')
    }

    throw new Error(`Đăng tin thất bại (HTTP ${response.status}).`)
  }

  return response.json()
}


export interface LandlordRoom {
  id: number
  title: string
  description: string | null
  address: string
  areaName: string | null
  price: number
  areaSqm: number
  maxOccupants: number
  moderationStatus: string
  rejectionReason: string | null
  occupancyStatus: string
  visibilityStatus: string
}

export async function getMyRoomsApi(
  token: string
): Promise<LandlordRoom[]> {
  const response = await fetch('/api/v1/landlord/rooms', {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Phiên đăng nhập đã hết hạn.')
    }

    if (response.status === 403) {
      throw new Error('Bạn không có quyền xem danh sách phòng.')
    }

    throw new Error(
      `Không thể tải danh sách phòng (HTTP ${response.status}).`
    )
  }

  return response.json()
}

export async function getMyRoomByIdApi(
  roomId: number,
  token: string
): Promise<LandlordRoom> {
  const response = await fetch(
    `/api/v1/landlord/rooms/${roomId}`,
    {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  )

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Phiên đăng nhập đã hết hạn.')
    }

    if (response.status === 403) {
      throw new Error('Bạn không có quyền xem phòng này.')
    }

    if (response.status === 404) {
      throw new Error('Không tìm thấy phòng hoặc phòng không thuộc tài khoản của bạn.')
    }

    throw new Error(
      `Không thể tải chi tiết phòng (HTTP ${response.status}).`
    )
  }

  return response.json()
}


export interface UpdateRoomRequest {
  title: string
  address: string
  areaName: string
  price: number
  areaSqm: number
  maxOccupants: number
  description: string
}

export async function updateMyRoomApi(
  roomId: number,
  data: UpdateRoomRequest,
  token: string
): Promise<LandlordRoom> {
  const response = await fetch(
    `/api/v1/landlord/rooms/${roomId}`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(data),
    }
  )

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Phiên đăng nhập đã hết hạn.')
    }

    if (response.status === 403) {
      throw new Error('Bạn không có quyền sửa phòng.')
    }

    if (response.status === 404) {
      throw new Error('Không tìm thấy phòng.')
    }

    if (response.status === 400) {
      throw new Error('Thông tin phòng không hợp lệ.')
    }

    throw new Error(`Lỗi cập nhật phòng: HTTP ${response.status}`)
  }

  return response.json()
}


export interface UpdateRoomStatusRequest {
  visibilityStatus: 'VISIBLE' | 'HIDDEN'
  occupancyStatus: 'AVAILABLE' | 'RENTED'
}

export async function updateRoomStatusApi(
  roomId: number,
  data: UpdateRoomStatusRequest,
  token: string
): Promise<LandlordRoom> {
  const response = await fetch(
    `/api/v1/landlord/rooms/${roomId}/status`,
    {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(data),
    }
  )

  if (!response.ok) {
    throw new Error(
      `Không thể cập nhật trạng thái phòng (HTTP ${response.status}).`
    )
  }

  return response.json()
}

export async function deleteMyRoomApi(
  roomId: number,
  token: string
): Promise<void> {
  const response = await fetch(
    `/api/v1/landlord/rooms/${roomId}`,
    {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  )

  if (!response.ok) {
    throw new Error(
      `Không thể xóa phòng (HTTP ${response.status}).`
    )
  }
}
