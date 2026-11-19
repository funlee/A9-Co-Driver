# Third-party dependencies

The root MIT license covers this project's code and bundled runtime resources. External dependencies retain their own licenses. The project maintainer confirmed authorization to publicly redistribute the bundled runtime resources on 2026-11-07. See `docs/runtime-assets.md` and the acknowledgments in `README.md`.

Dependencies are resolved from Maven Central / Google Maven rather than committed binary copies:

- LuaJava 4.1.0 (Lua 5.3 Java/Android bindings), upstream https://github.com/gudzpoz/luajava. Upstream license bundle: `app/src/main/assets/licenses/luajava.txt`.
- jnigen 3.1.1, upstream https://github.com/libgdx/gdx-jnigen. Upstream license bundle: `app/src/main/assets/licenses/jnigen.txt`.
- JSpecify 1.0.0, upstream https://github.com/jspecify/jspecify; use its upstream license.
- ML Kit Chinese text recognition 16.0.1, provided through Google Maven; use Google's terms for the SDK and bundled models.
- Android Gradle Plugin, Gradle Wrapper and Android tooling retain their upstream licenses. Wrapper scripts include their original notices.

The bundled Lua modules, routes, map rules, digit dictionary and configuration forms are included in the source distribution. Game installation packages, game images, account information, device logs and game trademarks are outside the scope of this license.
