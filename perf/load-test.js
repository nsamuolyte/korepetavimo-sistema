// Našumo testas NFR-N1: 50 naudotojų, 95 % užklausų ≤ 3 s.
// Paleidimas: k6 run perf/load-test.js

import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { parseHTML } from 'k6/html';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = __ENV.DEMO_PASSWORD || 'Demo12345';

const USERS = [
  'lukas@demo.lt', 'ieva@demo.lt', 'matas@demo.lt', 'gabija@demo.lt',
  'asta@demo.lt', 'darius@demo.lt',
];
const SUBJECTS = ['', 'matematika', 'anglų', 'fizika'];

export const options = {
  scenarios: {
    nfr_n1: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },
        { duration: '2m', target: 50 },
        { duration: '15s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
};

let loggedIn = false;

function login() {
  const email = USERS[(__VU - 1) % USERS.length];
  const page = http.get(`${BASE_URL}/login`, { tags: { name: 'GET /login' } });
  const csrf = parseHTML(page.body).find('input[name="_csrf"]').attr('value');
  if (!csrf) {
    fail('Nerastas _csrf laukas prisijungimo formoje');
  }
  const res = http.post(`${BASE_URL}/login`,
    { username: email, password: PASSWORD, _csrf: csrf },
    { tags: { name: 'POST /login' } });
  const ok = check(res, {
    'prisijungta': (r) => r.status === 200 && r.url.includes('/dashboard'),
  });
  if (!ok) {
    fail(`Nepavyko prisijungti: ${email}`);
  }
  loggedIn = true;
}

export default function () {
  if (!loggedIn) {
    login();
  }

  const subject = SUBJECTS[Math.floor(Math.random() * SUBJECTS.length)];
  const search = http.get(`${BASE_URL}/tutors?subject=${encodeURIComponent(subject)}`,
    { tags: { name: 'GET /tutors' } });
  check(search, { 'paieška 200': (r) => r.status === 200 });

  const links = parseHTML(search.body).find('a[href^="/tutors/"]').toArray()
    .map((a) => a.attr('href'));
  if (links.length > 0) {
    const href = links[Math.floor(Math.random() * links.length)];
    const profile = http.get(`${BASE_URL}${href}`, { tags: { name: 'GET /tutors/{id}' } });
    check(profile, { 'profilis 200': (r) => r.status === 200 });
  }
  sleep(1);

  const lessons = http.get(`${BASE_URL}/lessons`, { tags: { name: 'GET /lessons' } });
  check(lessons, { 'tvarkaraštis 200': (r) => r.status === 200 });

  const notifications = http.get(`${BASE_URL}/notifications`, { tags: { name: 'GET /notifications' } });
  check(notifications, { 'pranešimai 200': (r) => r.status === 200 });

  sleep(1 + Math.random() * 2);
}
