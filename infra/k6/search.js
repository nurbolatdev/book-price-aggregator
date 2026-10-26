// k6 load test for the public read API: search and edition card.
//
// Usage (from the repo root, stack running):
//   docker run --rm -i --add-host=host.docker.internal:host-gateway \
//     -e BASE_URL=http://host.docker.internal:8080 [-e SCENARIO=cold] \
//     grafana/k6 run - < infra/k6/search.js
//
// SCENARIO=warm (default): a short fixed list of queries, repeated, so the cache is hot.
// SCENARIO=cold: two-word combinations of a long list, so every request is a query not seen before.
// Rate limits must be off (RATE_LIMIT_ENABLED=false), otherwise 20 VUs from one IP get 429.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SCENARIO = __ENV.SCENARIO || 'warm';

const WARM_QUERIES = [
  'война', 'мир', 'толстой', 'гарри поттер', 'преступление', 'анна каренина',
  'linux', 'кочевники', 'скотный двор', 'достоевский',
];

const COLD_QUERIES = [
  'отцы и дети', 'тургенев', 'цветы', 'алджернон', 'элджернон', 'философский камень',
  'каренина', 'наказание', 'оруэлл', 'ферма', 'кочевник', 'поттер', 'роулинг',
  'bible', 'polyglot', 'hello world', 'программирование', 'язык c', 'java', 'python',
  'уравнения', 'физика', 'математика', 'учебник', 'практикум', 'словарь',
  'резонансные', 'усилители', 'телевизионные', 'микросхемы', 'проектирование',
  'сөздер', 'қара', 'абай', 'ауэзов', 'путь', 'человек', 'история', 'украина',
  'европа', 'происхождение', 'дарвин', 'poems', 'notes', 'catalog', 'digest',
  'bills', 'monthly', 'weekly', 'court', 'маленький принц', 'мастер', 'маргарита',
  'булгаков', 'чехов', 'пушкин', 'лермонтов', 'гоголь', 'шекспир', 'сказки',
  'детектив', 'роман', 'повесть', 'стихи', 'энциклопедия', 'химия', 'биология',
];

const queries = SCENARIO === 'cold' ? COLD_QUERIES : WARM_QUERIES;

export const options = {
  vus: 20,
  duration: '1m',
  thresholds: {
    // docs/01-architecture.md section 7: search warm p95 < 200 ms (cold < 500 ms), card p95 < 300 ms
    'http_req_duration{name:search}': [SCENARIO === 'cold' ? 'p(95)<500' : 'p(95)<200'],
    'http_req_duration{name:edition}': ['p(95)<300'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  const ids = [];
  for (const q of WARM_QUERIES) {
    const res = http.get(`${BASE_URL}/api/v1/search?q=${encodeURIComponent(q)}`);
    if (res.status === 200) {
      for (const item of res.json('content') || []) {
        ids.push(item.editionId);
      }
    }
  }
  if (ids.length === 0) {
    throw new Error('setup found no editions; is the demo catalog loaded?');
  }
  return { ids };
}

// Two-word combinations of the cold list: every request of a run is a query not seen before.
function coldQuery() {
  const n = COLD_QUERIES.length;
  const idx = (__ITER * 20 + (__VU - 1)) % (n * n);
  return `${COLD_QUERIES[idx % n]} ${COLD_QUERIES[Math.floor(idx / n) % n]}`;
}

export default function (data) {
  if (Math.random() < 0.7) {
    const q = SCENARIO === 'cold' ? coldQuery() : queries[Math.floor(Math.random() * queries.length)];
    const res = http.get(`${BASE_URL}/api/v1/search?q=${encodeURIComponent(q)}`, {
      tags: { name: 'search' },
    });
    check(res, { 'search 200': (r) => r.status === 200 });
  } else {
    const id = data.ids[Math.floor(Math.random() * data.ids.length)];
    const res = http.get(`${BASE_URL}/api/v1/editions/${id}`, { tags: { name: 'edition' } });
    check(res, { 'edition 200': (r) => r.status === 200 });
  }
  sleep(0.5 + Math.random() * 0.5);
}
