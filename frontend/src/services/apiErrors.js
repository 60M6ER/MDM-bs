export function extractApiError(err, fallbackMessage = 'Произошла ошибка') {
  const data = err?.response?.data || {}
  const code = data.code || data.message || data.title || null
  const detail = data.detail || null

  return {
    status: err?.response?.status ?? null,
    code,
    detail,
    fallbackMessage,
    text: detail || code || err?.message || fallbackMessage
  }
}

export function notifyApiError($q, err, fallbackMessage = 'Произошла ошибка') {
  const apiError = extractApiError(err, fallbackMessage)

  $q.notify({
    type: 'negative',
    multiLine: true,
    message: apiError.code || fallbackMessage,
    caption: apiError.detail && apiError.detail !== apiError.code ? apiError.detail : undefined
  })

  return apiError
}
