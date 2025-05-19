import http from 'k6/http';
import { sleep } from 'k6';

export let options = {
    vus: 10,
    duration: '1m',
};

export default function () {
    const userNum = __VU; // VU number (1-10)
    const reqNum = (__ITER % 20) + 1; // Request number for this VU (1-20)
    const id = `user-${userNum}-${reqNum}`;
    const duration = Math.floor(Math.random() * 1001) + 1000; // 1000-2000
    const error = (reqNum % 10 === 0);

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

    http.post('http://localhost:8080/api/produce', payload, params);

    sleep(3); // 20 requests per minute per user
}