<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import CustomInput from '../components/ui/CustomInput.vue'
import CustomButton from '../components/ui/CustomButton.vue'

const username = ref('')
const password = ref('')

const handleLogin = () => {
  console.log('Username:', username.value)
  console.log('Password:', password.value)
}
</script>

<template>
    <div>
        <CustomInput v-model="username" placeholder="Username" />
        <CustomInput v-model="password" type="password" placeholder="Password" />
        <CustomButton @click="handleLogin">Login</CustomButton>
    </div>
</template>
