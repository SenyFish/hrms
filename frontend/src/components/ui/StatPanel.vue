<template>
  <article class="stat-panel">
    <div class="stat-panel__meta">
      <p class="stat-panel__label">{{ label }}</p>
      <span v-if="hint" class="stat-panel__hint">{{ hint }}</span>
    </div>
    <strong class="stat-panel__value">{{ value }}</strong>
    <div v-if="$slots.default" class="stat-panel__footer">
      <slot />
    </div>
  </article>
</template>

<script setup lang="ts">
defineProps<{
  label: string;
  value: string | number;
  hint?: string;
}>();
</script>
