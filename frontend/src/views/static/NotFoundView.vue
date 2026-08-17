<template>
  <section class="mx-auto max-w-4xl px-5 py-12 sm:px-6 sm:py-16">
    <p class="text-sm font-semibold uppercase text-emerald-800">404</p>
    <h1 class="mt-3 text-4xl font-bold text-slate-950">Página no encontrada</h1>
    <p class="mt-4 leading-7 text-slate-700">
      La ruta solicitada no existe dentro de la navegación pública actual.
    </p>
    <RouterLink
      :to="{ name: 'home' }"
      class="mt-8 inline-flex rounded-md bg-sky-800 px-5 py-3 font-semibold text-white hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700"
    >
      Volver al inicio
    </RouterLink>
  </section>
</template>
