/*
 * Runs the app's SQLite database for androidx.sqlite's WebWorkerSQLiteDriver, following the
 * message protocol of androidx's reference worker (sqlite/sqlite-web-worker-test, Apache 2.0):
 * open, prepare, step and close, answered with the request id.
 *
 * Databases live in the origin private file system through the "opfs-sahpool" VFS, which needs
 * no cross-origin isolation headers (GitHub Pages cannot send them). Only one tab can hold that
 * pool; further tabs, and browsers without OPFS, get an in-memory database that the app fills
 * from the server again.
 */
import sqlite3InitModule from '@sqlite.org/sqlite-wasm';

let sqlite3 = null;
let pool = null;
let ready = false;

const databases = new Map();
const statements = new Map();
let nextDatabaseId = 0;
let nextStatementId = 0;

function reply(id, data) {
    postMessage({ id, data });
}

function fail(id, error) {
    postMessage({ id, error: error instanceof Error ? error.message : String(error) });
}

function open(id, request) {
    if (!sqlite3) {
        fail(id, 'SQLite could not be loaded');
        return;
    }
    try {
        const database = pool
            ? new pool.OpfsSAHPoolDb(request.fileName)
            : new sqlite3.oo1.DB(':memory:', 'ct');
        const databaseId = nextDatabaseId++;
        databases.set(databaseId, database);
        reply(id, { databaseId });
    } catch (error) {
        fail(id, error);
    }
}

function prepare(id, request) {
    const database = databases.get(request.databaseId);
    if (!database) {
        fail(id, 'Invalid database ID: ' + request.databaseId);
        return;
    }
    try {
        const statement = database.prepare(request.sql);
        const statementId = nextStatementId++;
        statements.set(statementId, statement);
        const columnNames = [];
        for (let i = 0; i < statement.columnCount; i++) {
            columnNames.push(sqlite3.capi.sqlite3_column_name(statement, i));
        }
        reply(id, {
            statementId,
            parameterCount: sqlite3.capi.sqlite3_bind_parameter_count(statement),
            columnNames,
        });
    } catch (error) {
        fail(id, error);
    }
}

function step(id, request) {
    const statement = statements.get(request.statementId);
    if (!statement) {
        fail(id, 'Invalid statement ID: ' + request.statementId);
        return;
    }
    try {
        const rows = [];
        const columnTypes = [];
        statement.reset();
        statement.clearBindings();
        request.bindings.forEach((value, index) => statement.bind(index + 1, value));
        while (statement.step()) {
            if (columnTypes.length === 0) {
                for (let i = 0; i < statement.columnCount; i++) {
                    columnTypes.push(sqlite3.capi.sqlite3_column_type(statement, i));
                }
            }
            rows.push(statement.get([]));
        }
        reply(id, { rows, columnTypes });
    } catch (error) {
        fail(id, error);
    }
}

// One-way command, the driver expects no answer on success
function close(id, request) {
    try {
        if (request.statementId !== undefined && request.statementId !== null) {
            statements.get(request.statementId)?.finalize();
            statements.delete(request.statementId);
        }
        if (request.databaseId !== undefined && request.databaseId !== null) {
            databases.get(request.databaseId)?.close();
            databases.delete(request.databaseId);
        }
    } catch (error) {
        fail(id, error);
    }
}

const commands = { open, prepare, step, close };

function handle(message) {
    const request = message.data;
    const command = request?.data?.cmd;
    const handler = commands[command];
    if (handler) {
        handler(request.id, request.data);
    } else {
        fail(request?.id, "Invalid request, unknown command: '" + command + "'.");
    }
}

const queue = [];
onmessage = (message) => (ready ? handle(message) : queue.push(message));

sqlite3InitModule()
    .then(async (instance) => {
        sqlite3 = instance;
        try {
            pool = await sqlite3.installOpfsSAHPoolVfs({ name: 'grocy' });
        } catch (error) {
            // Another tab holds the pool, or the browser has no OPFS
            pool = null;
        }
    })
    .finally(() => {
        ready = true;
        while (queue.length > 0) handle(queue.shift());
    });
