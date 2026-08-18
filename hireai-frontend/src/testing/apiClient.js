// HTTP API Client for HireAI Backend Testing
let activeToken = localStorage.getItem('hireai_token') || ''
let apiBaseUrl = localStorage.getItem('hireai_base_url') || ''

const listeners = new Set()

export function setGlobalToken(token) {
  activeToken = token || ''
  if (token) {
    localStorage.setItem('hireai_token', token)
  } else {
    localStorage.removeItem('hireai_token')
  }
  notifyListeners({ type: 'TOKEN_CHANGED', token: activeToken })
}

export function getGlobalToken() {
  return activeToken || localStorage.getItem('hireai_token') || ''
}

export function setApiBaseUrl(url) {
  apiBaseUrl = (url || '').trim().replace(/\/$/, '')
  localStorage.setItem('hireai_base_url', apiBaseUrl)
  notifyListeners({ type: 'BASE_URL_CHANGED', baseUrl: apiBaseUrl })
}

export function getApiBaseUrl() {
  return apiBaseUrl || localStorage.getItem('hireai_base_url') || ''
}

export function subscribeToApiEvents(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

function notifyListeners(event) {
  listeners.forEach(fn => {
    try {
      fn(event)
    } catch (e) {
      console.error('API event listener error', e)
    }
  })
}

/**
 * Perform an HTTP Request
 * @param {Object} options
 * @param {string} options.method GET | POST | PUT | PATCH | DELETE
 * @param {string} options.endpoint /auth/login or /jobs/open etc.
 * @param {Object} [options.body] Request payload
 * @param {Object} [options.params] Query parameters
 * @param {Object} [options.headers] Custom headers
 * @param {string} [options.token] Override auth token
 */
export async function apiRequest({
  method = 'GET',
  endpoint,
  body = null,
  params = null,
  headers = {},
  token = null,
}) {
  const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`
  const base = getApiBaseUrl()
  let url = `${base}${cleanEndpoint}`

  if (params && Object.keys(params).length > 0) {
    const queryParams = new URLSearchParams()
    Object.entries(params).forEach(([key, val]) => {
      if (val !== undefined && val !== null && val !== '') {
        queryParams.append(key, String(val))
      }
    })
    const queryString = queryParams.toString()
    if (queryString) {
      url += (url.includes('?') ? '&' : '?') + queryString
    }
  }

  const bearer = token !== null && token !== undefined ? token : getGlobalToken()
  const reqHeaders = {
    ...headers,
  }

  if (body !== null && body !== undefined && !(body instanceof FormData)) {
    reqHeaders['Content-Type'] = reqHeaders['Content-Type'] || 'application/json'
  }

  if (bearer) {
    reqHeaders['Authorization'] = bearer.startsWith('Bearer ') ? bearer : `Bearer ${bearer}`
  }

  const startTime = performance.now()
  const timestamp = new Date().toLocaleTimeString()

  let responseData = null
  let status = 0
  let statusText = 'Network Error'
  let ok = false
  let responseHeaders = {}

  try {
    const fetchOptions = {
      method: method.toUpperCase(),
      headers: reqHeaders,
    }

    if (body !== null && body !== undefined) {
      if (body instanceof FormData) {
        fetchOptions.body = body
      } else if (typeof body === 'string') {
        fetchOptions.body = body
      } else {
        fetchOptions.body = JSON.stringify(body)
      }
    }

    const res = await fetch(url, fetchOptions)
    const endTime = performance.now()
    const durationMs = Math.round(endTime - startTime)

    status = res.status
    statusText = res.statusText || (res.ok ? 'OK' : 'Error')
    ok = res.ok

    // Extract headers
    res.headers.forEach((value, key) => {
      responseHeaders[key] = value
    })

    const contentType = res.headers.get('content-type') || ''
    if (contentType.includes('application/json')) {
      responseData = await res.json().catch(() => null)
    } else {
      const text = await res.text()
      try {
        responseData = JSON.parse(text)
      } catch {
        responseData = text
      }
    }

    const logEntry = {
      id: Date.now() + Math.random().toString(36).substring(2, 7),
      timestamp,
      method: method.toUpperCase(),
      url,
      endpoint: cleanEndpoint,
      status,
      statusText,
      ok,
      durationMs,
      requestPayload: body,
      responseData,
      requestHeaders: reqHeaders,
      responseHeaders,
    }

    notifyListeners({ type: 'REQUEST_COMPLETED', log: logEntry })

    return {
      ok,
      status,
      statusText,
      data: responseData,
      headers: responseHeaders,
      durationMs,
      url,
      method: method.toUpperCase(),
      timestamp,
      error: ok ? null : (responseData?.message || responseData?.error || `HTTP ${status}: ${statusText}`),
    }
  } catch (err) {
    const endTime = performance.now()
    const durationMs = Math.round(endTime - startTime)

    const logEntry = {
      id: Date.now() + Math.random().toString(36).substring(2, 7),
      timestamp,
      method: method.toUpperCase(),
      url,
      endpoint: cleanEndpoint,
      status: 0,
      statusText: 'Failed to Connect',
      ok: false,
      durationMs,
      requestPayload: body,
      responseData: { error: err.message },
      requestHeaders: reqHeaders,
      responseHeaders: {},
    }

    notifyListeners({ type: 'REQUEST_COMPLETED', log: logEntry })

    return {
      ok: false,
      status: 0,
      statusText: 'Connection Refused',
      data: { error: err.message, note: 'Make sure hireai-backend is running on http://localhost:8080' },
      headers: {},
      durationMs,
      url,
      method: method.toUpperCase(),
      timestamp,
      error: err.message || 'Network error occurred',
    }
  }
}
