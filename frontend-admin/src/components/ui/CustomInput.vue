<script setup lang="ts">
defineProps<{
  modelValue: string
  type?: string
}>()
</script>

<template>
  <input 
    :type="type || 'text'"
    :value="modelValue"
    @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
  />
</template>
