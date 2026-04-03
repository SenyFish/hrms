<template>
  <section class="page-section">
    <header v-if="title || $slots.actions || description" class="page-section__header">
      <div>
        <p v-if="eyebrow" class="page-section__eyebrow">{{ eyebrow }}</p>
        <h2 v-if="title" class="page-section__title">{{ title }}</h2>
        <p v-if="description" class="page-section__description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="page-section__actions">
        <slot name="actions" />
      </div>
    </header>
    <div class="page-section__body" :class="{ 'page-section__body--flush': flush }">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow?: string;
  title?: string;
  description?: string;
  flush?: boolean;
}>();
</script>
