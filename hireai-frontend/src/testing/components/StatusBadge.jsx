import React from 'react'

export function StatusBadge({ status, type = 'general' }) {
  if (!status) return null

  let styleClass = 'badge-gray'
  const s = String(status).toUpperCase()

  if (s === 'OPEN' || s === 'ACTIVE' || s === 'SHORTLISTED' || s === 'OFFERED' || s === 'SUCCESS') {
    styleClass = 'badge-green'
  } else if (s === 'SCREENING' || s === 'INTERVIEW_SCHEDULED' || s === 'APPLIED') {
    styleClass = 'badge-blue'
  } else if (s === 'INACTIVE' || s === 'CLOSED' || s === 'WITHDRAWN') {
    styleClass = 'badge-amber'
  } else if (s === 'REJECTED' || s === 'FAILED' || s === 'ERROR') {
    styleClass = 'badge-red'
  } else if (s === 'ROLE_RECRUITER') {
    styleClass = 'badge-purple'
  } else if (s === 'ROLE_CANDIDATE') {
    styleClass = 'badge-indigo'
  } else if (s === 'ROLE_ADMIN') {
    styleClass = 'badge-pink'
  }

  return (
    <span className={`status-badge ${styleClass}`}>
      {status}
    </span>
  )
}

export function AtsScoreBadge({ score }) {
  if (score === null || score === undefined) return <span className="score-badge score-none">N/A</span>

  let scoreClass = 'score-low'
  if (score >= 80) scoreClass = 'score-high'
  else if (score >= 60) scoreClass = 'score-mid'

  return (
    <span className={`score-badge ${scoreClass}`}>
      🎯 {score}% Match
    </span>
  )
}
