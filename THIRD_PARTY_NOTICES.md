# Third-party dependencies

The root MIT license covers this project's independently written code, not external dependencies or private reference material.

Dependencies are resolved from Maven Central / Google Maven rather than committed binary copies:

- LuaJava 4.1.0 (Lua 5.3 Java/Android bindings), upstream https://github.com/gudzpoz/luajava. Upstream license bundle: `app/src/main/assets/licenses/luajava.txt`.
- jnigen 3.1.1, upstream https://github.com/libgdx/gdx-jnigen. Upstream license bundle: `app/src/main/assets/licenses/jnigen.txt`.
- JSpecify 1.0.0, upstream https://github.com/jspecify/jspecify; use its upstream license.
- ML Kit Chinese text recognition 16.0.1, provided through Google Maven; use Google's terms for the SDK and bundled models.
- Android Gradle Plugin, Gradle Wrapper and Android tooling retain their upstream licenses. Wrapper scripts include their original notices.

No license is granted here for extracted reference APK scripts, route libraries, recognition dictionaries, game images, or original APKs. These remain outside version control under `private/` and must not be attached to public releases.
