<script setup lang="ts">
defineProps<{ eyebrow: string; title: string; description: string }>()
</script>

<template>
  <main class="auth-shell">
    <section class="auth-identity" aria-label="一方书室介绍">
      <div class="brand-lockup">
        <span class="brand-mark" aria-hidden="true"><i /><i /><i /></span>
        <span>一方书室</span>
      </div>
      <div class="shelf-composition" aria-hidden="true">
        <div class="shelf shelf-one"><span>文</span><span>学</span></div>
        <div class="shelf shelf-two"><span>技</span><span>术</span></div>
        <div class="shelf shelf-three"><span>历</span><span>史</span></div>
        <div class="shelf-line" />
      </div>
      <div class="identity-copy">
        <p>CATALOGUE · BORROW · RETURN</p>
        <h1>让每一次借阅<br />都有清楚的去处。</h1>
        <span>书目、库存与借阅记录，在一处安静地运转。</span>
      </div>
    </section>
    <section class="auth-form-panel">
      <div class="auth-form-wrap">
        <p class="eyebrow">{{ eyebrow }}</p>
        <h2>{{ title }}</h2>
        <p class="form-description">{{ description }}</p>
        <slot />
      </div>
      <p class="auth-footnote">Library System · Spring Boot × Vue</p>
    </section>
  </main>
</template>
