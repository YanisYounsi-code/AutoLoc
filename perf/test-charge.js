// Test de charge k6 : simule des utilisateurs simultanes et mesure les temps de reponse.
// Lancement avec Docker :   docker compose --profile perf run --rm k6
// Version non optimisee :   docker compose --profile perf run --rm -e CIBLE=naif k6
import http from 'k6/http';
import encoding from 'k6/encoding';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const CIBLE = __ENV.CIBLE || 'optimise';

// HTTP Basic : "Authorization: Basic base64(identifiant:mot de passe)" envoye a chaque requete
function basic(identifiant, motDePasse) {
  return { headers: { Authorization: `Basic ${encoding.b64encode(`${identifiant}:${motDePasse}`)}` } };
}
const USER = basic('user@autoloc.fr', __ENV.USER_PASSWORD || 'user123');
const ADMIN = basic('admin@autoloc.fr', __ENV.ADMIN_PASSWORD || 'admin123');

export const options = {
  // Montee progressive : 0 -> 20 -> 50 utilisateurs virtuels, puis descente
  stages: [
    { duration: '15s', target: 20 },
    { duration: '30s', target: 50 },
    { duration: '15s', target: 0 },
  ],
  // Objectifs : 95 % des requetes en moins de 500 ms, moins de 1 % d'erreurs
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

function dans(jours) {
  const d = new Date();
  d.setDate(d.getDate() + jours);
  return d.toISOString().slice(0, 10);
}

// Execute en boucle par chaque utilisateur virtuel
export default function () {
  const vehicules = http.get(`${BASE}/api/vehicules?debut=${dans(3)}&fin=${dans(6)}`, USER);
  check(vehicules, { 'vehicules 200': (r) => r.status === 200 });

  const url = CIBLE === 'naif'
    ? `${BASE}/api/admin/locations/non-optimise`
    : `${BASE}/api/admin/locations?page=0`;
  const locations = http.get(url, ADMIN);
  check(locations, { 'locations 200': (r) => r.status === 200 });

  sleep(1);
}
