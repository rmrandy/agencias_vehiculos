<template>
  <div class="comment-item" :class="{ 'is-reply': !!comment.parentId }">
    <div class="comment-header">
      <span class="comment-author">{{ comment.userDisplayName || 'Usuario' }}</span>
      <span v-if="comment.rating != null" class="comment-rating" :title="`${comment.rating} estrellas`">
        <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= comment.rating }">★</span>
      </span>
      <span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
    </div>
    <p class="comment-body">{{ comment.body }}</p>
    <div v-if="isLoggedIn" class="comment-actions">
      <button type="button" class="btn-reply" @click="toggleReply">
        {{ showReplyForm ? 'Cancelar' : 'Responder' }}
      </button>
    </div>
    <div v-if="showReplyForm" class="reply-form">
      <textarea
        v-model="replyBody"
        placeholder="Escribe tu respuesta..."
        rows="2"
        class="reply-textarea"
      />
      <div class="reply-actions">
        <button type="button" class="btn btn-secondary btn-sm" @click="showReplyForm = false; replyBody = ''">Cancelar</button>
        <button type="button" class="btn btn-primary btn-sm" :disabled="!replyBody.trim()" @click="submitReply">
          Enviar
        </button>
      </div>
    </div>
    <div v-if="comment.children && comment.children.length" class="comment-children">
      <CommentItem
        v-for="child in comment.children"
        :key="child.reviewId"
        :comment="child"
        :part-id="partId"
        :current-user-id="currentUserId"
        :is-logged-in="isLoggedIn"
        @reply="emit('reply')"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useToast } from '../composables/useToast'
import { createComentario } from '../api/comentarios'

const props = defineProps({
  comment: { type: Object, required: true },
  partId: { type: [String, Number], required: true },
  currentUserId: { type: Number, default: null },
  isLoggedIn: { type: Boolean, default: false },
})

const emit = defineEmits(['reply'])

const { success, error: showError } = useToast()
const showReplyForm = ref(false)
const replyBody = ref('')

function formatDate(d) {
  if (!d) return ''
  const date = new Date(d)
  return date.toLocaleDateString('es', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function toggleReply() {
  if (!props.isLoggedIn) return
  showReplyForm.value = !showReplyForm.value
  if (!showReplyForm.value) replyBody.value = ''
}

async function submitReply() {
  const body = replyBody.value?.trim()
  if (!body || !props.isLoggedIn || !props.currentUserId) return
  try {
    await createComentario(Number(props.partId), {
      userId: props.currentUserId,
      body,
      parentId: props.comment.reviewId,
    })
    success('Respuesta publicada')
    replyBody.value = ''
    showReplyForm.value = false
    emit('reply')
  } catch (e) {
    showError(e.message || 'Error al enviar la respuesta')
  }
}
</script>

<style scoped>
.comment-item {
  padding: 12px 0;
  border-bottom: 1px solid #e5e7eb;
}
.comment-item.is-reply {
  margin-left: 24px;
  padding-left: 16px;
  border-left: 3px solid #e5e7eb;
}
.comment-item:last-child {
  border-bottom: none;
}
.comment-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 6px;
}
.comment-author {
  font-weight: 600;
  color: #1f2937;
  font-size: 14px;
}
.comment-rating {
  display: inline-flex;
  gap: 2px;
}
.star {
  color: #d1d5db;
  font-size: 14px;
}
.star.filled {
  color: #f59e0b;
}
.comment-date {
  font-size: 12px;
  color: #6b7280;
}
.comment-body {
  margin: 0 0 8px;
  font-size: 15px;
  line-height: 1.5;
  color: #374151;
}
.comment-actions {
  margin-top: 8px;
}
.btn-reply {
  background: none;
  border: none;
  color: #3b82f6;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}
.btn-reply:hover {
  text-decoration: underline;
}
.reply-form {
  margin-top: 12px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}
.reply-textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 14px;
  resize: vertical;
  margin-bottom: 8px;
}
.reply-actions {
  display: flex;
  gap: 8px;
}
.btn-sm {
  padding: 8px 14px;
  font-size: 13px;
}
.comment-children {
  margin-top: 12px;
}
</style>
