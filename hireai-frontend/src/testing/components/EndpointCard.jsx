import React, { useState } from 'react'
import { Play, Sparkles, RotateCcw, AlertTriangle, Key, ShieldCheck } from 'lucide-react'
import ResponseViewer from './ResponseViewer'
import { apiRequest } from '../apiClient'

export default function EndpointCard({
  title,
  description,
  method = 'GET',
  path,
  authRequired = 'NONE', // 'NONE' | 'CANDIDATE' | 'RECRUITER' | 'AUTHENTICATED'
  defaultPayload = null,
  pathParams = [], // ['jobId', 'userId']
  queryParams = [], // ['page', 'size']
  onSuccess = null,
  extraActions = null,
}) {
  const [paramValues, setParamValues] = useState({})
  const [queryValues, setQueryValues] = useState({})
  const [jsonBody, setJsonBody] = useState(
    defaultPayload ? JSON.stringify(defaultPayload, null, 2) : ''
  )
  const [jsonError, setJsonError] = useState('')
  const [loading, setLoading] = useState(false)
  const [response, setResponse] = useState(null)
  const [isExpanded, setIsExpanded] = useState(true)

  const handleParamChange = (param, value) => {
    setParamValues((prev) => ({ ...prev, [param]: value }))
  }

  const handleQueryChange = (queryKey, value) => {
    setQueryValues((prev) => ({ ...prev, [queryKey]: value }))
  }

  const handleJsonChange = (e) => {
    const val = e.target.value
    setJsonBody(val)
    if (!val.trim()) {
      setJsonError('')
      return
    }
    try {
      JSON.parse(val)
      setJsonError('')
    } catch (err) {
      setJsonError('Invalid JSON syntax: ' + err.message)
    }
  }

  const resetToDefault = () => {
    if (defaultPayload) {
      setJsonBody(JSON.stringify(defaultPayload, null, 2))
      setJsonError('')
    } else {
      setJsonBody('')
    }
  }

  const executeRequest = async () => {
    let resolvedPath = path
    for (const p of pathParams) {
      const val = paramValues[p] || ''
      if (!val && path.includes(`{${p}}`)) {
        alert(`Please specify the URL path parameter: ${p}`)
        return
      }
      resolvedPath = resolvedPath.replace(`{${p}}`, encodeURIComponent(val))
    }

    let parsedBody = null
    if (jsonBody && ['POST', 'PUT', 'PATCH'].includes(method.toUpperCase())) {
      try {
        parsedBody = JSON.parse(jsonBody)
      } catch (err) {
        setJsonError('Invalid JSON body')
        return
      }
    }

    setLoading(true)
    setResponse(null)

    try {
      const res = await apiRequest({
        method,
        endpoint: resolvedPath,
        params: queryValues,
        body: parsedBody,
      })

      setResponse(res)

      if (res.ok && onSuccess) {
        onSuccess(res.data, res)
      }
    } catch (e) {
      setResponse({
        ok: false,
        status: 0,
        statusText: 'Error',
        data: { error: e.message },
        durationMs: 0,
        url: resolvedPath,
        method,
        timestamp: new Date().toLocaleTimeString(),
      })
    } finally {
      setLoading(false)
    }
  }

  const getMethodBadgeClass = (m) => {
    switch (m.toUpperCase()) {
      case 'GET': return 'method-get'
      case 'POST': return 'method-post'
      case 'PUT': return 'method-put'
      case 'PATCH': return 'method-patch'
      case 'DELETE': return 'method-delete'
      default: return 'method-other'
    }
  }

  const getAuthBadge = () => {
    switch (authRequired) {
      case 'CANDIDATE':
        return <span className="auth-pill auth-candidate">Requires Candidate Role</span>
      case 'RECRUITER':
        return <span className="auth-pill auth-recruiter">Requires Recruiter Role</span>
      case 'AUTHENTICATED':
        return <span className="auth-pill auth-auth">Requires Bearer Token</span>
      default:
        return <span className="auth-pill auth-public">Public API</span>
    }
  }

  return (
    <div className="endpoint-card">
      <div className="endpoint-card-header" onClick={() => setIsExpanded(!isExpanded)}>
        <div className="endpoint-left-meta">
          <span className={`method-badge ${getMethodBadgeClass(method)}`}>{method}</span>
          <code className="endpoint-path">{path}</code>
          {getAuthBadge()}
        </div>
        <div className="endpoint-title-desc">
          <span className="card-title-text">{title}</span>
        </div>
      </div>

      {isExpanded && (
        <div className="endpoint-card-body">
          {description && <p className="endpoint-description">{description}</p>}

          {/* Path Parameters */}
          {pathParams.length > 0 && (
            <div className="form-section">
              <label className="section-label">Path Parameters (URL Variables):</label>
              <div className="params-grid">
                {pathParams.map((param) => (
                  <div key={param} className="param-input-group">
                    <span className="param-name">{`{${param}}`}</span>
                    <input
                      type="text"
                      className="param-field"
                      placeholder={`Enter ${param} (UUID)`}
                      value={paramValues[param] || ''}
                      onChange={(e) => handleParamChange(param, e.target.value)}
                    />
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Query Parameters */}
          {queryParams.length > 0 && (
            <div className="form-section">
              <label className="section-label">Query Parameters (?key=value):</label>
              <div className="params-grid">
                {queryParams.map((q) => (
                  <div key={q} className="param-input-group">
                    <span className="param-name">{q}</span>
                    <input
                      type="text"
                      className="param-field"
                      placeholder={`value for ${q}`}
                      value={queryValues[q] || ''}
                      onChange={(e) => handleQueryChange(q, e.target.value)}
                    />
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Request Body Editor */}
          {['POST', 'PUT', 'PATCH'].includes(method.toUpperCase()) && (
            <div className="form-section">
              <div className="editor-top-bar">
                <label className="section-label">Request Body (JSON):</label>
                <div className="editor-actions">
                  {defaultPayload && (
                    <button type="button" className="btn-tiny" onClick={resetToDefault}>
                      <RotateCcw size={12} /> Reset to Sample
                    </button>
                  )}
                </div>
              </div>
              <textarea
                rows={Math.min(14, Math.max(5, (jsonBody.match(/\n/g) || []).length + 2))}
                className={`code-textarea ${jsonError ? 'textarea-error' : ''}`}
                value={jsonBody}
                onChange={handleJsonChange}
                placeholder="Enter JSON payload"
                spellCheck={false}
              />
              {jsonError && (
                <div className="syntax-error-msg">
                  <AlertTriangle size={14} /> {jsonError}
                </div>
              )}
            </div>
          )}

          {/* Action Row */}
          <div className="endpoint-action-bar">
            <button
              type="button"
              className="btn-send-request"
              onClick={executeRequest}
              disabled={loading || !!jsonError}
            >
              <Play size={15} fill="currentColor" /> {loading ? 'Sending...' : 'Send Request'}
            </button>
            {extraActions}
          </div>

          {/* Live Response Panel */}
          <div className="response-container">
            <ResponseViewer response={response} loading={loading} />
          </div>
        </div>
      )}
    </div>
  )
}
