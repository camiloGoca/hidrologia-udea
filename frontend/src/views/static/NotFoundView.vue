<template>
  <section class="mx-auto max-w-4xl px-5 py-12 sm:px-6 sm:py-16">
    <div class="rounded-lg border border-slate-200 bg-white p-7 shadow-sm sm:p-10">
      <p class="text-sm font-black uppercase text-emerald-800">404</p>
      <h1 class="mt-3 text-4xl font-black text-slate-950">Página no encontrada</h1>
      <p class="mt-4 leading-7 text-slate-700">
        La ruta solicitada no existe dentro de la navegación pública actual.
      </p>
      <RouterLink
        :to="{ name: 'home' }"
        class="mt-8 inline-flex rounded-md bg-sky-900 px-6 py-3 font-black text-white hover:bg-sky-950 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700"
      >
        Volver al inicio
      </RouterLink>
    </div>
  </section>
</template>
