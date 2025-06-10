import http from 'k6/http';
import { sleep } from 'k6';

export let options = {
    vus: 10,
    duration: '5m',
};

export default function () {
    const userNum = __VU; // VU number (1-10)
    //const reqNum = (__ITER % 20) + 1; // Request number for this VU (1-20)
    const reqNum = __ITER;
    const id = `user-${userNum}-${reqNum}`;
    const duration = Math.floor(Math.random() * 1_001) + 2_000; // 2000-3000ms
    const error = (reqNum === 10);

    const payload = JSON.stringify({
        id,
        value: 'value1',
        duration,
        error,
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    http.post('http://localhost:8888/api/produce', payload, params);

    sleep(3); // 20 requests per minute per user
}