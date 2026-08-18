import React, { useState } from 'react'
import { apiRequest, getGlobalToken } from '../apiClient'
import ResponseViewer from '../components/ResponseViewer'
import { Play, Plus, Trash2, Send, Terminal } from 'lucide-react'

export default function RawApiRunner() {
  const [method, setMethod] = useState('GET')
  const [endpoint, setEndpoint] = useState('/users/me')
  const [tokenOverride, setTokenOverride] = useState('')
  const [headersList, setHeadersList] = useState([])
  const [paramsList, setParamsList] = useState([])
  const [jsonBody, setJsonBody] = useState('{\n  \n}')
  const [loading, setLoading] = useState(false)
  const [response, setResponse] = useState(null)

  const addHeader = () => setHeadersList(prev => [...prev, { key: '', value: '' }])
  const removeHeader = (idx) => setHeadersList(prev => prev.filter((_, i) => i !== idx))
  const updateHeader = (idx, field, val) => {
    setHeadersList(prev => prev.map((h, i) => i === idx ? { ...h, [field]: val } : h))
  }

  const addParam = () => setParamsList(prev => [...prev, { key: '', value: '' }])
  const removeParam = (idx) => setParamsList(prev => prev.filter((_, i) => i !== idx))
  const updateParam = (idx, field, val) => {
    setParamsList(prev => prev.map((p, i) => i === idx ? { ...p, [field]: val } : p))
  }

  const handleExecute = async () => {
    setLoading(true)
    setResponse(null)

    const headersObj = {}
    headersList.forEach(h => {
      if (h.key.trim()) headersObj[h.key.trim()] = h.value
    })

    const paramsObj = {}
    paramsList.forEach(p => {
      if (p.key.trim()) paramsObj[p.key.trim()] = p.value
    })

    let parsedBody = null
    if (['POST', 'PUT', 'PATCH'].includes(method)) {
      try {
        if (jsonBody.trim()) {
          parsedBody = JSON.parse(jsonBody)
        }
      } catch (err) {
        alert('Invalid JSON body: ' + err.message)
        setLoading(false)
        return
      }
    }

    try {
      const res = await apiRequest({
        method,
        endpoint,
        body: parsedBody,
        params: paramsObj,
        headers: headersObj,
        token: tokenOverride.trim() || undefined,
      })
      setResponse(res)
    } catch (e) {
      setResponse({
        ok: false,
        status: 0,
        statusText: 'Error',
        data: { error: e.message },
        durationMs: 0,
        url: endpoint,
        method,
        timestamp: new Date().toLocaleTimeString(),
      })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="tester-container">
      <div className="tester-header">
        <div className="tester-title-wrap">
          <h2>Custom API Explorer & HTTP Runner</h2>
          <p className="tester-subtitle">
            Craft custom HTTP requests with arbitrary methods, paths, headers, query parameters, and JSON payloads.
          </p>
        </div>
      </div>

      <div className="raw-runner-card">
        {/* Main Bar */}
        <div className="raw-top-bar">
          <select
            className={`raw-method-select method-${method.toLowerCase()}`}
            value={method}
            onChange={(e) => setMethod(e.target.value)}
          >
            <option value="GET">GET</option>
            <option value="POST">POST</option>
            <option value="PUT">PUT</option>
            <option value="PATCH">PATCH</option>
            <option value="DELETE">DELETE</option>
            <option value="OPTIONS">OPTIONS</option>
          </select>

          <input
            type="text"
            className="raw-url-input"
            value={endpoint}
            onChange={(e) => setEndpoint(e.target.value)}
            placeholder="/api/v1/resource or /jobs/open"
          />

          <button
            type="button"
            className="btn-primary"
            onClick={handleExecute}
            disabled={loading}
          >
            <Play size={16} fill="currentColor" /> {loading ? 'Sending...' : 'Send Request'}
          </button>
        </div>

        {/* Token Override */}
        <div className="raw-token-bar">
          <span className="token-label">Custom Bearer Token (optional override):</span>
          <input
            type="text"
            className="raw-token-input"
            placeholder="Leave empty to use active global JWT token"
            value={tokenOverride}
            onChange={(e) => setTokenOverride(e.target.value)}
          />
        </div>

        {/* Query Params Section */}
        <div className="raw-section">
          <div className="raw-section-header">
            <span>Query Parameters:</span>
            <button type="button" className="btn-tiny" onClick={addParam}>
              <Plus size={12} /> Add Query Param
            </button>
          </div>
          {paramsList.length === 0 ? (
            <p className="hint-text">No query parameters added.</p>
          ) : (
            <div className="raw-kv-list">
              {paramsList.map((p, i) => (
                <div key={i} className="raw-kv-row">
                  <input
                    type="text"
                    placeholder="Key (e.g. page)"
                    value={p.key}
                    onChange={(e) => updateParam(i, 'key', e.target.value)}
                  />
                  <input
                    type="text"
                    placeholder="Value (e.g. 0)"
                    value={p.value}
                    onChange={(e) => updateParam(i, 'value', e.target.value)}
                  />
                  <button type="button" className="btn-icon-danger" onClick={() => removeParam(i)}>
                    <Trash2 size={14} />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Headers Section */}
        <div className="raw-section">
          <div className="raw-section-header">
            <span>Custom Headers:</span>
            <button type="button" className="btn-tiny" onClick={addHeader}>
              <Plus size={12} /> Add Header
            </button>
          </div>
          {headersList.length === 0 ? (
            <p className="hint-text">No custom headers added.</p>
          ) : (
            <div className="raw-kv-list">
              {headersList.map((h, i) => (
                <div key={i} className="raw-kv-row">
                  <input
                    type="text"
                    placeholder="Header Name (e.g. X-Custom-Header)"
                    value={h.key}
                    onChange={(e) => updateHeader(i, 'key', e.target.value)}
                  />
                  <input
                    type="text"
                    placeholder="Header Value"
                    value={h.value}
                    onChange={(e) => updateHeader(i, 'value', e.target.value)}
                  />
                  <button type="button" className="btn-icon-danger" onClick={() => removeHeader(i)}>
                    <Trash2 size={14} />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Request Body Section */}
        {['POST', 'PUT', 'PATCH'].includes(method) && (
          <div className="raw-section">
            <div className="raw-section-header">
              <span>JSON Request Body:</span>
            </div>
            <textarea
              className="code-textarea"
              rows={8}
              value={jsonBody}
              onChange={(e) => setJsonBody(e.target.value)}
              placeholder="{\n  \n}"
              spellCheck={false}
            />
          </div>
        )}

        {/* Response Panel */}
        <div className="raw-response-wrap">
          <ResponseViewer response={response} loading={loading} />
        </div>
      </div>
    </div>
  )
}
