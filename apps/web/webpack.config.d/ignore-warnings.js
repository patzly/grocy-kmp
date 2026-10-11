// Generated Kotlin/Wasm glue code loads Node.js-only modules through a dynamic import.
// Webpack cannot resolve it statically, and the code path never runs in the browser.
config.ignoreWarnings = [
    ...(config.ignoreWarnings || []),
    /Critical dependency: the request of a dependency is an expression/,
];
