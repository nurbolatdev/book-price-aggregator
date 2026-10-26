// k6 load test for the public read API: search and edition card.
//
// Usage (from the repo root, stack running):
//   docker run --rm -i --add-host=host.docker.internal:host-gateway \
//     -e BASE_URL=http://host.docker.internal:8080 [-e SCENARIO=cold] \
//     grafana/k6 run - < infra/k6/search.js
//
// SCENARIO=warm (default): a short fixed list of queries, repeated, so the cache is hot.
// SCENARIO=cold: queries that return hits, made unique per request by sort and page size, so every
//   search is a cache key not seen before; the share of non-empty responses is reported as
//   search_nonempty. Each edition id is requested cold once (tag edition_first), later requests
//   for it are tagged edition.
// Rate limits must be off (RATE_LIMIT_ENABLED=false), otherwise 20 VUs from one IP get 429.
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SCENARIO = __ENV.SCENARIO || 'warm';

const WARM_QUERIES = [
  'война', 'мир', 'толстой', 'гарри поттер', 'преступление', 'анна каренина',
  'linux', 'кочевники', 'скотный двор', 'достоевский',
];

// Single words and short phrases that return hits in the demo catalog (titles, authors, topics).
const COLD_QUERIES = [
  'война', 'мир', 'толстой', 'гарри', 'преступление', 'каренина', 'кочевники', 'скотный',
  'достоевский', 'оруэлл', 'булгаков', 'мастер', 'маргарита', 'пушкин', 'история',
  'человек', 'принц', 'поттер', 'роулинг', 'тургенев', 'философский',
];
const COLD_SORTS = ['RELEVANCE', 'PRICE_ASC', 'DISCOUNT_DESC', 'STORES_DESC'];

const searchNonEmpty = new Rate('search_nonempty');

const queries = SCENARIO === 'cold' ? COLD_QUERIES : WARM_QUERIES;

export const options = {
  vus: 20,
  duration: '1m',
  thresholds: {
    // docs/01-architecture.md section 7: search warm p95 < 200 ms (cold < 500 ms), card p95 < 300 ms
    'http_req_duration{name:search}': [SCENARIO === 'cold' ? 'p(95)<500' : 'p(95)<200'],
    [SCENARIO === 'cold' ? 'http_req_duration{name:edition_first}' : 'http_req_duration{name:edition}']: ['p(95)<300'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  const ids = [];
  for (const q of WARM_QUERIES) {
    const res = http.get(`${BASE_URL}/api/v1/search?q=${encodeURIComponent(q)}`);
    if (res.status === 200) {
      for (const item of res.json('content') || []) {
        if (!ids.includes(item.editionId)) ids.push(item.editionId);
      }
    }
  }
  if (ids.length === 0) {
    throw new Error('setup found no editions; is the demo catalog loaded?');
  }
  return { ids };
}

// Unique (query, sort, size) per request of a run: 21 queries x 4 sorts x 46 sizes = 3864 keys; 21 is coprime with the 20 VU stride.
function coldSearchUrl() {
  const n = COLD_QUERIES.length;
  const idx = __ITER * 20 + (__VU - 1);
  const rest = Math.floor(idx / n);
  const sort = COLD_SORTS[rest % COLD_SORTS.length];
  const size = 5 + (Math.floor(rest / COLD_SORTS.length) % 46);
  return `${BASE_URL}/api/v1/search?q=${encodeURIComponent(COLD_QUERIES[idx % n])}&sort=${sort}&size=${size}`;
}

let cardRequests = 0;

export default function (data) {
  if (Math.random() < 0.7) {
    const url = SCENARIO === 'cold'
      ? coldSearchUrl()
      : `${BASE_URL}/api/v1/search?q=${encodeURIComponent(queries[Math.floor(Math.random() * queries.length)])}`;
    const res = http.get(url, { tags: { name: 'search' } });
    check(res, { 'search 200': (r) => r.status === 200 });
    searchNonEmpty.add(res.status === 200 && (res.json('content') || []).length > 0);
  } else {
    let id;
    let name = 'edition';
    const first = cardRequests * 20 + (__VU - 1);
    if (SCENARIO === 'cold' && first < data.ids.length) {
      id = data.ids[first];
      name = 'edition_first';
    } else {
      id = data.ids[Math.floor(Math.random() * data.ids.length)];
    }
    cardRequests += 1;
    const res = http.get(`${BASE_URL}/api/v1/editions/${id}`, { tags: { name } });
    check(res, { 'edition 200': (r) => r.status === 200 });
  }
  sleep(0.5 + Math.random() * 0.5);
}
