<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  title: string
  author: string
  bookId: number
  categoryId?: number | null
  size?: 'small' | 'medium' | 'large'
}>(), { size: 'medium', categoryId: null })

const palettes = ['forest', 'brick', 'ochre', 'navy', 'clay', 'olive', 'charcoal']
const palette = computed(() => palettes[Math.abs((props.bookId * 7) + (props.categoryId || 0)) % palettes.length])
const shortTitle = computed(() => props.title.trim().slice(0, 14))
const catalogueNo = computed(() => `L${String(props.bookId).padStart(4, '0')}`)
</script>

<template>
  <div class="book-cover" :class="[`book-cover--${palette}`, `book-cover--${size}`]">
    <div class="book-cover__rule" />
    <span class="book-cover__series">一方书室藏书</span>
    <strong>{{ shortTitle }}</strong>
    <span class="book-cover__author">{{ author }}</span>
    <span class="book-cover__number">{{ catalogueNo }}</span>
  </div>
</template>
