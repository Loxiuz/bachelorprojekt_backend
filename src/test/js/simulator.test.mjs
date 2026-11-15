import { strict as assert } from "node:assert";
import { spawn } from "node:child_process";
import { createServer } from "node:http";
import { once } from "node:events";
import { test } from "node:test";

test("sends snapshots with independent device credentials", { timeout: 12000 }, async (t) => {
    const tokenA = "A".repeat(43);
    const tokenB = "B".repeat(43);
    const requests = [];
    let finish;
    const received = new Promise((resolve) => { finish = resolve; });
    const backend = createServer(async (req, res) => {
        let body = "";
        for await (const chunk of req) body += chunk;
        requests.push({ path: req.url, headers: req.headers, body: JSON.parse(body) });
        res.writeHead(204);
        res.end();
        if (requests.length === 2) finish();
    });
    backend.listen(0, "127.0.0.1");
    await once(backend, "listening");
    t.after(() => backend.close());
    const simulator = spawn(process.execPath, ["simulator.js"], {
        env: {
            ...process.env,
            BACKEND_URL: `http://127.0.0.1:${backend.address().port}`,
            SIMULATOR_PORT: "0",
            SIMULATOR_TOKEN_A: tokenA,
            SIMULATOR_TOKEN_B: tokenB,
        },
        stdio: "ignore",
    });
    t.after(() => simulator.kill());
    await received;

    assert.deepEqual(new Set(requests.map((request) => request.headers.authorization)),
        new Set([`Bearer ${tokenA}`, `Bearer ${tokenB}`]));
    for (const request of requests) {
        assert.equal(request.path, "/status");
        assert.equal(request.headers.cookie, undefined);
        assert.equal(request.headers["x-csrf-token"], undefined);
        assert.deepEqual(Object.keys(request.body).sort(),
            ["batteryLevel", "doorOpen", "isOnline", "locked", "readAt"].sort());
        assert.equal(typeof request.body.isOnline, "boolean");
        assert.ok(!Number.isNaN(Date.parse(request.body.readAt)));
    }
});
