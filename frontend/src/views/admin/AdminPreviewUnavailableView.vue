<template>
  <main class="min-h-screen bg-slate-950 px-5 py-10 text-white sm:px-6">
    <section class="mx-auto flex min-h-[calc(100vh-5rem)] max-w-3xl items-center">
      <div class="rounded-[2rem] bg-white/96 p-6 text-slate-950 shadow-2xl ring-1 ring-white/20 sm:p-8">
        <RouterLink
          :to="{ name: 'home' }"
          class="inline-flex rounded-full bg-emerald-50 px-4 py-2 text-sm font-black uppercase tracking-[0.16em] text-emerald-800 ring-1 ring-emerald-100 transition hover:bg-emerald-100 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800"
        >
          Hidrología UdeA
        </RouterLink>
        <p class="mt-6 text-sm font-black uppercase text-emerald-700">Vista previa</p>
        <h1 class="mt-3 text-3xl font-black">Panel administrativo no disponible</h1>
        <p class="mt-4 leading-7 text-slate-700">
          Los previews de Pull Request son de solo lectura. Puedes revisar la experiencia pública,
          pero el acceso administrativo permanece deshabilitado para proteger producción.
        </p>
        <RouterLink
          :to="{ name: 'home' }"
          class="mt-7 inline-flex rounded-full bg-sky-950 px-5 py-3 text-sm font-black text-white transition hover:bg-emerald-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-800"
        >
          Volver al inicio
        </RouterLink>
      </div>
    </section>
  </main>
</template>
