<template>
  <div class="image-upload">
    <div v-if="previewUrl || modelValue" class="image-preview">
      <img :src="displayUrl" alt="Preview" />
      <button type="button" class="btn-remove" @click="removeImage" title="Eliminar imagen">
        ✕
      </button>
    </div>
    <div v-else class="upload-placeholder">
      <input
        ref="fileInput"
        type="file"
        accept="image/jpeg,image/jpg,image/png,image/gif,image/webp"
        @change="onFileChange"
        class="file-input"
      />
      <div class="upload-content" @click="triggerFileInput">
        <span class="upload-icon">📷</span>
        <p class="upload-text">Click para subir imagen</p>
        <p class="upload-hint">JPG, PNG, GIF, WEBP (máx. 5MB)</p>
      </div>
    </div>
    <div v-if="uploading" class="upload-progress">Subiendo...</div>
    <div v-if="error" class="upload-error">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: Object, // { imageData: string (base64), imageType: string }
})

const emit = defineEmits(['update:modelValue'])

const fileInput = ref(null)
const previewUrl = ref(null)
const uploading = ref(false)
const error = ref('')

const displayUrl = computed(() => {
  if (previewUrl.value) return previewUrl.value
  if (props.modelValue?.imageData) {
    // Si ya tiene el prefijo data:image, usarlo directamente
    if (props.modelValue.imageData.startsWith('data:')) {
      return props.modelValue.imageData
    }
    // Si no, construir el data URL
    return `data:${props.modelValue.imageType || 'image/jpeg'};base64,${props.modelValue.imageData}`
  }
  return null
})

function triggerFileInput() {
  fileInput.value?.click()
}

async function onFileChange(event) {
  const file = event.target.files?.[0]
  if (!file) return

  error.value = ''

  // Validar tamaño
  if (file.size > 5 * 1024 * 1024) {
    error.value = 'El archivo excede el tamaño máximo de 5MB'
    return
  }

  // Validar tipo
  if (!file.type.startsWith('image/')) {
    error.value = 'El archivo debe ser una imagen'
    return
  }

  const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
  if (!validTypes.includes(file.type)) {
    error.value = 'Formato no válido. Use: JPG, PNG, GIF o WEBP'
    return
  }

  uploading.value = true
  try {
    // Leer archivo como base64
    const reader = new FileReader()
    
    reader.onload = (e) => {
      const base64Data = e.target.result
      previewUrl.value = base64Data
      
      // Emitir objeto con imageData y imageType
      emit('update:modelValue', {
        imageData: base64Data,
        imageType: file.type
      })
      
      uploading.value = false
    }

    reader.onerror = () => {
      error.value = 'Error al leer el archivo'
      uploading.value = false
    }

    reader.readAsDataURL(file)
  } catch (e) {
    error.value = e.message || 'Error al procesar la imagen'
    uploading.value = false
  }
}

function removeImage() {
  previewUrl.value = null
  emit('update:modelValue', null)
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}
</script>

<style scoped>
.image-upload {
  width: 100%;
}

.image-preview {
  position: relative;
  width: 100%;
  max-width: 300px;
  aspect-ratio: 4 / 3;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid #e5e7eb;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.btn-remove {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(239, 68, 68, 0.9);
  color: white;
  border: none;
  cursor: pointer;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-remove:hover {
  background: rgba(220, 38, 38, 1);
  transform: scale(1.1);
}

.upload-placeholder {
  position: relative;
  width: 100%;
  max-width: 300px;
  aspect-ratio: 4 / 3;
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-placeholder:hover {
  border-color: #3b82f6;
  background: #f9fafb;
}

.file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 20px;
  text-align: center;
}

.upload-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.upload-text {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin: 0 0 4px 0;
}

.upload-hint {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
}

.upload-progress {
  margin-top: 8px;
  font-size: 14px;
  color: #3b82f6;
  font-weight: 500;
}

.upload-error {
  margin-top: 8px;
  font-size: 14px;
  color: #ef4444;
}
</style>
