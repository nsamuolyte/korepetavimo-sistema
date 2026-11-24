# Korepetavimo paslaugų sistema

ISPK kompleksinis projektas – komanda „404 Team Name Not Found“ (Prif-23/4).

## Prisijungimai

Slaptažodis visoms paskyroms: **`Demo12345`**

| Rolė | El. paštas | Vardas |
|---|---|---|
| Administratorius | `admin@demo.lt` | Sistemos Administratorius |
| Korepetitorius | `jonas@demo.lt` | Jonas Petraitis (matematika) |
| Korepetitorius | `ruta@demo.lt` | Rūta Jankauskienė (anglų kalba) |
| Korepetitorius | `tomas@demo.lt` | Tomas Žukauskas (fizika) |
| Korepetitorius | `egle@demo.lt` | Eglė Vaitkutė (chemija, laukia patvirtinimo) |
| Mokinys | `lukas@demo.lt` | Lukas Kazlauskas |
| Mokinys | `ieva@demo.lt` | Ieva Stankevičiūtė |
| Mokinys | `matas@demo.lt` | Matas Paulauskas |
| Mokinys | `gabija@demo.lt` | Gabija Urbonaitė |
| Globėjas | `asta@demo.lt` | Asta Kazlauskienė (Luko globėja) |
| Globėjas | `darius@demo.lt` | Darius Stankevičius (Ievos globėjas) |
| Globėjas | `rasa@demo.lt` | Rasa Paulauskienė (laukia patvirtinimo su Matu) |

## Funkcionalumas ir puslapiai

| Funkcija | Puslapis | Kas gali |
|---|---|---|
| Korepetitorių paieška (dalykas, kaina, laisva diena) | `/tutors` | visi |
| Korepetitoriaus profilis, paketai, rezervacija, žinutė | `/tutors/{id}` | visi (veiksmai – mokinys, globėjas) |
| Pamokos rezervacija | `/lessons/book/{id}` | mokinys, globėjas |
| Tvarkaraštis, perkėlimas, atšaukimas | `/lessons` | visi prisijungę |
| Darbo laikas | `/tutor/availability` | korepetitorius |
| Pamokos sukūrimas mokiniui | `/tutor/lessons/new` | korepetitorius |
| Uždarbis ir išmokos | `/tutor/earnings` | korepetitorius |
| Pamokų paketai | `/tutor/packages` | korepetitorius |
| Paketų pirkimas ir mokėjimų istorija | `/payments` | mokinys, globėjas |
| Žinutės | `/messages` | visi prisijungę |
| Pranešimai | `/notifications` | visi prisijungę |
| Globos susiejimas ir atšaukimas | `/guardian/students`, `/student/guardians` | globėjas, mokinys |
| Korepetitorių tvirtinimas | `/admin/tutors` | administratorius |
| Naudotojų valdymas ir blokavimas | `/admin/users` | administratorius |
