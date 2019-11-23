# Release checklist

- [ ] Update [`CHANGES.md`](CHANGES.md), [`plugin-gradle/CHANGES.md`](plugin-gradle/CHANGES.md), and [`plugin-maven/CHANGES.md`](plugin-maven/CHANGES.md)
- [ ] Upgrade [`gradle.properties`](gradle.properties).
- [ ] Run `./gradlew autostyleApply`
- [ ] Make sure all files are committed
- [ ] Run `./gradlew check`
- [ ] Make sure all tests pass and no files are changes
- [ ] Run :

```
./gradlew generatePomFileForPluginMavenPublication
./gradlew publish publishPlugins
# export GRGIT_USER=blahblahblah
./gradlew gitPublishPush
```

- [ ] Ensure CI passes
- [ ] Tag the releases
- [ ] Bump `gradle.properties` to next snapshot, run `autostyleApply`, commit any changees
- [ ] Comment on all released PRs / issues
