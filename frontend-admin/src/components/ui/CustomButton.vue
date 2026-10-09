<script setup lang="ts">
defineEmits<{
  click: () => void
}>()

defineProps<{
  type?: string
}>()
</script>

<template>
  <button @click="$emit('click')" :type="type || 'button'">
    <slot />
  </button>
</template>
