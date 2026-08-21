<template>
  <main class="px-5 py-10 sm:px-6">
    <section class="mx-auto max-w-5xl">
      <div class="rounded-[2rem] bg-white p-6 shadow-sm ring-1 ring-slate-200 sm:p-8">
        <p class="text-sm font-black uppercase text-emerald-700">Panel privado</p>
        <div class="mt-3 flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <h1 class="text-4xl font-black">Panel administrativo</h1>
            <p class="mt-3 max-w-2xl text-lg leading-8 text-slate-700">
              Sesión verificada. Desde aquí puedes revisar las preguntas enviadas por estudiantes.
            </p>
          </div>

          <RouterLink
            :to="{ name: 'admin-questions' }"
            class="inline-flex rounded-2xl bg-emerald-800 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-emerald-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800"
          >
            Ver preguntas
          </RouterLink>
        </div>
      </div>
    </section>
  </main>
</template>
