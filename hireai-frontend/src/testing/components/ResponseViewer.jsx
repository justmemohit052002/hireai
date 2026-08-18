import React, { useState } from 'react'
import { Check, Copy, Clock, Layers, AlertCircle, ChevronDown, ChevronRight } from 'lucide-react'

export default function ResponseViewer({ response, loading }) {
  const [copied, setCopied] = useState(false)
  const [showHeaders, setShowHeaders] = useState(false)

  if (loading) {
    return (
      <div className="response-box loading-box">
        <div className="spinner-dots">
          <span />
          <span />
          <span />
        </div>
        <p>Executing request against HireAI backend...</p>
      </div>
    )
  }

  if (!response) {
    return (
      <div className="response-box empty-box">
        <p className="hint-text">No request executed yet. Click "Send Request" to test this endpoint.</p>
      </div>
    )
  }

  const { ok, status, statusText, data, headers, durationMs, timestamp, method, url } = response

  const getStatusColor = (code) => {
    if (code >= 200 && code < 300) return 'status-success'
    if (code >= 400 && code < 500) return 'status-warn'
    if (code >= 500) return 'status-error'
    return 'status-neutral'
  }

  const handleCopy = () => {
    navigator.clipboard.writeText(
      typeof data === 'object' ? JSON.stringify(data, null, 2) : String(data)
    )
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const jsonString = typeof data === 'object' && data !== null
    ? JSON.stringify(data, null, 2)
    : (data !== undefined && data !== null ? String(data) : 'No response body')

  return (
    <div className={`response-box ${ok ? 'response-ok' : 'response-failed'}`}>
      <div className="response-header">
        <div className="response-meta-left">
          <span className={`status-pill ${getStatusColor(status)}`}>
            {status || '0'} {statusText}
          </span>
          <span className="timing-pill">
            <Clock size={13} /> {durationMs} ms
          </span>
          <span className="timestamp-pill">{timestamp}</span>
        </div>

        <div className="response-meta-right">
          {headers && Object.keys(headers).length > 0 && (
            <button
              type="button"
              className="btn-tiny"
              onClick={() => setShowHeaders(!showHeaders)}
            >
              <Layers size={13} /> {showHeaders ? 'Hide Headers' : 'Headers'}
            </button>
          )}
          <button type="button" className="btn-tiny" onClick={handleCopy}>
            {copied ? <Check size={13} color="#10b981" /> : <Copy size={13} />}
            {copied ? 'Copied' : 'Copy JSON'}
          </button>
        </div>
      </div>

      <div className="response-sub-bar">
        <code>{method} {url}</code>
      </div>

      {showHeaders && headers && (
        <div className="headers-panel">
          <div className="headers-title">Response Headers:</div>
          <pre>{JSON.stringify(headers, null, 2)}</pre>
        </div>
      )}

      {!ok && response.error && (
        <div className="response-error-banner">
          <AlertCircle size={16} />
          <span>{response.error}</span>
        </div>
      )}

      <div className="response-body">
        <pre className="json-code">
          <code>{jsonString}</code>
        </pre>
      </div>
    </div>
  )
}
