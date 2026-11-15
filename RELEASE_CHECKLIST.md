# Release Process

page4j is published as one artifact, `com.maxnth:page4j`, with sources and Javadoc. Versions follow
[Semantic Versioning](https://semver.org) and are calculated from Conventional Commit messages.

## Commit types

- `fix:` creates a patch release.
- `feat:` creates a minor release.
- A `!` after the type/scope or a `BREAKING CHANGE:` footer creates a major release.
- Other commit types do not create a release.

## Snapshots

Every push to `main` publishes `<next patch>-SNAPSHOT` (`1.0.0-SNAPSHOT` before the first release)
to `https://central.sonatype.com/repository/maven-snapshots/` through the CI (Snapshots) workflow.
Run that workflow by hand to publish a snapshot of another branch. Snapshots are signed like
releases but skip Central's release validation, and Central deletes them after 90 days.

To test a change in another project before releasing it, depend on the snapshot version and add
the snapshot repository (see the README), or run `./gradlew publishToMavenLocal` and use
`mavenLocal()`.

## Releases

1. The Release workflow builds and tests the project.
2. `python-semantic-release` calculates the next version, updates `gradle.properties` and
   `CHANGELOG.md`, creates the release commit and `v<version>` tag, and creates the GitHub release.
3. The tag starts the CI (Release) workflow, which builds that exact version and publishes it to
   Maven Central.

The Release workflow runs on pushes to `main` only while the repository variable `AUTO_RELEASE`
is `true`. Otherwise run it by hand; it can force a patch, minor or major release, or create a
prerelease.

## First release

1.0.0 is tagged by hand on `main` once the snapshot has been tested:

```bash
git tag -a v1.0.0 -m "v1.0.0"
git push origin v1.0.0
```

Add the release date to the `v1.0.0` entry in `CHANGELOG.md` before tagging, and create the
GitHub release for the tag. Later releases continue from this tag.

## Repository settings

- Secrets: `CENTRAL_USERNAME`, `CENTRAL_PASSWORD` (a Central Portal user token), `GPG_PRIVATE_KEY`,
  `GPG_PASSPHRASE`, and `RELEASE_TOKEN` (allowed to push the release commit and tag to `main`).
- SNAPSHOTs must be enabled for the `com.maxnth` namespace in the Central Portal.
- Variable: `AUTO_RELEASE` set to `true` to release on every push to `main`.
