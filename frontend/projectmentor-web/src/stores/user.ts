import { defineStore } from 'pinia'
import type { LoginResponse, UserInfo } from '@/types/api'

const TOKEN_KEY = 'projectmentor_token'
const USER_KEY = 'projectmentor_user'
const CREDITS_KEY = 'projectmentor_credits'

function readUserInfo(): UserInfo | null {
  const rawUser = localStorage.getItem(USER_KEY)
  if (!rawUser) {
    return null
  }

  try {
    return JSON.parse(rawUser) as UserInfo
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

function readCredits(): number {
  const rawCredits = localStorage.getItem(CREDITS_KEY)
  if (!rawCredits) {
    return 0
  }

  const credits = Number(rawCredits)
  return Number.isNaN(credits) ? 0 : credits
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: readUserInfo(),
    remainingCredits: readCredits()
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    setLoginState(loginResponse: LoginResponse) {
      this.token = loginResponse.token
      this.userInfo = loginResponse.userInfo
      this.remainingCredits = loginResponse.remainingCredits ?? 0

      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.userInfo))
      localStorage.setItem(CREDITS_KEY, String(this.remainingCredits))
    },
    updateCredits(credits: number) {
      this.remainingCredits = credits
      localStorage.setItem(CREDITS_KEY, String(credits))
    },
    logout() {
      this.token = ''
      this.userInfo = null
      this.remainingCredits = 0

      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      localStorage.removeItem(CREDITS_KEY)
    }
  }
})

export { TOKEN_KEY, USER_KEY, CREDITS_KEY }
