export const chatApi = {
  // Stream chat response
  chatStream(message: string, context?: string) {
    return fetch('/api/v1/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ message, context }),
    })
  }
}
