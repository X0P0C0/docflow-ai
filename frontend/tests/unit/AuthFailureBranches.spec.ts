import { describe, expect, it } from 'vitest'
import { buildAuthFailureRedirect, shouldSkipAuthFailureRedirect } from '../../src/utils/authFailure'

describe('auth failure branch coverage', () => {
  it('omits redirect when 401 happens on the login page itself', () => {
    expect(buildAuthFailureRedirect('/login', { status: 401 })).toEqual({
      path: '/login',
      query: {
        redirect: undefined,
        reason: 'session-expired',
      },
    })
  })

  it('omits from when 403 happens on the dashboard itself', () => {
    expect(buildAuthFailureRedirect('/dashboard', { status: 403 })).toEqual({
      path: '/dashboard',
      query: {
        reason: 'forbidden',
        from: undefined,
      },
    })
  })

  it('does not skip redirects for unrelated routes or missing forbidden markers', () => {
    expect(shouldSkipAuthFailureRedirect({
      path: '/tickets',
      query: {},
    }, { status: 401 })).toBe(false)

    expect(shouldSkipAuthFailureRedirect({
      path: '/dashboard',
    }, { status: 403 })).toBe(false)

    expect(shouldSkipAuthFailureRedirect({
      path: '/dashboard',
      query: {
        reason: 'other',
      },
    }, { status: 403 })).toBe(false)
  })

  it('skips duplicate login and forbidden redirects when the current route already matches the terminal state', () => {
    expect(shouldSkipAuthFailureRedirect({
      path: '/login',
      query: {},
    }, { status: 401 })).toBe(true)

    expect(shouldSkipAuthFailureRedirect({
      path: '/dashboard',
      query: {
        reason: 'forbidden',
      },
    }, { status: 403 })).toBe(true)
  })
})
