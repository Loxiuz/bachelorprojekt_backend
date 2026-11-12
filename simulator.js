// server.mjs
import { createServer } from 'node:http';

const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

const deviceTypes = {
    entryPhone: {
        createState: () => ({ battery: 100, signalStrength: 90, status: "idle" }),
        update: (state) => {
            const battery = Math.max(0, state.battery - 1);
            const signalStrength = Math.min(100, Math.max(0, state.signalStrength + (Math.random() * 10 - 5)));
            const roll = Math.random();
            const isInCall = roll < 0.15 ? "in_call" : "idle"
            const status = roll < 0.1 ? "ringing" : isInCall;
            return { battery, signalStrength: Math.round(signalStrength), status };
        },
    },
    digitalDoor: {
        createState: () => ({ battery: 100, locked: true, doorOpen: false, tamperAlert: false }),
        update: (state) => {
            const battery = Math.max(0, state.battery - 1);
            const doorOpen = Math.random() < 0.1 ? !state.doorOpen : state.doorOpen;
            const randomIsLocked = Math.random() < 0.1 ? !state.locked : state.locked;
            const locked = doorOpen ? false : randomIsLocked;
            const tamperAlert = Math.random() < 0.02;
            return { battery, locked, doorOpen, tamperAlert };
        },
    },
};

const deviceConfigs = [
    { id: "entry-phone-1", tenantId: "tenant-a", type: "entryPhone", intervalMs: 5000 },
    { id: "digital-door-1", tenantId: "tenant-a", type: "digitalDoor", intervalMs: 5000 },
    { id: "digital-door-2", tenantId: "tenant-b", type: "digitalDoor", intervalMs: 5000 },
];

function createDevice(config) {
    const definition = deviceTypes[config.type];
    if (!definition) {
        throw new Error(`Ukendt device type: ${config.type}`);
    }
    return { ...config, state: definition.createState() };
}

async function tick(device) {
    device.state = deviceTypes[device.type].update(device.state);

    try {
        const response = await fetch(`${backendUrl}/status`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                deviceId: device.id,
                tenantId: device.tenantId,
                type: device.type,
                ...device.state,
            }),
        });
        if (!response.ok) {
            console.error(`Backend svarede med HTTP ${response.status} for ${device.id}`);
        }
    } catch (error) {
        console.error(`Kunne ikke sende status for ${device.id}:`, error.message);
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

server.listen(3000, '127.0.0.1', () => {
    console.log('Listening on 127.0.0.1:3000');
});


