// server.mjs
import { createServer } from 'node:http';

const backendUrl = "http://localhost:8080";

const deviceTypes = {
    digitalDoor: {
        createState: () => ({ batteryLevel: 100, locked: true, doorOpen: false, isOnline: true, readAt: new Date().toISOString() }),
        update: (state) => {
            const batteryLevel = Math.max(0, state.batteryLevel - 1);
            const doorOpen = Math.random() < 0.1 ? !state.doorOpen : state.doorOpen;
            const randomIsLocked = Math.random() < 0.1 ? !state.locked : state.locked;
            const locked = doorOpen ? false : randomIsLocked;
            const isOnline = Math.random() < 0.05 ? !state.isOnline : state.isOnline;
            return { batteryLevel, locked, doorOpen, isOnline, readAt: new Date().toISOString() };
        },
    },
    // entryPhone: {
    //     createState: () => ({ batteryLevel: 100, signalStrength: 90, status: "idle" }),
    //     update: (state) => {
    //         const batteryLevel = Math.max(0, state.batteryLevel - 1);
    //         const signalStrength = Math.min(100, Math.max(0, state.signalStrength + (Math.random() * 10 - 5)));
    //         const roll = Math.random();
    //         const isInCall = roll < 0.15 ? "in_call" : "idle"
    //         const status = roll < 0.1 ? "ringing" : isInCall;
    //         return { batteryLevel, signalStrength: Math.round(signalStrength), status };
    //     },
    // },
};

const deviceConfigs = [
    { id: "d06c72ee-56f1-4b6a-9a7a-18b58e36a111", type: "digitalDoor", intervalMs: 5000, token: process.env.SIMULATOR_TOKEN_A },
    { id: "d06c72ee-56f1-4b6a-9a7a-18b58e36a222", type: "digitalDoor", intervalMs: 5000, token: process.env.SIMULATOR_TOKEN_B },
].filter((config) => {
    if (config.token && !/^[A-Za-z0-9_-]{43}$/.test(config.token)) {
        throw new Error(`Invalid credential format for device ${config.id}`);
    }
    return Boolean(config.token);
});

if (deviceConfigs.length === 0) {
    throw new Error("Set SIMULATOR_TOKEN_A or SIMULATOR_TOKEN_B");
}

function createDevice(config) {
    const definition = deviceTypes[config.type];
    if (!definition) {
        throw new Error(`Ukendt device type: ${config.type}`);
    }
    return { ...config, state: definition.createState(), sending: false };
}

async function tick(device) {
    if (device.sending) return;
    device.sending = true;
    device.state = deviceTypes[device.type].update(device.state);

    try {
        const response = await fetch(`${backendUrl}/status`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${device.token}`,
            },
            body: JSON.stringify(device.state),
        });
        if (!response.ok) {
            console.error(`Backend svarede med HTTP ${response.status} for ${device.id}`);
        }
    } catch (error) {
        console.error(`Kunne ikke sende status for ${device.id}:`, error.message);
    } finally {
        device.sending = false;
    }
}

const devices = deviceConfigs.map(createDevice);
for (const device of devices) {
    setInterval(() => tick(device), device.intervalMs);
}

const server = createServer((req, res) => {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('Simulator server kører\n');
});

const simulatorPort = 3000;
server.listen(simulatorPort, '127.0.0.1', () => {
    console.log(`Listening on 127.0.0.1:${server.address().port}`);
});
